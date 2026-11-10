package hospital.model;

/** Emergency Severity Index (ESI) levels 1 (most critical) through 5 (non-urgent). */
public enum TriageLevel {

    ESI_1(1, "Resuscitation", "กู้ชีพทันที", 0, 0xC62828),
    ESI_2(2, "Emergent", "ฉุกเฉินวิกฤต", 10, 0xEF6C00),
    ESI_3(3, "Urgent", "เร่งด่วน", 30, 0xF9A825),
    ESI_4(4, "Less urgent", "ไม่รุนแรง", 60, 0x2E7D32),
    ESI_5(5, "Non-urgent", "ไม่เร่งด่วน", 120, 0x1565C0);

    private final int level;
    private final String nameEn;
    private final String nameTh;
    private final int targetWaitMinutes;
    private final int rgb;

    TriageLevel(int level, String nameEn, String nameTh, int targetWaitMinutes, int rgb) {
        this.level = level;
        this.nameEn = nameEn;
        this.nameTh = nameTh;
        this.targetWaitMinutes = targetWaitMinutes;
        this.rgb = rgb;
    }

    public int getLevel() {
        return level;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getNameTh() {
        return nameTh;
    }

    public int getTargetWaitMinutes() {
        return targetWaitMinutes;
    }

    // Kept as 0xRRGGBB int so the model stays independent of java.awt.Color
    public int getRgb() {
        return rgb;
    }

    public boolean isCritical() {
        return level <= 2;
    }

    public static TriageLevel fromLevel(int level) {
        for (TriageLevel t : values()) {
            if (t.level == level) {
                return t;
            }
        }
        throw new IllegalArgumentException("ESI level must be 1-5 but was " + level);
    }

    @Override
    public String toString() {
        return "ESI " + level + " - " + nameEn;
    }
}
