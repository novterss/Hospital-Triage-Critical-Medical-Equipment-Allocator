package hospital.equipment;

/** Supported medical equipment categories and ID prefixes. */
public enum EquipmentType {
    VENTILATOR("Ventilator", "เครื่องช่วยหายใจ", "VEN"),
    DEFIBRILLATOR("Defibrillator", "เครื่องกระตุกหัวใจ", "DEF"),
    CARDIAC_MONITOR("Cardiac monitor", "เครื่องติดตามการทำงานของหัวใจ", "MON"),
    ICU_BED("ICU bed", "เตียง ICU", "ICU"),
    INFUSION_PUMP("Infusion pump", "เครื่องควบคุมการให้สารน้ำ", "INF");

    private final String displayName;
    private final String thaiName;
    private final String idPrefix;

    EquipmentType(String displayName, String thaiName, String idPrefix) {
        this.displayName = displayName;
        this.thaiName = thaiName;
        this.idPrefix = idPrefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getThaiName() {
        return thaiName;
    }

    public String getIdPrefix() {
        return idPrefix;
    }

    @Override
    public String toString() {
        return displayName + " / " + thaiName;
    }
}
