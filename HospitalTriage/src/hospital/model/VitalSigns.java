package hospital.model;

import hospital.exception.InvalidVitalSignsException;

/**
 * Patient vital signs with physiological bounds validation.
 * Mutable - callers like Patient copy instances on read/write.
 */
public class VitalSigns {

    public static final int MIN_HEART_RATE = 0;
    public static final int MAX_HEART_RATE = 300;
    public static final int MIN_SYSTOLIC = 0;
    public static final int MAX_SYSTOLIC = 300;
    public static final int MIN_RESP_RATE = 0;
    public static final int MAX_RESP_RATE = 80;
    public static final int MIN_SPO2 = 0;
    public static final int MAX_SPO2 = 100;
    public static final double MIN_TEMP_C = 25.0;
    public static final double MAX_TEMP_C = 45.0;

    private int heartRate;          // bpm
    private int systolicBP;         // mmHg
    private int respiratoryRate;    // breaths/min
    private int oxygenSaturation;   // SpO2 %
    private double temperature;     // Celsius

    public VitalSigns(int heartRate, int systolicBP, int respiratoryRate,
                      int oxygenSaturation, double temperature) throws InvalidVitalSignsException {
        this.setHeartRate(heartRate);
        this.setSystolicBP(systolicBP);
        this.setRespiratoryRate(respiratoryRate);
        this.setOxygenSaturation(oxygenSaturation);
        this.setTemperature(temperature);
    }

    public VitalSigns(VitalSigns other) {
        this.heartRate = other.heartRate;
        this.systolicBP = other.systolicBP;
        this.respiratoryRate = other.respiratoryRate;
        this.oxygenSaturation = other.oxygenSaturation;
        this.temperature = other.temperature;
    }

    // Returns baseline normal adult vital signs
    public static VitalSigns normal() {
        try {
            return new VitalSigns(80, 120, 16, 98, 37.0);
        } catch (InvalidVitalSignsException e) {
            throw new IllegalStateException("normal values must be valid", e);
        }
    }

    public int getHeartRate() {
        return heartRate;
    }

    public final void setHeartRate(int heartRate) throws InvalidVitalSignsException {
        this.heartRate = requireRange("Heart rate", heartRate, MIN_HEART_RATE, MAX_HEART_RATE);
    }

    public int getSystolicBP() {
        return systolicBP;
    }

    public final void setSystolicBP(int systolicBP) throws InvalidVitalSignsException {
        this.systolicBP = requireRange("Systolic BP", systolicBP, MIN_SYSTOLIC, MAX_SYSTOLIC);
    }

    public int getRespiratoryRate() {
        return respiratoryRate;
    }

    public final void setRespiratoryRate(int respiratoryRate) throws InvalidVitalSignsException {
        this.respiratoryRate = requireRange("Respiratory rate", respiratoryRate, MIN_RESP_RATE, MAX_RESP_RATE);
    }

    public int getOxygenSaturation() {
        return oxygenSaturation;
    }

    public final void setOxygenSaturation(int oxygenSaturation) throws InvalidVitalSignsException {
        this.oxygenSaturation = requireRange("SpO2", oxygenSaturation, MIN_SPO2, MAX_SPO2);
    }

    public double getTemperature() {
        return temperature;
    }

    public final void setTemperature(double temperature) throws InvalidVitalSignsException {
        if (Double.isNaN(temperature) || temperature < MIN_TEMP_C || temperature > MAX_TEMP_C) {
            throw new InvalidVitalSignsException("Temperature", temperature, MIN_TEMP_C, MAX_TEMP_C);
        }
        this.temperature = temperature;
    }

    private static int requireRange(String field, int value, int min, int max)
            throws InvalidVitalSignsException {
        if (value < min || value > max) {
            throw new InvalidVitalSignsException(field, value, min, max);
        }
        return value;
    }

    @Override
    public String toString() {
        return String.format("HR %d bpm | BP %d mmHg | RR %d/min | SpO2 %d%% | T %.1f C",
                heartRate, systolicBP, respiratoryRate, oxygenSaturation, temperature);
    }
}
