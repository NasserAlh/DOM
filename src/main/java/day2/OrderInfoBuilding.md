## OrderInfo 
**The `OrderInfo` class is a part of the `velox.api.layer1.data` package and serves as a base class to accumulate information
about an order. Here is a summary and how to use it with the Builder pattern:**
### Class Overview
**The `OrderInfo` class contains cumulative information about an order and implements the `Serializable` interface. It has
several fields that store details about an order.**
### Fields
**The class contains several fields that store information about an order, including:**
* averageFillPrice: The average fill price of the order.
* clientId: A string representing the client ID.
* doNotIncrease: A boolean indicating whether the order should not increase.
* duration: An OrderDuration object representing the duration of the order.
* exchangeId: A string representing the exchange ID.
* filled: An integer representing the number of filled orders.
* instrumentAlias: A string representing the instrument alias.
* isBuy: A boolean indicating whether the order is a buy order.
* isDuplicate: A boolean indicating whether the order is a duplicate (in case of cross-trading).
* isSimulated: A boolean indicating whether the order is simulated.
* limitPrice: The limit price of the order.
* modificationUtcTime: A long representing the modification time in UTC.
* orderId: A string representing the order ID.
* status: An OrderStatus object representing the status of the order.
* stopPrice: The stop price of the order.
* stopTriggered: A boolean indicating whether the stop has been triggered.
* type: An OrderType object representing the type of the order.
* unfilled: An integer representing the number of unfilled orders 
### Constructors
**The class has several constructors, but they are deprecated, and it is recommended to use the Builder pattern instead.
The constructors are of the form:**
```
public OrderInfo(String instrumentAlias,String orderId, boolean isBuy,OrderType type, String clientId, boolean 
                doNotIncrease, int filled, int unfilled, double averageFillPrice, OrderDuration duration, OrderStatus
                status, double limitPrice, double stopPrice, boolean stopTriggered, long modificationUtcTime, boolean
                isSimulated,boolean isDuplicate)
```
### Using the Builder Pattern
**To use the Builder pattern to create an OrderInfo object, you would first create a Builder class within the OrderInfo
class. This Builder class would have setter methods for each field in the OrderInfo class, and a build method to create
an OrderInfo object. Here is an example of how you might implement and use this Builder class:**
```
    public class OrderInfo {
    // Fields here

        public static class Builder {
            // Fields here
    
            public Builder setInstrumentAlias(String instrumentAlias) {
                this.instrumentAlias = instrumentAlias;
                return this;
            }
    
            // Other setter methods here
    
            public OrderInfo build() {
                return new OrderInfo(this);
            }
        }
    
        private OrderInfo(Builder builder) {
            // Initialize fields using builder
        }
    }

    // Usage:
    OrderInfo.Builder builder = new OrderInfo.Builder();
    builder.setInstrumentAlias("InstrumentAlias");
    // Set other fields using builder
    OrderInfo orderInfo = builder.build();`
```
**In this example, a `Builder` class is defined within the `OrderInfo` class, and used to create an `OrderInfo` object using
the `Builder` pattern. You would then use this OrderInfo object to create an `OrderInfoUpdate` object, as shown in the
previous example.**