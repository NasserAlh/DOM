package day1;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.simplified.*;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@Layer1SimpleAttachable
@Layer1StrategyName("S/R")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)

public class OnBarSupportResistanceDynamic implements CustomModule, BarDataListener {

    private Indicator closeIndicator;
    private Indicator supportIndicator;
    private Indicator resistanceIndicator;
    private SupportResistanceDynamic supportResistanceDynamic;
    private List<Indicator> horizontalGridLines;
    private List<Indicator> verticalGridLines;
    private List<Bar> recentBars = new ArrayList<>();
    private int barCounter = 0;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        closeIndicator = api.registerIndicator("Close", GraphType.PRIMARY);
        closeIndicator.setColor(Color.MAGENTA);

        supportIndicator = api.registerIndicator("Support", GraphType.PRIMARY);
        supportIndicator.setColor(Color.GREEN);

        resistanceIndicator = api.registerIndicator("Resistance", GraphType.PRIMARY);
        resistanceIndicator.setColor(Color.RED);

        supportResistanceDynamic = new SupportResistanceDynamic();

        horizontalGridLines = new ArrayList<>();
        verticalGridLines = new ArrayList<>();

        // Initialize grid lines
        for (int i = 0; i < 10; i++) {
            Indicator horizontalLine = api.registerIndicator("Horizontal Grid Line " + i, GraphType.PRIMARY);
            horizontalLine.setColor(Color.GRAY);
            horizontalGridLines.add(horizontalLine);

            Indicator verticalLine = api.registerIndicator("Vertical Grid Line " + i, GraphType.PRIMARY);
            verticalLine.setColor(Color.GRAY);
            verticalGridLines.add(verticalLine);
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public void onBar(OrderBook orderBook, Bar bar) {
        closeIndicator.addPoint(bar.getClose());

        // Calling the calculate method of SupportResistanceDynamic class with necessary bar data
        supportResistanceDynamic.calculate(bar.getClose(), bar.getHigh(), bar.getLow());

        // Retrieving and plotting the current support and resistance levels
        SupportResistanceDynamic.SR support = supportResistanceDynamic.getSupport();
        SupportResistanceDynamic.SR resistance = supportResistanceDynamic.getResistance();

        if (support != null) {
            supportIndicator.addPoint(support.y);
        }

        if (resistance != null) {
            resistanceIndicator.addPoint(resistance.y);
        }

        // Add the new bar to the list of recent bars
        recentBars.add(bar);

        // Increment the bar counter
        barCounter++;

        // Remove old bars if necessary (to keep only the last 100 bars, for example)
        if (recentBars.size() > 100) {
            recentBars.remove(0);
        }

        // Update grid lines
        updateGridLines();
    }

    private void updateGridLines() {
        if (recentBars.isEmpty()) {
            return;
        }

        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        int minX = 0;  // Set to 0 as the starting point
        int maxX = barCounter;  // Set to barCounter as the ending point

        for (Bar bar : recentBars) {
            minY = Math.min(minY, bar.getLow());
            maxY = Math.max(maxY, bar.getHigh());
        }

        double intervalY = (maxY - minY) / 10;
        int intervalX = (maxX - minX) / 10;

        for (int i = 0; i < 10; i++) {
            horizontalGridLines.get(i).addPoint(minY + i * intervalY);
            verticalGridLines.get(i).addPoint(minY + i * intervalY); // Adjust as necessary for vertical lines
        }
    }

    @Override
    public long getInterval() {
        return Intervals.INTERVAL_15_SECONDS;
    }
}
