package hospital.service;

import hospital.equipment.EquipmentType;

/**
 * Immutable audit record for equipment allocation, release, and pre-emption events.
 */
public final class AllocationRecord {

    public enum Action {
        ALLOCATED, RELEASED, PREEMPTED
    }

    private final int minute;
    private final Action action;
    private final String equipmentId;
    private final EquipmentType equipmentType;
    private final String patientId;
    private final String patientName;
    private final String staffName;

    AllocationRecord(int minute, Action action, String equipmentId, EquipmentType equipmentType,
                     String patientId, String patientName, String staffName) {
        this.minute = minute;
        this.action = action;
        this.equipmentId = equipmentId;
        this.equipmentType = equipmentType;
        this.patientId = patientId;
        this.patientName = patientName;
        this.staffName = staffName;
    }

    public int getMinute() {
        return minute;
    }

    public Action getAction() {
        return action;
    }

    public String getEquipmentId() {
        return equipmentId;
    }

    public EquipmentType getEquipmentType() {
        return equipmentType;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getStaffName() {
        return staffName;
    }

    @Override
    public String toString() {
        return String.format("[%s] %-9s %s -> %s %s (by %s)", HospitalSystem.formatMinutes(minute),
                action, equipmentId, patientId, patientName, staffName);
    }
}
