import velox.api.layer1.annotations.*;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.*;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.*;

import java.awt.*;

@Layer1TradingStrategy
@Layer1SimpleAttachable
@Layer1StrategyName("onBar SMA")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class OnBarSMA implements CustomModule, BarDataListener {

    private Indicator closeIndicator;
    private Indicator smaIndicator;
    private double pips;
    private SMA sma;
    private double previousClose = -1;
    private Double previousSMA = null;
    private Api api;
    private String alias;
    private int currentPosition = 0;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {

        closeIndicator = api.registerIndicator("Close", GraphType.PRIMARY);
        closeIndicator.setColor(Color.MAGENTA);
        smaIndicator = api.registerIndicator("SMA", GraphType.PRIMARY);
        smaIndicator.setColor(Color.BLUE);
        pips = info.pips;
        this.alias = alias;
        this.api = api;
        sma = new SMA(14);
    }

    @Override
    public void stop() {
    }

    @Override
    public void onBar(OrderBook orderBook, Bar bar) {

        double closePrice = bar.getClose();
        closeIndicator.addPoint(closePrice);

        Double smaValue = sma.calculate(closePrice);
        if (smaValue != null) {
            smaIndicator.addPoint(smaValue);

            // Check for crossover signals
            if (previousClose != -1 && previousSMA != null) {
                if (closePrice > smaValue && previousClose <= previousSMA) {
                    Log.info("Buy Signal at " + closePrice * pips);
                    if (currentPosition <= 0) {
                        placeOrder(true, closePrice, 1); // Place a buy order
                        currentPosition = 1; // Update the current position to long
                    }
                } else if (closePrice < smaValue && previousClose >= previousSMA) {
                    Log.info("Sell Signal at " + closePrice * pips);
                    if (currentPosition >= 0) {
                        placeOrder(false, closePrice, 1); // Place a sell order
                        currentPosition = -1; // Update the current position to short
                    }
                }
            }

            // Update previous values
            previousClose = closePrice;
            previousSMA = smaValue;
        }
    }

    private void placeOrder(boolean isBuy, double price, int quantity) {
        try {
            SimpleOrderSendParametersBuilder builder = new SimpleOrderSendParametersBuilder(alias, isBuy, quantity);
            builder.setDuration(OrderDuration.IOC);

            // Setting a stop loss and take profit offset (in ticks)
            int stopLossOffset = 10; // for example, 10 ticks
            int takeProfitOffset = 20; // for example, 20 ticks

            builder.setStopLossOffset(stopLossOffset);
            builder.setTakeProfitOffset(takeProfitOffset);
            SimpleOrderSendParameters order = builder.build();
            api.sendOrder(order);
        } catch (Exception e) {
            Log.error("Error placing order", e);
        }
    }

    public void onOrderUpdated(OrderInfoUpdate orderInfo) {
        // Handle order updates here
        // For example, you might log the order's status
        System.out.println("Order Status: " + orderInfo.status);

    }

    public void onOrderExecuted(ExecutionInfo executionInfo) {
        // Handle order executions here
        // For example, you might log the execution details
        System.out.println("Order Executed: " + executionInfo.price);
    }

    @Override
    public long getInterval() {
        return Intervals.INTERVAL_1_MINUTE;
    }
}

