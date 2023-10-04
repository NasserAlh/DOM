# OnTrade and VolumeProfilePanel Classes Documentation

## Objective:

The primary objective of these interrelated classes, `OnTrade` and `VolumeProfilePanel`, is to provide a graphical representation of a Volume Profile - a crucial tool for traders to understand price and volume relationship. The `OnTrade` class captures real-time trade data, calculates the Volume Profile, and manages the GUI components. The `VolumeProfilePanel` class, on the other hand, is responsible for rendering the Volume Profile graphically.

## Functionalities:

### OnTrade Class

1. **Initialization:**
   - The `initialize` method sets up the GUI components like JFrame, JScrollPane, and a couple of JCheckBoxes for toggling Point of Control (POC) and Value Area display.
   - It initializes the `VolumeProfilePanel` instance with the current volume profile data.

2. **Trade Data Handling:**
   - The `onTrade` method is triggered on each trade event, updating the Volume Profile data.
   - It calculates the Value Area and Point of Control (POC) based on the updated Volume Profile.

3. **Value Area Calculation:**
   - The `calculateValueArea` method computes the Value Area bounds based on 70% of the volume.

4. **Stop Functionality:**
   - The `stop` method disposes of the JFrame when called, providing a way to clean up the GUI resources.

5. **ValueArea Inner Class:**
   - An inner class used to encapsulate the lower and upper bounds of the Value Area.

### VolumeProfilePanel Class

1. **Initialization:**
   - The constructor accepts a `ConcurrentSkipListMap` representing the Volume Profile data and sets up basic GUI properties.

2. **Zooming Functionality:**
   - Users can zoom in and out of the volume profile using the mouse wheel.

3. **Rendering Volume Bars:**
   - The `paintComponent` method is overridden to render the volume bars, POC, and Value Area based on the current Volume Profile data.

4. **POC and Value Area Toggling:**
   - Methods `setShowPOC` and `setShowValueArea` allow for toggling the display of POC and Value Area respectively.

5. **Update Volume Profile:**
   - The `updateVolumeProfile` method provides a way to update the Volume Profile data, which triggers a re-render of the panel.

6. **Setting Value Area Bounds and POC:**
   - Methods `setValueAreaBounds` and `setPointOfControl` allow for updating the Value Area bounds and POC which are reflected graphically.

The collaboration between `OnTrade` and `VolumeProfilePanel` classes ensures a real-time, interactive, and visually informative Volume Profile tool, essential for making informed trading decisions, especially in a high-frequency trading environment. Through the `OnTrade` class, trade data is continuously processed and the Volume Profile along with the Value Area and POC are updated. The `VolumeProfilePanel` class ensures these updates are reflected graphically in a user-friendly and interactive manner.