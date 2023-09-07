# OrderInfoBuilder

## Class Overview
**The `OrderInfoUpdate` class extends the `OrderInfo` class and contains cumulative information about an order along with
boolean fields that indicate what has changed since the last time the information was reported.**

## Fields
**The class contains several boolean fields that indicate whether specific attributes of an order have changed. These
fields include:**

1. averageFillPriceChanged
2. durationChanged
3. filledChanged
4. limitPriceChanged
5. modificationTimeChanged
6. statusChanged
7. stopPriceChanged
8. stopTriggeredChanged
9. unfilledChanged

## Constructors
**The class has several constructors, but most of them are deprecated, and it is recommended to use the Builder pattern
instead. One of the constructors is:**

    public OrderInfoUpdate(OrderInfo orderInfo)

## Methods
**The class has a method toBuilder() which returns an OrderInfoBuilder object, allowing you to use the Builder pattern to
create an `OrderInfoUpdate` object. It also has a `toString()` method to get a string representation of the object.**

## Using the Builder Pattern
**To use the Builder pattern with the OrderInfoUpdate class, you would first create an OrderInfo object and then use it
to create an OrderInfoUpdate object. Here is an example:**
```
    Step 1: Create an OrderInfo object using the Builder pattern
    
    OrderInfo orderInfo = new OrderInfo.Builder()
        .setInstrumentAlias("InstrumentAlias")
        .setOrderId("OrderId")
        .setIsBuy(true)
        .setType(OrderType.LIMIT)
        .setClientId("ClientId")
        .setDoNotIncrease(true)
        .setFilled(0)
        .setUnfilled(100)
        .setAverageFillPrice(100.50)
        .setDuration(OrderDuration.DAY)
        .setStatus(OrderStatus.NEW)
        .setLimitPrice(100.50)
        .setStopPrice(99.50)
        .setStopTriggered(false)
        .build();
    
    // Step 2: Create an OrderInfoUpdate object using the OrderInfo object
     
    OrderInfoUpdate orderInfoUpdate = new OrderInfoUpdate(orderInfo);
            
    // Step 3: Use the toBuilder() method to get an OrderInfoBuilder object and modify the OrderInfoUpdate object
    
    OrderInfoBuilder orderInfoBuilder = orderInfoUpdate.toBuilder();
    orderInfoBuilder.setLimitPrice(101.00);
    OrderInfoUpdate modifiedOrderInfoUpdate = orderInfoBuilder.build();
```
**In this example, an `OrderInfo` object is created using the Builder pattern, and then used to create an `OrderInfoUpdate`
object. The `toBuilder()` method is then used to get an `OrderInfoBuilder` object, which is used to modify the 
`OrderInfoUpdate` object.**

