package day1;

import velox.api.layer1.annotations.*;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.*;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.awt.*;

@Layer1TradingStrategy
@Layer1SimpleAttachable
@Layer1StrategyName("onBar VWAP ID")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)

public class OnBarVWAP implements CustomModule, BarDataListener, OrdersListener {

    private static final double INITIAL_PREVIOUS_CLOSE = -1.0;
    private Indicator closeIndicator;
    private Indicator vwapIndicator;
    private double pips;
    private double cumulativePriceVolume = 0.0;
    private double cumulativeVolume = 0.0;
    private double vwap = 0.0;
    private double previousClose = INITIAL_PREVIOUS_CLOSE;
    private Api api;
    private String alias;
    private int currentPosition = 0;
    private static final String EXECUTED_CSV_FILE_PATH = "C:\\Bookmap\\Trading_Logs\\Order_Executed_log.csv";

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        closeIndicator = api.registerIndicator("Close", GraphType.PRIMARY);
        closeIndicator.setColor(Color.MAGENTA);
        vwapIndicator = api.registerIndicator("SMA", GraphType.PRIMARY);
        vwapIndicator.setColor(Color.BLUE);
        pips = info.pips;
        this.alias = alias;
        this.api = api;


        try (PrintWriter pw = new PrintWriter(new FileWriter(EXECUTED_CSV_FILE_PATH))) {
            pw.println("Order ID,Price,Time,Execution ID");
        } catch (IOException e) {
            Log.info("Error initializing CSV file", e);
        }
    }

    private String convertTimestampToHumanReadable(long timestampInNanoseconds) {
        long timestampInMilliseconds = timestampInNanoseconds / 1_000_000;
        Instant instant = Instant.ofEpochMilli(timestampInMilliseconds);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.of("America/New_York"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        return dateTime.format(formatter);
    }

    @Override
    public void stop() {
        Log.info("Stopping the OnBarSMA strategy...");
    }

    @Override
    public void onBar(OrderBook orderBook, Bar bar) {
        double closePrice = bar.getClose();
        double volume = bar.getVolumeTotal();

        cumulativePriceVolume += closePrice * volume;
        cumulativeVolume += volume;

        if (cumulativeVolume != 0) {
            vwap = cumulativePriceVolume / cumulativeVolume;
        }

        updateIndicators(closePrice);
        checkForCrossoverSignals(closePrice);
        updatePreviousValues(closePrice);
    }

    private void updateIndicators(double closePrice) {
        closeIndicator.addPoint(closePrice);
        vwapIndicator.addPoint(vwap);
    }

    private void checkForCrossoverSignals(double closePrice) {
        if (previousClose != INITIAL_PREVIOUS_CLOSE) {
            if (closePrice > vwap && previousClose <= vwap) {
                Log.info("Buy Signal at " + closePrice * pips);
                if (currentPosition <= 0) {
                    placeOrder(true, closePrice, 1); // Place a buy order
                    currentPosition = 1; // Update the current position to long
                }
            } else if (closePrice < vwap && previousClose >= vwap) {
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
    }

    private void placeOrder(boolean isBuy, double price, int quantity) {
        try {
            SimpleOrderSendParametersBuilder builder = new SimpleOrderSendParametersBuilder(alias, isBuy, quantity);
            builder.setDuration(OrderDuration.IOC);
            SimpleOrderSendParameters order = builder.build();
            api.sendOrder(order);
        } catch (Exception e) {
            Log.info("Error placing order", e);
        }
    }

    @Override
    public void onOrderExecuted(ExecutionInfo executionInfo) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(EXECUTED_CSV_FILE_PATH, true))) {
            String formattedTime = convertTimestampToHumanReadable(executionInfo.time);
            pw.println(executionInfo.orderId + "," + executionInfo.price + "," + formattedTime + "," +
                    executionInfo.executionId);
        } catch (IOException e) {
            Log.info("Error writing to CSV file", e);
        }
    }

    @Override
    public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate){}
    @Override
    public long getInterval() {
        return Intervals.INTERVAL_1_MINUTE;
    }
}
