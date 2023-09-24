package day3;

import java.util.HashMap;
import java.util.Map;

import velox.api.layer1.annotations.*;
import velox.api.layer1.common.Log;
import velox.api.layer1.simplified.*;

@Layer1SimpleAttachable
@Layer1StrategyName("Level II")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)


public class DepthOfMarket implements CustomModuleAdapter, DepthDataListener {
    private Map<Integer, Integer> bidLevels = new HashMap<>();
    private Map<Integer, Integer> askLevels = new HashMap<>();
    private long lastUpdateTime = 0;
    private final long TIME_THRESHOLD = 5000; // 1 second
    private final int SIZE_THRESHOLD = 2000; // Example size threshold

    @Override
    public void onDepth(boolean isBid, int price, int size) {
        long currentTime = System.currentTimeMillis();
        
        // Update levels
        if (isBid) {
            if (size == 0) bidLevels.remove(price);
            else bidLevels.put(price, size);
        } else {
            if (size == 0) askLevels.remove(price);
            else askLevels.put(price, size);
        }

        // Check for large market orders
        if (currentTime - lastUpdateTime < TIME_THRESHOLD) {
            int totalSizeChange = 0;
            for (int bidSize : bidLevels.values()) {  
                totalSizeChange += bidSize;
            }
            for (int askSize : askLevels.values()) {  
                totalSizeChange += askSize;
            }
            if (totalSizeChange >= SIZE_THRESHOLD) {
                Log.info("Large market order detected.");
            }
        }

        lastUpdateTime = currentTime;
    }
}


