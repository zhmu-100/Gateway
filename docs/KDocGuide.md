# KDoc Documentation Guide for MAD Gateway

This guide provides standards and best practices for writing KDoc documentation in the MAD Gateway project.

## What is KDoc?

KDoc is Kotlin's documentation generation tool, similar to JavaDoc for Java. It uses a markup syntax that's processed by Dokka to generate HTML documentation.

## Basic KDoc Format

KDoc comments start with `/**` and end with `*/`:

```kotlin
/**
 * This is a KDoc comment.
 */
fun example() {
    // Function implementation
}
```

## Documentation Standards

### Modules and Packages

- **Module Documentation**: Provide a high-level overview in the `Module.md` file
- **Package Documentation**: Add package-level documentation in a `package-info.kt` file

### Classes and Interfaces

Document every class and interface with:

1. A brief description of its purpose
2. Any important implementation details
3. Usage examples if applicable

```kotlin
/**
 * Client for the Auth service.
 * 
 * This client handles communication with the Auth microservice, which manages
 * user authentication, registration, and token management.
 * 
 * @property client The HTTP client used for making requests
 */
class AuthServiceClient(client: HttpClient) : ServiceClient(client), KoinComponent {
    // Implementation
}
```

### Functions and Properties

Document functions with:

1. A brief description of what the function does
2. Parameter descriptions using `@param`
3. Return value description using `@return`
4. Exception information using `@throws` if applicable

```kotlin
/**
 * Authenticates a user with the provided credentials.
 * 
 * @param username The user's username
 * @param password The user's password
 * @return A token response containing access and refresh tokens
 * @throws ServiceException If authentication fails
 */
suspend fun login(username: String, password: String): TokenResponse {
    // Implementation
}
```

### Route Functions

For route functions, include:

1. A description of the route's purpose
2. The HTTP method and path
3. Request and response details
4. Authentication requirements

```kotlin
/**
 * Login endpoint.
 * 
 * POST /api/auth/login
 * 
 * Authenticates a user with username and password, returning JWT tokens on success.
 */
post("/login") {
    // Implementation
}
```

### Data Classes

Document data classes and their properties:

```kotlin
/**
 * Request data for user login.
 * 
 * @property username The user's username
 * @property password The user's password
 */
data class LoginRequest(val username: String, val password: String)
```

## Markdown Features

KDoc supports Markdown syntax:

- **Bold**: `**bold text**`
- **Italic**: `*italic text*`
- **Code**: `` `code` ``
- **Lists**:

  ```
  * Item 1
  * Item 2
    * Subitem
  ```

- **Links**: `[link text](URL)`
- **Code blocks**:

  ```
  ```kotlin
  fun example() {
      // Code here
  }
  ```

  ```

## Section Tags

Use section tags to organize documentation:

- `@param name description` - Documents a parameter
- `@return description` - Documents the return value
- `@throws class description` - Documents an exception that may be thrown
- `@see reference` - Adds a "See Also" section with links to other elements
- `@sample identifier` - Includes a code sample
- `@property name description` - Documents a property of a class
- `@constructor description` - Documents a class constructor
- `@receiver description` - Documents an extension function's receiver

## Best Practices

1. **Be Concise**: Write clear, concise documentation
2. **Use Complete Sentences**: Start with a capital letter, end with a period
3. **Document Public API**: Always document public classes, functions, and properties
4. **Update Documentation**: Keep documentation in sync with code changes
5. **Use Code Examples**: Provide examples for complex functionality
6. **Link Related Items**: Use `@see` to link related classes or functions
7. **Document Exceptions**: Always document exceptions that may be thrown

## Example

Here's a complete example of well-documented code:

```kotlin
/**
 * Client for the Auth service.
 *
 * This client handles communication with the Auth microservice, which manages
 * user authentication, registration, and token management.
 *
 * @property client The HTTP client used for making requests
 * @see TokenResponse
 */
class AuthServiceClient(client: HttpClient) : ServiceClient(client), KoinComponent {
    /**
     * The base URL for the Auth service.
     *
     * Retrieved from the application configuration.
     */
    override val baseUrl: String =
            application.environment.config.property("services.auth.url").getString()

    /**
     * Authenticates a user with the provided credentials.
     *
     * @param username The user's username
     * @param password The user's password
     * @return A token response containing access and refresh tokens
     * @throws ServiceException If authentication fails
     */
    suspend fun login(username: String, password: String): TokenResponse {
        logger.info { "Authenticating user: $username" }
        val request = LoginRequest(username, password)
        return post("/auth/login", request)
    }
}
