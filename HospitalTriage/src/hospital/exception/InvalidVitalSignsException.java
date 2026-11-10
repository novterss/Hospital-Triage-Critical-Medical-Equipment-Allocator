package hospital.exception;

/**
 * Thrown when vital sign values are out of range or cannot be parsed.
 */
public class InvalidVitalSignsException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String field;
    private final double value;

    public InvalidVitalSignsException(String field, double value, double min, double max) {
        super(String.format("%s = %s is out of range (%s - %s)",
                field, trim(value), trim(min), trim(max)));
        this.field = field;
        this.value = value;
    }

    public InvalidVitalSignsException(String message) {
        super(message);
        this.field = "unknown";
        this.value = Double.NaN;
    }

    public String getField() {
        return field;
    }

    public double getValue() {
        return value;
    }

    private static String trim(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}
