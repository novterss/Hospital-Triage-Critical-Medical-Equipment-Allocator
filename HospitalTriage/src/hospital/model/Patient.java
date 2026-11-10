package hospital.model;

import hospital.contract.Prioritizable;
import hospital.equipment.MedicalEquipment;
import hospital.triage.TriageResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Emergency-room patient record with vitals, triage score, and assigned equipment.
 */
public class Patient extends Person implements Prioritizable {

    private static int sequence = 0;

    public static final int MAX_PAIN = 10;

    private final long arrivalOrder;
    private final int arrivalMinute;
    private String chiefComplaint;
    private VitalSigns vitals;
    private int painScore;
    private Consciousness consciousness;
    private int expectedResources;
    private boolean highRiskSituation;

    private TriageResult triageResult;
    private PatientStatus status = PatientStatus.WAITING;
    private final List<MedicalEquipment> assignedEquipment = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();

    /**
     * @param expectedResources estimated ER resources needed (labs, X-ray, IV fluids, etc.) for ESI 3-5
     * @param highRiskSituation chest pain, stroke signs, suicidal risk, etc. (triggers ESI 2)
     */
    public Patient(String name, int age, String chiefComplaint, VitalSigns vitals,
                   int painScore, Consciousness consciousness, int expectedResources,
                   boolean highRiskSituation, int arrivalMinute) {
        super(String.format("P-%04d", ++sequence), name, age);
        this.arrivalOrder = sequence;
        this.arrivalMinute = arrivalMinute;
        setChiefComplaint(chiefComplaint);
        updateVitals(vitals);
        setPainScore(painScore);
        setConsciousness(consciousness);
        setExpectedResources(expectedResources);
        this.highRiskSituation = highRiskSituation;
    }

    // Convenience constructor for a stable walk-in patient
    public Patient(String name, int age, String chiefComplaint, VitalSigns vitals) {
        this(name, age, chiefComplaint, vitals, 0, Consciousness.ALERT, 1, false, 0);
    }

    @Override
    public String getRole() {
        return "Patient";
    }

    public boolean isPediatric() {
        return false;
    }

    // Higher score means higher urgency: ESI level dominates, pain breaks ties
    @Override
    public int getPriorityScore() {
        if (triageResult == null) {
            return 0;
        }
        return (6 - triageResult.getLevel().getLevel()) * 100 + painScore;
    }

    @Override
    public long getArrivalOrder() {
        return arrivalOrder;
    }

    public int getArrivalMinute() {
        return arrivalMinute;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    // Final because it is called directly from the constructor
    public final void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = (chiefComplaint == null || chiefComplaint.isBlank())
                ? "Not specified" : chiefComplaint.trim();
    }

    // Return a defensive copy so external edits don't mutate patient vitals
    public VitalSigns getVitals() {
        return new VitalSigns(vitals);
    }

    public final void updateVitals(VitalSigns newVitals) {
        if (newVitals == null) {
            throw new IllegalArgumentException("Vital signs are required");
        }
        this.vitals = new VitalSigns(newVitals);
    }

    public int getPainScore() {
        return painScore;
    }

    public final void setPainScore(int painScore) {
        if (painScore < 0 || painScore > MAX_PAIN) {
            throw new IllegalArgumentException("Pain score must be 0-10");
        }
        this.painScore = painScore;
    }

    public Consciousness getConsciousness() {
        return consciousness;
    }

    public final void setConsciousness(Consciousness consciousness) {
        this.consciousness = (consciousness == null) ? Consciousness.ALERT : consciousness;
    }

    public int getExpectedResources() {
        return expectedResources;
    }

    public final void setExpectedResources(int expectedResources) {
        if (expectedResources < 0) {
            throw new IllegalArgumentException("Expected resources cannot be negative");
        }
        this.expectedResources = expectedResources;
    }

    public boolean isHighRiskSituation() {
        return highRiskSituation;
    }

    public void setHighRiskSituation(boolean highRiskSituation) {
        this.highRiskSituation = highRiskSituation;
    }

    public TriageResult getTriageResult() {
        return triageResult;
    }

    public void setTriageResult(TriageResult triageResult) {
        this.triageResult = triageResult;
    }

    public TriageLevel getTriageLevel() {
        return triageResult == null ? null : triageResult.getLevel();
    }

    public PatientStatus getStatus() {
        return status;
    }

    public void setStatus(PatientStatus status) {
        this.status = status;
    }

    public List<MedicalEquipment> getAssignedEquipment() {
        return Collections.unmodifiableList(assignedEquipment);
    }

    public void attachEquipment(MedicalEquipment equipment) {
        if (!assignedEquipment.contains(equipment)) {
            assignedEquipment.add(equipment);
        }
    }

    public void detachEquipment(MedicalEquipment equipment) {
        assignedEquipment.remove(equipment);
    }

    public void addNote(String note) {
        notes.add(note);
    }

    public List<String> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    @Override
    public String toString() {
        String level = triageResult == null ? "not triaged" : getTriageLevel().toString();
        return super.toString() + " (" + age + "y, " + level + ")";
    }
}
