# Generating and Using Dokka Documentation

This guide explains how to generate, view, and maintain the Dokka documentation for the MAD Gateway project.

## What is Dokka?

Dokka is the documentation generation tool for Kotlin, similar to Javadoc for Java. It processes KDoc comments in your code and generates comprehensive HTML documentation.

## Prerequisites

- JDK 11 or higher
- Gradle 7.6 or higher

## Generating Documentation

The project includes a custom Gradle task to generate documentation:

```bash
./gradlew generateDocs
```

This task will:

1. Process all Kotlin source files
2. Extract KDoc comments
3. Generate HTML documentation
4. Output the documentation to `build/dokka`

## Viewing Documentation

After generating the documentation, you can view it by opening `build/dokka/index.html` in your web browser:

```bash
# On Windows
start build\dokka\index.html

# On macOS
open build/dokka/index.html

# On Linux
xdg-open build/dokka/index.html
```

## Documentation Structure

The generated documentation includes:

1. **Module Overview**: General information about the MAD Gateway project
2. **Package List**: All packages in the project
3. **Class Index**: All classes, interfaces, and objects
4. **Member Index**: All functions and properties

## Customizing Documentation

The Dokka configuration is defined in `build.gradle.kts`. You can customize various aspects:

### Module Name

```kotlin
dokkaHtml {
    dokkaSourceSets {
        named("main") {
            moduleName.set("MAD Gateway")
        }
    }
}
```

### Including Additional Documentation

You can include additional Markdown files:

```kotlin
dokkaHtml {
    dokkaSourceSets {
        named("main") {
            includes.from("Module.md", "docs/additional-docs.md")
        }
    }
}
```

### Linking to External Documentation

You can link to external API documentation:

```kotlin
dokkaHtml {
    dokkaSourceSets {
        named("main") {
            externalDocumentationLink {
                url.set(java.net.URL("https://api.ktor.io/"))
                packageListUrl.set(java.net.URL("https://api.ktor.io/package-list"))
            }
        }
    }
}
```

## Best Practices

1. **Generate Documentation Regularly**: Update documentation before releases
2. **Review Documentation**: Check for missing or outdated documentation
3. **Include Examples**: Add code examples to illustrate usage
4. **Link to Source Code**: Consider enabling source links to your repository
5. **Customize for Readability**: Adjust styles and organization as needed

## Troubleshooting

### Missing Documentation

If some elements are not appearing in the documentation:

1. Ensure the element has KDoc comments
2. Check that the element is public (private elements are not documented by default)
3. Verify that the package is not excluded in the Dokka configuration

### Broken Links

If links between elements are broken:

1. Check for typos in class or function references
2. Ensure referenced elements are public
3. Verify that external documentation links have correct URLs

### Build Failures

If the documentation build fails:

1. Check for syntax errors in KDoc comments
2. Ensure all referenced files exist
3. Verify that URLs in external documentation links are valid

## Extending Documentation

### Adding New Sections

To add new sections to the documentation:

1. Create Markdown files in the `docs/` directory
2. Add these files to the `includes.from()` list in the Dokka configuration
3. Reference these sections from your KDoc comments using `@see` tags

### Customizing Output

To customize the documentation output:

1. Modify the Dokka configuration in `build.gradle.kts`
2. Consider adding a custom CSS file for styling
3. Explore Dokka plugins for additional functionality

## Conclusion

Maintaining good documentation is essential for project maintainability. By regularly generating and reviewing Dokka documentation, you ensure that developers have the information they need to work with the MAD Gateway codebase effectively.
