package VP1;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;

public class VolumeProfilePanel extends JPanel {
    private static final int START_X = 50;
    private static final int START_Y = 10;
    private static final int BAR_HEIGHT = 20;
    private static final int PADDING = 50;
    private ConcurrentSkipListMap<Double, Integer> volumeProfile;
    private double zoomFactor = 1.0;
    private static final double ZOOM_INCREMENT = 0.1;
    private double lowerBound = 0.0;
    private double upperBound = 0.0;
    private double pointOfControl = 0.0;
    private boolean showPOC = true; // default
    private boolean showValueArea = true; // default


    public VolumeProfilePanel(ConcurrentSkipListMap<Double, Integer> volumeProfile) {
        this.volumeProfile = volumeProfile;
        setPreferredSize(new Dimension(400, (int)((BAR_HEIGHT + 10) * volumeProfile.size() * zoomFactor)));

        addMouseWheelListener(e -> {
            if (e.getWheelRotation() < 0) {
                zoomFactor += ZOOM_INCREMENT;
            } else {
                zoomFactor = Math.max(0.7, zoomFactor - ZOOM_INCREMENT);
            }
            repaint();
        });
    }
    public void setShowPOC(boolean show) {
        this.showPOC = show;
        repaint();
    }
    
    public void setShowValueArea(boolean show) {
        this.showValueArea = show;
        repaint();
    }

    public void setValueAreaBounds(double lowerBound, double upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        repaint();
    }

    public void setPointOfControl(double poc) {
        this.pointOfControl = poc;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    
        List<Double> sortedPrices = new ArrayList<>(volumeProfile.keySet());
        Collections.sort(sortedPrices, Collections.reverseOrder());
    
        int xForBars = START_X + 20;
        int xForPrice = START_X;
        int y = (int)(START_Y * zoomFactor);

        int maxVolume = volumeProfile.values().stream().max((a, b) -> a.compareTo(b)).orElse(1);
        int maxBarWidth = this.getWidth() - xForBars - PADDING;
    
        for (Double price : sortedPrices) {
            int rawWidth = volumeProfile.get(price);
            int normalizedWidth = (int) ((rawWidth / (double) maxVolume) * maxBarWidth);
    
            // Check if price is within the value area
            if (price >= lowerBound && price <= upperBound && showValueArea) {
                g.setColor(new Color(255, 165, 0)); // using an orange color for illustration
            } else {
                float ratio = rawWidth / (float) maxVolume;
                Color gradientColor = new Color(0, 0, (int)(255 * ratio));
                g.setColor(gradientColor);
            }
    
            g.fillRect(xForBars, y, normalizedWidth, BAR_HEIGHT);
    
            String volumeText = String.valueOf(rawWidth);
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(volumeText);
            int textX = xForBars + (normalizedWidth - textWidth) / 2;
            int textY = y + (BAR_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
    
            g.setColor(Color.WHITE);
            g.drawString(volumeText, textX, textY);
    
            g.setColor(Color.BLACK);
            g.fillRect(xForPrice - 50, y, 45, BAR_HEIGHT);
    
            g.setColor(Color.WHITE);
            g.drawString(String.format("%.2f", price * 0.25), xForPrice - 40, y + BAR_HEIGHT / 2 + 5);
    
            y += (int)((BAR_HEIGHT + 10) * zoomFactor);
        }
    
        // After all bars are drawn
        if (showPOC) {
            Double pocPrice = getPocPrice();
            if (pocPrice != null) {
                g.setColor(Color.RED); // or any color you prefer for POC
                int pocY = (int)(START_Y * zoomFactor) + (int)((sortedPrices.indexOf(pocPrice) * (BAR_HEIGHT + 10)) * zoomFactor);
                g.fillRect(xForBars, pocY, maxBarWidth, BAR_HEIGHT);
            }
        }
    }

    private Double getPocPrice() {
        return volumeProfile.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    public void updateVolumeProfile(ConcurrentSkipListMap<Double, Integer> newVolumeProfile) {
        this.volumeProfile = newVolumeProfile;
        setPreferredSize(new Dimension(400, (int)((BAR_HEIGHT + 10) * volumeProfile.size() * zoomFactor)));
        revalidate();
        repaint();
    }
}
