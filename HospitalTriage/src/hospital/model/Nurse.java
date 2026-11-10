package hospital.model;

/** Registered nurse assigned to a ward (cannot authorize life-support equipment). */
public class Nurse extends Staff {

    private static int counter = 0;

    private final String ward;

    public Nurse(String name, String ward) {
        this(name, 30, ward);
    }

    public Nurse(String name, int age, String ward) {
        super(String.format("N-%03d", ++counter), name, age);
        this.ward = ward;
    }

    public String getWard() {
        return ward;
    }

    @Override
    public String getRole() {
        return "Nurse";
    }

    @Override
    public boolean canAuthorizeLifeSupport() {
        return false;
    }
}
