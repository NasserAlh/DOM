package day1;


import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Comparator;

import java.util.Date;
import java.util.TimeZone;
import java.util.TreeMap;

import velox.api.layer1.annotations.*;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.simplified.*;

@Layer1SimpleAttachable
@Layer1StrategyName("onDepth Indicator")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class MarketDataListener implements CustomModuleAdapter, DepthDataListener, TimeListener  {

    private final TreeMap<Integer, Integer> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Integer, Integer> asks = new TreeMap<>();
    private long timestamp;
    private String formattedTimestamp;
    private static boolean isHeaderWritten = false;
    private SimpleDateFormat dateFormat;

    public MarketDataListener() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-4")); // Set time zone to EDT (UTC-4)
    }

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {

        // Call the method here to create the file with headers
        demoBestPriceSize();
    }

    @Override
    public void onDepth(boolean isBid, int price, int size) {

        //Depending on whether the update is a bid (isBid is true) or an ask (isBid is false), it selects the
        // appropriate order book (bids or asks) to update.
        TreeMap<Integer, Integer> book = isBid ? bids : asks;

        //If the size of the update is 0, it removes the price level from the order book (indicating that there are no
        // more orders at that price level). Otherwise, it updates the order book with the new size at the given price
        // level.
        if (size == 0) {
            book.remove(price);
        } else {
            book.put(price, size);
        }

        // Call demoSumOfPriceLevels to calculate the sum of the top levels
        int sumOfTopLevels = demoSumOfPriceLevels(isBid, 5); // For example, summing top 5 levels
        Log.info("Sum of top " + (isBid ? "bid" : "ask") + " levels: " + sumOfTopLevels);

        //After updating the order book, it calls the demoBestPriceSize method to record the current best bid and ask
        // prices and sizes to a CSV file.
        demoBestPriceSize();
    }
    private void demoBestPriceSize() {

        // The first four lines inside the method are using the ternary operator to determine the best bid and ask
        // prices and sizes. If the bids or asks TreeMaps are empty, it assigns null to the respective variables;
        // otherwise, it retrieves the first key (best price) and value (size at the best price) from the TreeMap.
        Integer bestBidPrice = bids.isEmpty() ? null : bids.firstKey();
        Integer bestAskPrice = asks.isEmpty() ? null : asks.firstKey();
        Integer bestBidSize = bids.isEmpty() ? null : bids.firstEntry().getValue();
        Integer bestAskSize = asks.isEmpty() ? null : asks.firstEntry().getValue();

        // A FileWriter object is created to write data to a CSV file located at "C:\Bookmap\Logs\bestPriceSize.csv".
        // The true parameter indicates that data will be appended to the file rather than overwriting it.
        // It first checks if the header has been written to the file using the isHeaderWritten boolean variable.
        // If not, it writes the header line to the file and sets isHeaderWritten to true to prevent the header from
        // being written again. Next, it checks if both bestBidPrice and bestAskPrice are not null (indicating that
        // there are valid bid and ask prices available). If so, it writes a new line to the file with the formatted
        // timestamp, best bid price, best bid size, best ask price, and best ask size.
        try (FileWriter writer = new FileWriter("C:\\Bookmap\\Logs\\bestPriceSize.csv", true)) {
            if (!isHeaderWritten) {
                writer.append("Timestamp,BestBidPrice,BestBidSize,BestAskPrice,BestAskSize\n");
                isHeaderWritten = true;
            }
            if (bestBidPrice != null && bestAskPrice != null) {
                writer.append(String.valueOf(formattedTimestamp));
                writer.append(',');
                writer.append(String.valueOf(bestBidPrice));
                writer.append(',');
                writer.append(String.valueOf(bestBidSize));
                writer.append(',');
                writer.append(String.valueOf(bestAskPrice));
                writer.append(',');
                writer.append(String.valueOf(bestAskSize));
                writer.append('\n');
            }
        } catch (IOException e) {
            Log.error("Error writing to CSV file: " + e.getMessage());
        }
    }

    private int demoSumOfPriceLevels(boolean isBid, int numLevelsToSum) {
        int sizeOfTopLevels = 0;
        for (Integer size : (isBid ? bids : asks).values()) {
            if (--numLevelsToSum < 0) {
                break;
            }
            sizeOfTopLevels+= size;
            //Log.info("Current size: " + size + ", Cumulative size: " + sizeOfTopLevels);
        }
        Log.info("Total size of top levels: " + sizeOfTopLevels);

        return sizeOfTopLevels;
    }

    @Override
    public void onTimestamp(long nanoseconds) {
        timestamp = nanoseconds;
        Date date = new Date(nanoseconds / 1000000); // Convert nanoseconds to milliseconds
        formattedTimestamp = dateFormat.format(date); // Format date to a readable string format
    }
}