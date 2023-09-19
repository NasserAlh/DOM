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
@Layer1StrategyName("onDepth Indicator")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class MarketDepthAnalyzer implements CustomModuleAdapter, DepthDataListener, TimeListener  {

    private final TreeMap<Integer, Integer> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Integer, Integer> asks = new TreeMap<>();
    private TreeMap<Integer, Integer> bidVolumeClusters = new TreeMap<>(Comparator.reverseOrder());
    private TreeMap<Integer, Integer> askVolumeClusters = new TreeMap<>();
    private String formattedTimestamp;
    private static boolean isHeaderWritten = false;
    private final SimpleDateFormat dateFormat;
    private static final int VOLUME_CLUSTER_THRESHOLD = 100; // You can change 100 to any value that suits your logic
    private static final int LIQUIDITY_THRESHOLD = 1000; // Adjust this value based on your strategy
    private static boolean isVolumeClusterHeaderWritten = false;
    private static boolean isLiquidityPoolHeaderWritten = false;


    public MarketDepthAnalyzer() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-4")); // Set time zone to EDT (UTC-4)
    }

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        // Initialize the file with headers by calling demoBestPriceSize method
        writeBestPriceSizeToFile(null);
    }

    @Override
    public void onDepth(boolean isBid, int price, int size) {
        // Update the order book based on the new depth data received
        TreeMap<Integer, Integer> book = isBid ? bids : asks;
        TreeMap<Integer, Integer> volumeClusters = isBid ? bidVolumeClusters : askVolumeClusters;


        if (size == 0) {
            book.remove(price);
        } else {
            book.put(price, size);
        }

        volumeClusters.put(price, volumeClusters.getOrDefault(price, 0) + size);


        // Calculate the sum of top levels and log it
        VolumePair topLevelsSum = calculateTopLevelsVolume(5); // For example, summing top 5 levels
        int sumOfTopLevels = isBid ? topLevelsSum.bidVolume() : topLevelsSum.askVolume();
        Log.info("Sum of top " + (isBid ? "bid" : "ask") + " levels: " + sumOfTopLevels);

        // Update the best price size and calculate the OBI
        writeBestPriceSizeToFile(topLevelsSum);

        // Identify and log volume clusters
        identifyVolumeClusters(volumeClusters);

        // Identify and log liquidity pools
        identifyLiquidityPools(isBid ? bidVolumeClusters : askVolumeClusters, isBid);
    }
    private void identifyLiquidityPools(TreeMap<Integer, Integer> volumeClusters, boolean isBid) {
        for (Map.Entry<Integer, Integer> entry : volumeClusters.entrySet()) {
            int totalSizeAtPrice = getTotalSizeAtPrice(isBid, entry.getKey());
            if (totalSizeAtPrice >= LIQUIDITY_THRESHOLD) {
                logLiquidityPool(entry.getKey(), totalSizeAtPrice, isBid);
            }
        }
    }

    private void logLiquidityPool(int price, int totalSizeAtPrice, boolean isBid) {
        try (FileWriter writer = new FileWriter("C:\\Bookmap\\Logs\\liquidityPools.csv", true)) {
            if (!isLiquidityPoolHeaderWritten) {
                writer.append("Timestamp,Side,Price,TotalSize\n");
                isLiquidityPoolHeaderWritten = true;
            }
            writer.append(formattedTimestamp);
            writer.append(',');
            writer.append(isBid ? "Bid" : "Ask");
            writer.append(',');
            writer.append(String.valueOf(price));
            writer.append(',');
            writer.append(String.valueOf(totalSizeAtPrice));
            writer.append('\n');
        } catch (IOException e) {
            Log.error("Error writing to CSV file: " + e.getMessage());
        }
    }

    private int getTotalSizeAtPrice(boolean isBid, int price) {
        return (isBid ? bidVolumeClusters : askVolumeClusters).getOrDefault(price, 0);
    }

    private void identifyVolumeClusters(TreeMap<Integer, Integer> volumeClusters) {
        for (Map.Entry<Integer, Integer> entry : volumeClusters.entrySet()) {
            if (entry.getValue() > VOLUME_CLUSTER_THRESHOLD) {
                // This is a volume cluster
                logVolumeCluster(entry.getKey(), entry.getValue());
            }
        }
    }

    private void logVolumeCluster(int price, int volume) {
        try (FileWriter writer = new FileWriter("C:\\Bookmap\\Logs\\volumeClusters.csv", true)) {
            if (!isVolumeClusterHeaderWritten) {
                writer.append("Timestamp,Price,Volume\n");
                isVolumeClusterHeaderWritten = true;
            }
            writer.append(String.valueOf(formattedTimestamp));
            writer.append(',');
            writer.append(String.valueOf(price));
            writer.append(',');
            writer.append(String.valueOf(volume));
            writer.append('\n');
        } catch (IOException e) {
            Log.error("Error writing to CSV file: " + e.getMessage());
        }
    }

    private void writeBestPriceSizeToFile(VolumePair topLevelsSum) {
        Integer bestBidPrice = bids.isEmpty() ? null : bids.firstKey();
        Integer bestAskPrice = asks.isEmpty() ? null : asks.firstKey();
        Integer bestBidSize = bids.isEmpty() ? null : bids.firstEntry().getValue();
        Integer bestAskSize = asks.isEmpty() ? null : asks.firstEntry().getValue();

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

        if (topLevelsSum != null) {
            // Now use the passed topLevelsSum variable to calculate the OBI
            int bidVolume = topLevelsSum.bidVolume();
            int askVolume = topLevelsSum.askVolume();

            double obi = (double)(bidVolume - askVolume) / (bidVolume + askVolume);
            Log.info("Order Book Imbalance (OBI): " + obi);
        }
    }

    @Override
    public void onTimestamp(long nanoseconds) {
        // Update the timestamp and formatted timestamp based on the new timestamp received
        Date date = new Date(nanoseconds / 1000000); // Convert nanoseconds to milliseconds
        formattedTimestamp = dateFormat.format(date); // Format date to a readable string format
    }

    public record VolumePair(int bidVolume, int askVolume){}

    private VolumePair calculateTopLevelsVolume(int numLevelsToSum) {
        // Calculate the sum of the top bid and ask levels and return a VolumePair object holding this data
        int sizeOfTopBidLevels = 0;
        int sizeOfTopAskLevels = 0;

        int bidLevelsCount = 0;
        for (Integer size : bids.values()) {
            if (bidLevelsCount++ >= numLevelsToSum) {
                break;
            }
            sizeOfTopBidLevels += size;
        }

        int askLevelsCount = 0;
        for (Integer size : asks.values()) {
            if (askLevelsCount++ >= numLevelsToSum) {
                break;
            }
            sizeOfTopAskLevels += size;
        }

        return new VolumePair(sizeOfTopBidLevels, sizeOfTopAskLevels);
    }
}
