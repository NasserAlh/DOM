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
public class EmaBasedTradingStrategyERM implements CustomModule, BarDataListener, PositionListener {
    private static final int EMA_PERIOD = 200;
    private Indicator emaIndicator;
    private double ema = 0;
    private boolean isFirstBar = true;
    private double multiplier = 2.0 / (EMA_PERIOD + 1);
    private Api api;
    private String alias;
    private boolean inPosition = false;
    private boolean signalBuyNextBar = false; // Flag to indicate a buy signal on the next bar
    private boolean signalSellNextBar = false; // Flag to indicate a sell signal on the next bar

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        this.api = api;
        this.alias = alias;
        emaIndicator = api.registerIndicator("EMA", GraphType.PRIMARY);
        emaIndicator.setColor(Color.ORANGE);
        api.addStatusListeners(this);
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

        if (signalBuyNextBar && !inPosition) {
            // Execute buy order on the next bar after the signal
            SimpleOrderSendParametersBuilder orderBuilder = new SimpleOrderSendParametersBuilder(alias, true, 1);
            api.sendOrder(orderBuilder.build());
            signalBuyNextBar = false; // Reset the flag after executing the order
        } else if (signalSellNextBar && inPosition) {
            // Execute sell order on the next bar after the signal
            SimpleOrderSendParametersBuilder orderBuilder = new SimpleOrderSendParametersBuilder(alias, false, 1);
            api.sendOrder(orderBuilder.build());
            signalSellNextBar = false; // Reset the flag after executing the order
        }

        // Trading logic to set the flags
        if (closePrice > ema && !inPosition) {
            signalBuyNextBar = true; // Set the flag to buy on the next bar
        } else if (closePrice < ema && inPosition) {
            signalSellNextBar = true; // Set the flag to sell on the next bar
        }
    }

    @Override
    public void onPositionUpdate(StatusInfo statusInfo) {
        // Use the StatusInfo object to determine the current position size
        inPosition = statusInfo. != 0;
    }
}

    @Override
    public long getInterval() {
        return Intervals.INTERVAL_1_SECOND; // Adjust the interval as needed
    }

    @Override
    public void stop() {
        // Cleanup if necessary when stopping the strategy
    }

}
