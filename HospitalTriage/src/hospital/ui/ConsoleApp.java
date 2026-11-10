package hospital.ui;

import hospital.demo.ConceptDemo;
import hospital.device.LegacyMonitorAdapter;
import hospital.device.LegacyVendorMonitor;
import hospital.device.VitalSignsSource;
import hospital.equipment.EquipmentType;
import hospital.equipment.MedicalEquipment;
import hospital.event.HospitalEventAdapter;
import hospital.exception.EquipmentUnavailableException;
import hospital.exception.InvalidVitalSignsException;
import hospital.exception.UnauthorizedAllocationException;
import hospital.model.Consciousness;
import hospital.model.Patient;
import hospital.model.Staff;
import hospital.model.VitalSigns;
import hospital.service.HospitalSystem;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Console text-menu interface (English only to avoid Windows terminal encoding issues).
 */
public class ConsoleApp {

    private final HospitalSystem system;
    private final Scanner in;
    private final VitalSignsSource monitor = new LegacyMonitorAdapter(new LegacyVendorMonitor("LM-1001", 11));
    private Staff actingStaff;

    public ConsoleApp(HospitalSystem system, Scanner in) {
        this.system = system;
        this.in = in;
        this.actingStaff = system.getStaff().isEmpty() ? null : system.getStaff().get(0);
        system.addListener(new HospitalEventAdapter() {
            @Override
            public void onEquipmentShortage(EquipmentType type, Patient patient, String reason) {
                System.out.println("  !! SHORTAGE " + type.getDisplayName() + " for " + patient.getName() + ": " + reason);
            }
        });
    }

    public static void main(String[] args) {
        new ConsoleApp(HospitalSystem.createWithSampleData(), new Scanner(System.in)).run();
    }

    public void run() {
        System.out.println("==============================================================");
        System.out.println(" Hospital Triage & Critical Medical Equipment Allocator");
        System.out.println(" " + system.getName() + " - console mode");
        System.out.println("==============================================================");
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choose", 0, 13);
            System.out.println();
            switch (choice) {
                case 1: admit(); break;
                case 2: showQueue(); break;
                case 3: callNext(); break;
                case 4: showTreatment(); break;
                case 5: allocate(); break;
                case 6: release(); break;
                case 7: discharge(); break;
                case 8: showEquipment(); break;
                case 9: system.advanceClock(readInt("Minutes to advance", 1, 600)); break;
                case 10: System.out.println(system.createDailyReport().getText()); break;
                case 11: chooseStaff(); break;
                case 12: readMonitor(); break;
                case 13: ConceptDemo.runAll(System.out); break;
                default: running = false;
            }
        }
        System.out.println("Goodbye.");
    }

    private void printMenu() {
        System.out.println();
        System.out.printf("[%s] Acting staff: %s | waiting %d | in treatment %d%n",
                HospitalSystem.formatMinutes(system.getClockMinutes()),
                actingStaff == null ? "-" : actingStaff.getDisplayName(),
                system.getWaitingPatients().size(), system.getPatientsInTreatment().size());
        System.out.println(" 1 Admit & triage patient     8 Show equipment");
        System.out.println(" 2 Show waiting queue         9 Advance clock");
        System.out.println(" 3 Call next patient         10 Daily report");
        System.out.println(" 4 Show patients in treatment 11 Change acting staff");
        System.out.println(" 5 Allocate equipment        12 Read bedside monitor");
        System.out.println(" 6 Release equipment         13 OOP concept demonstration");
        System.out.println(" 7 Discharge patient          0 Exit");
    }

    private void admit() {
        String name = readLine("Name");
        if (name == null) {
            return;
        }
        int age = readInt("Age", 0, 130);
        String complaint = readLine("Chief complaint");
        try {
            VitalSigns v;
            if (readYesNo("Read vitals from the bedside monitor?")) {
                v = monitor.readVitals();
                System.out.println("  Monitor: " + v);
            } else {
                v = new VitalSigns(readInt("Heart rate", 0, 300), readInt("Systolic BP", 0, 300),
                        readInt("Respiratory rate", 0, 80), readInt("SpO2 %", 0, 100),
                        readDouble("Temperature C", 25, 45));
            }
            int pain = readInt("Pain 0-10", 0, 10);
            Consciousness[] levels = Consciousness.values();
            for (int i = 0; i < levels.length; i++) {
                System.out.println("  " + (i + 1) + " " + levels[i].getLabelEn());
            }
            Consciousness c = levels[readInt("Consciousness (AVPU)", 1, levels.length) - 1];
            int resources = readInt("Expected resources (0, 1, 2 = two or more)", 0, 2);
            boolean highRisk = readYesNo("High-risk situation (chest pain, stroke signs...)?");
            Patient p = system.admitPatient(name, age, complaint, v, pain, c, resources, highRisk);
            System.out.println("  -> " + p + ", reasons: " + p.getTriageResult().getReasons());
            System.out.println("  -> queue position " + system.queuePosition(p));
        } catch (InvalidVitalSignsException e) {
            System.out.println("  Invalid vital signs: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("  Invalid input: " + e.getMessage());
        }
    }

    private void showQueue() {
        List<Patient> waiting = system.getWaitingPatients();
        if (waiting.isEmpty()) {
            System.out.println("  Queue is empty.");
            return;
        }
        int i = 1;
        for (Patient p : waiting) {
            System.out.printf("  %2d. %-7s %-22s %3dy  %-22s waited %3d min  needs %s%n", i++, p.getId(), p.getName(),
                    p.getAge(), p.getTriageLevel(), system.getWaitingMinutes(p),
                    p.getTriageResult().getRecommendedEquipment().stream()
                            .map(EquipmentType::getDisplayName).collect(java.util.stream.Collectors.toList()));
        }
    }

    private void callNext() {
        Patient p = system.callNextPatient(actingStaff);
        if (p != null) {
            System.out.println("  Called " + p + " with equipment " + p.getAssignedEquipment());
        }
    }

    private void showTreatment() {
        List<Patient> list = system.getPatientsInTreatment();
        if (list.isEmpty()) {
            System.out.println("  Nobody in treatment.");
        }
        for (Patient p : list) {
            System.out.printf("  %-7s %-22s %-22s %s%n", p.getId(), p.getName(), p.getTriageLevel(), p.getAssignedEquipment());
        }
    }

    private void allocate() {
        Optional<Patient> p = askPatient();
        if (!p.isPresent()) {
            return;
        }
        EquipmentType[] types = EquipmentType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.println("  " + (i + 1) + " " + types[i].getDisplayName());
        }
        EquipmentType type = types[readInt("Equipment type", 1, types.length) - 1];
        try {
            MedicalEquipment e = system.allocate(p.get(), type, actingStaff);
            System.out.println("  Allocated " + e);
        } catch (EquipmentUnavailableException | UnauthorizedAllocationException e) {
            System.out.println("  Failed: " + e.getMessage());
        }
    }

    private void release() {
        String id = readLine("Equipment id (e.g. VEN-001)");
        Optional<MedicalEquipment> e = id == null ? Optional.empty() : system.findEquipment(id);
        if (e.isPresent()) {
            system.release(e.get(), actingStaff);
            System.out.println("  Released " + e.get().getId());
        } else {
            System.out.println("  No such equipment.");
        }
    }

    private void discharge() {
        askPatient().ifPresent(p -> {
            system.discharge(p, actingStaff);
            System.out.println("  Discharged " + p.getName());
        });
    }

    private void showEquipment() {
        for (MedicalEquipment e : system.getEquipment()) {
            Patient holder = e.getAssignedPatient();
            System.out.printf("  %-8s %-16s %-24s %-18s %s%n", e.getId(), e.getType().getDisplayName(),
                    e.getModel(), e.statusLabel(), holder == null ? "" : "-> " + holder.getName());
        }
    }

    private void chooseStaff() {
        List<Staff> staff = system.getStaff();
        for (int i = 0; i < staff.size(); i++) {
            System.out.println("  " + (i + 1) + " " + staff.get(i).getDisplayName());
        }
        actingStaff = staff.get(readInt("Staff", 1, staff.size()) - 1);
    }

    private void readMonitor() {
        try {
            System.out.println("  " + monitor.getSourceName() + ": " + monitor.readVitals());
        } catch (InvalidVitalSignsException e) {
            System.out.println("  Monitor error: " + e.getMessage());
        }
    }

    private Optional<Patient> askPatient() {
        String id = readLine("Patient id (e.g. P-0002)");
        Optional<Patient> p = id == null ? Optional.empty() : system.findPatient(id);
        if (!p.isPresent()) {
            System.out.println("  No such patient.");
        }
        return p;
    }

    // input helpers

    private String readLine(String prompt) {
        System.out.print(prompt + ": ");
        return in.hasNextLine() ? in.nextLine().trim() : null;
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            String s = readLine(prompt + " [" + min + "-" + max + "]");
            if (s == null) {
                return min; // avoid infinite loop if stdin closes (0 exits menu)
            }
            try {
                int value = Integer.parseInt(s);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("  Please enter a number between " + min + " and " + max);
        }
    }

    private double readDouble(String prompt, double min, double max) {
        while (true) {
            String s = readLine(prompt + " [" + min + "-" + max + "]");
            if (s == null) {
                return min;
            }
            try {
                double value = Double.parseDouble(s);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("  Please enter a number between " + min + " and " + max);
        }
    }

    private boolean readYesNo(String prompt) {
        String s = readLine(prompt + " (y/n)");
        return s != null && s.toLowerCase().startsWith("y");
    }
}
