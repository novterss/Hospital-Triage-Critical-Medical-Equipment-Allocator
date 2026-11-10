package hospital.device;

import hospital.exception.InvalidVitalSignsException;
import hospital.model.VitalSigns;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapts a LegacyVendorMonitor so it can be used as a VitalSignsSource.
 */
public class LegacyMonitorAdapter implements VitalSignsSource {

    private final LegacyVendorMonitor monitor;

    public LegacyMonitorAdapter(LegacyVendorMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public VitalSigns readVitals() throws InvalidVitalSignsException {
        String raw = monitor.getRawReading();
        Map<String, String> fields = parse(raw);
        try {
            int heartRate = Integer.parseInt(fields.get("HR"));
            int systolic = Integer.parseInt(fields.get("NIBP").split("/")[0]);
            int resp = Integer.parseInt(fields.get("RESP"));
            int spo2 = (int) Math.round(Double.parseDouble(fields.get("SAT")) * 100);     // fraction -> %
            double tempC = (Double.parseDouble(fields.get("TEMPF")) - 32) * 5.0 / 9.0;   // Fahrenheit -> Celsius
            tempC = Math.round(tempC * 10) / 10.0;
            return new VitalSigns(heartRate, systolic, resp, spo2, tempC);
        } catch (NumberFormatException | NullPointerException | ArrayIndexOutOfBoundsException e) {
            throw new InvalidVitalSignsException("Cannot read legacy monitor output: " + raw);
        }
    }

    @Override
    public String getSourceName() {
        return "Legacy monitor #" + monitor.getSerialNumber();
    }

    private static Map<String, String> parse(String raw) {
        Map<String, String> map = new HashMap<>();
        for (String part : raw.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }
}
