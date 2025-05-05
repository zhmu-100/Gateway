package com.mad.gateway.config

import com.mad.gateway.services.*
import io.ktor.client.*
import io.ktor.server.application.*
import mu.KotlinLogging
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

private val logger = KotlinLogging.logger {}

// Global HttpClient variable
private lateinit var httpClient: HttpClient

fun Application.configureKoin() {
    logger.info { "Starting Koin installation" }

    // Store the Application instance and HttpClient for services
    val app = this
    httpClient = createHttpClient() // Ensure HttpClient is created before Koin starts

    // Install Koin with proper configuration
    install(Koin) {
        slf4jLogger()
        modules(createAppModule(app))
    }

    logger.info { "Koin installation completed" }
}

// Create module with access to the Application instance
fun createAppModule(app: Application) = module {
    // Provide Application instance
    single { app }

    // Provide HttpClient
    single { httpClient }

    // Service clients that need baseUrl from config
    single {
        val baseUrl = app.environment.config.property("services.auth.url").getString()
        AuthServiceClient(get()).apply { this.baseUrl = baseUrl }
    }

    single {
        val baseUrl = app.environment.config.property("services.logging.url").getString()
        LoggingServiceClient(get()).apply { this.baseUrl = baseUrl }
    }

    single {
        val baseUrl = app.environment.config.property("services.profile.url").getString()
        ProfileServiceClient(get()).apply { this.baseUrl = baseUrl }
    }

    single {
        val baseUrl = app.environment.config.property("services.training.url").getString()
        TrainingServiceClient(get()).apply { this.baseUrl = baseUrl }
    }

    single {
        val baseUrl = app.environment.config.property("services.diet.url").getString()
        DietServiceClient(get()).apply { this.baseUrl = baseUrl }
    }

    single {
        val baseUrl = app.environment.config.property("services.feed.url").getString()
        FeedServiceClient(get()).apply { this.baseUrl = baseUrl }
    }

    single {
        val baseUrl = app.environment.config.property("services.notes.url").getString()
        NotesServiceClient(get()).apply { this.baseUrl = baseUrl }
    }

    single {
        val baseUrl = app.environment.config.property("services.statistics.url").getString()
        StatisticsServiceClient(get()).apply { this.baseUrl = baseUrl }
    }

    single {
        val baseUrl = app.environment.config.property("services.db.url").getString()
        DBServiceClient(get()).apply { this.baseUrl = baseUrl }
    }

    // File service client with URL from config
    single {
        val fileServiceUrl = app.environment.config.property("services.file.url").getString()
        FileServiceClient(get(), fileServiceUrl)
    }

    // Message broker
    singleOf(::RedisMessageBroker)

    logger.info { "Dependency injection module configured with all services" }
}
