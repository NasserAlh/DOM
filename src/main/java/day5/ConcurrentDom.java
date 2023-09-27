package day5;

import velox.api.layer1.annotations.*;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.DepthDataListener;
import velox.api.layer1.simplified.InitialState;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListMap;

@Layer1SimpleAttachable
@Layer1StrategyName("Perfecting Concurrent Dom")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class ConcurrentDom implements CustomModule, DepthDataListener {
    private final ConcurrentSkipListMap<Integer, Integer> bids = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    private final ConcurrentSkipListMap<Integer, Integer> asks = new ConcurrentSkipListMap<>();
    private JTable table;
    private DefaultTableModel tableModel;
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 100; // in milliseconds
    private volatile boolean dataChanged = false;  // Flag to indicate data change

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        initUI();
    }

    @Override
    public void stop() {
        // Cleanup logic here if needed
    }

    private void initUI() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Depth of Market");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(300, 200);

            String[] columnNames = {"Bid", "Price", "Ask"};
            tableModel = new DefaultTableModel(null, columnNames);
            table = new JTable(tableModel);

            frame.add(new JScrollPane(table), BorderLayout.CENTER);
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
        dataChanged = true;  // Set the flag to true as data has changed
        updateDOM();
    }

    private void updateDOM() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime > UPDATE_INTERVAL && dataChanged) {
            SwingUtilities.invokeLater(this::refreshTableData);
            lastUpdateTime = currentTime;
            dataChanged = false;  // Reset the flag after updating the UI
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
}
