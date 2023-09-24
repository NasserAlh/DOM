package day1;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import velox.api.layer1.annotations.*;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.simplified.*;

@Layer1SimpleAttachable
@Layer1StrategyName("DOM Creation 2")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class DOM implements CustomModuleAdapter, DepthDataListener, TimeListener {

    private final TreeMap<Integer, Integer> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Integer, Integer> asks = new TreeMap<>();
    private String formattedTimestamp;
    private final SimpleDateFormat dateFormat;
    private static boolean isHeaderWritten = false;

    public DOM() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-4")); // Set time zone to EDT (UTC-4)
    }

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        // Initialize the file with headers
        writeDataToFile(null, null);
    }

    @Override
    public void onDepth(boolean isBid, int price, int size) {
        // Update the order book based on the new depth data received
        TreeMap<Integer, Integer> book = isBid ? bids : asks;

        if (size == 0) {
            book.remove(price);
        } else {
            book.put(price, size);
        }

        // Write the current state of the order book to the file
        writeDataToFile(bids, asks);

        // Display the DOM
        displayDOM();
    }

    private void writeDataToFile(TreeMap<Integer, Integer> bidData, TreeMap<Integer, Integer> askData) {
        try (FileWriter writer = new FileWriter("C:\\Bookmap\\Logs\\MarketDepthData1.csv", true)) {
            if (!isHeaderWritten) {
                writer.append("Price,Bid Size,Ask Size\n");
                isHeaderWritten = true;
            }
    
            if (bidData != null && askData != null) {
                TreeSet<Integer> allPrices = new TreeSet<>();
                allPrices.addAll(bidData.keySet());
                allPrices.addAll(askData.keySet());
    
                for (Integer price : allPrices.descendingSet()) {
                    int bidSize = bidData.getOrDefault(price, 0);
                    int askSize = askData.getOrDefault(price, 0);
    
                    if (bidSize > 0 || askSize > 0) {
                        writer.append(String.valueOf(price));
                        writer.append(',');
                        writer.append(String.valueOf(bidSize));
                        writer.append(',');
                        writer.append(String.valueOf(askSize));
                        writer.append('\n');
                    }
                }
            }
        } catch (IOException e) {
            Log.error("Error writing to CSV file: " + e.getMessage());
        }
    }

    private void displayDOM() {
        Log.info("Depth of Market (DOM)");
        Log.info("---------------------");
        Log.info("Price\tBid Size\tAsk Size");

        TreeSet<Integer> allPrices = new TreeSet<>();
        allPrices.addAll(bids.keySet());
        allPrices.addAll(asks.keySet());

        for (Integer price : allPrices.descendingSet()) {
            int bidSize = bids.getOrDefault(price, 0);
            int askSize = asks.getOrDefault(price, 0);
            System.out.println(price + "\t" + bidSize + "\t" + askSize);
        }
    }
    
    @Override
    public void onTimestamp(long nanoseconds) {
        // Update the timestamp and formatted timestamp based on the new timestamp received
        Date date = new Date(nanoseconds / 1000000); // Convert nanoseconds to milliseconds
        formattedTimestamp = dateFormat.format(date); // Format date to a readable string format
    }
}
