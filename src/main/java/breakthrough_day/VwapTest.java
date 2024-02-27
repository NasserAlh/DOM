package breakthrough_day;

import velox.api.layer1.annotations.*;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.*;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.*;

import java.awt.*;

@Layer1TradingStrategy
@Layer1SimpleAttachable
@Layer1StrategyName("TradesOverlapFix VWAP")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class VwapTest implements CustomModule, BarDataListener, OrdersListener {

private static final double INITIAL_PREVIOUS_CLOSE = -1.0;
    private static final int STOP_LOSS_OFFSET = 10;
    private static final int TAKE_PROFIT_OFFSET = 20;
    private Indicator closeIndicator;
    private Indicator vwapIndicator;
    private double pips;
    private double cumulativeVolumePrice = 0;
    private double cumulativeVolume = 0;
    private double previousClose = INITIAL_PREVIOUS_CLOSE;
    private Double previousVWAP = null;
    private Api api;
    private String alias;
    private int currentPosition = 0;
    private InitialState initialState;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        Log.info("Initializing the TradesOverlapFixInitialState strategy...");

        closeIndicator = api.registerIndicator("Close", GraphType.PRIMARY);
        closeIndicator.setColor(Color.MAGENTA);
        vwapIndicator = api.registerIndicator("VWAP", GraphType.PRIMARY);
        vwapIndicator.setColor(Color.ORANGE);
        pips = info.pips;
        this.alias = alias;
        this.api = api;
        this.initialState = initialState;
    }

    @Override
    public void stop() {
        Log.info("Stopping the TradesOverlapFixInitialState strategy...");
        flattenPosition(initialState);
    }

    @Override
    public void onBar(OrderBook orderBook, Bar bar) {
        double closePrice = bar.getClose();
        // Assuming getVolumeTotal() correctly returns the total volume for the bar
        cumulativeVolumePrice += closePrice * bar.getVolumeTotal(); // Correct usage if getVolumeTotal() exists
        cumulativeVolume += bar.getVolumeTotal(); // Correct usage if getVolumeTotal() exists
        Double vwapValue = null;
        if (cumulativeVolume != 0) {
            vwapValue = cumulativeVolumePrice / cumulativeVolume;
        }
        updateIndicators(closePrice, vwapValue);
        checkForCrossoverSignals(closePrice, vwapValue);
        updatePreviousValues(closePrice, vwapValue);
    }

    private void updateIndicators(double closePrice, Double vwapValue) {
        closeIndicator.addPoint(closePrice);
        if (vwapValue != null) {
            vwapIndicator.addPoint(vwapValue);
        }
    }

    private void checkForCrossoverSignals(double closePrice, Double vwapValue) {
        if (vwapValue != null && previousClose != INITIAL_PREVIOUS_CLOSE && previousVWAP != null) {
            if (currentPosition == 0) {
                if (closePrice > vwapValue && previousClose <= previousVWAP) {
                    Log.info("Buy Signal at " + closePrice * pips);
                    placeOrder(true, closePrice, 1);
                    currentPosition = 1;
                } else if (closePrice < vwapValue && previousClose >= previousVWAP) {
                    Log.info("Sell Signal at " + closePrice * pips);
                    placeOrder(false, closePrice, 1);
                    currentPosition = -1;
                }
            }
        }
    }

    private void updatePreviousValues(double closePrice, Double vwapValue) {
        previousClose = closePrice;
        previousVWAP = vwapValue;
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
    public void onOrderExecuted(ExecutionInfo arg0) {
        // onOrderExecuted method remains the same
    }
}
