package day4;

import velox.api.layer1.annotations.*;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.simplified.*;

import java.util.*;

@Layer1SimpleAttachable
@Layer1StrategyName("Order Flow Understandings")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)

public class OrderFlows implements CustomModule, TimeListener, DepthDataListener {
    private final TreeMap<Integer, Integer> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Integer, Integer> asks = new TreeMap<>();
    private long timestamp;

    @Override
    public void initialize(String s, InstrumentInfo instrumentInfo, Api api, InitialState initialState) {
        // Initialization logic here
    }

    @Override
    public void onDepth(boolean isBid, int price, int size) {
        TreeMap<Integer, Integer> book = isBid ? bids : asks;
        if (size == 0) {
            book.remove(price);
        } else {
            book.put(price, size);
        }

        BestPriceSize();
        int sumOfTop5Bids = SumOfPriceLevels(true, 5);
        int sumOfTop5Asks = SumOfPriceLevels(false, 5);
        Log.info("Sum of top 5 Bids: " + sumOfTop5Bids);
        Log.info("Sum of top 5 Asks: " + sumOfTop5Asks);
    }

    private void BestPriceSize() {
        if (!bids.isEmpty() && !asks.isEmpty()) {
            int bestBidPrice = bids.firstKey();
            int bestAskPrice = asks.firstKey();
            int bestBidSize = bids.firstEntry().getValue();
            int bestAskSize = asks.firstEntry().getValue();
            Log.info("Best Bid: " + bestBidPrice + " @ " + bestBidSize);
            Log.info("Best Ask: " + bestAskPrice + " @ " + bestAskSize);
        } else {
            Log.info("Order Book is empty.");
        }
    }

    private int SumOfPriceLevels(boolean isBid, int numLevelsToSum) {
        int sizeOfTopLevels = 0;
        for (Integer size : (isBid ? bids : asks).values()) {
            if (--numLevelsToSum < 0) {
                break;
            }
            sizeOfTopLevels += size;
        }
        return sizeOfTopLevels;
    }

    @Override
    public void onTimestamp(long nanoseconds) {
        timestamp = nanoseconds;
        // You can use the timestamp here if needed
    }

    @Override
    public void stop() {
        // Cleanup logic here
    }
}
