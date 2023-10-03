package volume.profile;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

public class VolumeProfilePanel extends JPanel {
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

            g.setColor(Color.BLUE);
            g.fillRect(x, y, normalizedWidth, BAR_HEIGHT);

            String volumeText = String.valueOf(rawWidth);
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(volumeText);
            int textX = x + (normalizedWidth - textWidth) / 2;
            int textY = y + (BAR_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();

            g.setColor(Color.WHITE);
            g.drawString(volumeText, textX, textY);

            g.setColor(Color.BLACK);
            g.drawString(String.format("%.2f", price * .25), x - 40, y + BAR_HEIGHT / 2 + 5);

            y += BAR_HEIGHT + 10;
        }
    }

    public void updateVolumeProfile(ConcurrentHashMap<Double, Integer> newVolumeProfile) {
        this.volumeProfile = newVolumeProfile;
        repaint();
    }
}
