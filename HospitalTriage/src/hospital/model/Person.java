package hospital.model;

import java.util.Objects;

/**
 * Base class for patients and hospital staff.
 */
public abstract class Person {

    public static final int MAX_AGE = 130;

    private final String id;
    protected String name;
    protected int age;

    protected Person(String id, String name, int age) {
        this.id = Objects.requireNonNull(id, "id");
        // setName/setAge are final because they are called from the constructor
        this.setName(name);
        this.setAge(age);
    }

    public final String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public final void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be empty / ต้องระบุชื่อ");
        }
        this.name = name.trim();
    }

    public int getAge() {
        return age;
    }

    public final void setAge(int age) {
        if (age < 0 || age > MAX_AGE) {
            throw new IllegalArgumentException("Age must be 0-" + MAX_AGE + " / อายุไม่ถูกต้อง");
        }
        this.age = age;
    }

    public abstract String getRole();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Person)) {
            return false;
        }
        return id.equals(((Person) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getRole() + " " + id + " " + name;
    }
}
