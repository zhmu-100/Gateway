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
                // Service clients with baseUrl from config
                single {
                    val baseUrl =
                        app.environment.config.property("services.auth.url").getString()
                    logger.debug("Starting AuthServiceClient on $baseUrl")
                    AuthServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl =
                        app.environment.config.property("services.logging.url").getString()
                    logger.debug("Starting LoggingServiceClient on $baseUrl")
                    LoggingServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl =
                        app.environment.config.property("services.profile.url").getString()
                    logger.debug("Starting ProfileServiceClient on $baseUrl")
                    ProfileServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl =
                        app.environment.config.property("services.training.url").getString()
                    logger.debug("Starting TrainingServiceClient on $baseUrl")
                    TrainingServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl =
                        app.environment.config.property("services.diet.url").getString()
                    logger.debug("Starting DietServiceClient on $baseUrl")
                    DietServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl =
                        app.environment.config.property("services.feed.url").getString()
                    logger.debug("Starting FeedServiceClient on $baseUrl")
                    FeedServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl =
                        app.environment.config.property("services.notes.url").getString()
                    logger.debug("Starting NotesServiceClient on $baseUrl")
                    NotesServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl =
                        app.environment
                            .config
                            .property("services.statistics.url")
                            .getString()
                    logger.debug("Starting StatisticsServiceClient on $baseUrl")
                    StatisticsServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl = app.environment.config.property("services.db.url").getString()
                    logger.debug("Starting DBServiceClient on $baseUrl")
                    DBServiceClient(get(), baseUrl)
                }

                single {
                    val baseUrl =
                        app.environment.config.property("services.file.url").getString()
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
