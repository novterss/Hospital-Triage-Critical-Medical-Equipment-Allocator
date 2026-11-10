package hospital.service;

import hospital.equipment.EquipmentType;
import hospital.equipment.LifeSupport;
import hospital.equipment.MedicalEquipment;
import hospital.exception.EquipmentUnavailableException;
import hospital.exception.UnauthorizedAllocationException;
import hospital.generic.Inventory;
import hospital.model.Patient;
import hospital.model.PatientStatus;
import hospital.model.Staff;
import hospital.model.TriageLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles equipment allocation and pre-emption rules:
 * 1. Allocate a free device of the requested type if available.
 * 2. Otherwise pre-empt from the least urgent holder (strictly lower ESI only).
 * 3. Life-support devices require doctor authorization and an ESI 1-2 patient.
 */
class AllocationEngine {

    // Tie-breaker: pre-empt from the patient furthest back in queue order
    private static final java.util.Comparator<Patient> QUEUE_ORDER = hospital.contract.Prioritizable.byUrgency();

    private final HospitalSystem system;

    AllocationEngine(HospitalSystem system) {
        this.system = system;
    }

    MedicalEquipment allocate(Patient patient, EquipmentType type, Staff by)
            throws EquipmentUnavailableException, UnauthorizedAllocationException {
        if (patient.getStatus() == PatientStatus.DISCHARGED) {
            throw new IllegalStateException(patient.getName() + " has already been discharged");
        }
        for (MedicalEquipment owned : patient.getAssignedEquipment()) {
            if (owned.getType() == type) {
                throw new EquipmentUnavailableException(patient.getName() + " already has "
                        + owned.getId() + " / ผู้ป่วยมีอุปกรณ์นี้อยู่แล้ว");
            }
        }

        Inventory<MedicalEquipment> inventory = system.inventory();

        // 1. Allocate an available device if one exists
        Optional<MedicalEquipment> free = inventory.findFirst(e -> e.getType() == type && e.isAvailable());
        if (free.isPresent()) {
            MedicalEquipment device = free.get();
            authorize(device, patient, by);
            assign(device, patient, by, AllocationRecord.Action.ALLOCATED);
            return device;
        }

        // 2. Otherwise pre-empt from the least urgent holder (strictly lower ESI only)
        MedicalEquipment bestDevice = null;
        Patient bestVictim = null;
        for (MedicalEquipment device : inventory.findAll(e -> e.getType() == type && e.getAssignedPatient() != null)) {
            Patient holder = device.getAssignedPatient();
            if (isStrictlyMoreUrgent(patient, holder)
                    && (bestVictim == null || isStrictlyMoreUrgent(bestVictim, holder)
                    || (sameLevel(holder, bestVictim) && QUEUE_ORDER.compare(holder, bestVictim) > 0))) {
                bestVictim = holder;
                bestDevice = device;
            }
        }
        if (bestDevice == null) {
            throw new EquipmentUnavailableException("No " + type.getDisplayName()
                    + " available and every current user is at least as urgent"
                    + " / ไม่มี" + type.getThaiName() + "ว่าง");
        }
        authorize(bestDevice, patient, by);
        release(bestDevice, AllocationRecord.Action.PREEMPTED, by);
        bestVictim.addNote(system.clockText() + " " + bestDevice.getId()
                + " reassigned to more critical patient " + patient.getId()
                + " / อุปกรณ์ถูกโอนให้ผู้ป่วยที่วิกฤตกว่า");
        system.log("PRE-EMPTED " + bestDevice.getId() + " from " + bestVictim.getName()
                + " (" + bestVictim.getTriageLevel() + ") for " + patient.getName()
                + " (" + patient.getTriageLevel() + ")");
        assign(bestDevice, patient, by, AllocationRecord.Action.ALLOCATED);
        return bestDevice;
    }

    // Best-effort allocation of all protocol-recommended devices; returns error messages for any that fail.
    List<String> allocateRecommended(Patient patient, Staff by) {
        List<String> problems = new ArrayList<>();
        if (patient.getTriageResult() == null) {
            return problems;
        }
        for (EquipmentType type : patient.getTriageResult().getRecommendedEquipment()) {
            try {
                allocate(patient, type, by);
            } catch (EquipmentUnavailableException | UnauthorizedAllocationException e) {
                problems.add(type.getDisplayName() + ": " + e.getMessage());
                system.fire(l -> l.onEquipmentShortage(type, patient, e.getMessage()));
                system.log("SHORTAGE " + type.getDisplayName() + " for " + patient.getName() + ": " + e.getMessage());
            }
        }
        return problems;
    }

    void release(MedicalEquipment device, AllocationRecord.Action action, Staff by) {
        Patient previous = device.release();
        if (previous != null) {
            previous.detachEquipment(device);
            system.record(new AllocationRecord(system.getClockMinutes(), action, device.getId(),
                    device.getType(), previous.getId(), previous.getName(), nameOf(by)));
            system.fire(l -> l.onEquipmentReleased(device, previous));
        }
    }

    private void assign(MedicalEquipment device, Patient patient, Staff by, AllocationRecord.Action action)
            throws EquipmentUnavailableException {
        device.allocateTo(patient);
        patient.attachEquipment(device);
        system.record(new AllocationRecord(system.getClockMinutes(), action, device.getId(),
                device.getType(), patient.getId(), patient.getName(), nameOf(by)));
        system.fire(l -> l.onEquipmentAllocated(device, patient));
        system.log("Allocated " + device.getId() + " to " + patient.getName() + " by " + nameOf(by));
    }

    private void authorize(MedicalEquipment device, Patient patient, Staff by) throws UnauthorizedAllocationException {
        if (device instanceof LifeSupport) {
            if (by == null || !by.canAuthorizeLifeSupport()) {
                throw new UnauthorizedAllocationException(device.getType().getDisplayName()
                        + " is life-support equipment and must be authorized by a doctor"
                        + " / อุปกรณ์ช่วยชีวิตต้องให้แพทย์อนุมัติ");
            }
            TriageLevel level = patient.getTriageLevel();
            if (level == null || !level.isCritical()) {
                throw new UnauthorizedAllocationException("Life-support equipment is reserved for ESI 1-2 patients"
                        + " / สงวนไว้สำหรับผู้ป่วยวิกฤต ESI 1-2");
            }
        }
    }

    private static boolean isStrictlyMoreUrgent(Patient a, Patient b) {
        if (a.getTriageLevel() == null) {
            return false;
        }
        if (b.getTriageLevel() == null) {
            return true;
        }
        return a.getTriageLevel().getLevel() < b.getTriageLevel().getLevel();
    }

    private static boolean sameLevel(Patient a, Patient b) {
        return a.getTriageLevel() == b.getTriageLevel();
    }

    private static String nameOf(Staff s) {
        return s == null ? "system" : s.getDisplayName();
    }
}
