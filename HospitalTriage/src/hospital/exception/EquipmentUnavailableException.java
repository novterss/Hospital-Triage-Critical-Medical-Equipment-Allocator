package hospital.exception;

/** Thrown when requested equipment is unavailable or cannot be pre-empted. */
public class EquipmentUnavailableException extends Exception {

    private static final long serialVersionUID = 1L;

    public EquipmentUnavailableException(String message) {
        super(message);
    }
}
