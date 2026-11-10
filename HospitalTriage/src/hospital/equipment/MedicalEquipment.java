package hospital.equipment;

import hospital.contract.Allocatable;
import hospital.contract.Maintainable;
import hospital.exception.EquipmentUnavailableException;
import hospital.model.Patient;

/** Abstract base class for medical devices tracking allocation and maintenance state. */
public abstract class MedicalEquipment implements Allocatable, Maintainable, Comparable<MedicalEquipment> {

    private final String id;
    private final EquipmentType type;
    private final String model;
    private Patient assignedPatient;
    private boolean underMaintenance;

    protected double usageHours;

    protected MedicalEquipment(String id, EquipmentType type, String model) {
        this.id = id;
        this.type = type;
        this.model = model;
    }

    // Cost in THB per hour
    public abstract double getHourlyCost();

    public abstract String getSpecification();

    @Override
    public boolean isAvailable() {
        return assignedPatient == null && !underMaintenance;
    }

    @Override
    public void allocateTo(Patient patient) throws EquipmentUnavailableException {
        if (patient == null) {
            throw new IllegalArgumentException("patient must not be null");
        }
        if (!isAvailable()) {
            throw new EquipmentUnavailableException(id + " is " + statusLabel().toLowerCase());
        }
        this.assignedPatient = patient;
    }

    @Override
    public Patient release() {
        Patient previous = assignedPatient;
        assignedPatient = null;
        return previous;
    }

    @Override
    public Patient getAssignedPatient() {
        return assignedPatient;
    }

    @Override
    public double getUsageHours() {
        return usageHours;
    }

    @Override
    public boolean isUnderMaintenance() {
        return underMaintenance;
    }

    @Override
    public void startMaintenance() {
        if (assignedPatient != null) {
            throw new IllegalStateException(id + " is in use by " + assignedPatient.getName()
                    + "; release it first / ต้องคืนอุปกรณ์ก่อน");
        }
        underMaintenance = true;
    }

    @Override
    public void completeMaintenance() {
        underMaintenance = false;
        usageHours = 0;
    }

    // Accumulate usage only while assigned to a patient
    public void recordUsage(double hours) {
        if (hours > 0 && assignedPatient != null) {
            usageHours += hours;
        }
    }

    // Maintenance state takes precedence over allocation status
    @Override
    public String statusLabel() {
        if (isUnderMaintenance()) {
            return Maintainable.super.statusLabel();
        }
        String allocation = Allocatable.super.statusLabel();
        return needsService() ? allocation + " (service due)" : allocation;
    }

    public final String getId() {
        return id;
    }

    public final EquipmentType getType() {
        return type;
    }

    public String getModel() {
        return model;
    }

    public boolean isLifeSupport() {
        return this instanceof LifeSupport;
    }

    @Override
    public int compareTo(MedicalEquipment other) {
        return this.id.compareTo(other.id);
    }

    @Override
    public String toString() {
        return id + " " + type.getDisplayName() + " [" + model + "]";
    }
}
