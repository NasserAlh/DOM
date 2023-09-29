package day8;

import java.util.concurrent.ConcurrentHashMap;

public class SharedVolumeProfile {
    public static final ConcurrentHashMap<Double, Integer> volumeProfile = new ConcurrentHashMap<>();
}