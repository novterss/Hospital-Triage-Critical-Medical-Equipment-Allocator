package hospital.equipment;

/** Volumetric infusion pump for controlled fluid and medication delivery. */
public class InfusionPump extends MedicalEquipment {

    private static final double HOURLY_COST = 150.0;

    private final int channels;

    InfusionPump(String id, String model) {
        super(id, EquipmentType.INFUSION_PUMP, model);
        this.channels = 2;
    }

    public int getChannels() {
        return channels;
    }

    @Override
    public double getHourlyCost() {
        return HOURLY_COST;
    }

    @Override
    public String getSpecification() {
        return channels + "-channel volumetric pump, 0.1-1200 mL/h";
    }
}
