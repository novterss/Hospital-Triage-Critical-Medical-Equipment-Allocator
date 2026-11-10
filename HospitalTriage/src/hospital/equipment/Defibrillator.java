package hospital.equipment;

/** Biphasic defibrillator classified as life-support equipment. */
public class Defibrillator extends MedicalEquipment implements LifeSupport {

    private static final double HOURLY_COST = 500.0;

    private final int maxEnergyJoules;
    private int shocksDelivered;

    Defibrillator(String id, String model) {
        super(id, EquipmentType.DEFIBRILLATOR, model);
        this.maxEnergyJoules = 360;
    }

    public int getMaxEnergyJoules() {
        return maxEnergyJoules;
    }

    public int getShocksDelivered() {
        return shocksDelivered;
    }

    // Each shock counts as 0.5h of paddle wear toward service interval
    public void deliverShock() {
        shocksDelivered++;
        usageHours += 0.5;
    }

    @Override
    public double getHourlyCost() {
        return HOURLY_COST;
    }

    @Override
    public String getSpecification() {
        return "Biphasic, up to " + maxEnergyJoules + " J, AED + manual mode";
    }
}
