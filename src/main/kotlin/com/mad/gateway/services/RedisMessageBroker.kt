package com.mad.gateway.services

import com.mad.gateway.config.GsonProvider
import io.ktor.server.application.*
import java.time.Duration
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.JedisPubSub

private val logger = KotlinLogging.logger {}

/** Redis message broker for inter-service communication */
class RedisMessageBroker(application: Application) : KoinComponent {
    private val redisHost = application.environment.config.property("redis.host").getString()
    private val redisPort =
            application.environment.config.property("redis.port").getString().toInt()
    private val redisPassword =
            application.environment.config.propertyOrNull("redis.password")?.getString()

    private val poolConfig =
            JedisPoolConfig().apply {
                maxTotal = 100
                maxIdle = 30
                minIdle = 10
                testOnBorrow = true
                testOnReturn = true
                testWhileIdle = true
                setMaxWait(Duration.ofSeconds(30))
            }

    private val jedisPool = JedisPool(poolConfig, redisHost, redisPort, 2000, redisPassword)

    private val subscribers = mutableMapOf<String, MutableList<(String) -> Unit>>()
    private val pubSubThreads = mutableMapOf<String, Thread>()

    init {
        logger.info { "Initializing Redis message broker at $redisHost:$redisPort" }
    }

    /** Publish a message to a channel */
    suspend fun <T> publish(channel: String, message: T) {
        withContext(Dispatchers.IO) {
            jedisPool.resource.use { jedis ->
                val jsonMessage = GsonProvider.gson.toJson(message)
                jedis.publish(channel, jsonMessage)
                logger.debug { "Published message to channel $channel: $jsonMessage" }
            }
        }
    }

    /** Subscribe to a channel */
    fun <T> subscribe(channel: String, callback: (T) -> Unit, clazz: Class<T>) {
        if (!subscribers.containsKey(channel)) {
            subscribers[channel] = mutableListOf()

            // Create a new thread for this subscription
            val pubSubThread =
                    thread(start = true, name = "redis-pubsub-$channel") {
                        val jedis = Jedis(redisHost, redisPort)
                        if (redisPassword != null) {
                            jedis.auth(redisPassword)
                        }

                        try {
                            jedis.subscribe(
                                    object : JedisPubSub() {
                                        override fun onMessage(channel: String, message: String) {
                                            try {
                                                logger.debug {
                                                    "Received message from channel $channel: $message"
                                                }
                                                subscribers[channel]?.forEach { it(message) }
                                            } catch (e: Exception) {
                                                logger.error(e) {
                                                    "Error processing message from channel $channel"
                                                }
                                            }
                                        }
                                    },
                                    channel
                            )
                        } catch (e: Exception) {
                            logger.error(e) { "Error in Redis subscription for channel $channel" }
                        } finally {
                            jedis.close()
                        }
                    }

            pubSubThreads[channel] = pubSubThread
        }

        // Add the callback to the list of subscribers for this channel
        subscribers[channel]?.add { jsonMessage ->
            try {
                val typedMessage = GsonProvider.gson.fromJson(jsonMessage, clazz)
                callback(typedMessage)
            } catch (e: Exception) {
                logger.error(e) { "Error deserializing message from channel $channel" }
            }
        }

        logger.info { "Subscribed to channel $channel" }
    }

    /** Unsubscribe from a channel */
    fun unsubscribe(channel: String) {
        subscribers.remove(channel)
        pubSubThreads[channel]?.interrupt()
        pubSubThreads.remove(channel)
        logger.info { "Unsubscribed from channel $channel" }
    }

    /** Close the Redis connection pool */
    fun close() {
        pubSubThreads.values.forEach { it.interrupt() }
        jedisPool.close()
        logger.info { "Closed Redis message broker" }
    }
}
