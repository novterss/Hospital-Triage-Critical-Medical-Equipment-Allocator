package hospital.equipment;

import java.util.EnumMap;
import java.util.Map;

/** Factory creating medical equipment subtypes with sequential IDs (e.g. VEN-001). */
public final class EquipmentFactory {

    private static final Map<EquipmentType, Integer> SERIALS = new EnumMap<>(EquipmentType.class);

    private EquipmentFactory() {
    }

    public static MedicalEquipment create(EquipmentType type) {
        return create(type, defaultModel(type));
    }

    public static MedicalEquipment create(EquipmentType type, String model) {
        String id = nextId(type);
        switch (type) {
            case VENTILATOR:
                return new Ventilator(id, model);
            case DEFIBRILLATOR:
                return new Defibrillator(id, model);
            case CARDIAC_MONITOR:
                return new CardiacMonitor(id, model);
            case ICU_BED:
                return new IcuBed(id, model);
            case INFUSION_PUMP:
                return new InfusionPump(id, model);
            default:
                throw new IllegalArgumentException("Unknown equipment type " + type);
        }
    }

    public static Ventilator createVentilator() {
        return new Ventilator(nextId(EquipmentType.VENTILATOR), defaultModel(EquipmentType.VENTILATOR));
    }

    static String nextId(EquipmentType type) {
        int next = SERIALS.getOrDefault(type, 0) + 1;
        SERIALS.put(type, next);
        return String.format("%s-%03d", type.getIdPrefix(), next);
    }

    private static String defaultModel(EquipmentType type) {
        switch (type) {
            case VENTILATOR:
                return "Hamilton C6";
            case DEFIBRILLATOR:
                return "Zoll R Series";
            case CARDIAC_MONITOR:
                return "Philips IntelliVue MX450";
            case ICU_BED:
                return "Hill-Rom Progressa";
            case INFUSION_PUMP:
                return "B. Braun Infusomat";
            default:
                return "Generic";
        }
    }
}
