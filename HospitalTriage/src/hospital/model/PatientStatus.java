package hospital.model;

/** Lifecycle states for an ER patient. */
public enum PatientStatus {
    WAITING("Waiting", "รอตรวจ"),
    IN_TREATMENT("In treatment", "กำลังรักษา"),
    DISCHARGED("Discharged", "จำหน่ายแล้ว");

    private final String labelEn;
    private final String labelTh;

    PatientStatus(String labelEn, String labelTh) {
        this.labelEn = labelEn;
        this.labelTh = labelTh;
    }

    public String getLabelEn() {
        return labelEn;
    }

    public String getLabelTh() {
        return labelTh;
    }

    @Override
    public String toString() {
        return labelEn + " / " + labelTh;
    }
}
