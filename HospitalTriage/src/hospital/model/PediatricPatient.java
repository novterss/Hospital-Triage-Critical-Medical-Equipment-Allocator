package hospital.model;

/** Pediatric patient (under 15 years) assessed with age-adjusted triage thresholds. */
public class PediatricPatient extends Patient {

    public static final int AGE_LIMIT = 15;

    // Give children slight priority over adults within the same ESI level
    public static final int PEDIATRIC_PRIORITY_BONUS = 5;

    private String guardianName;

    public PediatricPatient(String name, int age, String chiefComplaint, VitalSigns vitals,
                            int painScore, Consciousness consciousness, int expectedResources,
                            boolean highRiskSituation, int arrivalMinute, String guardianName) {
        super(name, age, chiefComplaint, vitals, painScore, consciousness,
                expectedResources, highRiskSituation, arrivalMinute);
        if (age >= AGE_LIMIT) {
            throw new IllegalArgumentException("Pediatric patients must be younger than " + AGE_LIMIT);
        }
        this.guardianName = (guardianName == null || guardianName.isBlank()) ? "-" : guardianName.trim();
    }

    @Override
    public String getRole() {
        return "Pediatric patient";
    }

    @Override
    public boolean isPediatric() {
        return true;
    }

    @Override
    public int getPriorityScore() {
        int base = super.getPriorityScore();
        return base == 0 ? 0 : base + PEDIATRIC_PRIORITY_BONUS;
    }

    // Age bracket used by pediatric vital-sign thresholds
    public String getAgeGroup() {
        if (age < 1) {
            return "Infant";
        } else if (age < 3) {
            return "Toddler";
        } else if (age < 8) {
            return "Child";
        }
        return "Adolescent";
    }

    public String getGuardianName() {
        return guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = guardianName;
    }
}
