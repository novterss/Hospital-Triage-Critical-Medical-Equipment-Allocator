package hospital.contract;

import hospital.exception.EquipmentUnavailableException;
import hospital.model.Patient;

/** Contract for hospital resources that can be allocated to one patient at a time. */
public interface Allocatable {

    boolean isAvailable();

    void allocateTo(Patient patient) throws EquipmentUnavailableException;

    // Returns the previously assigned patient, or null if none.
    Patient release();

    Patient getAssignedPatient();

    default String statusLabel() {
        return isAvailable() ? "Available" : "In use";
    }
}
