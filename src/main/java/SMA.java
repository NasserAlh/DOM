
import java.util.LinkedList;
import java.util.Queue;

public class SMA {

    private Queue<Double> window = new LinkedList<>();
    private int period;
    private double sum = 0;

    public SMA(int period) {
        this.period = period;
    }

    public Double calculate(double newPrice) {
        sum += newPrice;
        window.add(newPrice);

        if (window.size() > period) {
            sum -= window.remove();
        }

        return (window.size() == period) ? sum / period : null;
    }
}

