package day2;

import velox.api.layer1.common.Log;
import velox.api.layer1.data.OrderInfoUpdate;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class OrderUpdatedLogger {

    private static final String CSV_FILE_PATH = "C:\\Bookmap\\Logs\\Nasser2_log.csv";

    public void logOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(CSV_FILE_PATH, true))) {
            pw.println("Order Updated," + orderInfoUpdate.orderId + "," + orderInfoUpdate.isBuy + "," +
                    orderInfoUpdate.status + "," + orderInfoUpdate.stopPrice + "," + orderInfoUpdate.limitPrice);
        } catch (IOException e) {
            Log.error("Error writing to CSV file", e);
        }
    }
}

