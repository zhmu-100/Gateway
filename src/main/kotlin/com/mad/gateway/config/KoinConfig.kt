package com.mad.gateway.config

import com.mad.gateway.services.*
import io.ktor.client.*
import io.ktor.server.application.*
import mu.KotlinLogging
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

private val logger = KotlinLogging.logger {}

// Global HttpClient instance
private lateinit var httpClient: HttpClient

fun Application.configureKoin() {
    logger.info { "Starting Koin installation" }

    // Create HttpClient first
    httpClient = createHttpClient()

    // Create Application reference for the module
    val app = this

    install(Koin) {
        slf4jLogger()
        modules(
            module {
                // Provide the HttpClient
                single { httpClient }

                // Provide the Application
                single { app }

                // Basic services
                // Service clients with baseUrl from environment variables
                single {
                    val baseUrl = System.getenv("AUTH_SERVICE_URL")
                    logger.debug("Starting AuthServiceClient on $baseUrl")
                    AuthServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl = System.getenv("PROFILE_SERVICE_URL")
                    logger.debug("Starting ProfileServiceClient on $baseUrl")
                    ProfileServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl = System.getenv("TRAINING_SERVICE_URL")
                    logger.debug("Starting TrainingServiceClient on $baseUrl")
                    TrainingServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl = System.getenv("DIET_SERVICE_URL")
                    logger.debug("Starting DietServiceClient on $baseUrl")
                    DietServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl = System.getenv("FEED_SERVICE_URL")
                    logger.debug("Starting FeedServiceClient on $baseUrl")
                    FeedServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl = System.getenv("NOTES_SERVICE_URL")
                    logger.debug("Starting NotesServiceClient on $baseUrl")
                    NotesServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl = System.getenv("STATISTICS_SERVICE_URL")
                    logger.debug("Starting StatisticsServiceClient on $baseUrl")
                    StatisticsServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl = System.getenv("DB_SERVICE_URL")
                    logger.debug("Starting DBServiceClient on $baseUrl")
                    DBServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl = System.getenv("FILE_SERVICE_URL")
                    logger.debug("Starting FileServiceClient on $baseUrl")
                    FileServiceClient(get(), baseUrl)
                }

                // Message broker
                single { RedisMessageBroker(get()) }
            }
        )
    }

    logger.info { "Koin installation completed" }
}
