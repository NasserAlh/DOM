# Market Depth Analyzer

## Overview
The `MarketDepthAnalyzer` is a Java class designed to analyze market depth data in real-time. It implements the `CustomModuleAdapter`, `DepthDataListener`, and `TimeListener` interfaces, which allow it to interact with the Bookmap API and process market depth and time updates.

## Annotations
- `@Layer1SimpleAttachable`: Indicates that this class can be attached as a simple layer 1 module in the Bookmap platform.
- `@Layer1StrategyName("onDepth Indicator")`: Assigns a name to the strategy, which will be displayed on the Bookmap platform.
- `@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)`: Specifies the version of the Layer 1 API that this class is compatible with.

## Class Structure

### Attributes
- `bids` and `asks`: TreeMaps to store bid and ask orders respectively, sorted by price.
- `formattedTimestamp`: A string to store the formatted timestamp of the current market data update.
- `isHeaderWritten`: A boolean flag to indicate whether the header has been written to the CSV file.
- `dateFormat`: A SimpleDateFormat object to format timestamps.

### Constructor
- `MarketDepthAnalyzer()`: Initializes the `dateFormat` object and sets the time zone to EDT (UTC-4).

### Methods

#### `initialize(String alias, InstrumentInfo info, Api api, InitialState initialState)`
- Initializes the file with headers by calling the `demoBestPriceSize` method.

#### `onDepth(boolean isBid, int price, int size)`
- Updates the order book based on new depth data received and logs the sum of the top bid or ask levels and calculates the Order Book Imbalance (OBI).

#### `demoBestPriceSize(VolumePair topLevelsSum)`
- Writes the best bid and ask prices and sizes to a CSV file and calculates the OBI.

#### `onTimestamp(long nanoseconds)`
- Updates the `formattedTimestamp` attribute based on the new timestamp received.

#### `VolumePair(int bidVolume, int askVolume)`
- A record class to hold the volume of the top bid and ask levels.

#### `demoSumOfPriceLevels(int numLevelsToSum)`
- Calculates the sum of the top bid and ask levels and returns a `VolumePair` object holding this data.

## Error Handling
The class includes error handling to manage issues that may occur during file writing operations.

## Logging
Logging is implemented to provide information on the sum of top bid or ask levels and the calculated OBI.

## Usage
This class is intended to be used as a module in the Bookmap platform to analyze market depth data for trading strategies.

## Recommendations
- Ensure that the file path specified in the `demoBestPriceSize` method is correct and accessible.
- Consider adding more comprehensive error handling and logging features to track and manage potential issues more effectively.

## Questions
- How might you extend this class to include more complex trading strategies based on market depth data?
- What improvements can be made to optimize the performance of the `demoSumOfPriceLevels` method?

## Conclusion
The `MarketDepthAnalyzer` class is a robust tool for analyzing market depth data in real-time, providing valuable insights for trading strategies.

