package hospital.device;

import java.util.Random;

/**
 * Simulates a legacy third-party bedside monitor returning raw text lines
 * (SpO2 as a decimal fraction, temperature in Fahrenheit).
 * Format: HR=118;NIBP=88/54;RESP=26;SAT=0.88;TEMPF=101.8
 */
public class LegacyVendorMonitor {

    private final String serialNumber;
    private final Random random;

    public LegacyVendorMonitor(String serialNumber, long seed) {
        this.serialNumber = serialNumber;
        this.random = new Random(seed);
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    // Simulates a mix of mild and critical patient readings
    public String getRawReading() {
        boolean critical = random.nextInt(3) == 0;
        int hr = critical ? 125 + random.nextInt(40) : 70 + random.nextInt(35);
        int sys = critical ? 72 + random.nextInt(20) : 105 + random.nextInt(30);
        int dia = sys - 35;
        int resp = critical ? 26 + random.nextInt(14) : 12 + random.nextInt(8);
        double sat = critical ? 0.80 + random.nextInt(10) / 100.0 : 0.94 + random.nextInt(6) / 100.0;
        double tempF = 97.5 + random.nextInt(50) / 10.0;
        return String.format(java.util.Locale.US, "HR=%d;NIBP=%d/%d;RESP=%d;SAT=%.2f;TEMPF=%.1f",
                hr, sys, dia, resp, sat, tempF);
    }
}
