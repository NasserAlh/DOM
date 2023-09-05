import java.awt.Color;
import java.util.LinkedList;
import velox.api.layer1.annotations.*;
import velox.api.layer1.data.*;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.*;

@Layer1TradingStrategy
@Layer1SimpleAttachable
@Layer1StrategyName("SMA by BAR 2")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class BarDemoWithHistoryData implements CustomModule, BarDataListener, HistoricalDataListener {

    private Indicator closeIndicator;
    private LinkedList<Double> closePrices = new LinkedList<>();
    private int smaPeriod = 20; // Define the period for SMA
    private boolean isPositionOpen = false; // To track if a position is already open
    private String alias;
    private Api api;
    private double sumClosePrices = 0.0; // Maintain a running sum
    private int warmUpBars = 50; // Number of bars for the warm-up period
    private int barsReceived = 0; // Counter for the number of bars received

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        this.alias = alias;
        this.api = api;
        closeIndicator = api.registerIndicator("Close", GraphType.PRIMARY);
        closeIndicator.setColor(Color.MAGENTA);
    }

    @Override
    public void stop() {
    }

    @Override
    public void onBar(OrderBook orderBook, Bar bar) {
        barsReceived++; // Increment the counter for each bar received

        double closePrice = bar.getClose();
        closeIndicator.addPoint(closePrice);
        closePrices.add(closePrice);
        sumClosePrices += closePrice;

        // Only allow trading after the warm-up period
        if (barsReceived > warmUpBars && closePrices.size() >= smaPeriod) {
            double sma = sumClosePrices / smaPeriod;

            if (!isPositionOpen) {
                SimpleOrderSendParametersBuilder builder = new SimpleOrderSendParametersBuilder(alias, true, 1);
                builder.setStopLossOffset(10); // Stop loss offset
                builder.setTakeProfitOffset(10); // Take profit offset
                builder.setDuration(OrderDuration.IOC); // Order duration

                if (bar.getClose() > sma) {
                    // Buy signal
                    builder.setBuy(true);
                    SimpleOrderSendParameters order = builder.build();
                    api.sendOrder(order);
                    isPositionOpen = true;
                } else if (bar.getClose() < sma) {
                    // Sell signal
                    builder.setBuy(false);
                    SimpleOrderSendParameters order = builder.build();
                    api.sendOrder(order);
                    isPositionOpen = true;
                }
            }

            // Update the running sum by subtracting the oldest price
            sumClosePrices -= closePrices.removeFirst();
        }
    }

    @Override
    public long getInterval() {
        return Intervals.INTERVAL_2_MINUTES;
    }
}