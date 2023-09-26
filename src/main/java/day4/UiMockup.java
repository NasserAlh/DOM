package day4;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.DepthDataListener;
import velox.api.layer1.simplified.InitialState;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListMap;

@Layer1SimpleAttachable
@Layer1StrategyName("Ui Mockup Armor")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class UiMockup implements CustomModule, DepthDataListener {

    private final HashMap<Integer, Integer> volumeProfile = new HashMap<>();  // Initialize volumeProfile

    private final ConcurrentSkipListMap<Integer, Integer> bids = new ConcurrentSkipListMap<>();
    private final ConcurrentSkipListMap<Integer, Integer> asks = new ConcurrentSkipListMap<>();
    private DefaultTableModel tableModel;
    private JPanel volumeProfilePanel;
    private JPanel marketDepthPanel;
    private final ArrayList<DepthLevel> marketDepth = new ArrayList<>();
    private final ArrayList<DepthLevel> marketDepthCopy = new ArrayList<>();
    private final Object lock = new Object();

    public static class DepthLevel {
        int price;
        int bidDepth;
        int askDepth;
    }


    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        Log.info("Hello");

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Concurrent DOM");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            // Initialize the table model and JTable
            tableModel = new DefaultTableModel(new Object[]{"Price", "Size"}, 0);
            JTable priceLadderTable = new JTable(tableModel);
            frame.add(new JScrollPane(priceLadderTable), BorderLayout.WEST);

            volumeProfilePanel = new JPanel() {
                @Override
                public void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    int x = 10;
                    for (Integer price : volumeProfile.keySet()) {
                        int volume = volumeProfile.get(price);
                        g.fillRect(x, getHeight() - volume, 10, volume);
                        x += 15;
                    }
                }
            };
            frame.add(volumeProfilePanel, BorderLayout.CENTER);

            marketDepthPanel = new JPanel() {
                @Override
                public void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    int x = 10;
                    ArrayList<DepthLevel> localCopy;
                    synchronized (lock) {
                        localCopy = new ArrayList<>(marketDepthCopy);
                    }
                    for (DepthLevel depth : localCopy) {
                        g.setColor(Color.BLUE);
                        g.fillRect(x, getHeight() - depth.bidDepth, 10, depth.bidDepth);
                        g.setColor(Color.RED);
                        g.fillRect(x + 15, getHeight() - depth.askDepth, 10, depth.askDepth);
                        x += 30;
                    }
                }
            };
            frame.add(marketDepthPanel, BorderLayout.EAST);

            frame.pack();
            frame.setVisible(true);
        });
    }

    @Override
    public void onDepth(boolean isBid, int price, int size) {
        ConcurrentSkipListMap<Integer, Integer> book = isBid ? bids : asks;
        if (size == 0) {
            book.remove(price);
        } else {
            book.put(price, size);
        }

        // Update volumeProfile
        volumeProfile.put(price, size);

        // Update marketDepth
        DepthLevel depthLevel = null;
        for (DepthLevel existing : marketDepth) {
            if (existing.price == price) {
                depthLevel = existing;
                break;
            }
        }
        if (depthLevel == null) {
            depthLevel = new DepthLevel();
            depthLevel.price = price;
            marketDepth.add(depthLevel);
        }

        if (isBid) {
            depthLevel.bidDepth = size;
        } else {
            depthLevel.askDepth = size;
        }

        synchronized (lock) {
            ArrayList<DepthLevel> newMarketDepthCopy = new ArrayList<>(marketDepth);
            SwingUtilities.invokeLater(() -> {
                synchronized (lock) {
                    marketDepthCopy.clear();
                    marketDepthCopy.addAll(newMarketDepthCopy);

                    tableModel.setRowCount(0);
                    for (Integer p : bids.keySet()) {
                        tableModel.addRow(new Object[]{p, bids.get(p)});
                    }
                    for (Integer p : asks.keySet()) {
                        tableModel.addRow(new Object[]{p, asks.get(p)});
                    }

                    if (volumeProfilePanel != null && marketDepthPanel != null) {
                        volumeProfilePanel.repaint();
                        marketDepthPanel.repaint();
                    }
                }
            });
        }
    }

    @Override
    public void stop() {
        Log.info("Bye");
    }
}
