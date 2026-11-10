package hospital.model;

/** AVPU consciousness scale used during triage assessment. */
public enum Consciousness {
    ALERT("Alert", "รู้สึกตัวดี"),
    VOICE("Responds to voice", "ตอบสนองต่อเสียง"),
    PAIN("Responds to pain only", "ตอบสนองต่อความเจ็บปวด"),
    UNRESPONSIVE("Unresponsive", "ไม่ตอบสนอง");

    private final String labelEn;
    private final String labelTh;

    Consciousness(String labelEn, String labelTh) {
        this.labelEn = labelEn;
        this.labelTh = labelTh;
    }

    public String getLabelEn() {
        return labelEn;
    }

    public String getLabelTh() {
        return labelTh;
    }

    // ESI classifies Pain-responsive (P) and Unresponsive (U) as unresponsive
    public boolean isUnresponsive() {
        return this == PAIN || this == UNRESPONSIVE;
    }

    @Override
    public String toString() {
        // Short display label for UI drop-downs (e.g. "V - Voice / ตอบสนองต่อเสียง")
        String shortEn = labelEn.startsWith("Responds to ") ? labelEn.substring(12, 13).toUpperCase()
                + labelEn.substring(13).replace(" only", "") : labelEn;
        return name().charAt(0) + " - " + shortEn + " / " + labelTh;
    }
}
