package day5;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.InitialState;
import java.net.Socket;
import java.io.PrintWriter;

@Layer1SimpleAttachable
@Layer1StrategyName("HelloFxBook")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class HelloFxBook implements CustomModule {

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        Log.info("Hello");
        sendMessage("Hello");
    }

    @Override
    public void stop() {
        Log.info("Bye");
        sendMessage("Bye");
    }

    private void sendMessage(String message) {
        try (Socket socket = new Socket("localhost", 5555);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
