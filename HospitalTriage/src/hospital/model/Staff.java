package hospital.model;

/** Base class for clinical staff members. */
public abstract class Staff extends Person {

    protected Staff(String id, String name, int age) {
        super(id, name, age);
    }

    // Only qualified roles (e.g. doctors) may authorize ventilators or defibrillators
    public abstract boolean canAuthorizeLifeSupport();

    public String getDisplayName() {
        return name + " (" + getRole() + ")";
    }
}
