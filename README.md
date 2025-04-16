# MAD Gateway Service

A Kotlin/Ktor-based API Gateway for the MAD (Mobile Athletic Development) microservices architecture. This gateway serves as the entry point for mobile clients to communicate with the backend microservices.

## Architecture

The MAD Gateway connects mobile clients to the following microservices:

1. **Auth Service** - Authentication and authorization using Keycloak
2. **Logging Service** - Centralized logging with a rollbar-like architecture
3. **Profile Service** - User profiles and social connections
4. **Training Service** - Workouts, exercises, and training plans
5. **Diet Service** - Meals, nutrition, and vitamin recommendations
6. **Feed Service** - Social feed with posts, comments, and reactions
7. **Notes Service** - User notes and notifications
8. **Statistics Service** - User statistics and workout data (GPS, heart rate, etc.)
9. **File Service** - File storage using MinIO
10. **DB Service** - PostgreSQL database service for direct database operations
11. **Redis Message Broker** - Inter-service communication

## Features

- **REST API Gateway** - Exposes RESTful endpoints for mobile clients
- **Authentication** - JWT-based authentication with Keycloak
- **Request Routing** - Routes requests to appropriate microservices
- **Data Transformation** - Transforms data between client and microservices
- **Logging** - Comprehensive logging for monitoring and debugging
- **Metrics** - Prometheus metrics for monitoring
- **CORS** - Cross-Origin Resource Sharing support
- **Content Negotiation** - JSON serialization/deserialization with GSON
- **Dependency Injection** - Koin for dependency injection

## Getting Started

### Prerequisites

- JDK 17 or higher
- Gradle 7.6 or higher
- Docker and Docker Compose (for running with microservices)

### Running Locally

1. Clone the repository:

   ```bash
   git clone git@github.com:zhmu-100/Gateway.git
   cd Gateway
   ```

2. Build the project:

   ```bash
   ./gradlew build
   ```

3. Run the application:

   ```bash
   ./gradlew run
   ```

The gateway will be available at <http://localhost:8080>.

### Running with Docker

1. Build and run using Docker Compose:

   ```bash
   docker-compose up -d
   ```

This will start the gateway and all required services in containers.

## Configuration

Configuration is managed through the `application.conf` file and environment variables:

- **Port**: `PORT` (default: 8080)
- **JWT**: `JWT_SECRET`, `JWT_ISSUER`, `JWT_AUDIENCE`
- **Service URLs**: Environment variables for each service URL (auth, profile, training, diet, feed, notes, statistics, file, db)
- **Redis**: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`

## API Documentation

The gateway exposes the following API endpoints:

### Authentication

- `POST /api/auth/login` - Login with username and password
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/refresh` - Refresh an access token
- `POST /api/auth/logout` - Logout a user
- `GET /api/auth/validate` - Validate a token

### Profiles

- `GET /api/profiles/me` - Get authenticated user's profile
- `GET /api/profiles/{id}` - Get profile by ID
- `GET /api/profiles` - List profiles
- `POST /api/profiles` - Create a profile
- `PUT /api/profiles/{id}` - Update a profile
- `DELETE /api/profiles/{id}` - Delete a profile
- `POST /api/profiles/{id}/follow` - Follow a user
- `POST /api/profiles/{id}/unfollow` - Unfollow a user
- `GET /api/profiles/{id}/followers` - List followers
- `GET /api/profiles/{id}/following` - List following

### Training

- `GET /api/training/workouts/{id}` - Get workout by ID
- `GET /api/training/workouts` - List workouts
- `GET /api/training/workouts/{id}/exercises` - Get exercises for a workout
- `POST /api/training/workouts` - Create a workout
- `PUT /api/training/workouts/{id}` - Update a workout
- `DELETE /api/training/workouts/{id}` - Delete a workout
- `POST /api/training/workouts/custom` - Create a custom workout

### Diet

- `GET /api/diet/foods/{id}` - Get food by ID
- `GET /api/diet/foods` - List foods
- `POST /api/diet/foods` - Create a food
- `GET /api/diet/meals/{id}` - Get meal by ID
- `GET /api/diet/meals` - List meals
- `POST /api/diet/meals` - Create a meal

### Feed

- `GET /api/feed/posts/{id}` - Get post by ID
- `GET /api/feed/posts` - List feed posts
- `POST /api/feed/posts` - Create a post
- `GET /api/feed/posts/{postId}/comments` - List comments for a post
- `POST /api/feed/posts/{postId}/comments` - Create a comment
- `POST /api/feed/posts/{postId}/reactions` - Add a reaction
- `DELETE /api/feed/posts/{postId}/reactions` - Remove a reaction
- `GET /api/feed/users/{userId}/posts` - List posts for a user

### Notes

- `GET /api/notebook/notes/{id}` - Get note by ID
- `GET /api/notebook/notes` - List notes
- `POST /api/notebook/notes` - Create a note
- `PUT /api/notebook/notes/{id}` - Update a note
- `DELETE /api/notebook/notes/{id}` - Delete a note
- `GET /api/notebook/notifications/{id}` - Get notification by ID
- `GET /api/notebook/notifications` - List notifications
- `POST /api/notebook/notifications` - Create a notification
- `POST /api/notebook/notifications/{id}/actions` - Perform action on a notification

### Statistics

- `GET /api/statistics/gps` - Get GPS data
- `POST /api/statistics/gps` - Upload GPS data
- `GET /api/statistics/heartrate` - Get heart rate data
- `POST /api/statistics/heartrate` - Upload heart rate data
- `GET /api/statistics/calories` - Get calories data
- `POST /api/statistics/calories` - Upload calories data

### Files

- `GET /api/files/{id}` - Get file by ID
- `GET /api/files/{id}/stream` - Stream file by ID
- `GET /api/files/{id}/url` - Get file URL
- `POST /api/files/upload` - Upload a file
- `POST /api/files/fix-upload/{id}` - Replace an existing file

### Database

- `POST /api/db/create` - Create a new record in a specified table
- `POST /api/db/read` - Execute a custom SQL query with parameters
- `POST /api/db/update` - Update records in a specified table with conditions
- `POST /api/db/delete` - Delete records from a specified table with conditions

## Development

### Project Structure

- `src/main/kotlin/com/mad/gateway/`
  - `Application.kt` - Main application entry point
  - `config/` - Configuration classes
  - `services/` - Service clients for microservices
  - `routes/` - API route definitions
- `src/main/resources/`
  - `application.conf` - Application configuration
  - `logback.xml` - Logging configuration

### Documentation

The project uses Dokka to generate API documentation. To generate the documentation:

```bash
./gradlew generateDocs
```

This will create HTML documentation in the `build/dokka` directory. Open `build/dokka/index.html` in a browser to view the documentation.

The documentation includes:

- Module overview
- Package structure
- Class and function documentation
- API endpoints

### Adding a New Service

1. Create a new service client in `services/`
2. Add the service URL to `application.conf`
3. Create route definitions in `routes/`
4. Register the service client in `KoinConfig.kt`
5. Add KDoc documentation to your new classes and functions
