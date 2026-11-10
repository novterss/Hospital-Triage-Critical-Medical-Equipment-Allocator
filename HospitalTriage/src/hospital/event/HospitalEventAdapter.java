package hospital.event;

import hospital.equipment.EquipmentType;
import hospital.equipment.MedicalEquipment;
import hospital.model.Patient;

/**
 * Empty adapter for HospitalEventListener so subclasses only override the callbacks they need.
 */
public abstract class HospitalEventAdapter implements HospitalEventListener {

    @Override
    public void onPatientAdmitted(Patient patient) {
    }

    @Override
    public void onPatientCalled(Patient patient) {
    }

    @Override
    public void onPatientDischarged(Patient patient) {
    }

    @Override
    public void onEquipmentAllocated(MedicalEquipment equipment, Patient patient) {
    }

    @Override
    public void onEquipmentReleased(MedicalEquipment equipment, Patient patient) {
    }

    @Override
    public void onEquipmentShortage(EquipmentType type, Patient patient, String reason) {
    }

    @Override
    public void onLog(String message) {
    }
}
