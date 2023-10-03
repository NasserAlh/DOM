package volume.profile;

import velox.api.layer1.annotations.*;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.common.Log;
import velox.api.layer1.simplified.TradeDataListener;
import javax.swing.*;
import java.util.concurrent.ConcurrentHashMap;

@Layer1SimpleAttachable
@Layer1StrategyName("VP ZEG")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class TradeDataTest implements TradeDataListener, CustomModule {

    private final ConcurrentHashMap<Double, Integer> volumeProfile = new ConcurrentHashMap<>();
    private VolumeProfilePanel volumeProfilePanel;
    private JFrame frame;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Volume Profile");
            volumeProfilePanel = new VolumeProfilePanel(volumeProfile);
            frame.add(volumeProfilePanel);
            frame.setSize(400, 400);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(true);
        });
    }

    @Override
    public void stop() {
        if (frame != null) {
            frame.dispose();
        }
    }

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        volumeProfile.merge(price, size, Integer::sum);
        Log.info("Updated Volume Profile: " + volumeProfile.toString());
        SwingUtilities.invokeLater(() -> volumeProfilePanel.updateVolumeProfile(new ConcurrentHashMap<>(volumeProfile)));
    }
}
