package breakthrough_day;

import day1.SMA;
import velox.api.layer1.annotations.*;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.*;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.*;

import java.awt.*;

/**
 * This class represents a trading strategy called "TradesOverlapFixInitialState".
 * It implements the CustomModule, BarDataListener, and OrdersListener interfaces.
 * The strategy uses a Simple Moving Average (SMA) indicator to generate buy and sell signals.
 * It also includes functionality to initialize the strategy, stop the strategy, and handle order updates.
 */
@Layer1TradingStrategy
@Layer1SimpleAttachable
@Layer1StrategyName("TradesOverlapFix initialState 1")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class TradesOverlapFixInitialState implements CustomModule, BarDataListener, OrdersListener {

    private static final double INITIAL_PREVIOUS_CLOSE = -1.0;
    private static final int SMA_PERIOD = 50;
    private static final int STOP_LOSS_OFFSET = 10;
    private static final int TAKE_PROFIT_OFFSET = 20;
    private Indicator closeIndicator;
    private Indicator smaIndicator;
    private double pips;
    private SMA sma;
    private double previousClose = INITIAL_PREVIOUS_CLOSE;
    private Double previousSMA = null;
    private Api api;
    private String alias;
    private int currentPosition = 0;
    private InitialState initialState; // Class member to store the initial state

    /**
     * Initializes the OnBarSMA strategy.
     * 
     * @param alias         the alias of the strategy
     * @param info          the instrument information
     * @param api           the API instance
     * @param initialState  the initial state
     */
    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        Log.info("Initializing the OnBarSMA strategy...");

        closeIndicator = api.registerIndicator("Close", GraphType.PRIMARY);
        closeIndicator.setColor(Color.MAGENTA);
        smaIndicator = api.registerIndicator("SMA", GraphType.PRIMARY);
        smaIndicator.setColor(Color.BLUE);
        pips = info.pips;
        this.alias = alias;
        this.api = api;
        sma = new SMA(SMA_PERIOD);

        this.initialState = initialState; // Set the initial state
    }

    /**
     * Stops the OnBarSMA strategy.
     * This method is called when the strategy needs to be stopped.
     * It logs a message and flattens the position using the stored initial state.
     */
    @Override
    public void stop() {
        Log.info("Stopping the OnBarSMA strategy...");
        flattenPosition(initialState); // Use the stored initial state
    }

    /**
     * This method is called for each bar in the order book.
     * It calculates the simple moving average (SMA) once per bar,
     * updates the indicators, checks for crossover signals,
     * and updates the previous values with the current close price and SMA value.
     *
     * @param orderBook The order book containing the bar.
     * @param bar The current bar.
     */
    @Override
    public void onBar(OrderBook orderBook, Bar bar) {
        double closePrice = bar.getClose();
        Double smaValue = sma.calculate(closePrice); // Calculate SMA once per bar
        updateIndicators(closePrice, smaValue); // Pass the SMA value to updateIndicators
        checkForCrossoverSignals(closePrice, smaValue); // Pass the SMA value to checkForCrossoverSignals
        updatePreviousValues(closePrice, smaValue); // Update previous values with the current close price and SMA value
    }
    
    /**
     * Updates the indicators with the given close price and SMA value (if not null).
     *
     * @param closePrice the close price to be added to the close indicator
     * @param smaValue the SMA value to be added to the SMA indicator (can be null)
     */
    private void updateIndicators(double closePrice, Double smaValue) {
        closeIndicator.addPoint(closePrice);
        if (smaValue != null) {
            smaIndicator.addPoint(smaValue);
        }
    }
    
    /**
     * Checks for crossover signals and places buy or sell orders accordingly.
     * 
     * @param closePrice The current closing price.
     * @param smaValue The current Simple Moving Average (SMA) value.
     */
    private void checkForCrossoverSignals(double closePrice, Double smaValue) {
        if (smaValue != null && previousClose != INITIAL_PREVIOUS_CLOSE && previousSMA != null) {
            if (currentPosition == 0) { 
                if (closePrice > smaValue && previousClose <= previousSMA) {
                    Log.info("Buy Signal at " + closePrice * pips);
                    placeOrder(true, closePrice, 1); // Place a buy order
                    currentPosition = 1; // Update the current position to long
                } else if (closePrice < smaValue && previousClose >= previousSMA) {
                    Log.info("Sell Signal at " + closePrice * pips);
                    placeOrder(false, closePrice, 1); // Place a sell order
                    currentPosition = -1; // Update the current position to short
                }
            }
        }
    }

    /**
     * Updates the previous close price and SMA value.
     *
     * @param closePrice The new close price.
     * @param smaValue The new SMA value.
     */
    private void updatePreviousValues(double closePrice, Double smaValue) {
        previousClose = closePrice;
        previousSMA = smaValue;
    }

    /**
     * Places an order with the specified parameters.
     *
     * @param isBuy    true if the order is a buy order, false if it is a sell order
     * @param price    the price at which the order should be placed
     * @param quantity the quantity of the order
     */
    private void placeOrder(boolean isBuy, double price, int quantity) {
        try {
            SimpleOrderSendParametersBuilder builder = new SimpleOrderSendParametersBuilder(alias, isBuy, quantity);
            builder.setDuration(OrderDuration.IOC);
            builder.setStopLossOffset(STOP_LOSS_OFFSET);
            builder.setTakeProfitOffset(TAKE_PROFIT_OFFSET);
            SimpleOrderSendParameters order = builder.build();
            api.sendOrder(order);
        } catch (Exception e) {
            Log.info("Error placing order", e);
        }
    }

    /**
     * Flattens the current position by sending an order to the API.
     * If the current position is not zero, it determines the order side needed to flatten the position,
     * gets the last trade price from the InitialState, creates an order builder with the necessary parameters,
     * builds the order parameters, and sends the order to the API.
     * Finally, it resets the current position to zero.
     *
     * @param initialState the initial state object containing the last trade price
     */
    private void flattenPosition(InitialState initialState) {
        if (currentPosition != 0) {
            boolean isBuy = currentPosition < 0; 
            double lastTradePrice = initialState.getLastTradePrice(); 
    
            SimpleOrderSendParametersBuilder builder = new SimpleOrderSendParametersBuilder(alias, isBuy, 1) 
                    .setDuration(OrderDuration.GTC) 
                    .setLimitPrice(lastTradePrice); 
            SimpleOrderSendParameters orderParameters = builder.build();
    
            api.sendOrder(orderParameters);
    
            currentPosition = 0; 
        }
    }
    
    /**
     * Called when an order is updated.
     * 
     * @param orderInfoUpdate The updated order information.
     */
    @Override
    public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
    // Check if the order is fully filled
    if (orderInfoUpdate.filled > 0 && orderInfoUpdate.unfilled == 0) {
            // If the executed order was a buy, and we had a short position, or it was a sell and we had a long position
            if ((currentPosition == -1 && orderInfoUpdate.isBuy) || (currentPosition == 1 && !orderInfoUpdate.isBuy)) {
                currentPosition = 0; // Reset the current position indicating no open positions
            }
        }
    }

    @Override
    public long getInterval() {
        return Intervals.INTERVAL_1_SECOND;
    }

    @Override
    public void onOrderExecuted(ExecutionInfo arg0) {}
}
