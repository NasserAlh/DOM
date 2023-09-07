package day1;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.TradeDataListener;

@Layer1SimpleAttachable
@Layer1StrategyName("Simplified demo: SSP single line")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class SimplifiedDemoSSPSingleLine implements CustomModule, TradeDataListener {
    private static final String INDICATOR_NAME = "Screen space canvas demo";
    private SingleLineSspFactory ssp;
    private Api api;
    private String indicatorName;


    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        this.api = api;
        ssp = new SingleLineSspFactory(alias);
        indicatorName = INDICATOR_NAME + " " + alias;
        api.sendUserMessage(ssp.getUserMessage(indicatorName, true));
    }

    @Override
    public void stop() {
        api.sendUserMessage(ssp.getUserMessage(indicatorName, false));
    }

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        ssp.onTrade(price);
    }

}