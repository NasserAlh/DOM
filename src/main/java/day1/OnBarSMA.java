package day1;

import velox.api.layer1.annotations.*;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.*;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.*;

import java.awt.*;

/**
 * This class implements a simple moving average (SMA) trading strategy.
 */


@Layer1TradingStrategy
@Layer1SimpleAttachable
@Layer1StrategyName("onBar SMA")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class OnBarSMA implements CustomModule, BarDataListener, OrdersListener  {

    private Indicator closeIndicator;
    private Indicator smaIndicator;
    private double pips;
    private SMA sma;
    private double previousClose = -1;
    private Double previousSMA = null;
    private Api api;
    private String alias;
    private int currentPosition = 0;
    private static final int SMA_PERIOD = 14;
    private static final int STOP_LOSS_OFFSET = 10;
    private static final int TAKE_PROFIT_OFFSET = 20;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {

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

    /**
     * Places an order with the specified parameters.
     *
     * @param isBuy    whether the order is a buy order
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
            Log.error("Error placing order", e);
        }
    }

    @Override
    public long getInterval() {
        return Intervals.INTERVAL_1_MINUTE;
    }

    @Override
    public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {

    }

    @Override
    public void onOrderExecuted(ExecutionInfo executionInfo) {
        // Logging the order ID
        Log.info("The market order with OrderId: " + executionInfo.orderId + " has been executed.");
    }
}

