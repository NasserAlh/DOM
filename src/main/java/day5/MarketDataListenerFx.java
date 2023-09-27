package day5;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.simplified.*;

import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Comparator;
import java.util.TreeMap;


@Layer1SimpleAttachable
@Layer1StrategyName("Market Data Listener FX")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class MarketDataListenerFx implements CustomModule, DepthDataListener {

    private final TreeMap<Integer, Integer> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Integer, Integer> asks = new TreeMap<>();

    @Override
    public void initialize(String s, InstrumentInfo instrumentInfo, Api api, InitialState initialState) {
        startServer();
    }

    public void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(5555)) {
                Socket clientSocket = serverSocket.accept();
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

                // Send data to JavaFX client
                out.writeObject(bids);
                out.writeObject(asks);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onDepth(boolean isBid, int price, int size) {
        TreeMap<Integer, Integer> book = isBid ? bids : asks;
        if (size == 0) {
            book.remove(price);
        } else {
            book.put(price, size);
        }
    }

    @Override
    public void stop() {

    }
}
