package hospital.triage;

import hospital.model.Patient;
import hospital.model.VitalSigns;

import java.util.List;

/** ESI v4 triage thresholds for adult patients (15 years and older). */
public class AdultTriageProtocol extends TriageProtocol {

    public AdultTriageProtocol() {
        super("ESI v4 - Adult");
    }

    @Override
    protected boolean requiresImmediateIntervention(Patient p, VitalSigns v, List<String> reasons) {
        boolean critical = false;
        if (p.getConsciousness().isUnresponsive()) {
            reasons.add("Unresponsive (AVPU " + p.getConsciousness().name().charAt(0) + ") / ไม่ตอบสนอง");
            critical = true;
        }
        if (v.getHeartRate() < 40 || v.getHeartRate() > 150) {
            reasons.add("Heart rate " + v.getHeartRate() + " bpm / ชีพจรผิดปกติรุนแรง");
            critical = true;
        }
        if (v.getRespiratoryRate() < 8 || v.getRespiratoryRate() > 35) {
            reasons.add("Respiratory rate " + v.getRespiratoryRate() + "/min / การหายใจผิดปกติรุนแรง");
            critical = true;
        }
        if (v.getOxygenSaturation() < 88) {
            reasons.add("SpO2 " + v.getOxygenSaturation() + "% / ออกซิเจนต่ำมาก");
            critical = true;
        }
        if (v.getSystolicBP() < 80) {
            reasons.add("Systolic BP " + v.getSystolicBP() + " mmHg (shock) / ความดันต่ำ ภาวะช็อก");
            critical = true;
        }
        return critical;
    }

    @Override
    protected boolean hasDangerZoneVitals(Patient p, VitalSigns v, List<String> reasons) {
        boolean danger = false;
        if (v.getHeartRate() > 100) {
            reasons.add("Danger zone: HR " + v.getHeartRate() + " > 100 / ชีพจรเร็ว");
            danger = true;
        }
        if (v.getRespiratoryRate() > 20) {
            reasons.add("Danger zone: RR " + v.getRespiratoryRate() + " > 20 / หายใจเร็ว");
            danger = true;
        }
        if (v.getOxygenSaturation() < 92) {
            reasons.add("Danger zone: SpO2 " + v.getOxygenSaturation() + "% < 92% / ออกซิเจนต่ำ");
            danger = true;
        }
        return danger;
    }
}
