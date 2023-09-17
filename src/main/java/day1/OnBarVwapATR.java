package day1;

import velox.api.layer1.annotations.*;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.*;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.*;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;

@Layer1TradingStrategy
@Layer1SimpleAttachable
@Layer1StrategyName("onBar VWAP ATR")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class OnBarVwapATR implements CustomModule, BarDataListener, OrdersListener {

    private static final double INITIAL_PREVIOUS_CLOSE = -1.0;
    private static final int ATR_PERIOD = 14;
    private static final double ATR_MULTIPLIER = 2.0;
    private static final String EXECUTED_CSV_FILE_PATH = "C:\\Bookmap\\Trading_Logs\\Order_Executed_log.csv";

    private Indicator closeIndicator;
    private Indicator vwapIndicator;
    private Indicator atrIndicator;
    private double pips;
    private double cumulativePriceVolume = 0.0;
    private double cumulativeVolume = 0.0;
    private double vwap = 0.0;
    private double previousClose = INITIAL_PREVIOUS_CLOSE;
    private double atr = 0.0;
    private double previousHigh = -1.0;
    private double previousLow = -1.0;
    private LinkedList<Double> trueRanges = new LinkedList<>();
    private double cumulativeTrueRange = 0.0;
    private Api api;
    private String alias;
    private int currentPosition = 0;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        this.alias = alias;
        this.api = api;
        pips = info.pips;

        closeIndicator = api.registerIndicator("Close", GraphType.PRIMARY);
        closeIndicator.setColor(Color.MAGENTA);

        vwapIndicator = api.registerIndicator("VWAP", GraphType.PRIMARY);
        vwapIndicator.setColor(Color.BLACK);

        atrIndicator = api.registerIndicator("ATR", GraphType.BOTTOM);
        atrIndicator.setColor(Color.GREEN);

        initializeCSVFile();
    }

    private void initializeCSVFile() {
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
        Log.info("Stopping the OnBarVwapATR strategy...");
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

        double high = bar.getHigh();
        double low = bar.getLow();
        double trueRange = calculateTrueRange(high, low, previousClose);

        if (trueRanges.size() >= ATR_PERIOD) {
            cumulativeTrueRange -= trueRanges.removeFirst();
        }
        trueRanges.add(trueRange);
        cumulativeTrueRange += trueRange;

        if (trueRanges.size() >= ATR_PERIOD) {
            atr = cumulativeTrueRange / ATR_PERIOD;
        }

        updateIndicators(closePrice);
        checkForCrossoverSignals(closePrice);
        updatePreviousValues(closePrice);

        previousHigh = high;
        previousLow = low;
    }


    /**
     * Calculates the true range which is the greatest of the following:
     * - Current high less the current low
     * - The absolute value of the current high less the previous close
     * - The absolute value of the current low less the previous close
     */
    private double calculateTrueRange(double high, double low, double previousClose) {
        if (previousClose == INITIAL_PREVIOUS_CLOSE) {
            return high - low;
        }
        return Math.max(high - low, Math.max(Math.abs(high - previousClose), Math.abs(low - previousClose)));
    }

    private void updateIndicators(double closePrice) {
        closeIndicator.addPoint(closePrice);
        vwapIndicator.addPoint(vwap);
        atrIndicator.addPoint(atr);

    }

    private void checkForCrossoverSignals(double closePrice) {
        double riskManagementPrice = ATR_MULTIPLIER * atr * pips;

        if (previousClose != INITIAL_PREVIOUS_CLOSE) {
            if (closePrice > vwap + riskManagementPrice && previousClose <= vwap) {
                Log.info("Buy Signal at " + closePrice * pips);
                if (currentPosition <= 0) {
                    placeOrder(true, closePrice, 1); // Place a buy order
                    currentPosition = 1; // Update the current position to long
                }
            } else if (closePrice < vwap - riskManagementPrice && previousClose >= vwap) {
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
    public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {}

    @Override
    public long getInterval() {
        return Intervals.INTERVAL_1_MINUTE;
    }
}
