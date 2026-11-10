package hospital.demo;

import hospital.device.LegacyMonitorAdapter;
import hospital.device.LegacyVendorMonitor;
import hospital.device.VitalSignsSource;
import hospital.equipment.EquipmentFactory;
import hospital.equipment.EquipmentType;
import hospital.equipment.LifeSupport;
import hospital.equipment.MedicalEquipment;
import hospital.equipment.Ventilator;
import hospital.event.HospitalEventAdapter;
import hospital.exception.InvalidVitalSignsException;
import hospital.generic.EquipmentReports;
import hospital.generic.Inventory;
import hospital.generic.PriorityQueueManager;
import hospital.model.Consciousness;
import hospital.model.Doctor;
import hospital.model.Nurse;
import hospital.model.Patient;
import hospital.model.PediatricPatient;
import hospital.model.Person;
import hospital.model.TriageLevel;
import hospital.model.VitalSigns;
import hospital.service.HospitalSystem;
import hospital.triage.AdultTriageProtocol;
import hospital.triage.PediatricTriageProtocol;
import hospital.triage.TriageProtocol;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Console/GUI demonstration of the OOP concepts implemented across the project.
 */
public final class ConceptDemo {

    private ConceptDemo() {
    }

    public static void main(String[] args) {
        runAll(System.out);
    }

    public static void runAll(PrintStream out) {
        try {
            encapsulation(out);
            passByValueAndReference(out);
            inheritanceThisSuper(out);
            polymorphismAndAbstract(out);
            interfaces(out);
            generics(out);
            wildcards(out);
            helperClasses(out);
            others(out);
            out.println();
            out.println("=== End of demo / จบการสาธิต ===");
        } catch (InvalidVitalSignsException e) {
            out.println("Unexpected: " + e.getMessage());
        }
    }

    private static void title(PrintStream out, String text) {
        out.println();
        out.println("------------------------------------------------------------------");
        out.println(text);
        out.println("------------------------------------------------------------------");
    }

    private static void encapsulation(PrintStream out) {
        title(out, "1. Encapsulation / การห่อหุ้มข้อมูล");
        VitalSigns v = VitalSigns.normal();
        out.println("Fields of VitalSigns are private; we must use setters, which validate:");
        try {
            v.setHeartRate(-5);
        } catch (InvalidVitalSignsException e) {
            out.println("  v.setHeartRate(-5) -> rejected: " + e.getMessage());
        }
        out.println("  v is still valid: " + v);
    }

    private static void tryToChangeHeartRate(int heartRate) {
        heartRate = 150;
    }

    private static void changeHeartRate(VitalSigns vitals) throws InvalidVitalSignsException {
        vitals.setHeartRate(150);
    }

    private static void replaceVitals(VitalSigns vitals) throws InvalidVitalSignsException {
        vitals = new VitalSigns(40, 60, 6, 70, 35.0);
    }

    private static void passByValueAndReference(PrintStream out) throws InvalidVitalSignsException {
        title(out, "2. Pass by value vs. pass by reference / การส่งค่าแบบ value และ reference");
        int heartRate = 80;
        tryToChangeHeartRate(heartRate);
        out.println("Primitive int: heartRate = 80, tryToChangeHeartRate(heartRate)");
        out.println("  -> after call heartRate = " + heartRate + "   (value copied)");

        VitalSigns v = VitalSigns.normal();
        changeHeartRate(v);
        out.println("Object: v.heartRate = 80, changeHeartRate(v)");
        out.println("  -> after call v.heartRate = " + v.getHeartRate() + "   (reference copied, same object)");

        replaceVitals(v);
        out.println("Object: replaceVitals(v) does vitals = new VitalSigns(...) inside the method");
        out.println("  -> after call v.heartRate = " + v.getHeartRate()
                + "   (reassigning the parameter inside the method does not affect the caller)");

        Patient p = new Patient("Demo Patient", 40, "Demo", VitalSigns.normal());
        VitalSigns outside = p.getVitals();
        outside.setHeartRate(200);
        out.println("Defensive copy: VitalSigns outside = patient.getVitals(); outside.setHeartRate(200)");
        out.println("  -> patient's heart rate is still " + p.getVitals().getHeartRate()
                + " because getVitals() returns a copy (protects encapsulation)");
    }

    private static void inheritanceThisSuper(PrintStream out) throws InvalidVitalSignsException {
        title(out, "3. Inheritance, this, super / การสืบทอด this และ super");
        Doctor d = new Doctor("Demo Doctor", "Cardiology");
        Nurse n = new Nurse("Demo Nurse", "ER");
        PediatricPatient child = new PediatricPatient("Demo Child", 5, "Cough",
                new VitalSigns(110, 100, 24, 97, 37.8), 2, Consciousness.ALERT, 1, false, 0, "Mother");
        out.println("Person <- Staff <- Doctor : " + d + "  (age " + d.getAge() + " set by this(...) default)");
        out.println("Person <- Staff <- Nurse  : " + n);
        out.println("Person <- Patient <- PediatricPatient : " + child.getRole() + ", age group " + child.getAgeGroup());
        out.println("Patient.toString() calls super.toString(): " + child);
    }

    private static void polymorphismAndAbstract(PrintStream out) throws InvalidVitalSignsException {
        title(out, "4. Polymorphism and abstract classes / พอลีมอร์ฟิซึมและคลาสนามธรรม");
        List<Person> people = new ArrayList<>();
        people.add(new Doctor("A. Doctor", "ER"));
        people.add(new Nurse("B. Nurse", "ICU"));
        people.add(new Patient("C. Adult", 30, "Headache", VitalSigns.normal()));
        out.println("One List<Person>, one call getRole(), different answers:");
        for (Person p : people) {
            out.println("  " + p.getClass().getSimpleName() + ".getRole() -> " + p.getRole());
        }

        out.println("MedicalEquipment is abstract; each subclass implements getHourlyCost():");
        for (EquipmentType t : EquipmentType.values()) {
            MedicalEquipment e = EquipmentFactory.create(t);
            out.printf("  %-14s %-28s %,6.0f THB/h  %s%n", e.getClass().getSimpleName(), e.getId() + " " + e.getModel(),
                    e.getHourlyCost(), e.getSpecification());
        }

        out.println("Same 2-year-old (HR 155, RR 38), two TriageProtocol subclasses:");
        Patient toddler = new Patient("Toddler", 2, "Fever", new VitalSigns(155, 95, 38, 96, 37.5),
                2, Consciousness.ALERT, 2, false, 0);
        TriageProtocol[] protocols = {new AdultTriageProtocol(), new PediatricTriageProtocol()};
        for (TriageProtocol protocol : protocols) {
            out.println("  " + protocol.getName() + " -> " + protocol.assess(toddler).getLevel());
        }
        out.println("  (adult thresholds treat a normal toddler heart rate as critical, while pediatric rules do not)");
    }

    private static void interfaces(PrintStream out) {
        title(out, "5. Interfaces / อินเทอร์เฟซ");
        MedicalEquipment vent = EquipmentFactory.create(EquipmentType.VENTILATOR);
        MedicalEquipment bed = EquipmentFactory.create(EquipmentType.ICU_BED);
        out.println("MedicalEquipment implements Allocatable, Maintainable, Comparable");
        out.println("Default-method conflict resolved with Allocatable.super / Maintainable.super:");
        out.println("  " + vent.getId() + ".statusLabel() = " + vent.statusLabel());
        vent.startMaintenance();
        out.println("  after startMaintenance(): " + vent.statusLabel());
        out.println("Interface constant Maintainable.DEFAULT_SERVICE_INTERVAL_HOURS = "
                + hospital.contract.Maintainable.DEFAULT_SERVICE_INTERVAL_HOURS
                + ", but Ventilator overrides getServiceIntervalHours() = " + vent.getServiceIntervalHours());
        out.println("Tag interface LifeSupport (no methods):");
        out.println("  ventilator instanceof LifeSupport = " + (vent instanceof LifeSupport));
        out.println("  ICU bed    instanceof LifeSupport = " + (bed instanceof LifeSupport));
    }

    private static void generics(PrintStream out) throws InvalidVitalSignsException {
        title(out, "6. Generics / เจเนอริก");
        PriorityQueueManager<Patient> queue = new PriorityQueueManager<>();
        TriageProtocol adult = new AdultTriageProtocol();
        Patient mild = new Patient("Mild case", 30, "Rash", VitalSigns.normal(), 1, Consciousness.ALERT, 0, false, 0);
        Patient severe = new Patient("Severe case", 60, "Chest pain", new VitalSigns(120, 100, 22, 94, 37), 9,
                Consciousness.ALERT, 2, true, 0);
        mild.setTriageResult(adult.assess(mild));
        severe.setTriageResult(adult.assess(severe));
        queue.enqueue(mild);
        queue.enqueue(severe);
        out.println("PriorityQueueManager<Patient>: mild arrived first, severe second");
        out.println("  pollNext() -> " + queue.pollNext().getName() + " (" + severe.getTriageLevel() + ")");

        Inventory<Ventilator> ventilators = new Inventory<>();
        ventilators.add(EquipmentFactory.createVentilator());
        ventilators.add(EquipmentFactory.createVentilator());
        Ventilator first = ventilators.getAll().get(0);
        out.println("Inventory<Ventilator>: first.getMaxTidalVolumeMl() = " + first.getMaxTidalVolumeMl() + " (no cast)");
        List<String> ids = ventilators.map(MedicalEquipment::getId);
        out.println("Generic method map(): " + ids);

        HospitalSystem h = HospitalSystem.createWithSampleData();
        List<Ventilator> fromSystem = h.getEquipmentOf(Ventilator.class);
        out.println("getEquipmentOf(Ventilator.class) returns List<Ventilator> with " + fromSystem.size() + " items");
    }

    private static void wildcards(PrintStream out) {
        title(out, "7. Wildcards / ไวลด์การ์ด");
        List<Ventilator> vents = new ArrayList<>();
        EquipmentReports.addBackupVentilators(vents, 1);
        List<MedicalEquipment> mixed = new ArrayList<>();
        EquipmentReports.addBackupVentilators(mixed, 2);
        mixed.add(EquipmentFactory.create(EquipmentType.ICU_BED));
        List<Object> anything = new ArrayList<>();
        EquipmentReports.addBackupVentilators(anything, 1);

        out.println("Lower bound  <? super Ventilator>  accepted List<Ventilator>, List<MedicalEquipment>, List<Object>");
        out.println("Upper bound  <? extends MedicalEquipment>: totalHourlyCostInUse(List<Ventilator>) = "
                + EquipmentReports.totalHourlyCostInUse(vents) + " (none in use)");
        out.println("Unbounded    <?>: describeAll(List<String>) and describeAll(List<MedicalEquipment>):");
        out.print(indent(EquipmentReports.describeAll(java.util.Arrays.asList("alpha", "beta"))));
        out.print(indent(EquipmentReports.describeAll(mixed)));
        out.println("Bounded generic method <T extends Comparable<? super T>> maxOf(List<Ventilator>) = "
                + EquipmentReports.maxOf(vents).getId());
        out.println("  (works because Ventilator inherits compareTo from MedicalEquipment -> needs '? super T')");
    }

    private static void helperClasses(PrintStream out) {
        title(out, "8. Helper classes / คลาสช่วยเหลือ");
        HospitalSystem h = HospitalSystem.createWithSampleData();

        HospitalSystem.DailyReport report = h.createDailyReport();
        String firstLine = report.getText().split("\\R")[0];
        out.println("Inner class HospitalSystem.DailyReport uses HospitalSystem.this.name:");
        out.println("  " + firstLine);

        Inventory<MedicalEquipment> inv = new Inventory<>();
        for (MedicalEquipment e : h.getEquipment()) {
            inv.add(e);
        }
        Iterator<MedicalEquipment> it = inv.iteratorOf(EquipmentType.DEFIBRILLATOR);
        out.print("Local class FilteredIterator (defibrillators only):");
        while (it.hasNext()) {
            out.print(" " + it.next().getId());
        }
        out.println();

        List<MedicalEquipment> list = new ArrayList<>(h.getEquipment());
        Collections.sort(list, new Comparator<MedicalEquipment>() {
            @Override
            public int compare(MedicalEquipment a, MedicalEquipment b) {
                return Double.compare(b.getHourlyCost(), a.getHourlyCost());
            }
        });
        out.println("Anonymous Comparator, most expensive first: " + list.get(0).getId());
        list.sort((a, b) -> Double.compare(a.getHourlyCost(), b.getHourlyCost()));
        out.println("Lambda Comparator, cheapest first:          " + list.get(0).getId());

        final int[] counter = {0};
        h.addListener(new HospitalEventAdapter() {
            @Override
            public void onPatientAdmitted(Patient p) {
                counter[0]++;
            }
        });
        h.admitPatient("Listener Test", 30, "Test", VitalSigns.normal(), 0, Consciousness.ALERT, 0, false);
        out.println("Anonymous subclass of HospitalEventAdapter counted admissions: " + counter[0]);

        VitalSignsSource source = new LegacyMonitorAdapter(new LegacyVendorMonitor("DEMO-1", 7));
        try {
            out.println("Adapter pattern: " + source.getSourceName() + " -> " + source.readVitals());
        } catch (InvalidVitalSignsException e) {
            out.println("Adapter pattern failed: " + e.getMessage());
        }
    }

    private static void others(PrintStream out) {
        title(out, "9. Other concepts / แนวคิดอื่น ๆ");
        out.print("Enum with fields: ");
        for (TriageLevel t : TriageLevel.values()) {
            out.print(t.getLevel() + "=" + t.getNameEn() + "(" + t.getTargetWaitMinutes() + "min) ");
        }
        out.println();
        out.println("Factory: EquipmentFactory.create(ICU_BED) -> " + EquipmentFactory.create(EquipmentType.ICU_BED));
        out.println("Static counter: patient ids are generated from a static field (P-0001, P-0002, ...)");
        out.println("Checked exceptions: InvalidVitalSignsException, EquipmentUnavailableException, UnauthorizedAllocationException");
        out.println("Template method: TriageProtocol.assess() is final; subclasses fill in the hooks");
        out.println("Iterator: Inventory implements Iterable, so 'for (MedicalEquipment e : inventory)' works");
    }

    private static String indent(String text) {
        StringBuilder sb = new StringBuilder();
        for (String line : text.split("\\R")) {
            sb.append("    ").append(line).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
