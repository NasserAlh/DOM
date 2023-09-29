package day6;


import velox.api.layer1.simplified.DepthDataListener;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.annotations.*;
import velox.api.layer1.common.Log;

import javax.swing.table.DefaultTableCellRenderer;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.table.DefaultTableModel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.*;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import java.awt.*;

@Layer1SimpleAttachable
@Layer1StrategyName("Dom With + Volume Profile 6")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)

public class DomWithVp implements CustomModule, DepthDataListener {
    private final ConcurrentSkipListMap<Integer, Integer> bids = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    private final ConcurrentSkipListMap<Integer, Integer> volumeProfile = new ConcurrentSkipListMap<>();
    private final ConcurrentSkipListMap<Integer, Integer> asks = new ConcurrentSkipListMap<>();
    private final BlockingQueue<DepthData> depthDataQueue = new LinkedBlockingQueue<>();
    private final ReentrantLock bidsLock = new ReentrantLock();
    private final ReentrantLock asksLock = new ReentrantLock();
    private static final long UPDATE_INTERVAL = 100;
    private VolumeProfilePanel volumeProfilePanel;
    private volatile boolean uiInitialized = false;
    private volatile boolean dataChanged = false;
    private ExecutorService executorService;
    private DefaultTableModel tableModel;
    private long lastUpdateTime = 0;
    private JTable table;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        Log.info("Initialize called.");
        initUI();  // Initialize the UI first
        executorService = Executors.newSingleThreadExecutor();
        startBatchProcessing();
    }

    private static class DepthData {
        boolean isBid;
        int price;
        int size;

        DepthData(boolean isBid, int price, int size) {
            this.isBid = isBid;
            this.price = price;
            this.size = size;
        }
    }

    public static class CustomTableCellRenderer extends DefaultTableCellRenderer {
        public CustomTableCellRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);  // Align columns in the middle
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean
                hasFocus, int row, int column) {
            Component cellComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);

            if (column == 0) { // Bid column
                cellComponent.setBackground(Color.decode("#5072A7"));  // Custom Blue
                cellComponent.setForeground(Color.WHITE);
            } else if (column == 2) { // Ask column
                cellComponent.setBackground(Color.decode("#58111A"));  // Custom Blue
                cellComponent.setForeground(Color.WHITE);
            } else {
                cellComponent.setBackground(table.getBackground());
                cellComponent.setForeground(table.getForeground());
            }

            return cellComponent;
        }
    }

    public class VolumeProfilePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int panelHeight = getHeight();
            int panelWidth = getWidth();
            int maxVolume = volumeProfile.values().stream().max(Integer::compare).orElse(1);

            for (Map.Entry<Integer, Integer> entry : volumeProfile.entrySet()) {
                int price = entry.getKey();
                int volume = entry.getValue();

                // Normalize the volume to fit within the panel
                int rectWidth = (int) ((double) volume / maxVolume * panelWidth);

                // Assuming price is in a reasonable range, normalize it to fit within the panel
                int rectHeight = 10;  // Height of each rectangle
                int yPos = panelHeight - (price % panelHeight);  // Y-position based on price

                g.fillRect(0, yPos, rectWidth, rectHeight);
            }
        }
    }

    private void startBatchProcessing() {
        executorService.submit(() -> {
            try {
                while (true) {
                    processBatchData();
                    throttleBatchProcessing();
                }
            } catch (Exception e) {
                Log.info("Exception in startBatchProcessing: " + e.getMessage());
            }
        });
    }

    private void processBatchData() {
        List<DepthData> batch = new ArrayList<>();
        depthDataQueue.drainTo(batch);
        for (DepthData data : batch) {
            updateBook(data);
        }
        if (!batch.isEmpty()) {
            dataChanged = true;
            updateDOM();
        }
    }

    private void updateBook(DepthData data) {
        ReentrantLock lock = data.isBid ? bidsLock : asksLock;
        lock.lock();
        try {

            ConcurrentSkipListMap<Integer, Integer> book = data.isBid ? bids : asks;
            if (data.size == 0) {
                book.remove(data.price);
            } else {
                book.put(data.price, data.size);
            }
            // Update the volume profile within the lock
            volumeProfile.put(data.price, (int) (Math.random() * 100));  // Mockup volume data

        } finally {
            lock.unlock();
        }
    }

    private void throttleBatchProcessing() {
        try {
            TimeUnit.MILLISECONDS.sleep(UPDATE_INTERVAL);
        } catch (InterruptedException e) {
            Log.info("Batch processing thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private void initUI() {
        try {
            SwingUtilities.invokeLater(() -> {
                try {
                    JFrame frame = new JFrame("Depth of Market");
                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    frame.setSize(800, 400);  // Increased size to accommodate the VolumeProfilePanel

                    String[] columnNames = {"Bid", "Price", "Ask"};
                    tableModel = new DefaultTableModel(null, columnNames);
                    table = new JTable(tableModel);

                    // Set the custom cell renderer
                    table.setDefaultRenderer(Object.class, new CustomTableCellRenderer());

                    // Add the table to the frame
                    frame.add(new JScrollPane(table), BorderLayout.CENTER);

                    // Create and add the VolumeProfilePanel
                    volumeProfilePanel = new VolumeProfilePanel();
                    volumeProfilePanel.setPreferredSize(new Dimension(200, 400));  // Set preferred size here
                    frame.add(volumeProfilePanel, BorderLayout.EAST);

                    frame.setVisible(true);

                    // Set the flag to true after UI is initialized
                    uiInitialized = true;

                    Log.info("UI successfully initialized.");
                } catch (Exception e) {
                    Log.info("Exception while initializing UI: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.info("Exception in initUI: " + e.getMessage());
        }
    }

    @Override
    public void onDepth(boolean isBid, int price, int size) {
        // Only proceed if the UI has been initialized
        if (!uiInitialized) {
            return;
        }

        try {
            // Try to offer the new DepthData to the queue
            boolean offered = depthDataQueue.offer(new DepthData(isBid, price, size));

            // If the queue is full, you might want to handle it here, e.g., by logging it at a debug level
            if (!offered) {
                // Log at debug level or handle as you see fit
            }
        } catch (Exception e) {
            // Handle the exception and maybe log it at an error level
            Log.info("Exception in onDepth: " + e.getMessage());
        }
    }

    private void updateDOM() {
        // Check if the UI has been initialized
        if (!uiInitialized) {
            Log.info("UI not initialized yet, skipping updateDOM.");
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime > UPDATE_INTERVAL && dataChanged) {
            SwingUtilities.invokeLater(this::refreshTableData);
            lastUpdateTime = currentTime;
            dataChanged = false;
        }

        // Null check before dereferencing
        if (volumeProfilePanel != null) {
            volumeProfilePanel.repaint();
        } else {
            Log.info("volumeProfilePanel is null, skipping repaint.");
        }
    }

    private void refreshTableData() {
        int maxRows = Math.max(bids.size(), asks.size());
        Object[][] data = new Object[maxRows][3];

        int midRow = maxRows / 2;
        populateBidData(data, midRow);
        populateAskData(data, midRow);

        tableModel.setDataVector(data, new Object[]{"Bid", "Price", "Ask"});
    }

    private void populateBidData(Object[][] data, int midRow) {
        int i = 0;
        for (Integer price : bids.keySet()) {
            int targetRow = midRow + i;  // Changed from midRow - i - 1
            if (targetRow >= data.length) break;  // Changed from targetRow < 0
            data[targetRow][0] = bids.get(price);
            data[targetRow][1] = price * 0.25;
            i++;
        }
    }

    private void populateAskData(Object[][] data, int midRow) {
        int i = 0;
        for (Integer price : asks.keySet()) {
            int targetRow = midRow - i - 1;  // Changed from midRow + i
            if (targetRow < 0) break;  // Changed from targetRow >= data.length
            data[targetRow][2] = asks.get(price);
            if (data[targetRow][1] == null) {
                data[targetRow][1] = price * 0.25;
            }
            i++;
        }
    }

    @Override
    public void stop() {
        try {
            if (executorService != null) {
                executorService.shutdown();
            }
        } catch (Exception e) {
            Log.info("Exception in stop: " + e.getMessage());
        }
    }
}
