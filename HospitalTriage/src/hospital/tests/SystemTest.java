package hospital.tests;

import hospital.device.LegacyMonitorAdapter;
import hospital.device.LegacyVendorMonitor;
import hospital.device.VitalSignsSource;
import hospital.equipment.EquipmentFactory;
import hospital.equipment.EquipmentType;
import hospital.equipment.MedicalEquipment;
import hospital.equipment.Ventilator;
import hospital.event.HospitalEventAdapter;
import hospital.exception.EquipmentUnavailableException;
import hospital.exception.InvalidVitalSignsException;
import hospital.exception.UnauthorizedAllocationException;
import hospital.generic.EquipmentReports;
import hospital.generic.Inventory;
import hospital.generic.PriorityQueueManager;
import hospital.model.Consciousness;
import hospital.model.Doctor;
import hospital.model.Nurse;
import hospital.model.Patient;
import hospital.model.PatientStatus;
import hospital.model.Staff;
import hospital.model.TriageLevel;
import hospital.model.VitalSigns;
import hospital.service.AllocationRecord;
import hospital.service.HospitalSystem;
import hospital.triage.AdultTriageProtocol;
import hospital.triage.PediatricTriageProtocol;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Standalone automated test suite.
 * Run with: hospital.Main --test  or execute main() directly.
 */
public final class SystemTest {

    @FunctionalInterface
    interface TestBody {
        void run() throws Exception;
    }

    private static int passed;
    private static int failed;
    private static final List<String> failures = new ArrayList<>();

    private SystemTest() {
    }

    public static void main(String[] args) {
        System.exit(runAll() ? 0 : 1);
    }

    public static boolean runAll() {
        passed = 0;
        failed = 0;
        failures.clear();
        System.out.println("Running Hospital Triage test suite...\n");

        // vital signs
        test("TC-01", "Valid vital signs are accepted", () -> {
            VitalSigns v = new VitalSigns(72, 118, 14, 99, 36.9);
            check(v.getHeartRate() == 72 && v.getOxygenSaturation() == 99, "values stored");
        });
        test("TC-02", "Impossible heart rate is rejected", () ->
                expectThrows(InvalidVitalSignsException.class, () -> new VitalSigns(-5, 120, 16, 98, 37)));
        test("TC-03", "SpO2 above 100% is rejected by setter", () -> {
            VitalSigns v = VitalSigns.normal();
            expectThrows(InvalidVitalSignsException.class, () -> v.setOxygenSaturation(101));
            check(v.getOxygenSaturation() == 98, "old value kept");
        });
        test("TC-04", "Patient keeps a defensive copy of vital signs", () -> {
            VitalSigns v = VitalSigns.normal();
            Patient p = new Patient("Copy Test", 30, "x", v);
            v.setHeartRate(200);                 // change the caller's object
            p.getVitals().setHeartRate(190);     // change the returned copy
            check(p.getVitals().getHeartRate() == 80, "patient vitals unchanged, was " + p.getVitals().getHeartRate());
        });

        // triage
        test("TC-05", "Unresponsive adult with low SpO2 -> ESI 1", () ->
                checkLevel(adult(new VitalSigns(38, 70, 6, 82, 36), 0, Consciousness.UNRESPONSIVE, 3, false), TriageLevel.ESI_1));
        test("TC-06", "Chest pain (high-risk) -> ESI 2", () ->
                checkLevel(adult(new VitalSigns(95, 140, 18, 97, 37), 5, Consciousness.ALERT, 2, true), TriageLevel.ESI_2));
        test("TC-07", "Severe pain 8/10 -> ESI 2", () ->
                checkLevel(adult(VitalSigns.normal(), 8, Consciousness.ALERT, 1, false), TriageLevel.ESI_2));
        test("TC-08", "2+ resources, normal vitals -> ESI 3", () ->
                checkLevel(adult(VitalSigns.normal(), 3, Consciousness.ALERT, 2, false), TriageLevel.ESI_3));
        test("TC-09", "2+ resources with danger-zone HR 110 -> upgraded to ESI 2", () ->
                checkLevel(adult(new VitalSigns(110, 120, 18, 97, 37), 3, Consciousness.ALERT, 2, false), TriageLevel.ESI_2));
        test("TC-10", "1 resource -> ESI 4", () ->
                checkLevel(adult(VitalSigns.normal(), 2, Consciousness.ALERT, 1, false), TriageLevel.ESI_4));
        test("TC-11", "0 resources -> ESI 5", () ->
                checkLevel(adult(VitalSigns.normal(), 1, Consciousness.ALERT, 0, false), TriageLevel.ESI_5));
        test("TC-12", "Polymorphism: toddler HR 155 is ESI 1 for adult rules but ESI 3 for pediatric rules", () -> {
            Patient toddler = new Patient("Toddler", 2, "Fever", new VitalSigns(155, 95, 38, 96, 37.5),
                    2, Consciousness.ALERT, 2, false, 0);
            check(new AdultTriageProtocol().assess(toddler).getLevel() == TriageLevel.ESI_1, "adult protocol");
            check(new PediatricTriageProtocol().assess(toddler).getLevel() == TriageLevel.ESI_3, "pediatric protocol");
        });
        test("TC-13", "Child under 3 with fever 39.4 C -> ESI 2 (pediatric rule)", () -> {
            HospitalSystem h = newSystem();
            Patient child = h.admitPatient("Fever Child", 2, "Fever", new VitalSigns(140, 95, 30, 97, 39.4),
                    2, Consciousness.ALERT, 2, false);
            check(child.isPediatric(), "PediatricPatient created automatically");
            checkLevel(child, TriageLevel.ESI_2);
        });

        // queue & priority
        test("TC-14", "More urgent patient jumps ahead of earlier arrival", () -> {
            HospitalSystem h = newSystem();
            Patient mild = h.admitPatient("Mild", 30, "x", VitalSigns.normal(), 1, Consciousness.ALERT, 1, false);
            Patient critical = h.admitPatient("Critical", 50, "x", new VitalSigns(38, 70, 6, 82, 36),
                    0, Consciousness.UNRESPONSIVE, 3, false);
            check(h.getWaitingPatients().get(0) == critical, "critical first");
            check(h.queuePosition(mild) == 2, "mild second");
        });
        test("TC-15", "Same ESI level and pain -> first come, first served", () -> {
            HospitalSystem h = newSystem();
            Patient first = h.admitPatient("First", 30, "x", VitalSigns.normal(), 2, Consciousness.ALERT, 1, false);
            Patient second = h.admitPatient("Second", 30, "x", VitalSigns.normal(), 2, Consciousness.ALERT, 1, false);
            check(h.callNextPatient(null) == first, "first called first");
            check(h.callNextPatient(null) == second, "then second");
        });
        test("TC-16", "Child gets priority bonus over adult at the same ESI level", () -> {
            HospitalSystem h = newSystem();
            Patient adult = h.admitPatient("Adult", 30, "x", VitalSigns.normal(), 2, Consciousness.ALERT, 1, false);
            Patient child = h.admitPatient("Child", 10, "x", VitalSigns.normal(), 2, Consciousness.ALERT, 1, false);
            check(adult.getTriageLevel() == child.getTriageLevel(), "same level");
            check(h.getWaitingPatients().get(0) == child, "child first");
        });
        test("TC-17", "Re-triage with worse vitals moves patient up the queue", () -> {
            HospitalSystem h = newSystem();
            h.admitPatient("A", 30, "x", VitalSigns.normal(), 3, Consciousness.ALERT, 2, false);
            Patient b = h.admitPatient("B", 30, "x", VitalSigns.normal(), 1, Consciousness.ALERT, 1, false);
            check(h.queuePosition(b) == 2, "starts second");
            h.retriage(b, new VitalSigns(160, 75, 30, 85, 38));
            check(b.getTriageLevel() == TriageLevel.ESI_1 && h.queuePosition(b) == 1, "now first and ESI 1");
        });
        test("TC-18", "Wildcard drainTo(Collection<? super Patient>) accepts List<Object>", () -> {
            PriorityQueueManager<Patient> q = new PriorityQueueManager<>();
            HospitalSystem h = newSystem();
            Patient p = h.admitPatient("Q", 30, "x", VitalSigns.normal(), 1, Consciousness.ALERT, 1, false);
            q.enqueue(p);
            List<Object> sink = new ArrayList<>();
            check(q.drainTo(sink) == 1 && sink.get(0) == p && q.isEmpty(), "moved into List<Object>");
        });

        // equipment allocation
        test("TC-19", "Free device is allocated and linked both ways", () -> {
            HospitalSystem h = newSystem();
            h.addEquipment(EquipmentType.CARDIAC_MONITOR);
            Patient p = h.admitPatient("Alloc", 30, "x", VitalSigns.normal(), 3, Consciousness.ALERT, 2, false);
            MedicalEquipment e = h.allocate(p, EquipmentType.CARDIAC_MONITOR, nurse(h));
            check(e.getAssignedPatient() == p && p.getAssignedEquipment().contains(e), "linked");
            check(!e.isAvailable(), "no longer available");
        });
        test("TC-20", "Nurse cannot allocate a ventilator (life support needs a doctor)", () -> {
            HospitalSystem h = newSystem();
            h.addEquipment(EquipmentType.VENTILATOR);
            Patient p = critical(h);
            expectThrows(UnauthorizedAllocationException.class, () -> h.allocate(p, EquipmentType.VENTILATOR, nurse(h)));
            h.allocate(p, EquipmentType.VENTILATOR, doctor(h));
        });
        test("TC-21", "Ventilator is refused for a non-critical (ESI 4) patient", () -> {
            HospitalSystem h = newSystem();
            h.addEquipment(EquipmentType.VENTILATOR);
            Patient p = h.admitPatient("Mild", 30, "x", VitalSigns.normal(), 1, Consciousness.ALERT, 1, false);
            expectThrows(UnauthorizedAllocationException.class, () -> h.allocate(p, EquipmentType.VENTILATOR, doctor(h)));
        });
        test("TC-22", "ESI 1 patient pre-empts a monitor from an ESI 3 patient", () -> {
            HospitalSystem h = newSystem();
            MedicalEquipment monitor = h.addEquipment(EquipmentType.CARDIAC_MONITOR);
            Patient esi3 = h.admitPatient("Stable", 30, "x", VitalSigns.normal(), 3, Consciousness.ALERT, 2, false);
            h.allocate(esi3, EquipmentType.CARDIAC_MONITOR, nurse(h));
            Patient esi1 = critical(h);
            MedicalEquipment got = h.allocate(esi1, EquipmentType.CARDIAC_MONITOR, doctor(h));
            check(got == monitor && monitor.getAssignedPatient() == esi1, "moved to ESI 1");
            check(esi3.getAssignedEquipment().isEmpty() && !esi3.getNotes().isEmpty(), "ESI 3 patient informed");
            boolean logged = false;
            for (AllocationRecord r : h.getHistory()) {
                logged |= r.getAction() == AllocationRecord.Action.PREEMPTED;
            }
            check(logged, "PREEMPTED in audit trail");
        });
        test("TC-23", "No pre-emption between patients of the same ESI level", () -> {
            HospitalSystem h = newSystem();
            h.addEquipment(EquipmentType.CARDIAC_MONITOR);
            Patient a = critical(h);
            Patient b = critical(h);
            h.allocate(a, EquipmentType.CARDIAC_MONITOR, doctor(h));
            expectThrows(EquipmentUnavailableException.class, () -> h.allocate(b, EquipmentType.CARDIAC_MONITOR, doctor(h)));
        });
        test("TC-24", "Calling a critical patient auto-allocates the recommended equipment", () -> {
            HospitalSystem h = newSystem();
            for (EquipmentType t : EquipmentType.values()) {
                h.addEquipment(t);
            }
            Patient p = critical(h);
            check(h.callNextPatient(doctor(h)) == p && p.getStatus() == PatientStatus.IN_TREATMENT, "called");
            check(p.getAssignedEquipment().size() == p.getTriageResult().getRecommendedEquipment().size(),
                    "got " + p.getAssignedEquipment() + " for " + p.getTriageResult().getRecommendedEquipment());
        });
        test("TC-25", "Discharge returns every device to the pool", () -> {
            HospitalSystem h = newSystem();
            for (EquipmentType t : EquipmentType.values()) {
                h.addEquipment(t);
            }
            Patient p = critical(h);
            h.callNextPatient(doctor(h));
            h.discharge(p, doctor(h));
            check(p.getAssignedEquipment().isEmpty() && p.getStatus() == PatientStatus.DISCHARGED, "patient cleared");
            for (MedicalEquipment e : h.getEquipment()) {
                check(e.isAvailable(), e.getId() + " available again");
            }
        });

        // equipment & maintenance
        test("TC-26", "Maintenance: device in use cannot be serviced; serviced device is unavailable", () -> {
            HospitalSystem h = newSystem();
            MedicalEquipment pump = h.addEquipment(EquipmentType.INFUSION_PUMP);
            Patient p = h.admitPatient("P", 30, "x", VitalSigns.normal(), 3, Consciousness.ALERT, 2, false);
            h.allocate(p, EquipmentType.INFUSION_PUMP, nurse(h));
            expectThrows(IllegalStateException.class, () -> h.startMaintenance(pump));
            h.release(pump, nurse(h));
            h.startMaintenance(pump);
            check(!pump.isAvailable() && pump.statusLabel().equals("Under maintenance"), pump.statusLabel());
            h.completeMaintenance(pump);
            check(pump.isAvailable() && pump.getUsageHours() == 0, "back in service");
        });
        test("TC-27", "Clock adds usage hours only to devices in use; ventilator service due at 50 h", () -> {
            HospitalSystem h = newSystem();
            MedicalEquipment vent = h.addEquipment(EquipmentType.VENTILATOR);
            MedicalEquipment idle = h.addEquipment(EquipmentType.VENTILATOR);
            h.allocate(critical(h), EquipmentType.VENTILATOR, doctor(h));
            h.advanceClock(50 * 60);
            check(Math.abs(vent.getUsageHours() - 50) < 1e-9 && idle.getUsageHours() == 0, "usage hours");
            check(vent.needsService() && vent.statusLabel().contains("service due"), vent.statusLabel());
        });
        test("TC-28", "Factory creates the right subclass and unique ids", () -> {
            MedicalEquipment a = EquipmentFactory.create(EquipmentType.VENTILATOR);
            MedicalEquipment b = EquipmentFactory.create(EquipmentType.VENTILATOR);
            check(a instanceof Ventilator && a.isLifeSupport(), "Ventilator + LifeSupport");
            check(!a.getId().equals(b.getId()), "unique ids");
            check(!EquipmentFactory.create(EquipmentType.ICU_BED).isLifeSupport(), "ICU bed is not life support");
        });

        // generics, adapters, reports
        test("TC-29", "Wildcards: addBackupVentilators into List<Object>, maxOf(List<Ventilator>)", () -> {
            List<Object> objects = new ArrayList<>();
            EquipmentReports.addBackupVentilators(objects, 2);
            check(objects.size() == 2 && objects.get(0) instanceof Ventilator, "added");
            List<Ventilator> vents = new ArrayList<>();
            EquipmentReports.addBackupVentilators(vents, 3);
            check(EquipmentReports.maxOf(vents) == vents.get(2), "largest id is the last created");
        });
        test("TC-30", "Inventory inner iterator and local-class filtered iterator", () -> {
            Inventory<MedicalEquipment> inv = new Inventory<>();
            inv.add(EquipmentFactory.create(EquipmentType.VENTILATOR));
            inv.add(EquipmentFactory.create(EquipmentType.ICU_BED));
            inv.add(EquipmentFactory.create(EquipmentType.VENTILATOR));
            int all = 0;
            for (MedicalEquipment ignored : inv) {
                all++;
            }
            int vents = 0;
            Iterator<MedicalEquipment> it = inv.iteratorOf(EquipmentType.VENTILATOR);
            while (it.hasNext()) {
                it.next();
                vents++;
            }
            check(all == 3 && vents == 2, "all=" + all + " vents=" + vents);
        });
        test("TC-31", "Adapter converts legacy monitor units (fraction -> %, F -> C)", () -> {
            VitalSignsSource source = new LegacyMonitorAdapter(new LegacyVendorMonitor("T", 1));
            for (int i = 0; i < 20; i++) {
                VitalSigns v = source.readVitals();
                check(v.getOxygenSaturation() > 50 && v.getOxygenSaturation() <= 100, "SpO2 in %");
                check(v.getTemperature() > 35 && v.getTemperature() < 41, "temperature in C");
            }
        });
        test("TC-32", "Event adapter subclass receives only the events it overrides", () -> {
            HospitalSystem h = newSystem();
            final int[] admitted = {0};
            h.addListener(new HospitalEventAdapter() {
                @Override
                public void onPatientAdmitted(Patient p) {
                    admitted[0]++;
                }
            });
            h.admitPatient("E1", 30, "x", VitalSigns.normal(), 1, Consciousness.ALERT, 1, false);
            h.admitPatient("E2", 30, "x", VitalSigns.normal(), 1, Consciousness.ALERT, 1, false);
            check(admitted[0] == 2, "2 admissions heard");
        });
        test("TC-33", "Shortage check (local class) reports missing ventilators", () -> {
            HospitalSystem h = newSystem();
            critical(h);
            critical(h);
            List<String> shortages = h.checkShortages();
            boolean ventilator = false;
            for (String s : shortages) {
                ventilator |= s.startsWith("Ventilator");
            }
            check(ventilator, "ventilator shortage listed: " + shortages);
        });
        test("TC-34", "Daily report (inner class) uses the outer hospital name", () -> {
            HospitalSystem h = HospitalSystem.createWithSampleData();
            String text = h.createDailyReport().getText();
            check(text.startsWith("=== RSU Teaching Hospital"), text.split("\\R")[0]);
        });
        test("TC-35", "Sample data loads: 6 patients, 4 staff, 12 devices", () -> {
            HospitalSystem h = HospitalSystem.createWithSampleData();
            check(h.getAllPatients().size() == 6 && h.getStaff().size() == 4 && h.getEquipment().size() == 12,
                    h.getAllPatients().size() + "/" + h.getStaff().size() + "/" + h.getEquipment().size());
            check(h.getWaitingPatients().get(0).getTriageLevel() == TriageLevel.ESI_1, "ESI 1 at the front");
        });

        System.out.println();
        System.out.println("==============================================");
        System.out.printf("Result: %d passed, %d failed, %d total%n", passed, failed, passed + failed);
        for (String f : failures) {
            System.out.println("  FAILED " + f);
        }
        System.out.println("==============================================");
        return failed == 0;
    }

    // test helpers

    private static void test(String id, String name, TestBody body) {
        try {
            body.run();
            passed++;
            System.out.println("PASS  " + id + "  " + name);
        } catch (Throwable t) {
            failed++;
            failures.add(id + " " + name + " -> " + t);
            System.out.println("FAIL  " + id + "  " + name + "  -> " + t);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static <T extends Throwable> void expectThrows(Class<T> expected, TestBody body) {
        try {
            body.run();
        } catch (Throwable t) {
            if (expected.isInstance(t)) {
                return;
            }
            throw new AssertionError("expected " + expected.getSimpleName() + " but got " + t);
        }
        throw new AssertionError("expected " + expected.getSimpleName() + " but nothing was thrown");
    }

    // test fixtures

    private static HospitalSystem newSystem() {
        HospitalSystem h = new HospitalSystem("Test Hospital");
        h.addStaff(new Doctor("Test Doctor", "ER"));
        h.addStaff(new Nurse("Test Nurse", "ER"));
        return h;
    }

    private static Staff doctor(HospitalSystem h) {
        return h.getStaff().get(0);
    }

    private static Staff nurse(HospitalSystem h) {
        return h.getStaff().get(1);
    }

    private static Patient adult(VitalSigns v, int pain, Consciousness c, int resources, boolean highRisk) {
        return newSystem().admitPatient("Adult", 40, "test", v, pain, c, resources, highRisk);
    }

    private static Patient critical(HospitalSystem h) throws InvalidVitalSignsException {
        return h.admitPatient("Critical", 55, "Collapsed", new VitalSigns(38, 70, 6, 82, 36),
                0, Consciousness.UNRESPONSIVE, 3, true);
    }

    private static void checkLevel(Patient p, TriageLevel expected) {
        check(p.getTriageLevel() == expected, "expected " + expected + " but was " + p.getTriageLevel()
                + " " + p.getTriageResult().getReasons());
    }
}
