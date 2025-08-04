# Bookmap Add-on Build Configuration Guide

## Overview
This document outlines the specific build requirements and best practices for developing Bookmap add-ons, which differ from typical standalone Java applications.

## Key Differences from Standalone Java Applications

### 1. No Main Method Required
Bookmap add-ons **do not** use `public static void main(String[] args)`. Instead:
- The add-on lifecycle is managed by Bookmap itself
- Entry points are through interface implementations like `CustomModule`, `DepthDataListener`, etc.
- Bookmap instantiates and manages your classes directly

### 2. No Java Executable Path Needed
Since Bookmap add-ons run within the Bookmap platform:
- No need for Java toolchain configuration
- No standalone executable generation
- The JAR is loaded as a plugin/library by Bookmap

### 3. Build Configuration
The build should focus on creating a proper JAR library that Bookmap can load:

```gradle
plugins {
    id 'java-library'  // Use java-library, not application
}

java {
    // No toolchain configuration needed
    withSourcesJar()    // Optional: for development
    withJavadocJar()    // Optional: for documentation
}
```

## Typical Bookmap Add-on Structure

### Interface Implementations
Your classes implement Bookmap-specific interfaces:
```java
// Example: Depth of Market listener
public class MyDomAddon implements CustomModule, DepthDataListener {
    // No main method needed
    
    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        // Bookmap calls this when loading the add-on
    }
    
    @Override
    public void onDepthUpdate(String alias) {
        // Handle market data updates
    }
}
```

### Common Interface Types
- `CustomModule` - Basic add-on interface
- `DepthDataListener` - Market depth data
- `TradeDataListener` - Trade execution data  
- `BarDataListener` - OHLC bar data
- `TimeListener` - Time-based events
- `OrdersListener` - Order management events
- `CustomSettingsPanelProvider` - UI configuration panels

## Build Process

### 1. Compilation
The build compiles your Java sources and packages them into a JAR:
```bash
gradle build
```

### 2. JAR Generation
Creates the main JAR file:
- `bookmap-addon-1.0.0.jar` - Your add-on code
- Optional: sources and javadoc JARs

### 3. Deployment
The build automatically copies the JAR to Bookmap's add-on directory:
```gradle
task copyJar(type: Copy) {
    dependsOn jar
    from jar.archiveFile
    into '/mnt/c/Bookmap/addons/my_addons/'
}
```

## Dependencies

### Bookmap APIs
Your add-on depends on Bookmap's API libraries:
```gradle
dependencies {
    implementation 'com.bookmap.api:api-core:7.7.0.3'
    implementation 'com.bookmap.api:api-simplified:7.7.0.3'
}
```

### Utility Libraries
Common utility libraries for data processing:
```gradle
dependencies {
    implementation 'org.apache.commons:commons-lang3:3.14.0'
    implementation 'com.google.guava:guava:32.1.2-jre'
    implementation 'com.opencsv:opencsv:5.8'
}
```

## Development Workflow

### 1. Code Development
- Implement required Bookmap interfaces
- Use Bookmap API methods for market data access
- No main method or standalone execution logic

### 2. Build and Test
```bash
gradle clean build
```

### 3. Load in Bookmap
- JAR is automatically copied to Bookmap's add-on directory
- Restart Bookmap or use hot-reload if supported
- Add-on appears in Bookmap's add-on list

## Best Practices

### 1. Interface Implementation
```java
public class MyAddon implements CustomModule, DepthDataListener {
    private Api api;
    
    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        this.api = api;
        // Initialize your add-on logic
    }
}
```

### 2. Resource Management
- Use Bookmap's threading model
- Properly handle market data updates
- Clean up resources in stop() methods

### 3. Error Handling
- Log errors appropriately for Bookmap's logging system
- Handle market data edge cases gracefully
- Don't crash the main Bookmap application

## Troubleshooting

### Java Path Errors
If you see errors about Java executable paths:
- Remove `toolchain` configuration from build.gradle
- Bookmap handles Java runtime, not your build

### ClassPath Issues
- Ensure all dependencies are properly declared
- Check Bookmap API version compatibility
- Verify JAR is in correct add-on directory

### Loading Issues
- Check that classes implement required interfaces
- Verify proper package structure
- Review Bookmap logs for specific errors

## Current Build Configuration

The modernized build.gradle provides:
- ✅ Proper `java-library` plugin usage
- ✅ Version catalog for dependency management  
- ✅ Reproducible JAR naming (no timestamps)
- ✅ Automatic deployment to Bookmap directory
- ✅ Sources and javadoc JAR generation
- ✅ No unnecessary Java toolchain configuration

This configuration is optimized specifically for Bookmap add-on development and follows current Gradle best practices while respecting the unique requirements of the Bookmap platform.