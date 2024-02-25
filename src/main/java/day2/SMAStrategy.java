package day2;

import day1.SMA;
import velox.api.layer1.annotations.*;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.*;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.awt.*;

@Layer1TradingStrategy
@Layer1SimpleAttachable
@Layer1StrategyName("SMA Strategy")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class SMAStrategy implements CustomModule, BarDataListener, OrdersListener {

    private static final double INITIAL_PREVIOUS_CLOSE = -1.0;
    private static final int SMA_PERIOD = 14;
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

    private OrderUpdatedLogger orderUpdatedLogger = new OrderUpdatedLogger();
    private OrderExecutedLogger orderExecutedLogger = new OrderExecutedLogger();
    private static final String CSV_FILE_PATH = "C:\\Bookmap\\Logs\\Nasser2_log.csv";

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

        // Initialize CSV file with headers
        try (PrintWriter pw = new PrintWriter(new FileWriter(CSV_FILE_PATH))) {
            pw.println("Log Type,Order ID,Is Buy,Status,Stop Price,Limit Price");
        } catch (IOException e) {
            Log.error("Error initializing CSV file", e);
        }

        Log.info("OnBarSMA strategy initialized successfully.");
    }

    @Override
    public void stop() {
        Log.info("Stopping the OnBarSMA strategy...");
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

    private void checkForCrossoverSignals(double closePrice) {
        Double smaValue = sma.calculate(closePrice);
        if (smaValue != null && previousClose != INITIAL_PREVIOUS_CLOSE && previousSMA != null) {
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
    }

    private void updatePreviousValues(double closePrice) {
        previousClose = closePrice;
        previousSMA = sma.calculate(closePrice);
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

    @Override
    public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
        orderUpdatedLogger.logOrderUpdated(orderInfoUpdate);
        String orderId = orderInfoUpdate.getOrderId();
    }

    @Override
    public void onOrderExecuted(ExecutionInfo executionInfo) {
        orderExecutedLogger.logOrderExecuted(executionInfo);
    }

    @Override
    public long getInterval() {
        return Intervals.INTERVAL_1_MINUTE;
    }


}
