# Build & Deployment Guide - BYE PRO FINANCE

## Build Requirements
- **JDK Version**: Java 17 or Java 21
- **Gradle**: Kotlin DSL
- **Android Gradle Plugin (AGP)**: 9.1.1
- **Kotlin**: 2.2.10

## Environment Secrets
Configure in AI Studio Secrets panel or `.env`:
```properties
GEMINI_API_KEY=your_gemini_api_key_here
```

## Build Commands
```bash
# Compile debug APK
gradle assembleDebug

# Compile release APK
gradle assembleRelease

# Run all local JVM & Robolectric unit tests
gradle :app:testDebugUnitTest
```
