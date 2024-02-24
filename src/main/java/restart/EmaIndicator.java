package restart;

import velox.api.layer0.annotations.Layer1SimpleAttachable;
import velox.api.layer0.annotations.Layer1StrategyName;
import velox.api.layer0.annotations.Layer1ApiVersion;
import velox.api.layer0.data.*;
import velox.api.layer0.live.ExternalLiveBaseProvider;
import velox.api.layer1.Layer1ApiVersionValue;
import velox.api.layer1.data.*;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.simplified.*;

@Layer1SimpleAttachable
@Layer1StrategyName("EMA Indicator")
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
}
