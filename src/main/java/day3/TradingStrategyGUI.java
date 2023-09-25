package day3;

import javax.swing.*;
import java.awt.*;

import velox.api.layer1.annotations.*;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.CustomSettingsPanelProvider;
import velox.api.layer1.simplified.InitialState;
import velox.gui.StrategyPanel;

@Layer1SimpleAttachable
@Layer1StrategyName("Trading Strategy GUI")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)

public class TradingStrategyGUI implements CustomModule, CustomSettingsPanelProvider {

    private Api api;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        this.api = api;
        // Initialize your indicators and settings here
    }

    @Override
    public StrategyPanel[] getCustomSettingsPanels() {
        // Create your custom settings panels here

        // Panel for adjusting risk level
        StrategyPanel riskPanel = new StrategyPanel("Risk Level");
        JSpinner riskSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        riskPanel.add(riskSpinner);

        // Panel for selecting trading strategy
        StrategyPanel strategyPanel = new StrategyPanel("Select Strategy");
        JComboBox<String> strategyBox = new JComboBox<>(new String[] {"Strategy1", "Strategy2", "Strategy3"});
        strategyPanel.add(strategyBox);

        // Panel for adjusting some threshold
        StrategyPanel thresholdPanel = new StrategyPanel("Threshold");
        JSlider thresholdSlider = new JSlider(0, 100, 50);
        thresholdPanel.add(thresholdSlider);

        // Return an array of StrategyPanel objects
        return new StrategyPanel[] { riskPanel, strategyPanel, thresholdPanel };
    }

    public static StrategyPanel[] getCustomDisabledSettingsPanels() {
        // Create a disabled version of your UI with default values
        // ...

        // Return an array of StrategyPanel objects
        return new StrategyPanel[] { /* your panels */ };
    }

    @Override
    public void stop() {
        // Cleanup code here
    }

}
