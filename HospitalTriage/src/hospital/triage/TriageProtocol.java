package hospital.triage;

import hospital.equipment.EquipmentType;
import hospital.model.Consciousness;
import hospital.model.Patient;
import hospital.model.TriageLevel;
import hospital.model.VitalSigns;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Base ESI v4 triage protocol. Runs steps A-D in fixed order:
 *   A. Immediate life-saving intervention needed? -> ESI 1
 *   B. High-risk, confused/lethargic, or severe pain? -> ESI 2
 *   C. Resource count (2+ -> ESI 3, 1 -> ESI 4, 0 -> ESI 5)
 *   D. If 2+ resources and vitals in danger zone -> upgrade to ESI 2
 * Subclasses provide age-specific thresholds for A and D.
 */
public abstract class TriageProtocol {

    public static final int SEVERE_PAIN = 7;

    private final String name;

    protected TriageProtocol(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public final TriageResult assess(Patient patient) {
        List<String> reasons = new ArrayList<>();
        VitalSigns v = patient.getVitals();
        TriageLevel level;

        if (requiresImmediateIntervention(patient, v, reasons)) {                   // A
            level = TriageLevel.ESI_1;
        } else if (isHighRisk(patient, v, reasons)) {                               // B
            level = TriageLevel.ESI_2;
        } else {
            int resources = estimateResources(patient);                             // C
            if (resources >= 2) {
                level = hasDangerZoneVitals(patient, v, reasons)                    // D
                        ? TriageLevel.ESI_2 : TriageLevel.ESI_3;
                if (level == TriageLevel.ESI_3) {
                    reasons.add("Needs 2+ resources / ต้องใช้ทรัพยากร 2 อย่างขึ้นไป");
                }
            } else if (resources == 1) {
                level = TriageLevel.ESI_4;
                reasons.add("Needs 1 resource / ต้องใช้ทรัพยากร 1 อย่าง");
            } else {
                level = TriageLevel.ESI_5;
                reasons.add("No resources needed / ไม่ต้องใช้ทรัพยากร");
            }
        }
        Set<EquipmentType> equipment = recommendEquipment(level, patient, v);
        return new TriageResult(level, reasons, equipment, name);
    }

    // Step A: immediate life-threatening signs
    protected abstract boolean requiresImmediateIntervention(Patient p, VitalSigns v, List<String> reasons);

    // Step D: danger-zone vitals for 2+ resource cases
    protected abstract boolean hasDangerZoneVitals(Patient p, VitalSigns v, List<String> reasons);

    protected boolean isHighRisk(Patient p, VitalSigns v, List<String> reasons) {
        boolean highRisk = false;
        if (p.isHighRiskSituation()) {
            reasons.add("High-risk situation / สถานการณ์เสี่ยงสูง");
            highRisk = true;
        }
        if (p.getConsciousness() == Consciousness.VOICE) {
            reasons.add("Confused or lethargic / ซึมหรือสับสน");
            highRisk = true;
        }
        if (p.getPainScore() >= SEVERE_PAIN) {
            reasons.add("Severe pain " + p.getPainScore() + "/10 / ปวดรุนแรง");
            highRisk = true;
        }
        return highRisk;
    }

    protected int estimateResources(Patient p) {
        return p.getExpectedResources();
    }

    protected Set<EquipmentType> recommendEquipment(TriageLevel level, Patient p, VitalSigns v) {
        Set<EquipmentType> set = EnumSet.noneOf(EquipmentType.class);
        switch (level) {
            case ESI_1:
                set.add(EquipmentType.CARDIAC_MONITOR);
                set.add(EquipmentType.ICU_BED);
                if (needsAirwaySupport(p, v)) {
                    set.add(EquipmentType.VENTILATOR);
                }
                if (hasLethalRhythmRisk(p, v)) {
                    set.add(EquipmentType.DEFIBRILLATOR);
                }
                break;
            case ESI_2:
                set.add(EquipmentType.CARDIAC_MONITOR);
                if (v.getSystolicBP() < 90) {
                    set.add(EquipmentType.INFUSION_PUMP);
                }
                break;
            case ESI_3:
                set.add(EquipmentType.INFUSION_PUMP);
                break;
            default:
                // ESI 4-5 don't need critical equipment
                break;
        }
        return set;
    }

    protected boolean needsAirwaySupport(Patient p, VitalSigns v) {
        return v.getOxygenSaturation() < 90 || v.getRespiratoryRate() < 8
                || v.getRespiratoryRate() > 35 || p.getConsciousness() == Consciousness.UNRESPONSIVE;
    }

    protected boolean hasLethalRhythmRisk(Patient p, VitalSigns v) {
        return v.getHeartRate() < 40 || v.getHeartRate() > 150;
    }

    @Override
    public String toString() {
        return name;
    }
}
