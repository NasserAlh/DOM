package VP1;

import velox.api.layer1.annotations.*;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.common.Log;
import velox.api.layer1.simplified.TradeDataListener;

import javax.swing.*;

import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;

@Layer1SimpleAttachable
@Layer1StrategyName("NullPointerException VP")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class OnTrade implements TradeDataListener, CustomModule {

    private final AtomicReference<ConcurrentSkipListMap<Double, Integer>> volumeProfileRef = new AtomicReference<>(new ConcurrentSkipListMap<>());
    private double pointOfControl = 0.0;

    private VolumeProfilePanel volumeProfilePanel;
    private JFrame frame;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        Runnable guiInitRunnable = () -> {
            frame = new JFrame("Volume Profile");
            volumeProfilePanel = new VolumeProfilePanel(volumeProfileRef.get());  // Pass the initial map to the panel
    
            // New code: Toggle buttons
            JCheckBox showPOC = new JCheckBox("Show POC", true); // Default is true
            showPOC.addActionListener(e -> {
                volumeProfilePanel.setShowPOC(showPOC.isSelected());
            });
            JCheckBox showValueArea = new JCheckBox("Show Value Area", true); // Default is true
            showValueArea.addActionListener(e -> {
                volumeProfilePanel.setShowValueArea(showValueArea.isSelected());
            });
    
            JPanel optionsPanel = new JPanel();
            optionsPanel.add(showPOC);
            optionsPanel.add(showValueArea);
    
            frame.add(optionsPanel, BorderLayout.NORTH);
    
            JScrollPane scrollPane = new JScrollPane(volumeProfilePanel);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    
            frame.add(scrollPane);
    
            frame.setSize(400, 400);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(true);
        };
    
        if (SwingUtilities.isEventDispatchThread()) {
            guiInitRunnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(guiInitRunnable);
            } catch (InterruptedException | InvocationTargetException e) {
                // Handle exceptions, for example, log them
                Log.error("Error initializing GUI", e);
            }
        }
    }
    

    @Override
    public void stop() {
        if (frame != null) {
            frame.dispose();
        }
    }

    @Override
    public synchronized void onTrade(double price, int size, TradeInfo tradeInfo) {
        // Create a new map with existing data
        ConcurrentSkipListMap<Double, Integer> newVolumeProfile = new ConcurrentSkipListMap<>(volumeProfileRef.get());
        // Update the new map with the new trade data
        newVolumeProfile.merge(price, size, (a, b) -> a + b);
        // Atomically update the reference to the new map
        volumeProfileRef.set(newVolumeProfile);

        // Check for empty data
        if (newVolumeProfile.isEmpty()) {
            Log.info("Volume Profile is empty.");
            return;
        }

        // Calculate Value Area based on 70% of volume
        ValueArea va = calculateValueArea(newVolumeProfile, 0.7);
    
        // Determine the point of control
        pointOfControl = newVolumeProfile.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(pointOfControl);
    
        // Update the panel with the new point of control
        volumeProfilePanel.setPointOfControl(pointOfControl);
    
        // Update the panel with the new volume profile data and value area bounds
        SwingUtilities.invokeLater(() -> {
            volumeProfilePanel.updateVolumeProfile(new ConcurrentSkipListMap<>(newVolumeProfile));
            volumeProfilePanel.setValueAreaBounds(va.lowerBound, va.upperBound);
        });
    }
    
    private ValueArea calculateValueArea(ConcurrentSkipListMap<Double, Integer> volumeProfile, double threshold) {
        double totalVolume = volumeProfile.values().stream().mapToDouble(Integer::doubleValue).sum();
        double targetVolume = totalVolume * threshold;

        List<Map.Entry<Double, Integer>> sortedEntries = new ArrayList<>(volumeProfile.entrySet());
        sortedEntries.sort(Map.Entry.<Double, Integer>comparingByValue().reversed());

        double accumulatedVolume = 0;
        double lower = Double.MAX_VALUE;
        double upper = Double.MIN_VALUE;

        for (Map.Entry<Double, Integer> entry : sortedEntries) {
            double price = entry.getKey();
            accumulatedVolume += entry.getValue();
            lower = Math.min(lower, price);
            upper = Math.max(upper, price);
            if (accumulatedVolume >= targetVolume) {
                break;
            }
        }

        return new ValueArea(lower, upper);
    }

    private static class ValueArea {
        public final double lowerBound;
        public final double upperBound;

        public ValueArea(double lowerBound, double upperBound) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }
    }
}

