# MAD Gateway Documentation

Welcome to the MAD Gateway documentation. This documentation provides information about the MAD Gateway project, its architecture, and how to use and extend it.

## Contents

- [KDoc Guide](KDocGuide.md) - Guidelines for writing KDoc documentation
- [Generating Dokka](GeneratingDokka.md) - How to generate and view Dokka documentation

## Project Overview

The MAD Gateway is a microservice gateway application built with Ktor that routes requests to various backend microservices. It serves as a central entry point for the MAD application ecosystem.

## Getting Started

To generate the API documentation for the project:

1. On Windows, run:

   ```bash
   generate-docs.bat
   ```

2. On Unix-based systems (Linux, macOS), run:

   ```bash
   ./generate-docs.sh
   ```

This will generate the documentation and open it in your default browser.

## Documentation Structure

The documentation is organized as follows:

1. **Module Overview** - General information about the MAD Gateway project
2. **Package Documentation** - Documentation for each package in the project
3. **Class Documentation** - Detailed documentation for each class, interface, and object
4. **Function Documentation** - Documentation for functions and properties

## Contributing to Documentation

When contributing to the project, please follow these guidelines:

1. Add KDoc comments to all public classes, functions, and properties
2. Follow the [KDoc Guide](KDocGuide.md) for formatting and content
3. Generate and review documentation before submitting changes
4. Update documentation when making code changes

## Additional Resources

- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Ktor Documentation](https://ktor.io/docs/welcome.html)
- [Dokka GitHub Repository](https://github.com/Kotlin/dokka)
