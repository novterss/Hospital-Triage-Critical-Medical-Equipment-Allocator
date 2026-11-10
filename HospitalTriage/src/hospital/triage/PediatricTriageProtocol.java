package hospital.triage;

import hospital.equipment.EquipmentType;
import hospital.model.Patient;
import hospital.model.TriageLevel;
import hospital.model.VitalSigns;

import java.util.List;
import java.util.Set;

/**
 * Pediatric ESI thresholds (under 15). Young children have higher baseline
 * heart and breathing rates, so adult limits would over-triage them.
 */
public class PediatricTriageProtocol extends TriageProtocol {

    public PediatricTriageProtocol() {
        super("ESI v4 - Pediatric");
    }

    @Override
    protected boolean requiresImmediateIntervention(Patient p, VitalSigns v, List<String> reasons) {
        boolean critical = false;
        int minSystolic = 70 + 2 * p.getAge(); // PALS hypotension rule
        if (p.getConsciousness().isUnresponsive()) {
            reasons.add("Unresponsive child / เด็กไม่ตอบสนอง");
            critical = true;
        }
        if (v.getHeartRate() < 60) {
            reasons.add("Bradycardia " + v.getHeartRate() + " bpm / หัวใจเต้นช้าอันตราย");
            critical = true;
        }
        if (v.getHeartRate() > 220) {
            reasons.add("Heart rate " + v.getHeartRate() + " bpm / ชีพจรเร็วมาก");
            critical = true;
        }
        if (v.getRespiratoryRate() < 10 || v.getRespiratoryRate() > 60) {
            reasons.add("Respiratory rate " + v.getRespiratoryRate() + "/min / การหายใจผิดปกติรุนแรง");
            critical = true;
        }
        if (v.getOxygenSaturation() < 90) {
            reasons.add("SpO2 " + v.getOxygenSaturation() + "% / ออกซิเจนต่ำมาก");
            critical = true;
        }
        if (v.getSystolicBP() < minSystolic) {
            reasons.add("Systolic BP " + v.getSystolicBP() + " < " + minSystolic + " (hypotension) / ความดันต่ำ");
            critical = true;
        }
        return critical;
    }

    @Override
    protected boolean hasDangerZoneVitals(Patient p, VitalSigns v, List<String> reasons) {
        int age = p.getAge();
        int maxHr;
        int maxRr;
        // Age-specific upper limits for heart rate and respiratory rate (<1, <3, <8, 8+)
        if (age < 1) {
            maxHr = 180;
            maxRr = 50;
        } else if (age < 3) {
            maxHr = 160;
            maxRr = 40;
        } else if (age < 8) {
            maxHr = 140;
            maxRr = 30;
        } else {
            maxHr = 100;
            maxRr = 20;
        }
        boolean danger = false;
        if (v.getHeartRate() > maxHr) {
            reasons.add("Danger zone: HR " + v.getHeartRate() + " > " + maxHr + " for age " + age);
            danger = true;
        }
        if (v.getRespiratoryRate() > maxRr) {
            reasons.add("Danger zone: RR " + v.getRespiratoryRate() + " > " + maxRr + " for age " + age);
            danger = true;
        }
        if (v.getOxygenSaturation() < 92) {
            reasons.add("Danger zone: SpO2 " + v.getOxygenSaturation() + "% < 92%");
            danger = true;
        }
        return danger;
    }

    // Children under 3 with fever >= 38.0 C are also flagged as high-risk (ESI 2)
    @Override
    protected boolean isHighRisk(Patient p, VitalSigns v, List<String> reasons) {
        boolean highRisk = super.isHighRisk(p, v, reasons);
        if (p.getAge() < 3 && v.getTemperature() >= 38.0) {
            reasons.add("Fever " + v.getTemperature() + " C in child under 3 / ไข้ในเด็กอายุต่ำกว่า 3 ปี");
            highRisk = true;
        }
        return highRisk;
    }

    // Children get a monitor even at ESI 3 because they decompensate quickly.
    @Override
    protected Set<EquipmentType> recommendEquipment(
            TriageLevel level, Patient p, VitalSigns v) {
        Set<EquipmentType> set = super.recommendEquipment(level, p, v);
        if (level == TriageLevel.ESI_3) {
            set.add(EquipmentType.CARDIAC_MONITOR);
        }
        return set;
    }
}
