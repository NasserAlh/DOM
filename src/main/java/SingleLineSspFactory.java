import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CanvasIcon;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CompositeCoordinateBase;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CompositeHorizontalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CompositeVerticalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.PreparedImage;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvasFactory;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvasFactory.ScreenSpaceCanvasType;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpacePainter;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpacePainterAdapter;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpacePainterFactory;
import velox.api.layer1.messages.indicators.AliasFilter;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyScreenSpacePainter;

public class SingleLineSspFactory implements ScreenSpacePainterFactory {

    public static class SspLine implements ScreenSpacePainterAdapter {
        private ScreenSpaceCanvas canvas;

        private int heatmapFullPixelsWidth;
        private CanvasIcon icon;
        private PreparedImage gridPattern;
        private double lastTrade;

        {
            BufferedImage gridPatternImage = new BufferedImage(1, 1,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics graphics = gridPatternImage.getGraphics();
            graphics.setColor(Color.CYAN);
            graphics.fillRect(0, 0, 1, 1);
            graphics.dispose();
            gridPattern = new PreparedImage(gridPatternImage);
        }

        public SspLine(ScreenSpaceCanvasFactory screenSpaceCanvasFactory, double lastTrade) {
            super();
            canvas = screenSpaceCanvasFactory.createCanvas(ScreenSpaceCanvasType.HEATMAP);
            this.lastTrade = lastTrade;
        }

        @Override
        public void onHeatmapFullPixelsWidth(int heatmapFullPixelsWidth) {
            if (this.heatmapFullPixelsWidth != heatmapFullPixelsWidth) {
                this.heatmapFullPixelsWidth = heatmapFullPixelsWidth;
                update();
            }
            this.heatmapFullPixelsWidth = heatmapFullPixelsWidth;
        }

        @Override
        public void onMoveEnd() {
            update();
        }

        private synchronized void update() {
            if (icon != null) {
                canvas.removeShape(icon);
            }
            CompositeHorizontalCoordinate x1 = new CompositeHorizontalCoordinate(CompositeCoordinateBase.PIXEL_ZERO, 0, 0);
            CompositeVerticalCoordinate y1 = new CompositeVerticalCoordinate(CompositeCoordinateBase.DATA_ZERO, 0, lastTrade);
            CompositeHorizontalCoordinate x2 = new CompositeHorizontalCoordinate(CompositeCoordinateBase.PIXEL_ZERO, heatmapFullPixelsWidth, 0);
            CompositeVerticalCoordinate y2 = new CompositeVerticalCoordinate(CompositeCoordinateBase.DATA_ZERO, 1, lastTrade);

            icon = new CanvasIcon(gridPattern, x1, y1, x2, y2);
            canvas.addShape(icon);
        }

        @Override
        public void dispose() {
            canvas.dispose();
        }

        public void setLastTrade(double price) {
            lastTrade = price;
        }
    }

    private String indicatorAlias;
    private SspLine sspa;
    private double lastTrade;//lastTrade backup

    public SingleLineSspFactory(String alias) {
        super();
        indicatorAlias = alias;
    }

    public Layer1ApiUserMessageModifyScreenSpacePainter getUserMessage(String userName, boolean isAdd) {
        Layer1ApiUserMessageModifyScreenSpacePainter painter = Layer1ApiUserMessageModifyScreenSpacePainter
                .builder(SingleLineSspFactory.class, userName)
                .setIsAdd(isAdd)
                .setAliasFilter(new AliasFilter() {
                    @Override
                    public boolean isDisplayedForAlias(String alias) {
                        return indicatorAlias.equals(alias);
                    }
                })
                .setScreenSpacePainterFactory(this)
                .build();
        return painter;
    }

    @Override
    public ScreenSpacePainter createScreenSpacePainter(String indicatorName, String indicatorAlias,
                                                       ScreenSpaceCanvasFactory screenSpaceCanvasFactory) {
        sspa = new SspLine(screenSpaceCanvasFactory, lastTrade);
        return sspa;
    }

    public void onTrade(double price) {
        lastTrade = price;

        if (sspa != null) {
            sspa.setLastTrade(price);
            sspa.update();
        }
    }
}