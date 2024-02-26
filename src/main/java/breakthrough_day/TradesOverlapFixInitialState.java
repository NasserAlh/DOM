package breakthrough_day;


import day1.SMA;
import velox.api.layer1.annotations.*;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.*;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.*;

import java.awt.*;

@Layer1TradingStrategy
@Layer1SimpleAttachable
@Layer1StrategyName("TradesOverlapFix initialState")
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

    @Override
    public void stop() {
        Log.info("Stopping the OnBarSMA strategy...");
        flattenPosition(initialState); // Use the stored initial state
    }

    @Override
    public void onBar(OrderBook orderBook, Bar bar) {
        double closePrice = bar.getClose();
        Double smaValue = sma.calculate(closePrice); // Calculate SMA once per bar
        updateIndicators(closePrice, smaValue); // Pass the SMA value to updateIndicators
        checkForCrossoverSignals(closePrice, smaValue); // Pass the SMA value to checkForCrossoverSignals
        updatePreviousValues(closePrice, smaValue); // Update previous values with the current close price and SMA value
    }
    
    private void updateIndicators(double closePrice, Double smaValue) {
        closeIndicator.addPoint(closePrice);
        if (smaValue != null) {
            smaIndicator.addPoint(smaValue);
        }
    }
    
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
    
    private void updatePreviousValues(double closePrice, Double smaValue) {
        previousClose = closePrice;
        previousSMA = smaValue;
    }

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

    private void flattenPosition(InitialState initialState) {
        if (currentPosition != 0) {
            boolean isBuy = currentPosition < 0; // Determine the order side needed to flatten the position
            double lastTradePrice = initialState.getLastTradePrice(); // Get the last trade price from InitialState
    
            // Create an order builder with the necessary parameters
            SimpleOrderSendParametersBuilder builder = new SimpleOrderSendParametersBuilder(alias, isBuy, 1) // Always trading 1 contract
                    .setDuration(OrderDuration.GTC) // Use 'Good Till Cancel' to ensure the order remains until filled
                    .setLimitPrice(lastTradePrice); // Set limit price to last trade price for controlled execution
    
            // Build the order parameters
            SimpleOrderSendParameters orderParameters = builder.build();
    
            // Send the order to flatten the position
            api.sendOrder(orderParameters);
    
            currentPosition = 0; // Reset the current position
        }
    }
    

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
