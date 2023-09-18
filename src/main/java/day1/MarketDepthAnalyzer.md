# MarketDepthAnalyzer

## Overview

The `MarketDepthAnalyzer` is a robust Java class designed to monitor and analyze real-time market data, particularly focusing on the depth of market data and order book imbalances. It is a part of a larger trading strategy algorithm project, which aims to provide insights into the market's current state, facilitating informed trading decisions.

## Functionality

The `MarketDepthAnalyzer` class performs the following key functions:

1. **Market Data Monitoring**: It continuously monitors market data updates, specifically tracking changes in bid and ask prices along with their respective sizes.

2. **Order Book Imbalance (OBI) Calculation**: The class calculates the OBI, a crucial metric that gives insights into market direction, by analyzing the top levels of the order book. The OBI is calculated using the formula:
                                   
     `OBI = (BidVolume - AskVolume) / (BidVolume + AskVolume)`

3. **Data Logging**: It logs important data points such as the sum of top bid and ask levels and the OBI into the console for real-time analysis.

4. **Data Recording**: The class also records the best bid and ask prices along with their sizes into a CSV file, facilitating data analysis over time.

## Code Structure

The `MarketDepthAnalyzer` class implements the `CustomModuleAdapter`, `DepthDataListener`, and `TimeListener` interfaces, and contains the following key components:

- **Data Structures**: Utilizes `TreeMap` data structures to efficiently store and manage bid and ask data.
- **Time Handling**: Handles timestamp data effectively, converting nanoseconds to a readable string format.
- **Error Handling**: Implements error handling mechanisms to manage potential IO exceptions during file operations.
- **File Operations**: Writes data to a CSV file, storing vital market data for further analysis.

## Usage

To use the `MarketDepthAnalyzer` class, integrate it into your existing trading strategy algorithm project. Ensure to set up the necessary environment, including the required libraries and APIs, to facilitate seamless data retrieval and analysis.

## Conclusion

The `MarketDepthAnalyzer` class serves as a powerful tool in the arsenal of any trader looking to leverage algorithmic trading strategies. By providing real-time insights into market dynamics, it aids in making informed trading decisions, potentially leading to more profitable outcomes.
