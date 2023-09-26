package day4;

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
import java.util.TreeMap;

@Layer1SimpleAttachable
@Layer1StrategyName("DOM UI")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class DomUi implements CustomModule, DepthDataListener {
    private final TreeMap<Integer, Integer> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Integer, Integer> asks = new TreeMap<>();
    private JTable table;
    private DefaultTableModel tableModel;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        initUI();
    }

    @Override
    public void stop() {
        // Add resource cleanup logic here if needed
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
        TreeMap<Integer, Integer> book = isBid ? bids : asks;
        synchronized (book) {
            if (size == 0) {
                book.remove(price);
            } else {
                book.put(price, size);
            }
        }
        updateDOM();
    }

    private void updateDOM() {
        SwingUtilities.invokeLater(() -> {
            TreeMap<Integer, Integer> bidsCopy;
            TreeMap<Integer, Integer> asksCopy;

            synchronized (bids) {
                bidsCopy = new TreeMap<>(bids);
            }

            synchronized (asks) {
                asksCopy = new TreeMap<>(asks);
            }

            Object[][] data = prepareDataForDOM(bidsCopy, asksCopy);
            tableModel.setDataVector(data, new Object[]{"Bid", "Price", "Ask"});
        });
    }

    private Object[][] prepareDataForDOM(TreeMap<Integer, Integer> bidsCopy, TreeMap<Integer, Integer> asksCopy) {
        Object[][] data = new Object[Math.max(bidsCopy.size(), asksCopy.size())][3];
        populateData(bidsCopy, data, 0, 1);
        populateData(asksCopy, data, 2, 1);
        return data;
    }

    private void populateData(TreeMap<Integer, Integer> book, Object[][] data, int sizeIndex, int priceIndex) {
        int i = 0;
        for (Integer price : book.keySet()) {
            data[i][sizeIndex] = book.get(price);
            if (data[i][priceIndex] == null) {
                data[i][priceIndex] = price;
            }
            i++;
        }
    }
}
