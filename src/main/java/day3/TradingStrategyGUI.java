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
        // Use JSpinners, JComboBoxes, etc., to allow users to adjust parameters
        // ...

        // Return an array of StrategyPanel objects
        return new StrategyPanel[] { /* your panels */ };
    }

    public static StrategyPanel[] getCustomDisabledSettingsPanels() {
        // Create a disabled version of your UI with default values
        // ...

        // Return an array of StrategyPanel objects
        return new StrategyPanel[] { /* your panels */ };
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException("Unimplemented method 'stop'");
    }
    
}
