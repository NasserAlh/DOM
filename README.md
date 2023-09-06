## General tips on running Bookmap with any IDE

- Check the JRE version used in your Bookmap release: open `C:\Program Files\Bookmap\jre\bin` in your terminal (the path might be different on your machine
  if you changed the installation directory), and execute `.\java --version`. You can use the same JDK version for development.
  Also, at the time of writing, Bookmap ver. 7.2 and onwards can be run with JVM ver. 14. Note, however, that this might be changed in the future.
- Sometimes your IDE may ignore your gradle source/target compatibility settings for Java (if you have those). Ensure the Java version is the same in your project environment/compiler settings. If not, set it explicitly in the project settings.
- Sometimes your IDE may handle your gradle compileOnly dependencies incorrectly and still add those to classpath. **This will usually result in NoSuchMethodError or NoClassDefFoundError**. You can verify that this is the case by checking `java.class.path` system properly of the process that you started via Java VisualVM on in any other way - it shouldn't contain any of compileOnly dependencies, so if it does - this is a problem. To solve it you can either (hackish simple way) edit build.gradle to point to the libraries from `C:\Program Files\Bookmap\lib` directory (which will ensure that those are the same exact libraries that bookmap expects, preventing the crash) or (more correct way, but exact way to achieve it differs from one IDE to another) remove the project and project dependencies from run configuration.
- Working directory will determine where your config folder will be. On Windows you can set `C:\Bookmap`, which is the default during installation, but you can also maintain multiple separate Bookmap configs, if you want.
- Add `C:\Program Files\Bookmap\Bookmap.jar` to the classpath. It should list the dependencies in manifest, so that will often be enough, but you can include libraries from `C:\Program Files\Bookmap\lib` if Bookmap complains about missing classes.
- If you are using Java 16 or newer, add the list of `--add-opens` JVM args to your run configuration:
```
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id 'java-library'
    id 'idea'
    id 'org.jetbrains.gradle.plugin.idea-ext' version '1.1.6'
}

repositories {
    mavenCentral()
    maven {
        url "https://maven.bookmap.com/maven2/releases/"
    }
}

dependencies {
    if (findProperty('is_built_from_main_bookmap_project')) {
        implementation fileTree(dir: "${main_libs}", include: ['*.jar'])
        implementation project(':Level1Api')
        implementation project(':SimplifiedApiWrapper')
    } else {
        implementation  group: 'com.bookmap.api', name: 'api-core', version: '7.4.0.19'
        implementation  group: 'com.bookmap.api', name: 'api-simplified', version: '7.4.0.19'
        implementation  group: 'org.apache.commons', name: 'commons-lang3', version: '3.11'
    }
}

// Get current date and time
def currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_M_d_HH_mm"))

jar {
    archiveFileName = "Bookmap_Add_On_${currentDateTime}.jar"
}

idea.module.downloadJavadoc = true


if(idea.project) {
    // Create 'BookmapJar' run configuration for IntelliJ
    idea.project.settings.runConfigurations {
        def addOpensOptions = '--add-opens=java.base/java.lang=ALL-UNNAMED ' +
                '--add-opens=java.base/java.io=ALL-UNNAMED ' +
                '--add-opens=java.base/java.math=ALL-UNNAMED ' +
                '--add-opens=java.base/java.util=ALL-UNNAMED ' +
                '--add-opens=java.base/java.util.concurrent=ALL-UNNAMED ' +
                '--add-opens=java.base/java.net=ALL-UNNAMED ' +
                '--add-opens=java.base/java.text=ALL-UNNAMED ' +
                '--add-opens=java.desktop/java.awt=ALL-UNNAMED ' +
                '--add-opens=java.desktop/java.awt.color=ALL-UNNAMED ' +
                '--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED ' +
                '--add-opens=java.desktop/com.sun.java.swing=ALL-UNNAMED ' +
                '--add-opens=java.prefs/java.util.prefs=ALL-UNNAMED'
        if (org.gradle.internal.os.OperatingSystem.current().isWindows()) {
            addOpensOptions += ' --add-opens=java.desktop/sun.awt.windows=ALL-UNNAMED'
        }

        'BookmapJar'(org.jetbrains.gradle.ext.JarApplication) {
            // Change the path to Bookmap.jar here if you changed the default installation directory
            jarPath = 'C:\\Program Files\\Bookmap\\Bookmap.jar'
            workingDirectory = 'C:\\Bookmap'
            jvmArgs = addOpensOptions
        }
    }
}

```
- When started from an IDE on a Windows machine, your Bookmap might look different from what you see when you
  start it from the desktop shortcut. If you see black areas on a heatmap, this is caused by Java scaling issues.
  To fix this:
    - Find the `java.exe` file of the JDK that you use to start Bookmap from an IDE
    - Right-click on it, go to `Properties -> Compatibility`.
    - Press the `Change high DPI settings` button. In the opened window, check the `Override high DPI scaling behavior. Scaling performed by:` checkbox.
    - In the dropdown below select `System` or `System (Enhanced)`
- Start `velox.ib.Main`

## Place Orders Ordeals

### The Problem
The stop loss and take profit limit orders are hanging after the associated market order has been executed. So, I need
a way to make the code logic forcefully cancel any stop loss and take profit limit orders after their market order is
executed.

### Possible Solution

- Step 1: Modify the `placeOrder` Method to Store Order IDs
  - Modify the `placeOrder` method to store the order IDs of the stop loss and take profit orders in member variables:
  
        private String stopLossOrderId;
        private String takeProfitOrderId;

        private void placeOrder(boolean isBuy, double price, int quantity) {
          try {
          SimpleOrderSendParametersBuilder builder = new SimpleOrderSendParametersBuilder(alias, isBuy, quantity);
          builder.setDuration(OrderDuration.IOC);
  
            // Setting a stop loss and take profit offset (in ticks)
            int stopLossOffset = 10; // for example, 10 ticks
            int takeProfitOffset = 20; // for example, 20 ticks
            
            builder.setStopLossOffset(stopLossOffset);
            builder.setTakeProfitOffset(takeProfitOffset);
            
            SimpleOrderSendParameters order = builder.build();
            OrderSendResult result = api.sendOrder(order);
            
            // Store the order IDs of the stop loss and take profit orders
            stopLossOrderId = result.getStopLossOrderId();
            takeProfitOrderId = result.getTakeProfitOrderId();
          } catch (Exception e) {
            Log.error("Error placing order", e);
          } 
        }

- Step 2: Add a Method to Cancel Stop Loss and Take Profit Orders
  - Add a method to cancel the stop loss and take profit orders using the stored order IDs:

        private void cancelStopLossAndTakeProfitOrders() {
          try {
            if (stopLossOrderId != null) {
            api.cancelOrder(stopLossOrderId);
            stopLossOrderId = null;
          }
            if (takeProfitOrderId != null) {
              api.cancelOrder(takeProfitOrderId);
              takeProfitOrderId = null;
            }
          } catch (Exception e) {
            Log.error("Error cancelling orders", e);
          }
        }
  
- Step 3: Modify the onBar Method to Cancel Orders
  - Modify the onBar method to call the `cancelStopLossAndTakeProfitOrders` method before placing a new order:
        
        if (closePrice > smaValue && previousClose <= previousSMA) {
          Log.info("Buy Signal at " + closePrice * pips);
          if (currentPosition <= 0) {
            cancelStopLossAndTakeProfitOrders(); // Cancel existing stop loss and take profit orders
            placeOrder(true, closePrice, 1); // Place a buy order
            currentPosition = 1; // Update the current position to long
          }
        } else if (closePrice < smaValue && previousClose >= previousSMA) {
            Log.info("Sell Signal at " + closePrice * pips);
            if (currentPosition >= 0) {
                cancelStopLossAndTakeProfitOrders(); // Cancel existing stop loss and take profit orders
                placeOrder(false, closePrice, 1); // Place a sell order
                currentPosition = -1; // Update the current position to short
            }
        }

However,the Api class does not have a cancelOrder method. The problem lays under the orderId, which must be retrieved 
to track the order status. 

There are classes that has methods to track orders in the strategy. For instance, I can use the `OrderInfoUpdate` class
to track the status of orders and make decisions based on the current status of orders (whether they are filled,
working, or cancelled). I can also use the `ExecutionInfo` class to track the execution details of orders. 