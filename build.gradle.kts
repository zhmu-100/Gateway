val ktor_version = "2.3.7"
val kotlin_version = "1.9.22"
val logback_version = "1.4.11"
val koin_version = "3.5.0"
val gson_version = "2.10.1"
val micrometer_version = "1.12.0"
val micrometer_prometheus_version = "1.12.0"
val simpleclient_version = "0.16.0"

plugins {
    kotlin("jvm") version "1.9.22"
    id("io.ktor.plugin") version "2.3.7"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jetbrains.dokka") version "1.9.10"
}

group = "com.mad"

version = "0.0.1"

application {
    mainClass.set("com.mad.gateway.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks {
    shadowJar {
        archiveBaseName.set("gateway")
        archiveClassifier.set("")
        archiveVersion.set("${project.version}")
        mergeServiceFiles()
    }

    dokkaHtml {
        outputDirectory.set(file("${buildDir}/dokka"))

        // Configure Dokka
        dokkaSourceSets {
            named("main") {
                moduleName.set("MAD Gateway")
                includes.from("Module.md")

                // Link to Ktor API docs
                externalDocumentationLink {
                    url.set(uri("https://api.ktor.io/").toURL())
                    packageListUrl.set(uri("https://api.ktor.io/package-list").toURL())
                }

                // Link to Kotlin standard library
                externalDocumentationLink {
                    url.set(uri("https://kotlinlang.org/api/latest/jvm/stdlib/").toURL())
                    packageListUrl.set(uri("https://kotlinlang.org/api/latest/jvm/stdlib/package-list").toURL())
                }

                // Source link to GitHub
                sourceLink {
                    localDirectory.set(file("src/main/kotlin"))
                    remoteUrl.set(uri("https://github.com/zhmu-100/Gateway/").toURL())
                    remoteLineSuffix.set("#L")
                }
            }
        }
    }

    // Custom task to generate documentation
    register("generateDocs") {
        dependsOn("dokkaHtml")
        group = "documentation"
        description = "Generates Dokka HTML documentation"

        doLast {
            println("Documentation generated in ${buildDir}/dokka")
            println("Open ${buildDir}/dokka/index.html to view the documentation")
        }
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    // Kotlin standard library
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")

    // Ktor server
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("io.ktor:ktor-server-metrics:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-request-validation:$ktor_version")
    implementation("io.ktor:ktor-server-call-id:$ktor_version")
    implementation("io.ktor:ktor-server-sessions:$ktor_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-http:$ktor_version")
    implementation("io.ktor:ktor-utils:$ktor_version")
    implementation("io.ktor:ktor-io:$ktor_version")

    // Ktor client
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")
    implementation("io.ktor:ktor-client-auth:$ktor_version")

    // Serialization
    implementation("io.ktor:ktor-serialization-gson:$ktor_version")
    implementation("com.google.code.gson:gson:$gson_version")

    // JWT Authentication
    implementation("com.auth0:java-jwt:4.4.0")

    // Dependency Injection
    implementation("io.insert-koin:koin-core:$koin_version")
    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")

    // Redis for message broker
    implementation("redis.clients:jedis:5.0.2")
    implementation("io.lettuce:lettuce-core:6.3.0.RELEASE")

    // Micrometer monitoring
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktor_version")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometer_prometheus_version")
    implementation("io.micrometer:micrometer-core:$micrometer_version")
    implementation("io.micrometer:micrometer-registry-jmx:$micrometer_version")
    implementation("io.prometheus:simpleclient:$simpleclient_version")
    implementation("io.prometheus:simpleclient_common:$simpleclient_version")
    implementation("io.prometheus:simpleclient_hotspot:$simpleclient_version")
    implementation("io.prometheus:simpleclient_pushgateway:$simpleclient_version")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // Testing
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.mockk:mockk:1.13.8")
}
