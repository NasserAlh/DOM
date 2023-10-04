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
import java.util.concurrent.ConcurrentSkipListMap;

@Layer1SimpleAttachable
@Layer1StrategyName("VP from VS Code")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class OnTrade implements TradeDataListener, CustomModule {

    private final ConcurrentSkipListMap<Double, Integer> volumeProfile = new ConcurrentSkipListMap<>();
    private VolumeProfilePanel volumeProfilePanel;
    private JFrame frame;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Volume Profile");
            volumeProfilePanel = new VolumeProfilePanel(volumeProfile);

            JScrollPane scrollPane = new JScrollPane(volumeProfilePanel);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

            frame.add(scrollPane);

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
        SwingUtilities.invokeLater(() -> volumeProfilePanel.updateVolumeProfile(new ConcurrentSkipListMap<>(volumeProfile)));
    }
}
