## onDepth Method

What if the given code is removed from the onDepth method?  Do I still have the ability to perform any subsequent
analytics or trading algorithms based on the incoming market depth data?

     public void onDepth(boolean isBid, int price, int size) {
        TreeMap<Integer, Integer> book = isBid ? bids : asks;
        if (size == 0) {
            book.remove(price);
        } else {
            book.put(price, size);
        } 
    }

### Key Takeaway

If the given code is removed from the `onDepth` method, you'll lose the ability to maintain an accurate Order Book.
_**This will severely limit or even disable your capacity to perform any subsequent analytics or trading algorithms based
on the incoming market depth data.**_

### Detailed Explanation

1. **Loss of Order Book Maintenance**: The code is crucial for updating the Order Book (`bids` and `asks`). Without it,
     you won't have a real-time, accurate representation of the market depth.

2. **Impact on Analytics**: Many analytics, like Volume Weighted Average Price (VWAP), Order Book Imbalance, and
     liquidity analysis, rely on an accurate Order Book. Without this code, these analytics would be compromised.

3. **Trading Algorithms**: Trading algorithms that rely on order flow or market depth would not function as intended.
     Strategies that depend on best bid/ask prices, for example, would be rendered useless.

4. **Data Integrity**: The code ensures that if the size at a particular price level becomes zero, that price level
     is removed from the Order Book. This is crucial for maintaining data integrity, which would be lost if the code
     is removed.

### Critical Questions

1. What alternative methods could you employ to maintain the Order Book if this code is removed?
2. How would you adapt your trading strategy to work without an accurate Order Book?

In summary, the given code is a foundational element for maintaining the Order Book, and its absence would significantly
impact any subsequent analytics or trading strategies that rely on market depth information.

Below is a flowchart-like diagram that depicts the function of the given code snippet.

```
                       +------------------+
                       | Start onDepth()  |
                       +------------------+
                               |
                               V
               +-------------------------------+
               | TreeMap<Integer, Integer> book|
               | = isBid ? bids : asks;        |
               +-------------------------------+
                               |
                               V
                       +---------------+
                       |  size == 0 ?  |
                       +---------------+
                               |
                  +------------+------------+
                  |                         |
                  V                         V
       +---------------------+   +---------------------+
       | book.remove(price); |   | book.put(price, size);|
       +---------------------+   +---------------------+
                  |                         |
                  +------------+------------+
                               |
                               V
                       +---------------+
                       |    End of     |
                       |  onDepth()    |
                       +---------------+
```
1. The function starts with the `onDepth()` method.
2. It then selects either the `bids` or `asks` TreeMap based on the `isBid` boolean value and assigns it to `book`.
3. A conditional check on `size` is performed.
    - If `size` is zero, the `price` level is removed from `book`.
    - If `size` is not zero, the `price` and `size` are either added or updated in `book`.
4. The function ends after performing one of these actions.

Here's a table that breaks down the function of the given code snippet:

| Condition     | TreeMap Selected | Action Taken       | Description                                                                                     |
|---------------|------------------|--------------------|-------------------------------------------------------------------------------------------------|
| `isBid == true`  | `bids`           | `book = bids`      | If the incoming data is for a bid, the `bids` TreeMap is selected.                              |
| `isBid == false` | `asks`           | `book = asks`      | If the incoming data is for an ask, the `asks` TreeMap is selected.                             |
| `size == 0`      | `book`           | `book.remove(price)` | If the size is zero, the price level is removed from the selected TreeMap (`bids` or `asks`).   |
| `size != 0`      | `book`           | `book.put(price, size)` | If the size is not zero, the price level and size are added or updated in the selected TreeMap. |

### Key Takeaways:
1. The TreeMap named book is dynamically set to either bids or asks based on whether the incoming data is a bid (isBid == true) or an ask (isBid == false).
2. If the size of the incoming data is zero, the corresponding price level is removed from the selected TreeMap.
3. If the size is not zero, the price and size are either added as a new entry or updated if the price already exists in the selected TreeMap.
