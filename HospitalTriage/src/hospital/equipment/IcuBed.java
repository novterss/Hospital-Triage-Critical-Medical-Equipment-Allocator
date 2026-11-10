package hospital.equipment;

/** Intensive-care hospital bed with extended 200-hour service interval. */
public class IcuBed extends MedicalEquipment {

    private static final double HOURLY_COST = 3000.0;

    private final String ward;

    IcuBed(String id, String model) {
        this(id, model, "ICU-A");
    }

    IcuBed(String id, String model, String ward) {
        super(id, EquipmentType.ICU_BED, model);
        this.ward = ward;
    }

    public String getWard() {
        return ward;
    }

    @Override
    public double getHourlyCost() {
        return HOURLY_COST;
    }

    @Override
    public String getSpecification() {
        return "Electric ICU bed in ward " + ward + " with pressure-relief mattress";
    }

    // Deep-clean / service interval is 200h (longer than active devices)
    @Override
    public double getServiceIntervalHours() {
        return 200.0;
    }
}
