package hospital.generic;

import hospital.equipment.EquipmentFactory;
import hospital.equipment.EquipmentType;
import hospital.equipment.MedicalEquipment;
import hospital.equipment.Ventilator;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Summary and reporting helpers for medical equipment.
 */
public final class EquipmentReports {

    private EquipmentReports() {
    }

    public static String describeAll(Collection<?> items) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Object o : items) {
            sb.append(i++).append(". ").append(o).append(System.lineSeparator());
        }
        return sb.toString();
    }

    public static double totalHourlyCostInUse(Collection<? extends MedicalEquipment> items) {
        double total = 0;
        for (MedicalEquipment e : items) {
            if (e.getAssignedPatient() != null) {
                total += e.getHourlyCost();
            }
        }
        return total;
    }

    public static Map<EquipmentType, int[]> utilization(Iterable<? extends MedicalEquipment> items) {
        // counts: [0] = in use, [1] = total, [2] = under maintenance
        Map<EquipmentType, int[]> map = new EnumMap<>(EquipmentType.class);
        for (EquipmentType t : EquipmentType.values()) {
            map.put(t, new int[3]);
        }
        for (MedicalEquipment e : items) {
            int[] counts = map.get(e.getType());
            counts[1]++;
            if (e.getAssignedPatient() != null) {
                counts[0]++;
            }
            if (e.isUnderMaintenance()) {
                counts[2]++;
            }
        }
        return map;
    }

    public static void addBackupVentilators(Collection<? super Ventilator> target, int count) {
        for (int i = 0; i < count; i++) {
            target.add(EquipmentFactory.createVentilator());
        }
    }

    public static <T extends Comparable<? super T>> T maxOf(Collection<? extends T> items) {
        Iterator<? extends T> it = items.iterator();
        if (!it.hasNext()) {
            return null;
        }
        T best = it.next();
        while (it.hasNext()) {
            T candidate = it.next();
            if (candidate.compareTo(best) > 0) {
                best = candidate;
            }
        }
        return best;
    }
}
