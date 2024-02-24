package restart;

import velox.api.layer1.annotations.*;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.data.*;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.*;

@Layer1Attachable
@Layer1StrategyName("EMA Indicator Restart")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class EmaIndicator implements CustomModule, TradeDataListener, IntervalListener {

    private Indicator indicator;
    private double ema = 0;
    private final int period = 14; // EMA period
    private final double multiplier = 2.0 / (period + 1);
    private boolean isFirstTrade = true;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        indicator = api.registerIndicator("EMA", GraphType.PRIMARY);
    }

    @Override
    public long getInterval() {
        return Intervals.INTERVAL_1_SECOND; // Update frequency
    }

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        if (isFirstTrade) {
            ema = price; // Starting point for EMA
            isFirstTrade = false;
        } else {
            ema = (price - ema) * multiplier + ema;
        }
    }

    @Override
    public void onInterval() {
        indicator.addPoint(ema); // Update the indicator with the latest EMA value
    }

    @Override
    public void stop() {}
}
