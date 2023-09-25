package day3;

import velox.api.layer1.annotations.*;
import velox.api.layer1.common.Log;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.simplified.*;
import velox.gui.StrategyPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

@Layer1SimpleAttachable
@Layer1StrategyName("Stacked Imbalance")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class StackedImbalance implements CustomModuleAdapter, DepthDataListener,
        CustomSettingsPanelProvider, IntervalListener {

    private final TreeMap<Integer, Integer> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Integer, Integer> asks = new TreeMap<>();
    private velox.api.layer1.simplified.Indicator askBidIndicator;
    private velox.api.layer1.simplified.Indicator bidAskIndicator;
    private int imbalanceRatio = 300;
    private int imbalanceVolume = 30;
    private int imbalanceStack = 3;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        askBidIndicator = api.registerIndicator("Ask/Bid Imbalance", GraphType.BOTTOM);
        bidAskIndicator = api.registerIndicator("Bid/Ask Imbalance", GraphType.BOTTOM);

        askBidIndicator.setColor(Color.MAGENTA);
        bidAskIndicator.setColor(Color.BLUE);
    }

    @Override
    public void onDepth(boolean isBid, int price, int size) {
        TreeMap<Integer, Integer> book = isBid ? bids : asks;
        if (size == 0) {
            book.remove(price);
        } else {
            book.put(price, size);
        }

        // Update indicators
        askBidIndicator.addPoint(calculateAskBidImbalance());
        bidAskIndicator.addPoint(calculateBidAskImbalance());
    }

    private int calculateAskBidImbalance() {
        int imbalance = 0;
        for (Map.Entry<Integer, Integer> ask : asks.entrySet()) {
            int askPrice = ask.getKey();
            int askSize = ask.getValue();
            int bidSize = bids.getOrDefault(askPrice, 0);
            if (askSize > bidSize * imbalanceRatio / 100 && askSize > imbalanceVolume) {
                imbalance += askSize - bidSize;
            }
        }
        Log.info("Ask/Bid Imbalance detected: " + imbalance);
        return imbalance;
    }

    private int calculateBidAskImbalance() {
        int imbalance = 0;
        for (Map.Entry<Integer, Integer> bid : bids.entrySet()) {
            int bidPrice = bid.getKey();
            int bidSize = bid.getValue();
            int askSize = asks.getOrDefault(bidPrice, 0);
            if (bidSize > askSize * imbalanceRatio / 100 && bidSize > imbalanceVolume) {
                imbalance += bidSize - askSize;
            }
        }
        Log.info("Bid/Ask Imbalance detected: " + imbalance);
        return imbalance;
    }

    private void recalculateIndicators() {
        askBidIndicator.addPoint(calculateAskBidImbalance());
        bidAskIndicator.addPoint(calculateBidAskImbalance());
    }

    @Override
    public StrategyPanel[] getCustomSettingsPanels() {
        // Panel for adjusting imbalance ratio
        StrategyPanel ratioPanel = new StrategyPanel("Imbalance Ratio");
        JSpinner ratioSpinner = new JSpinner(new SpinnerNumberModel(imbalanceRatio, 100, 500, 10));
        ratioSpinner.addChangeListener(e -> {
            imbalanceRatio = (int) ((JSpinner) e.getSource()).getValue();
            recalculateIndicators();
        });
        ratioPanel.add(ratioSpinner);

        // Panel for adjusting imbalance volume
        StrategyPanel volumePanel = new StrategyPanel("Imbalance Volume");
        JSpinner volumeSpinner = new JSpinner(new SpinnerNumberModel(imbalanceVolume, 10, 171111, 1000));
        volumeSpinner.addChangeListener(e -> {
            imbalanceVolume = (int) ((JSpinner) e.getSource()).getValue();
            recalculateIndicators();
        });
        volumePanel.add(volumeSpinner);

        // Panel for adjusting imbalance stack
        StrategyPanel stackPanel = new StrategyPanel("Imbalance Stack");
        JSpinner stackSpinner = new JSpinner(new SpinnerNumberModel(imbalanceStack, 1, 10, 1));
        stackSpinner.addChangeListener(e -> {
            imbalanceStack = (int) ((JSpinner) e.getSource()).getValue();
            recalculateIndicators();
        });
        stackPanel.add(stackSpinner);

        return new StrategyPanel[] { ratioPanel, volumePanel, stackPanel };
    }

    @Override
    public long getInterval() {
        return Intervals.INTERVAL_1_MINUTE;
    }

    @Override
    public void onInterval() {

    }
}

