package hospital.model;

/** Physician authorized to approve life-support equipment. */
public class Doctor extends Staff {

    private static int counter = 0;

    private final String specialty;

    public Doctor(String name, String specialty) {
        this(name, 40, specialty);
    }

    public Doctor(String name, int age, String specialty) {
        super(String.format("D-%03d", ++counter), name, age);
        this.specialty = specialty;
    }

    public String getSpecialty() {
        return specialty;
    }

    @Override
    public String getRole() {
        return "Doctor";
    }

    @Override
    public boolean canAuthorizeLifeSupport() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "Dr. " + name + " (" + specialty + ")";
    }
}
