# OnBarSMA Class

This class implements a simple moving average (SMA) trading strategy. It listens to bar data and order updates and places orders based on the SMA crossover strategy.

## Class Annotations

- `@Layer1TradingStrategy`: Indicates that this class represents a trading strategy.
- `@Layer1SimpleAttachable`: Allows this class to be attached as a simple layer.
- `@Layer1StrategyName("onBar SMA")`: Sets the name of the strategy to "onBar SMA".
- `@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)`: Specifies that this class uses version 2 of the Layer1 API.

## Class Variables

- `closeIndicator`: An indicator representing the close prices of bars.
- `smaIndicator`: An indicator representing the simple moving average (SMA) values.
- `pips`: The number of pips for the instrument being traded.
- `sma`: An instance of the SMA class used to calculate SMA values.
- `previousClose`: The close price of the previous bar.
- `previousSMA`: The SMA value of the previous bar.
- `api`: An instance of the API class used to interact with the trading platform.
- `alias`: A string representing the alias of the strategy.
- `currentPosition`: An integer representing the current position (long, short, or flat).

## Methods

### `initialize(String alias, InstrumentInfo info, Api api, InitialState initialState)`

Initializes the strategy with the given alias, instrument information, API instance, and initial state. It also registers the close and SMA indicators.

### `stop()`

Logs a message indicating that the strategy is stopping.

### `onBar(OrderBook orderBook, Bar bar)`

Called when a new bar is received. It updates the indicators, checks for crossover signals, and updates the previous values.

### `updateIndicators(double closePrice)`

Updates the close and SMA indicators with the given close price.

### `checkForCrossoverSignals(double closePrice)`

Checks for SMA crossover signals and places orders accordingly.

### `updatePreviousValues(double closePrice)`

Updates the previous close price and SMA value with the given close price.

### `placeOrder(boolean isBuy, double price, int quantity)`

Places an order with the given parameters (buy/sell, price, and quantity).

### `getInterval()`

Returns the interval at which the strategy receives bar data (1 minute).

### `onOrderUpdated(OrderInfoUpdate orderInfoUpdate)`

Logs a message when an order is updated.

### `onOrderExecuted(ExecutionInfo executionInfo)`

Logs a message when an order is executed.

## Constants

- `INITIAL_PREVIOUS_CLOSE`: Initial value for the `previousClose` variable.
- `SMA_PERIOD`: The period used for calculating the SMA.
- `STOP_LOSS_OFFSET`: The offset used for setting the stop loss level.
- `TAKE_PROFIT_OFFSET`: The offset used for setting the take profit level.
