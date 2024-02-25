package restart;

import velox.api.layer1.annotations.*;
import velox.api.layer1.data.*;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.*;
import java.awt.*;

@Layer1TradingStrategy
@Layer1SimpleAttachable
@Layer1StrategyName("EMA Based Trading Strategy 2")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class EmaBasedTradingStrategy1 implements CustomModule, BarDataListener {
    private static final int EMA_PERIOD = 200;
    private static final double ENTRY_THRESHOLD = 2;
    private static final double EXIT_THRESHOLD_LONG = 6;
    private static final double EXIT_THRESHOLD_SHORT = -6;
    private Indicator emaIndicator;
    private double ema = 0;
    private boolean isFirstBar = true;
    private double multiplier = 2.0 / (EMA_PERIOD + 1);
    private Api api;
    private String alias;
    private boolean isLongPosition = false; // Track if the current position is long
    private boolean isShortPosition = false; // Track if the current position is short

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        this.api = api;
        this.alias = alias;
        emaIndicator = api.registerIndicator("EMA", GraphType.PRIMARY);
        emaIndicator.setColor(Color.ORANGE);
    }

    @Override
    public void onBar(OrderBook orderBook, Bar bar) {
        double closePrice = bar.getClose();
        double highPrice = bar.getHigh();
        double lowPrice = bar.getLow();

        // Update the EMA
        if (isFirstBar) {
            ema = closePrice; // Starting point for EMA
            isFirstBar = false;
        } else {
            ema = (closePrice - ema) * multiplier + ema;
        }

        emaIndicator.addPoint(ema);

        // Long entry condition
        if (highPrice > ema + ENTRY_THRESHOLD && !isLongPosition && !isShortPosition) {
            sendOrder(true, 1);
            isLongPosition = true;
            isShortPosition = false;
        }
        // Short entry condition
        else if (lowPrice < ema - ENTRY_THRESHOLD && !isShortPosition && !isLongPosition) {
            sendOrder(false, 1);
            isShortPosition = true;
            isLongPosition = false;
        }

        // Long exit conditions
        if (isLongPosition) {
            if (highPrice > ema + EXIT_THRESHOLD_LONG || lowPrice < ema - ENTRY_THRESHOLD) {
                sendOrder(false, 1);
                isLongPosition = false;
            }
        }

        // Short exit conditions
        if (isShortPosition) {
            if (lowPrice < ema + EXIT_THRESHOLD_SHORT || highPrice > ema + ENTRY_THRESHOLD) {
                sendOrder(true, 1);
                isShortPosition = false;
            }
        }
    }

    private void sendOrder(boolean isBuy, int size) {
        SimpleOrderSendParametersBuilder orderBuilder = new SimpleOrderSendParametersBuilder(alias, isBuy, size);
        api.sendOrder(orderBuilder.build());
    }

    @Override
    public long getInterval() {
        return Intervals.INTERVAL_1_SECOND; // Interval can be adjusted as needed
    }

    @Override
    public void stop() {
        // Cleanup if necessary when stopping the strategy
    }
}
