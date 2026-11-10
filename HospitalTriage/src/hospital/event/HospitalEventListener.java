package hospital.event;

import hospital.equipment.EquipmentType;
import hospital.equipment.MedicalEquipment;
import hospital.model.Patient;

/**
 * Callback interface for patient and equipment events fired by HospitalSystem.
 */
public interface HospitalEventListener {

    void onPatientAdmitted(Patient patient);

    void onPatientCalled(Patient patient);

    void onPatientDischarged(Patient patient);

    void onEquipmentAllocated(MedicalEquipment equipment, Patient patient);

    void onEquipmentReleased(MedicalEquipment equipment, Patient patient);

    void onEquipmentShortage(EquipmentType type, Patient patient, String reason);

    void onLog(String message);
}
