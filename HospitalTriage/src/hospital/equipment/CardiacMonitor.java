package hospital.equipment;

/** Multi-parameter cardiac monitor (non-life-support). */
public class CardiacMonitor extends MedicalEquipment {

    private static final double HOURLY_COST = 200.0;

    private final int leads;

    CardiacMonitor(String id, String model) {
        super(id, EquipmentType.CARDIAC_MONITOR, model);
        this.leads = 12;
    }

    public int getLeads() {
        return leads;
    }

    @Override
    public double getHourlyCost() {
        return HOURLY_COST;
    }

    @Override
    public String getSpecification() {
        return leads + "-lead ECG, SpO2, NIBP, alarms";
    }
}
