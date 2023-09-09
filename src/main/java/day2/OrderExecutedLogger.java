package day2;

import velox.api.layer1.common.Log;
import velox.api.layer1.data.ExecutionInfo;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class OrderExecutedLogger {

    private static final String CSV_FILE_PATH = "C:\\Bookmap\\Logs\\Nasser2_log.csv";

    public void logOrderExecuted(ExecutionInfo executionInfo) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(CSV_FILE_PATH, true))) {
            pw.println("Order Executed," + executionInfo.orderId + ",,,,");
        } catch (IOException e) {
            Log.error("Error writing to CSV file", e);
        }
    }
}

