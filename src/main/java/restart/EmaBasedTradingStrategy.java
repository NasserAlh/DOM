package restart;

import velox.api.layer1.annotations.*;
import velox.api.layer1.data.*;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.*;
import java.awt.*;

@Layer1TradingStrategy
@Layer1SimpleAttachable
@Layer1StrategyName("EMA Based Trading Strategy")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class EmaBasedTradingStrategy implements CustomModule, BarDataListener {
    private static final int EMA_PERIOD = 200;
    private Indicator emaIndicator;
    private double ema = 0;
    private boolean isFirstBar = true;
    private double multiplier = 2.0 / (EMA_PERIOD + 1);
    private Api api;
    private boolean inPosition = false;
    private String alias;

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

        if (isFirstBar) {
            ema = closePrice; // Starting point for EMA
            isFirstBar = false;
        } else {
            ema = (closePrice - ema) * multiplier + ema;
        }

        emaIndicator.addPoint(ema);

        // Trading logic
        if (closePrice > ema && !inPosition) {
            // Send a market order to buy
            SimpleOrderSendParametersBuilder orderBuilder = new SimpleOrderSendParametersBuilder(alias, true, 1);
            api.sendOrder(orderBuilder.build());
            inPosition = true;
        } else if (closePrice < ema && inPosition) {
            // Close the position
            SimpleOrderSendParametersBuilder orderBuilder = new SimpleOrderSendParametersBuilder(alias, false, 1);
            api.sendOrder(orderBuilder.build());
            inPosition = false;
        }
    }

    @Override
    public long getInterval() {
        return Intervals.INTERVAL_1_SECOND; // You can adjust the interval as needed
    }

    @Override
    public void stop() {
        // Perform any cleanup if necessary when stopping the strategy
    }
}
