# README: Volume Profile Bookmap Addon

## Main Objective:

The primary focus of this Java application is to display a live Volume Profile chart, tailored for traders. The code listens for real-time trade data for a given financial instrument, updates a volume profile, and visualizes this data in a Swing-based graphical interface.

## Components:

### Annotations:
- `@Layer1SimpleAttachable`, `@Layer1StrategyName`, and `@Layer1ApiVersion`: These annotations indicate that this class should be attachable as a trading strategy and specify the API version and strategy name.

### Classes and Interfaces:

#### TradeDataTest Class
Implements `TradeDataListener` and `CustomModule` interfaces. This is the main class handling the trading data and UI.

- **ConcurrentHashMap `volumeProfile`**: Stores the real-time volume data by price level.
- **VolumeProfilePanel `volumeProfilePanel`**: An instance of the JPanel subclass that will display the Volume Profile.
- **JFrame `frame`**: The main window for the Swing UI.

##### Key Methods:
- `initialize()`: Initializes the JFrame and JPanel and sets them up for display.
- `stop()`: Disposes of the JFrame when the application stops.
- `onTrade()`: Listens for trade events, updates the `volumeProfile`, and triggers the UI update.

#### VolumeProfilePanel Class (Nested)
Extends `JPanel`. This class is responsible for rendering the Volume Profile graphically.

- **ConcurrentHashMap `volumeProfile`**: Local copy of the main class's `volumeProfile`.

##### Key Methods:
- `paintComponent()`: Overrides the default `paintComponent` method to draw volume bars and labels.
- `drawBar()`: Utility function to draw a volume bar.
- `drawPriceLabel()`: Utility function to draw a price label.
- `updateVolumeProfile()`: Updates `volumeProfile` and repaints the component.

## How It Works:
1. `initialize()` sets up the JFrame and JPanel, displaying an initially empty Volume Profile.
2. `onTrade()` updates the `volumeProfile` with the latest trade data every time a new trade occurs.
3. The `VolumeProfilePanel` class uses Swing's Graphics API to render this data as a set of bars in the panel.

## Technologies Used:
- Java Swing for the UI
- ConcurrentHashMap for thread-safe data storage
- Annotations for easy integration with the Layer1 trading system

## Future Considerations:
Given that you're into intraday activities and are keen on using AI models, you might consider integrating machine learning predictions into this live chart to signal potential trading opportunities.

Would you like to explore how to make this Swing-based Volume Profile interactive, like allowing zooming and panning?