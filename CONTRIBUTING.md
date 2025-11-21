# Contributing to Japtos

Thank you for your interest in contributing to the Japtos SDK! This document provides guidelines and instructions for contributing.

## Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment for all contributors.

## How to Contribute

### Reporting Bugs

If you find a bug, please open an issue on GitHub with:

- A clear, descriptive title
- Steps to reproduce the issue
- Expected behavior
- Actual behavior
- Environment details (Java version, OS, SDK version)
- Relevant code snippets or error messages
- Any additional context that might help

### Suggesting Features

Feature suggestions are welcome! Please open an issue with:

- A clear description of the feature
- Use cases and examples
- Potential implementation approach (if you have ideas)
- Any related issues or discussions

### Pull Requests

1. **Fork the repository** and create a feature branch from `main`
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**
   - Follow the existing code style and conventions
   - Add JavaDoc comments for public APIs
   - Write tests for new functionality
   - Ensure all existing tests pass

3. **Commit your changes**
   - Write clear, descriptive commit messages
   - Reference related issues in commit messages (e.g., "Fixes #123")
   - Keep commits focused and atomic

4. **Run tests**
   ```bash
   mvn test
   ```

5. **Update documentation**
   - Update README.md if adding new features
   - Add JavaDoc for new public APIs
   - Update CHANGELOG.md with your changes

6. **Push and create a Pull Request**
   - Push your branch to your fork
   - Create a PR with a clear description
   - Reference any related issues

## Development Setup

### Prerequisites

- Java 8 or higher (Java 17 recommended)
- Maven 3.6 or higher
- Git

### Building the Project

```bash
# Clone the repository
git clone https://github.com/aptos-labs/japtos.git
cd japtos

# Compile the project
mvn compile

# Run tests
mvn test

# Package the JAR
mvn package
```

### Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=AccountGenerationTests

# Run a specific test method
mvn test -Dtest=AccountGenerationTests#testAccountGeneration
```

## Code Style Guidelines

### Java Code Style

- Follow standard Java naming conventions
- Use 4 spaces for indentation (no tabs)
- Maximum line length: 120 characters
- Use meaningful variable and method names
- Add JavaDoc comments for all public classes and methods

### JavaDoc Format

```java
/**
 * Brief description of the class/method.
 *
 * <p>More detailed description if needed.</p>
 *
 * @param paramName description of parameter
 * @return description of return value
 * @throws ExceptionType when this exception is thrown
 * @since version number
 */
```

### Testing Guidelines

- Write unit tests for all new functionality
- Aim for high test coverage
- Use descriptive test method names (e.g., `testAccountGenerationWithValidKey`)
- Test both success and failure cases
- Mock external dependencies when appropriate

## Project Structure

```
japtos/
├── src/
│   ├── main/java/com/aptoslabs/japtos/
│   │   ├── account/          # Account management
│   │   ├── api/              # Configuration
│   │   ├── bcs/              # BCS serialization
│   │   ├── client/            # HTTP client and DTOs
│   │   ├── core/              # Core blockchain primitives
│   │   ├── gasstation/        # Gas station support
│   │   ├── transaction/       # Transaction handling
│   │   ├── types/             # Type system
│   │   └── utils/             # Utility classes
│   └── test/java/             # Test classes
├── pom.xml                    # Maven configuration
├── README.md                  # Project documentation
└── CONTRIBUTING.md           # This file
```

## Review Process

1. All PRs require at least one approval from a maintainer
2. Maintainers may request changes or ask questions
3. Once approved, a maintainer will merge the PR
4. PRs are typically reviewed within a few business days

## Release Process

Releases are managed by maintainers. When a release is ready:

1. Update version numbers in `pom.xml`
2. Update `CHANGELOG.md` with release notes
3. Create a git tag for the version
4. Publish to Maven Central

## Questions?

If you have questions about contributing, please:

- Open an issue with the `question` label
- Check existing issues and discussions
- Review the README.md for usage examples

Thank you for contributing to Japtos! 🚀

