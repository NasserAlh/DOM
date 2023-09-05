import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.Bar;
import velox.api.layer1.simplified.BarDataListener;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.Intervals;

import java.awt.*;

@Layer1SimpleAttachable
@Layer1StrategyName("onBar")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class OnBar implements CustomModule, BarDataListener {

    private Indicator closeIndicator;
    private Indicator smaIndicator;
    private double pips;
    private SMA sma;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {

        closeIndicator = api.registerIndicator("Close", GraphType.PRIMARY);
        closeIndicator.setColor(Color.MAGENTA);

        smaIndicator = api.registerIndicator("SMA", GraphType.PRIMARY);
        smaIndicator.setColor(Color.BLUE);

        pips = info.pips;

        sma = new SMA(14); // Initialize SMA with a period of 14
    }

    @Override
    public void stop() {
    }

    @Override
    public void onBar(OrderBook orderBook, Bar bar) {

        double closePrice = bar.getClose();
        closeIndicator.addPoint(closePrice);
        Log.info("Close Price " + closePrice * pips );

        Double smaValue = sma.calculate(closePrice);
        if (smaValue != null) {
            smaIndicator.addPoint(smaValue);
        }
    }

    @Override
    public long getInterval() {
        return Intervals.INTERVAL_15_SECONDS;
    }
}
