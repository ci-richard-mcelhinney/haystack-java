# AGENTS.md - Development Guidelines for Haystack Java

This file contains guidelines for agentic coding agents working in this repository.

## Build Commands

### Primary Commands
- `./gradlew build` - Build and test the entire project
- `./gradlew test` - Run all tests
- `./gradlew compileJava` - Compile main source code only
- `./gradlew compileTestJava` - Compile test source code only

### Single Test Commands
- `./gradlew test --tests TestClassName` - Run specific test class
- `./gradlew test --tests TestClassName.methodName` - Run specific test method
- `./gradlew test --tests org.projecthaystack.*Test` - Run tests matching pattern

### Development Commands
- `./gradlew clean` - Clean build artifacts
- `./gradlew jar` - Build JAR only
- `./gradlew war` - Build WAR (for server components)
- `./gradlew javadoc` - Generate Javadoc

## Code Style Guidelines

### Imports
- Place `java.*` imports first, followed by `javax.*`, then `org.projecthaystack.*`
- No wildcard imports (e.g., `import java.util.*;`)
- Group imports by package with blank lines between groups
- Sort imports alphabetically within each group

### Formatting
- Use 2-space indentation (not 4 spaces)
- No trailing whitespace
- Line length should be reasonable (aim for ~80-100 chars)
- Opening braces on same line for methods/classes
- Closing braces on new line
- Use spaces around operators: `if (x == y)` not `if (x==y)`

### Naming Conventions
- Classes: PascalCase (`HClient`, `HNum`)
- Methods: camelCase (`getBool()`, `toZinc()`)
- Constants: UPPER_SNAKE_CASE (`EMPTY`, `ZERO`)
- Package names: lowercase with dots (`org.projecthaystack.client`)
- Variables: camelCase with descriptive names
- Private fields: camelCase, no Hungarian notation

### Type System
- Target Java 7 compatibility (JavaVersion.VERSION_1_7)
- Use abstract base classes for value types (`HVal`, `HDict`)
- Implement `Comparable` for ordered types
- Override `equals()`, `hashCode()`, and `toString()` consistently
- Use `HVal` as base class for all haystack value types

### Error Handling
- Custom exceptions extend `RuntimeException` (e.g., `UnknownNameException`)
- Use checked exceptions for recoverable errors
- Throw `UnknownNameException` for missing tags with checked access
- Use `UnsupportedOperationException` for unimplemented methods
- Include descriptive error messages

### File Organization
- Package declaration at top, immediately after copyright header
- Imports follow package declaration
- Class-level Javadoc comment
- Sections separated with `//////` comment lines
- Inner classes at bottom of file
- Static methods before instance methods where appropriate

### Testing Patterns
- Extend `HaystackTest` for common test utilities
- Use TestNG annotations (`@Test`, `@BeforeTest`, `@AfterTest`)
- Static imports: `import static org.testng.Assert.*;`
- Test methods should be descriptive and follow `verifyXxx()` pattern
- Use `verifyZinc(val, expectedString)` for testing Zinc serialization
- Group related tests in abstract base classes (e.g., `HValTest`)

### Code Structure
- Immutable value objects wherever possible
- Factory methods for common constructions (`HNum.make()`, `HClient.open()`)
- Use `final` for constants and immutable fields
- Package-private constructors with public factory methods
- Static final constants for common values (e.g., `HNum.ZERO`)

### Documentation
- Javadoc comments on all public classes and methods
- Include @see references to Project Haystack documentation
- Use meaningful method names that reduce need for excessive comments
- Copyright header with license and history on all files

### Project-Specific Patterns
- Haystack values implement `toZinc()` and `toJson()` methods
- Dictionary-style access methods: `has()`, `get()`, `missing()`
- Type-specific getters: `getBool()`, `getStr()`, `getRef()`, etc.
- Builder pattern for mutable construction (e.g., `HDictBuilder`)
- Iterator pattern for traversing collections

## Notes
- This is a library implementation of the Project Haystack data model
- The project includes both client and server components
- Zinc format is the primary text representation
- All hayst sack value types are immutable and value-based