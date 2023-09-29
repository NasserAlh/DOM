package day9;


import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.simplified.*;

import com.google.common.util.concurrent.RateLimiter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

@Layer1SimpleAttachable
@Layer1StrategyName("Nasser Dom Day9")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)

public class NasserDom implements CustomModule, DepthDataListener, TradeDataListener {
    private final ConcurrentSkipListMap<Integer, Integer> bids = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    private final ConcurrentSkipListMap<Integer, Integer> volumeProfile = new ConcurrentSkipListMap<>();
    private final ConcurrentSkipListMap<Integer, Integer> asks = new ConcurrentSkipListMap<>();
    private final BlockingQueue<DepthData> depthDataQueue = new LinkedBlockingQueue<>();
    private final ReentrantLock bidsLock = new ReentrantLock();
    private final ReentrantLock asksLock = new ReentrantLock();
    private static final long UPDATE_INTERVAL = 100;
    private volatile boolean uiInitialized = false;
    private volatile boolean dataChanged = false;
    private ExecutorService executorService;
    private DefaultTableModel tableModel;
    private RateLimiter rateLimiter;
    private long lastUpdateTime = 0;
    private JTable table;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        Log.info("Initialize called.");
        initUI();
        executorService = Executors.newSingleThreadExecutor();
        rateLimiter = RateLimiter.create(10.0);  // 10 operations per second
        startBatchProcessing();
    }

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        int priceInTicks = (int) (price / 0.25);  // Assuming 1 tick = 0.25, adjust as needed
        volumeProfile.merge(priceInTicks, size, Integer::sum);
        dataChanged = true;
        updateDOM();
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
        } finally {
            lock.unlock();
        }
    }

    private void throttleBatchProcessing() {
        rateLimiter.acquire();  // This will throttle the rate based on the RateLimiter settings
    }

    private void initUI() {
        try {
            SwingUtilities.invokeLater(this::createUI);  // Point 2: Call createUI here
        } catch (Exception e) {
            Log.info("Exception in initUI: " + e.getMessage());
        }
    }

    private void createUI() {  // Point 1: Extracted UI creation logic
        JFrame frame = new JFrame("Depth of Market");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 400);  // Increased size to accommodate the VolumeProfilePanel

        // Add a "VP" column for Volume Profile
        String[] columnNames = {"Bid", "Price", "Ask", "VP"};
        tableModel = new DefaultTableModel(null, columnNames);
        table = new JTable(tableModel);

        // Set the custom cell renderer
        table.setDefaultRenderer(Object.class, new CustomTableCellRenderer());

        // Add the table to the frame
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        frame.setVisible(true);

        // Set the flag to true after UI is initialized
        uiInitialized = true;

        Log.info("UI successfully initialized.");
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
            // Update the table data, which now includes VP data
            SwingUtilities.invokeLater(this::refreshTableData);
            lastUpdateTime = currentTime;
            dataChanged = false;
        }
    }

    private void refreshTableData() {
        int maxRows = Math.max(bids.size(), asks.size());
        Object[][] data = new Object[maxRows][4];  // Added a new column for VP

        int midRow = maxRows / 2;
        populateBidData(data, midRow);
        populateAskData(data, midRow);
        populateVolumeProfileData(data, midRow);  // New method to populate VP data

        tableModel.setDataVector(data, new Object[]{"Bid", "Price", "Ask", "VP"});  // Added "VP" as a new column header
    }

    private void populateVolumeProfileData(Object[][] data, int midRow) {
        for (Map.Entry<Integer, Integer> entry : volumeProfile.entrySet()) {
            int priceInTicks = entry.getKey();
            int volume = entry.getValue();
            double price = priceInTicks * 0.25;  // Convert back to price

            // Find the row where this price is displayed
            int targetRow = findRowByPrice((int) price, midRow, data);
            if (targetRow != -1) {
                data[targetRow][3] = volume;  // Update the "VP" column
            }
        }
    }


    private void populateBidData(Object[][] data, int midRow) {
        int i = 0;
        for (Integer price : bids.keySet()) {
            int targetRow = midRow + i;  // Start from the middle row and go downwards
            if (targetRow >= data.length) break;
            data[targetRow][0] = bids.get(price);
            data[targetRow][1] = price * 0.25;  // Assuming price is in ticks and 1 tick = 0.25
            i++;
        }
    }

    private void populateAskData(Object[][] data, int midRow) {
        int i = 0;
        for (Integer price : asks.keySet()) {
            int targetRow = midRow - i - 1;  // Start from one row above the middle row and go upwards
            if (targetRow < 0) break;
            data[targetRow][2] = asks.get(price);
            data[targetRow][1] = price * 0.25;
            i++;
        }
    }

    // Custom method to find the row by price
    private int findRowByPrice(int price, int midRow, Object[][] data) {
        for (int i = 0; i < data.length; i++) {
            if (data[i][1] != null && (Double) data[i][1] == price * 0.25) {
                return i;
            }
        }
        return -1;  // Return -1 if the price is not found
    }

    @Override
    public void stop() {
        try {
            if (executorService != null) {
                executorService.shutdown();
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            Log.info("Exception in stop: " + e.getMessage());
        }
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
}
