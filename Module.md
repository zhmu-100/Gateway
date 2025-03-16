# Module MAD Gateway

The MAD Gateway is a microservice gateway application built with Ktor that routes requests to various backend microservices. It serves as a central entry point for the MAD application ecosystem.

## Architecture

The gateway follows a clean architecture pattern with the following components:

- **Routes**: Define the API endpoints and handle HTTP requests/responses
- **Services**: Client implementations for communicating with backend microservices
- **Configuration**: Setup for various components like security, serialization, and HTTP clients

Key Features:

- Authentication and authorization via JWT
- Request routing to appropriate microservices
- Error handling and logging
- Metrics collection
- Content negotiation

Package Structure:

- `com.mad.gateway`
  - `.config`: Configuration classes for the application
  - `.routes`: API route definitions
  - `.services`: Service client implementations

API Endpoints:

The gateway exposes the following main API endpoints:

- `/api/auth`: Authentication endpoints (login, register, etc.)
- `/api/profile`: User profile management
- `/api/training`: Training-related endpoints
- `/api/diet`: Diet and nutrition endpoints
- `/api/feed`: Social feed endpoints
- `/api/notes`: User notes endpoints
- `/api/statistics`: User statistics endpoints
- `/api/files`: File upload/download endpoints

Getting Started:

To run the gateway locally:

1. Ensure you have JDK 11+ installed
2. Configure the `application.conf` file with appropriate service URLs
3. Run `./gradlew run` to start the server

## Documentation

This documentation is generated using Dokka, the documentation engine for Kotlin. For more information on how to use and contribute to the documentation:

- See the [KDoc Guide](https://kotlinlang.org/docs/kotlin-doc.html) for writing documentation
- See [Generating Dokka](https://kotlinlang.org/docs/dokka-gradle.html) for generating documentation
- Run the `./gradlew dokkaHtml` command to generate and view the documentation

For a complete overview of the documentation, see the Documentation Index.
