package day5;

import velox.api.layer1.common.Log;
import velox.api.layer1.simplified.DepthDataListener;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.annotations.*;

import java.util.concurrent.locks.ReentrantLock;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import java.awt.*;

@Layer1SimpleAttachable
@Layer1StrategyName("Multi Threaded Dom Color ")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class MultiThreadedDom implements CustomModule, DepthDataListener {
    private final ConcurrentSkipListMap<Integer, Integer> bids = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    private final ConcurrentSkipListMap<Integer, Integer> asks = new ConcurrentSkipListMap<>();
    private final BlockingQueue<DepthData> depthDataQueue = new LinkedBlockingQueue<>();
    private final ReentrantLock bidsLock = new ReentrantLock();
    private final ReentrantLock asksLock = new ReentrantLock();
    private static final long UPDATE_INTERVAL = 100;
    private volatile boolean dataChanged = false;
    private ExecutorService executorService;
    private DefaultTableModel tableModel;
    private long lastUpdateTime = 0;
    private JTable table;

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
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean
                hasFocus, int row, int column) {
            Component cellComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);

            if (column == 0) { // Bid column
                cellComponent.setBackground(Color.BLUE);
                cellComponent.setForeground(Color.WHITE);
            } else if (column == 2) { // Ask column
                cellComponent.setBackground(Color.RED);
                cellComponent.setForeground(Color.WHITE);
            } else {
                cellComponent.setBackground(table.getBackground());
                cellComponent.setForeground(table.getForeground());
            }

            return cellComponent;
        }
    }

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        executorService = Executors.newSingleThreadExecutor();
        initUI();
        startBatchProcessing();
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
                JFrame frame = new JFrame("Depth of Market");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setSize(300, 200);

                String[] columnNames = {"Bid", "Price", "Ask"};
                tableModel = new DefaultTableModel(null, columnNames);
                table = new JTable(tableModel);

                // Set the custom cell renderer
                table.setDefaultRenderer(Object.class, new CustomTableCellRenderer());

                frame.add(new JScrollPane(table), BorderLayout.CENTER);
                frame.setVisible(true);
            });
        } catch (Exception e) {
            Log.info("Exception in initUI: " + e.getMessage());
        }
    }

    @Override
    public void onDepth(boolean isBid, int price, int size) {
        try {
            boolean offered = depthDataQueue.offer(new DepthData(isBid, price, size));
            if (!offered) {
                Log.info("Queue is full, data could not be added.");
            }
        } catch (Exception e) {
            Log.info("Exception in onDepth: " + e.getMessage());
        }
    }

    private void updateDOM() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime > UPDATE_INTERVAL && dataChanged) {
            SwingUtilities.invokeLater(this::refreshTableData);
            lastUpdateTime = currentTime;
            dataChanged = false;
        }
    }

    private void refreshTableData() {
        int maxRows = Math.max(bids.size(), asks.size());
        Object[][] data = new Object[maxRows][3];

        populateBidData(data, maxRows);
        populateAskData(data, maxRows);

        tableModel.setDataVector(data, new Object[]{"Bid", "Price", "Ask"});
    }

    private void populateBidData(Object[][] data, int maxRows) {
        int i = 0;
        for (Integer price : bids.keySet()) {
            if (i >= maxRows) break;
            data[i][0] = bids.get(price);
            data[i][1] = price * 0.25;
            i++;
        }
    }

    private void populateAskData(Object[][] data, int maxRows) {
        int i = 0;
        for (Integer price : asks.keySet()) {
            if (i >= maxRows) break;
            data[i][2] = asks.get(price);
            if (data[i][1] == null) {
                data[i][1] = price * 0.25;
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
