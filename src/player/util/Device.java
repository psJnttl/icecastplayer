package player.util;

public class Device {
    private final static boolean forceNoSoundDevice = false;

    public static int forceNoSoundDevice(int device) {
        if(forceNoSoundDevice) {
            return 0;
        }
        return device;
    }

    public static int forceFrequency(int freq) {
        return freq;
    }
}
