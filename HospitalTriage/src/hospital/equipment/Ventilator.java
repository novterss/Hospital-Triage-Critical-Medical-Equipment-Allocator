package hospital.equipment;

/** Mechanical ventilator (life-support) requiring maintenance every 50 usage hours. */
public class Ventilator extends MedicalEquipment implements LifeSupport {

    private static final double HOURLY_COST = 1500.0;
    private static final double SERVICE_INTERVAL = 50.0;

    private final int maxTidalVolumeMl;

    Ventilator(String id, String model) {
        this(id, model, 2000);
    }

    Ventilator(String id, String model, int maxTidalVolumeMl) {
        super(id, EquipmentType.VENTILATOR, model);
        this.maxTidalVolumeMl = maxTidalVolumeMl;
    }

    public int getMaxTidalVolumeMl() {
        return maxTidalVolumeMl;
    }

    @Override
    public double getHourlyCost() {
        return HOURLY_COST;
    }

    @Override
    public String getSpecification() {
        return "Invasive/non-invasive ventilation, tidal volume up to " + maxTidalVolumeMl + " mL";
    }

    // Service every 50h (twice as often as standard devices)
    @Override
    public double getServiceIntervalHours() {
        return SERVICE_INTERVAL;
    }
}
