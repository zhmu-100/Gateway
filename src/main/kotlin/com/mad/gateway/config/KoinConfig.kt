package com.mad.gateway.config

import com.mad.gateway.services.*
import io.ktor.server.application.*
import mu.KotlinLogging
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

private val logger = KotlinLogging.logger {}

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(appModule(this@configureKoin))
    }
}

fun appModule(application: Application) = module {
    // Application instance
    single { application }

    // Service clients
    single { AuthServiceClient(get()) }
    single { LoggingServiceClient(get()) }
    single { ProfileServiceClient(get()) }
    single { TrainingServiceClient(get()) }
    single { DietServiceClient(get()) }
    single { FeedServiceClient(get()) }
    single { NotesServiceClient(get()) }
    single { StatisticsServiceClient(get()) }
    single { DBServiceClient(get()) }

    // File service client with URL from config
    single {
        val fileServiceUrl =
                application.environment.config.property("services.file.url").getString()
        FileServiceClient(get(), fileServiceUrl)
    }

    // Message broker
    single { RedisMessageBroker(get()) }

    // HTTP client
    single { createHttpClient() }

    logger.info { "Dependency injection configured with all services" }
}
