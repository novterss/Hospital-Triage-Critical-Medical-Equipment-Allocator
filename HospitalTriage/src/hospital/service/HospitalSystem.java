package hospital.service;

import hospital.equipment.EquipmentFactory;
import hospital.equipment.EquipmentType;
import hospital.equipment.MedicalEquipment;
import hospital.event.HospitalEventListener;
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
import hospital.model.PediatricPatient;
import hospital.model.Staff;
import hospital.model.TriageLevel;
import hospital.model.VitalSigns;
import hospital.triage.AdultTriageProtocol;
import hospital.triage.PediatricTriageProtocol;
import hospital.triage.TriageProtocol;
import hospital.triage.TriageResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Main hospital service coordinating triage, waiting queue, equipment inventory, and clock.
 */
public class HospitalSystem {

    public static final int DAY_START_MINUTE = 8 * 60; // simulation starts at 08:00

    private final String name;
    private final PriorityQueueManager<Patient> waitingQueue = new PriorityQueueManager<>();
    private final Inventory<MedicalEquipment> inventory = new Inventory<>();
    private final List<Patient> patients = new ArrayList<>();
    private final List<Staff> staff = new ArrayList<>();
    private final List<HospitalEventListener> listeners = new ArrayList<>();
    private final List<AllocationRecord> history = new ArrayList<>();
    private final List<String> log = new ArrayList<>();

    private final TriageProtocol adultProtocol = new AdultTriageProtocol();
    private final TriageProtocol pediatricProtocol = new PediatricTriageProtocol();
    private final AllocationEngine engine;

    private int clockMinutes = DAY_START_MINUTE;

    public HospitalSystem(String name) {
        this.name = name;
        this.engine = new AllocationEngine(this);
    }

    // Patients under 15 are instantiated as PediatricPatient to apply pediatric triage rules
    public Patient admitPatient(String patientName, int age, String complaint, VitalSigns vitals,
                                int painScore, Consciousness consciousness, int expectedResources,
                                boolean highRisk) {
        Patient p;
        if (age < PediatricPatient.AGE_LIMIT) {
            p = new PediatricPatient(patientName, age, complaint, vitals, painScore, consciousness,
                    expectedResources, highRisk, clockMinutes, "Guardian");
        } else {
            p = new Patient(patientName, age, complaint, vitals, painScore, consciousness,
                    expectedResources, highRisk, clockMinutes);
        }
        return admit(p);
    }

    public Patient admit(Patient p) {
        patients.add(p);
        TriageResult result = triage(p);
        p.setStatus(PatientStatus.WAITING);
        waitingQueue.enqueue(p);
        log("Admitted " + p.getId() + " " + p.getName() + " -> " + result.getLevel()
                + " (queue position " + waitingQueue.positionOf(p) + ")");
        fire(l -> l.onPatientAdmitted(p));
        return p;
    }

    public TriageProtocol protocolFor(Patient p) {
        return p.isPediatric() ? pediatricProtocol : adultProtocol;
    }

    private TriageResult triage(Patient p) {
        TriageResult result = protocolFor(p).assess(p);
        p.setTriageResult(result);
        return result;
    }

    public TriageResult retriage(Patient p, VitalSigns newVitals) {
        TriageLevel before = p.getTriageLevel();
        p.updateVitals(newVitals);
        TriageResult result = triage(p);
        if (p.getStatus() == PatientStatus.WAITING) {
            waitingQueue.reprioritize(p);
        }
        log("Re-triaged " + p.getName() + ": " + before + " -> " + result.getLevel());
        return result;
    }

    /** Calls the next waiting patient and allocates recommended equipment (null if queue empty). */
    public Patient callNextPatient(Staff by) {
        Patient next = waitingQueue.pollNext();
        if (next == null) {
            log("Queue is empty / ไม่มีผู้ป่วยรอ");
            return null;
        }
        next.setStatus(PatientStatus.IN_TREATMENT);
        log("Called " + next.getName() + " (" + next.getTriageLevel() + ") after waiting "
                + getWaitingMinutes(next) + " min");
        fire(l -> l.onPatientCalled(next));
        engine.allocateRecommended(next, by);
        return next;
    }

    public MedicalEquipment allocate(Patient p, EquipmentType type, Staff by)
            throws EquipmentUnavailableException, UnauthorizedAllocationException {
        return engine.allocate(p, type, by);
    }

    public void release(MedicalEquipment equipment, Staff by) {
        Patient holder = equipment.getAssignedPatient();
        engine.release(equipment, AllocationRecord.Action.RELEASED, by);
        if (holder != null) {
            log("Released " + equipment.getId() + " from " + holder.getName());
        }
    }

    public void discharge(Patient p, Staff by) {
        for (MedicalEquipment e : new ArrayList<>(p.getAssignedEquipment())) {
            release(e, by);
        }
        waitingQueue.remove(p);
        p.setStatus(PatientStatus.DISCHARGED);
        log("Discharged " + p.getName());
        fire(l -> l.onPatientDischarged(p));
    }

    // Advance simulated clock, update equipment usage hours, and log wait-time warnings
    public void advanceClock(int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("minutes must be positive");
        }
        clockMinutes += minutes;
        double hours = minutes / 60.0;
        for (MedicalEquipment e : inventory) {
            boolean dueBefore = e.needsService();
            e.recordUsage(hours);
            if (!dueBefore && e.needsService()) {
                log("Service due for " + e.getId() + " / ถึงรอบบำรุงรักษา");
            }
        }
        for (Patient p : waitingQueue) {
            int waited = getWaitingMinutes(p);
            int target = p.getTriageLevel().getTargetWaitMinutes();
            if (waited > target && waited - minutes <= target) {
                log("WARNING " + p.getName() + " has waited " + waited + " min (target for "
                        + p.getTriageLevel() + " is " + target + " min)");
            }
        }
        log("Clock advanced " + minutes + " min");
    }

    public MedicalEquipment addEquipment(EquipmentType type) {
        MedicalEquipment e = EquipmentFactory.create(type);
        inventory.add(e);
        log("Added " + e);
        return e;
    }

    public void startMaintenance(MedicalEquipment e) {
        e.startMaintenance();
        log("Maintenance started: " + e.getId());
    }

    public void completeMaintenance(MedicalEquipment e) {
        e.completeMaintenance();
        log("Maintenance completed: " + e.getId());
    }

    public void addStaff(Staff s) {
        staff.add(s);
    }

    public List<Staff> getStaff() {
        return Collections.unmodifiableList(staff);
    }

    public String getName() {
        return name;
    }

    public List<Patient> getWaitingPatients() {
        return waitingQueue.snapshot();
    }

    public List<Patient> getPatientsInTreatment() {
        List<Patient> list = new ArrayList<>();
        for (Patient p : patients) {
            if (p.getStatus() == PatientStatus.IN_TREATMENT) {
                list.add(p);
            }
        }
        list.sort(hospital.contract.Prioritizable.byUrgency());
        return list;
    }

    public List<Patient> getAllPatients() {
        return Collections.unmodifiableList(patients);
    }

    public List<MedicalEquipment> getEquipment() {
        return inventory.getAll();
    }

    public <T extends MedicalEquipment> List<T> getEquipmentOf(Class<T> kind) {
        List<T> result = new ArrayList<>();
        for (MedicalEquipment e : inventory) {
            if (kind.isInstance(e)) {
                result.add(kind.cast(e));
            }
        }
        return result;
    }

    public Optional<MedicalEquipment> findEquipment(String id) {
        return inventory.findById(id);
    }

    public Optional<Patient> findPatient(String id) {
        for (Patient p : patients) {
            if (p.getId().equalsIgnoreCase(id)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    public int queuePosition(Patient p) {
        return waitingQueue.positionOf(p);
    }

    public int getWaitingMinutes(Patient p) {
        return clockMinutes - p.getArrivalMinute();
    }

    public int getClockMinutes() {
        return clockMinutes;
    }

    public List<AllocationRecord> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public List<String> getLog() {
        return Collections.unmodifiableList(log);
    }

    public static String formatMinutes(int minutes) {
        int m = ((minutes % (24 * 60)) + 24 * 60) % (24 * 60);
        return String.format("%02d:%02d", m / 60, m % 60);
    }

    String clockText() {
        return formatMinutes(clockMinutes);
    }

    public void addListener(HospitalEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(HospitalEventListener listener) {
        listeners.remove(listener);
    }

    // package-private helpers for AllocationEngine

    Inventory<MedicalEquipment> inventory() {
        return inventory;
    }

    void record(AllocationRecord r) {
        history.add(r);
    }

    void fire(Consumer<HospitalEventListener> event) {
        for (HospitalEventListener l : new ArrayList<>(listeners)) {
            event.accept(l);
        }
    }

    void log(String message) {
        String line = "[" + clockText() + "] " + message;
        log.add(line);
        fire(l -> l.onLog(line));
    }

    /** Checks which equipment types lack sufficient stock (+1 reserve) for waiting patients. */
    public List<String> checkShortages() {
        final int reserve = 1; // Keep at least 1 spare unit in reserve

        class Shortage {
            private final EquipmentType type;
            private final int available;
            private final int demand;

            Shortage(EquipmentType type, int available, int demand) {
                this.type = type;
                this.available = available;
                this.demand = demand;
            }

            boolean isCritical() {
                return available - demand < reserve;
            }

            String describe() {
                return String.format("%-16s available %d, needed by waiting patients %d -> %s",
                        type.getDisplayName(), available, demand,
                        available < demand ? "SHORTAGE / ไม่พอ" : "no spare left / ไม่มีสำรอง");
            }
        }

        List<String> result = new ArrayList<>();
        for (EquipmentType type : EquipmentType.values()) {
            int available = inventory.count(e -> e.getType() == type && e.isAvailable());
            int demand = 0;
            for (Patient p : waitingQueue) {
                if (p.getTriageResult().getRecommendedEquipment().contains(type)) {
                    demand++;
                }
            }
            Shortage s = new Shortage(type, available, demand);
            if (demand > 0 && s.isCritical()) {
                result.add(s.describe());
            }
        }
        return result;
    }

    public DailyReport createDailyReport() {
        return new DailyReport();
    }

    public class DailyReport {
        private final String name = "Emergency Department Summary / สรุปห้องฉุกเฉิน";

        public String getText() {
            StringBuilder sb = new StringBuilder();
            String nl = System.lineSeparator();
            sb.append("=== ").append(HospitalSystem.this.name).append(" - ").append(this.name).append(" ===").append(nl);
            sb.append("Time / เวลา: ").append(clockText()).append(nl).append(nl);

            int[] byLevel = new int[6];
            for (Patient p : patients) {
                if (p.getTriageLevel() != null) {
                    byLevel[p.getTriageLevel().getLevel()]++;
                }
            }
            sb.append("Patients by ESI level / จำนวนผู้ป่วยตามระดับ ESI").append(nl);
            for (TriageLevel t : TriageLevel.values()) {
                sb.append(String.format("  %-26s %3d%n", t, byLevel[t.getLevel()]));
            }
            sb.append(String.format("Waiting %d | In treatment %d | Total %d%n%n",
                    waitingQueue.size(), getPatientsInTreatment().size(), patients.size()));

            sb.append("Equipment utilization / การใช้งานอุปกรณ์").append(nl);
            Map<EquipmentType, int[]> util = EquipmentReports.utilization(inventory);
            for (Map.Entry<EquipmentType, int[]> entry : util.entrySet()) {
                int[] c = entry.getValue();
                int pct = c[1] == 0 ? 0 : Math.round(100f * c[0] / c[1]);
                sb.append(String.format("  %-16s %d/%d in use (%3d%%), %d in maintenance%n",
                        entry.getKey().getDisplayName(), c[0], c[1], pct, c[2]));
            }
            sb.append(String.format("Running cost of equipment in use: %,.0f THB/hour%n%n",
                    EquipmentReports.totalHourlyCostInUse(inventory.getAll())));

            List<String> shortages = checkShortages();
            sb.append("Shortage check / ตรวจสอบอุปกรณ์ไม่พอ").append(nl);
            if (shortages.isEmpty()) {
                sb.append("  none / ไม่มี").append(nl);
            } else {
                for (String s : shortages) {
                    sb.append("  ").append(s).append(nl);
                }
            }
            sb.append(nl).append("Allocation history / ประวัติการจัดสรร (").append(history.size()).append(")").append(nl);
            for (AllocationRecord r : history) {
                sb.append("  ").append(r).append(nl);
            }
            return sb.toString();
        }
    }

    // sample data

    /** Builds a hospital preloaded with sample staff, limited equipment, and 6 patients. */
    public static HospitalSystem createWithSampleData() {
        HospitalSystem h = new HospitalSystem("RSU Teaching Hospital");
        h.addStaff(new Doctor("Somchai Jaidee", 45, "Emergency Medicine"));
        h.addStaff(new Doctor("Pimchanok Srisuk", 38, "Pediatrics"));
        h.addStaff(new Nurse("Kanya Thongdee", 29, "ER"));
        h.addStaff(new Nurse("Arthit Wongsa", 33, "ICU"));

        int[] stock = {2, 2, 3, 2, 3}; // VEN, DEF, MON, ICU, INF (limited stock for testing)
        EquipmentType[] types = EquipmentType.values();
        for (int i = 0; i < types.length; i++) {
            for (int n = 0; n < stock[i]; n++) {
                h.inventory.add(EquipmentFactory.create(types[i]));
            }
        }
        try {
            h.admitPatient("Malee Sukjai", 34, "Twisted ankle", new VitalSigns(88, 125, 16, 99, 36.8),
                    4, Consciousness.ALERT, 1, false);
            h.advanceClock(4);
            h.admitPatient("Somsak Rakdee", 67, "Crushing chest pain", new VitalSigns(112, 95, 24, 93, 37.2),
                    8, Consciousness.ALERT, 2, true);
            h.advanceClock(3);
            h.admitPatient("Ploy Charoen", 22, "Sore throat", new VitalSigns(78, 118, 14, 99, 37.4),
                    2, Consciousness.ALERT, 0, false);
            h.advanceClock(5);
            h.admitPatient("Napat Khamdee", 2, "High fever, poor feeding", new VitalSigns(150, 90, 38, 95, 39.4),
                    3, Consciousness.ALERT, 2, false);
            h.advanceClock(2);
            h.admitPatient("Anan Meesuk", 45, "Abdominal pain, vomiting", new VitalSigns(96, 130, 18, 98, 38.1),
                    6, Consciousness.ALERT, 2, false);
            h.advanceClock(6);
            h.admitPatient("Wichai Boonmee", 54, "Collapsed, gasping", new VitalSigns(38, 70, 6, 82, 36.0),
                    0, Consciousness.UNRESPONSIVE, 3, true);
        } catch (InvalidVitalSignsException e) {
            throw new IllegalStateException("Sample data is invalid", e);
        }
        return h;
    }
}
