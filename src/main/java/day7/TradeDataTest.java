package day7;

import velox.api.layer1.annotations.*;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.common.Log;
import velox.api.layer1.simplified.TradeDataListener;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

@Layer1SimpleAttachable
@Layer1StrategyName("VP ------------------------VP")
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
        SwingUtilities.invokeLater(() -> volumeProfilePanel.updateVolumeProfile
                (new ConcurrentHashMap<>(volumeProfile)));
    }
}

class VolumeProfilePanel extends JPanel {
    private static final int START_X = 50;
    private static final int START_Y = 10;
    private static final int BAR_HEIGHT = 20;
    private static final int PADDING = 50;

    private ConcurrentHashMap<Double, Integer> volumeProfile;

    public VolumeProfilePanel(ConcurrentHashMap<Double, Integer> volumeProfile) {
        this.volumeProfile = volumeProfile;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int x = START_X;
        int y = START_Y;

        int maxVolume = volumeProfile.values().stream().max(Integer::compare).orElse(1);
        int maxBarWidth = this.getWidth() - x - PADDING;

        for (Double price : volumeProfile.keySet()) {
            int rawWidth = volumeProfile.get(price);
            int normalizedWidth = (int) ((rawWidth / (double) maxVolume) * maxBarWidth);

            drawBar(g, x, y, normalizedWidth, rawWidth);

            double normalizedPrice = price * 0.25;
            drawPriceLabel(g, x, y, normalizedPrice);

            y += BAR_HEIGHT + 10;
        }
    }

    private void drawBar(Graphics g, int x, int y, int normalizedWidth, int rawWidth) {
        g.setColor(Color.BLUE);
        g.fillRect(x, y, normalizedWidth, BAR_HEIGHT);

        String volumeText = String.valueOf(rawWidth);
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(volumeText);
        int textX = x + (normalizedWidth - textWidth) / 2;
        int textY = y + (BAR_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();

        g.setColor(Color.WHITE);
        g.drawString(volumeText, textX, textY);
    }

    private void drawPriceLabel(Graphics g, int x, int y, double normalizedPrice) {
        g.setColor(Color.BLACK);
        g.drawString(String.format("%.2f", normalizedPrice), x - 40, y + BAR_HEIGHT / 2 + 5);
    }

    public void updateVolumeProfile(ConcurrentHashMap<Double, Integer> newVolumeProfile) {
        this.volumeProfile = newVolumeProfile;
        repaint();
    }
}
