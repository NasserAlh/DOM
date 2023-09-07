package day1;

import java.awt.*;
import java.awt.image.BufferedImage;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.TradeDataListener;

@Layer1SimpleAttachable
@Layer1StrategyName("Last trade: live")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class ScreenSpacePainter implements CustomModule, TradeDataListener
{
    protected Indicator lastTradeIndicator;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        lastTradeIndicator = api.registerIndicator("Last trade, no history",
                GraphType.PRIMARY);
        lastTradeIndicator.setColor(Color.GREEN);
    }

    @Override
    public void stop() {
    }

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        lastTradeIndicator.addPoint(price);
        BufferedImage arrow = makeRandomArrow(tradeInfo.isBidAggressor);
        lastTradeIndicator.addIcon(price, arrow, 3, 3);
    }

    private BufferedImage makeRandomArrow(boolean isBid) {
        BufferedImage icon = new BufferedImage(50, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = icon.createGraphics();
        graphics.setColor(isBid ? Color.GREEN : Color.RED);
        graphics.setStroke(new BasicStroke(5));
        graphics.drawLine(3, 3, icon.getWidth(), icon.getHeight());
        graphics.drawLine(3, 3, 20, 3);
        graphics.drawLine(3, 3, 3, 20);
        graphics.dispose();
        return icon;
    }
}