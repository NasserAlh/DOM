# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Bookmap add-on development project focused on Depth of Market (DOM) analysis and trading strategies. The codebase contains multiple Bookmap add-ons that implement various market data analysis techniques, trading indicators, and visualization components.

## Build System & Development Commands

### Core Build Commands
```bash
# Build the add-on and auto-deploy to Bookmap
gradle build

# Clean build
gradle clean build

# Build without tests (if tests exist)
gradle build -x test

# Generate sources and javadoc JARs
gradle sourcesJar javadocJar
```

### Development Workflow
- The build automatically copies the generated JAR to `/mnt/c/Bookmap/addons/my_addons/` via the `copyJar` task
- JAR naming follows the pattern: `bookmap-addon-1.0.0.jar` (reproducible, version-based)
- Uses Gradle version catalog in `gradle/libs.versions.toml` for dependency management

### Dependency Management
- Bookmap API versions are centralized in the version catalog (currently 7.7.0.3)
- Conditional dependency loading supports both standalone and integrated Bookmap builds
- Common utilities: Apache Commons Lang3, Guava, OpenCSV

## Architecture & Code Structure

### Bookmap Add-on Pattern
All add-ons follow the Bookmap plugin architecture:
- **No main() methods** - Bookmap manages the lifecycle
- **Interface-based entry points**: `CustomModule`, `DepthDataListener`, `TradeDataListener`, etc.
- **Annotations for registration**: `@Layer1SimpleAttachable`, `@Layer1StrategyName`, `@Layer1ApiVersion`

### Core Interface Implementations
- `CustomModule`/`CustomModuleAdapter`: Basic add-on lifecycle management
- `DepthDataListener`: Market depth (order book) data processing  
- `TradeDataListener`: Trade execution data processing
- `BarDataListener`: OHLC bar data for indicators
- `TimeListener`: Time-based events and synchronization
- `OrdersListener`: Order management and execution events
- `CustomSettingsPanelProvider`: UI configuration panels

### Key Components by Category

#### Market Depth Analysis (`day1/`, `day3/`, `day8/`, `day9/`)
- **DOM.java**: Basic depth of market visualization with CSV logging
- **MarketDepthAnalyzer.java**: Advanced order book imbalance (OBI) calculations
- **NasserDom.java**: Multi-threaded DOM with volume profile integration and custom UI renderers
- Uses `TreeMap`/`ConcurrentSkipListMap` for price-ordered market data

#### Trading Strategies (`day1/`, `day2/`, `breakthrough_day/`, `restart/`)
- **SMA/EMA-based strategies**: Moving average crossover systems
- **VWAP strategies**: Volume-weighted average price implementations  
- **POC Trading**: Point of Control trading based on volume profile
- Pattern: Initialize indicators → Process bar data → Generate trading signals

#### Volume Analysis (`day7/`, `volume/profile/`)
- **OnTrade.java**: Real-time volume profile construction
- **VolumeProfilePanel.java**: Swing-based volume profile visualization
- Uses `ConcurrentHashMap` for thread-safe volume aggregation

#### UI Components (`day4/`, `day5/`, `day6/`)
- **Custom Swing panels**: Market data visualization with real-time updates
- **Multi-threading**: Separate threads for data processing and UI updates
- **Custom cell renderers**: Specialized table rendering for market data

### Threading & Concurrency Patterns
- **Producer-Consumer**: `BlockingQueue` for data processing pipelines
- **Thread-safe collections**: `ConcurrentSkipListMap`, `ConcurrentHashMap`
- **Lock-based coordination**: `ReentrantLock` for critical sections
- **Swing EDT compliance**: UI updates via `SwingUtilities.invokeLater()`

## Development Guidelines

### Adding New Add-ons
1. Create class in appropriate package (e.g., `day10/`, `strategies/`)
2. Implement required Bookmap interfaces (`CustomModule` + data listeners)
3. Add Bookmap annotations (`@Layer1SimpleAttachable`, `@Layer1StrategyName`, `@Layer1ApiVersion`)
4. Initialize UI components in `initialize()` method
5. Handle data updates in listener methods (`onDepth()`, `onTrade()`, etc.)

### Data Processing Patterns
- **Order Book Management**: Use `TreeMap` with custom comparators for price ordering
- **Time Series Data**: Implement circular buffers or sliding windows for indicators
- **UI Updates**: Batch updates and use proper Swing threading
- **File I/O**: Use try-with-resources for CSV writing and error handling

### Testing & Deployment
- Build generates JAR automatically deployed to Bookmap add-ons directory  
- Load add-on in Bookmap: Indicators → Add-ons → Select your strategy
- Debug via Bookmap logs and add-on console output using `Log.info()`

## Key Dependencies & APIs
- **Bookmap Layer1 API**: Core trading platform integration (`velox.api.layer1.*`)
- **Swing**: UI components and real-time data visualization
- **Java Concurrency**: Thread-safe data structures and processing
- **Apache Commons**: Utility functions for data manipulation
- **Guava**: Enhanced collections and caching utilities

## Important Files
- `BOOKMAP_ADDON_BUILD_GUIDE.md`: Comprehensive build system documentation
- `VERSION_CATALOG_GUIDE.md`: Dependency management with version catalog  
- `BUILD_MODERNIZATION.md`: Build system modernization changelog
- `gradle/libs.versions.toml`: Centralized dependency version management