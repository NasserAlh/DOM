package restart;


import velox.api.layer1.annotations.*;
import velox.api.layer1.data.*;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.*;
import java.awt.*;

@Layer1SimpleAttachable
@Layer1StrategyName("EMA Indicator Restarter")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class EmaIndicatorOnBar implements CustomModule, BarDataListener {

    private static final int EMA_PERIOD = 200;
    private Indicator emaIndicator;
    private double ema = 0;
    private boolean isFirstBar = true;
    private double multiplier = 2.0 / (EMA_PERIOD + 1);
    

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        emaIndicator = api.registerIndicator("EMA", GraphType.PRIMARY);
        emaIndicator.setColor(Color.ORANGE);
    }

    @Override
    public void onBar(OrderBook orderBook, Bar bar) {
        double closePrice = bar.getClose();
        
        if (isFirstBar) {
            ema = closePrice; // Starting point for EMA
            isFirstBar = false;
        } else {
            ema = (closePrice - ema) * multiplier + ema;
        }
        
        emaIndicator.addPoint(ema);
    }

    @Override
    public long getInterval() {
        return Intervals.INTERVAL_1_SECOND; // You can adjust the interval as needed
    }

    @Override
    public void stop() {
        // Perform any cleanup if necessary when stopping the indicator
    }
}
