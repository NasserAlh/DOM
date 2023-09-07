package day1;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Logger;

public class SupportResistanceDynamic {

    private static final Logger LOGGER = Logger.getLogger(SupportResistanceDynamic.class.getName());

    private double mult = 8.0;
    private int atrLen = 50;
    private int extLast = 4;

    private double avg = 0;
    private double hold_atr = 0;
    private int os = 0;
    private int n = 0;
    private double breakout_atr = 0;

    private ArrayList<SR> records = new ArrayList<>();
    private LinkedList<Double> highPrices = new LinkedList<>();
    private LinkedList<Double> lowPrices = new LinkedList<>();
    private LinkedList<Double> closePrices = new LinkedList<>();
    private LinkedList<Double> atrValues = new LinkedList<>();

    /**
     * Inner class to represent Support and Resistance records.
     */
    public class SR {
        double y;
        double area;
        int x;
        boolean support;

        SR(double y, double area, int x, boolean support) {
            this.y = y;
            this.area = area;
            this.x = x;
            this.support = support;
        }
    }

    /**
     * Retrieves the support record from the list of records.
     * @return the support record, or null if no support record is found.
     */
    public SR getSupport() {
        for (SR record : records) {
            if (record.support) {
                return record;
            }
        }
        return null;
    }

    /**
     * Retrieves the resistance record from the list of records.
     * @return the resistance record, or null if no resistance record is found.
     */
    public SR getResistance() {
        for (SR record : records) {
            if (!record.support) {
                return record;
            }
        }
        return null;
    }

    /**
     * Calculates support and resistance levels based on the input data.
     * @param close the close price
     * @param high the high price
     * @param low the low price
     */
    public void calculate(double close, double high, double low) {
        try {
            n++;
            closePrices.add(close);
            highPrices.add(high);
            lowPrices.add(low);

            if (closePrices.size() > atrLen) {
                closePrices.removeFirst();
                highPrices.removeFirst();
                lowPrices.removeFirst();
            }

            double atr = calculateATR();
            atrValues.add(atr);

            if (atrValues.size() > atrLen) {
                atrValues.removeFirst();
            }

            breakout_atr = calculateAverage(atrValues) * mult;

            avg = Math.abs(close - avg) > breakout_atr ? close : avg;
            hold_atr = avg == close ? breakout_atr : hold_atr;
            os = avg > getPreviousAvg(1) ? 1 : avg < getPreviousAvg(1) ? 0 : os;

            updateRecords(close);

        } catch (Exception e) {
            LOGGER.severe("An error occurred during calculation: " + e.getMessage());
        }
    }

    /**
     * Updates the records list based on the current average and close price.
     * @param close the close price
     */
    private void updateRecords(double close) {
        double upper_res = os == 0 ? avg + hold_atr / mult : Double.NaN;
        double lower_res = os == 0 ? avg + hold_atr / mult / 2 : Double.NaN;
        double upper_sup = os == 1 ? avg - hold_atr / mult / 2 : Double.NaN;
        double lower_sup = os == 1 ? avg - hold_atr / mult : Double.NaN;

        if (close == avg) {
            if (os == 1) {
                records.add(0, new SR(lower_sup, upper_sup, n, true));
            } else {
                records.add(0, new SR(upper_res, lower_res, n, false));
            }
        }
    }

    /**
     * Calculates the Average True Range (ATR) based on the recent prices.
     * @return the calculated ATR
     */
    private double calculateATR() {
        if (closePrices.size() < 2) {
            return 0.0;
        }

        double prevClose = closePrices.get(closePrices.size() - 2);
        double currentHigh = highPrices.getLast();
        double currentLow = lowPrices.getLast();

        double tr1 = currentHigh - currentLow;
        double tr2 = Math.abs(currentHigh - prevClose);
        double tr3 = Math.abs(currentLow - prevClose);

        return Math.max(tr1, Math.max(tr2, tr3));
    }

    /**
     * Calculates the average of a list of values.
     * @param values the list of values
     * @return the calculated average
     */
    private double calculateAverage(LinkedList<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }

        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    /**
     * Retrieves the average close price from a specified number of periods ago.
     * @param periodsAgo the number of periods ago
     * @return the average close price from the specified number of periods ago
     */
    private double getPreviousAvg(int periodsAgo) {
        int index = closePrices.size() - 1 - periodsAgo;
        return index >= 0 ? closePrices.get(index) : Double.NaN;
    }
}
