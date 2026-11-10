package hospital.exception;

/** Thrown when equipment allocation violates staff authorization or triage rules. */
public class UnauthorizedAllocationException extends Exception {

    private static final long serialVersionUID = 1L;

    public UnauthorizedAllocationException(String message) {
        super(message);
    }
}
