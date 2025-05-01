package tools.vitruv.dsls.tgg.emoflonintegration;

import java.util.concurrent.TimeUnit;

/**
 * Measure time between calling {@link Timer#start()} and {@link Timer#stop()} in nanoseconds with {@link System#nanoTime()}.
 */
public class Timer {
    private long startTime;
    private long stopTime;

    public Timer() { }

    public Timer(long startTime, long stopTime) {
        this.startTime = startTime;
        this.stopTime = stopTime;
    }

    public void start() {
        startTime = System.nanoTime();
    }

    public void stop() {
        stopTime = System.nanoTime();
    }

    /**
     * @return duration of this timer in the given unit
     */
    public long getTime(TimeUnit unit) {
        return unit.convert(stopTime - startTime, TimeUnit.NANOSECONDS);
    }

    public String toString() {
        return "("
                + getTimeString(TimeUnit.MILLISECONDS) + " | "
                + getTimeString(TimeUnit.SECONDS) + " | "
                + getTimeString(TimeUnit.MINUTES) + ")";
    }

    public String getTimeString(TimeUnit unit) {
        return switch (unit) {
            case NANOSECONDS -> getTime(unit) + " ns";
            case MICROSECONDS -> getTime(unit) + " us";
            case MILLISECONDS -> getTime(unit) + " ms";
            case SECONDS -> {
                long milliseconds = getTime(TimeUnit.MILLISECONDS);
                yield fillToNChars(milliseconds / 1000, 2)
                        + "."
                        + fillToNChars(milliseconds % 1000, 3) + "s";
            }
            case MINUTES -> {
                long seconds = getTime(TimeUnit.SECONDS);
                yield fillToNChars(seconds / 60, 2)
                        + ":"
                        + fillToNChars(seconds % 60, 2) + " min";
            }
            default -> getTime(unit) + " " + unit.toString();
        };
    }

    private String fillToNChars(long number, int n) {
        String numberString = Long.toString(number);
        int i = numberString.length();
        while (i++ < n) {
            numberString = "0" + numberString;
        }
        return numberString;
    }

    /**
     * [Helper]
     * @param other the other timer
     * @return an artificial Timer that has the added duration of this and the other timer.
     */
    public Timer add(Timer other) {
        return new Timer(0,
                this.getTime(TimeUnit.NANOSECONDS) + other.getTime(TimeUnit.NANOSECONDS));
    }
}
