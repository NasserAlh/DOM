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
 * This class represents a trading strategy called TradesOverlapFix.
 * It is a custom module that implements the CustomModule interface and listens to bar data and orders events.
 * The strategy uses a Simple Moving Average (SMA) indicator to generate buy and sell signals based on the crossover of the close price and the SMA value.
 * It also includes methods for initializing the strategy, stopping the strategy, updating indicators, checking for crossover signals, placing orders, and flattening the position.
 */
@Layer1TradingStrategy
@Layer1SimpleAttachable
@Layer1StrategyName("TradesOverlapFix_200")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class TradesOverlapFix implements CustomModule, BarDataListener, OrdersListener {

    private static final double INITIAL_PREVIOUS_CLOSE = -1.0;
    private static final int SMA_PERIOD = 200;
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
    }

    @Override
    public void stop() {
        Log.info("Stopping the OnBarSMA strategy...");
        flattenPosition();
    }

    @Override
    public void onBar(OrderBook orderBook, Bar bar) {
        double closePrice = bar.getClose();
        updateIndicators(closePrice);
        checkForCrossoverSignals(closePrice);
        updatePreviousValues(closePrice);
    }

    private void updateIndicators(double closePrice) {
        closeIndicator.addPoint(closePrice);
        Double smaValue = sma.calculate(closePrice);
        if (smaValue != null) {
            smaIndicator.addPoint(smaValue);
        }
    }

    /**
     * Checks for crossover signals based on the close price.
     * If there is no open position, it checks if the close price crosses above or below the Simple Moving Average (SMA) value.
     * If a crossover signal is detected, it places a buy or sell order accordingly and updates the current position.
     *
     * @param closePrice The current close price.
     */
    private void checkForCrossoverSignals(double closePrice) {
        Double smaValue = sma.calculate(closePrice);
        if (smaValue != null && previousClose != INITIAL_PREVIOUS_CLOSE && previousSMA != null) {
            // Check if there is no open position before placing a new order
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
     * Updates the previous values with the given close price.
     * 
     * @param closePrice the close price to update the previous values with
     */
    private void updatePreviousValues(double closePrice) {
        previousClose = closePrice;
        previousSMA = sma.calculate(closePrice);
    }

    /**
     * Places an order with the specified parameters.
     *
     * @param isBuy    true if the order is a buy order, false if it is a sell order
     * @param price    the price at which to place the order
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
     * Flattens the current position by placing a market order to buy or sell
     * in order to bring the position back to zero.
     */
    private void flattenPosition() {
        if (currentPosition != 0) {
            boolean isBuy = currentPosition < 0; // If current position is negative, we need to buy to flatten
            int quantity = Math.abs(currentPosition); // Quantity to flatten the position
            placeOrder(isBuy, 0, quantity); // Price set to 0 to indicate market order
            currentPosition = 0; // Reset the current position
        }
    }

    /**
     * This method is called when an order is updated.
     * It checks if the order is fully filled and updates the current position accordingly.
     * If the executed order was a buy and there was a short position, or it was a sell and there was a long position,
     * the current position is reset to indicate no open positions.
     *
     * @param orderInfoUpdate The updated information of the order.
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
