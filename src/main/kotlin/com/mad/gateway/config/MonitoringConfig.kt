package com.mad.gateway.config

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.jvm.*
import io.micrometer.core.instrument.binder.system.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import java.util.UUID
import org.slf4j.event.Level

/** Configure metrics and monitoring for the application */
fun Application.configureMonitoring() {
    // Configure call logging
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
        callIdMdc("call-id")
    }

    // Configure call ID generation
    install(CallId) {
        header(HttpHeaders.XRequestId)
        verify { callId -> callId.isNotEmpty() }
        generate { UUID.randomUUID().toString() }
    }

    // Configure Micrometer metrics with Prometheus
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) {
        registry = appMicrometerRegistry

        // Add various metrics collectors
        meterBinders = listOf(JvmMemoryMetrics(), JvmGcMetrics(), ProcessorMetrics())
    }

    // Expose the Prometheus metrics endpoint
    routing { get("/metrics") { call.respondText(appMicrometerRegistry.scrape()) } }
}
