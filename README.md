# Hospital Triage & Critical Medical Equipment Allocator

[![tests](https://github.com/novterss/Hospital-Triage-Critical-Medical-Equipment-Allocator/actions/workflows/tests.yml/badge.svg)](https://github.com/novterss/Hospital-Triage-Critical-Medical-Equipment-Allocator/actions/workflows/tests.yml)
![Java 17+](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk)
![Swing GUI](https://img.shields.io/badge/UI-Swing-blue)
![35 tests](https://img.shields.io/badge/tests-35%20passing-brightgreen)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**ระบบคัดกรองผู้ป่วยฉุกเฉินและจัดสรรอุปกรณ์การแพทย์วิกฤต**

A Java Swing desktop application that simulates an emergency room. It triages
patients with the **Emergency Severity Index (ESI v4)**, keeps a live priority
queue, and allocates scarce equipment (ventilators, defibrillators, cardiac
monitors, ICU beds, infusion pumps). A more critical patient can take equipment
away from a less critical one.

โปรแกรมจำลองห้องฉุกเฉิน: คัดกรองผู้ป่วยตามเกณฑ์ ESI v4 จัดคิวตามความเร่งด่วน
และจัดสรรอุปกรณ์ที่มีจำกัด ผู้ป่วยที่วิกฤตกว่าสามารถรับอุปกรณ์ต่อจากผู้ป่วยที่อาการเบากว่าได้

> CSC250 Object-Oriented Programming capstone project, Rangsit University.
> Group **chill until final**.

![Demo: triage, treatment, equipment and report tabs](docs/demo.gif)

## Features / ความสามารถ

- **ESI v4 triage**: separate adult and pediatric protocols (age-adjusted vital-sign thresholds, under-3 fever rule).
- **Priority queue**: ordered by urgency score, then arrival time. Children get a small bonus within the same ESI level.
- **Equipment allocation with pre-emption**: uses a free device first; otherwise takes one from a strictly less urgent patient. Life-support devices need a doctor's authorization and an ESI 1–2 patient.
- **Maintenance tracking**: usage hours, service intervals, and maintenance mode.
- **Audit log and daily report**: every allocation, release, and pre-emption is recorded.
- **Legacy device adapter**: reads a vendor bedside monitor (°F, fractional SpO₂) and converts it.
- **Three ways to run**: Swing GUI, text console, and an OOP concept demo, plus 35 automated tests.

## Screenshots

| Treatment | Equipment | Reports & log |
|---|---|---|
| ![](report_assets/screenshots/gui_2_treatment.png) | ![](report_assets/screenshots/gui_3_equipment.png) | ![](report_assets/screenshots/gui_4_report.png) |

## Requirements / สิ่งที่ต้องมี

- JDK **17 or newer** (the code is compiled with `--release 17`)
- Any of: Apache NetBeans, IntelliJ IDEA, Eclipse, or just the command line

## Quick start / วิธีรัน

**Windows**

```bat
cd HospitalTriage
run.bat
```

**macOS / Linux**

```bash
cd HospitalTriage
./run.sh
```

Options (add after `run.bat` / `./run.sh`):

| Argument | Mode |
|---|---|
| *(none)* | Swing GUI |
| `--console` | Text menu in the terminal |
| `--demo` | Walk-through of every OOP concept |
| `--test` | Run the 35 automated tests |

**IDE**: open the `HospitalTriage` folder as a project (NetBeans: *File → Open Project*;
IntelliJ: *Open*; Eclipse: *Import → Existing Projects*). The main class is `hospital.Main`.

See the **[User Manual](docs/USER_MANUAL.md)** for a step-by-step guide.

## Design decisions / แนวคิดการออกแบบ

**Triage as a Template Method.** `TriageProtocol.assess()` is `final` and fixes the
ESI v4 decision order (immediate intervention → high risk → resources → danger-zone
vitals). `AdultTriageProtocol` and `PediatricTriageProtocol` override only the hook
methods with their own thresholds, so the algorithm cannot be broken by a subclass
and a new protocol only needs the thresholds.

**Pre-emption that never harms an equal.** When no device is free, `AllocationEngine`
looks for the least urgent holder and takes the device only if that holder is
*strictly* less urgent. Patients at the same ESI level never take equipment from each
other; among several candidates, the one who arrived later gives it up. Every step is
written to an immutable `AllocationRecord` audit log.

**Rules carried by types.** Ventilators and defibrillators implement the empty
`LifeSupport` marker interface. The engine checks `instanceof LifeSupport` and then
requires a `Staff` whose `canAuthorizeLifeSupport()` is true (only `Doctor`) and an
ESI 1–2 patient. Adding a new life-support device needs no change to the engine.

**Controlled object creation.** Device constructors are package-private, so code
outside `hospital.equipment` must use `EquipmentFactory`, which guarantees unique IDs
such as `VEN-001`.

**One entry point for the UI.** `HospitalSystem` is a Facade: the Swing GUI, the
console and the tests all call the same methods, and the UI is notified through
`HospitalEventListener` (Observer). `HospitalEventAdapter` lets a listener override
only the events it cares about.

**Integrating a device we cannot change.** `LegacyVendorMonitor` reports °F and SpO₂
as a fraction in a raw string. `LegacyMonitorAdapter` implements `VitalSignsSource`
and converts the values, so the rest of the system sees one interface.

**Generics with PECS.** `PriorityQueueManager<T extends Prioritizable>` and
`Inventory<T extends MedicalEquipment>` are reusable containers. Methods that read use
`? extends T` (`enqueueAll`), methods that write use `? super T` (`drainTo`).
Because `PriorityQueue` does not re-sort when an item changes, re-triaged patients are
removed and re-inserted, and views are built from a sorted snapshot.

**Safe data.** `VitalSigns` validates every value in its setters and throws a checked
`InvalidVitalSignsException`; `Patient` returns defensive copies so outside code
cannot change a patient's vitals without re-triage.

## Project structure / โครงสร้างโปรเจกต์

```
HospitalTriage/
├── src/hospital/
│   ├── Main.java            entry point (GUI / console / demo / test)
│   ├── model/               Person, Patient, PediatricPatient, Staff, Doctor, Nurse, VitalSigns, enums
│   ├── contract/            Prioritizable, Allocatable, Maintainable
│   ├── equipment/           MedicalEquipment + 5 device types, LifeSupport, EquipmentFactory
│   ├── triage/              TriageProtocol (Template Method), Adult/Pediatric protocols, TriageResult
│   ├── generic/             PriorityQueueManager<T>, Inventory<T>, EquipmentReports (wildcards)
│   ├── device/              VitalSignsSource, LegacyVendorMonitor, LegacyMonitorAdapter
│   ├── event/               HospitalEventListener, HospitalEventAdapter
│   ├── exception/           custom checked exceptions
│   ├── service/             HospitalSystem (Facade), AllocationEngine, AllocationRecord
│   ├── ui/                  HospitalGUI (Swing), ConsoleApp
│   ├── demo/                ConceptDemo
│   └── tests/               SystemTest (TC-01 … TC-35)
├── docs/uml/                5 UML class diagrams
├── run.bat, run.sh
└── NetBeans / Eclipse / IntelliJ project files
```

## OOP concepts covered / แนวคิด OOP ที่ใช้

| Concept | Where |
|---|---|
| Encapsulation | private fields with validating setters in `VitalSigns`, defensive copies in `Patient` |
| Pass by value vs. reference | `ConceptDemo` shows primitives vs. object references |
| Inheritance | `Person → Patient → PediatricPatient`, `Person → Staff → Doctor/Nurse`, `MedicalEquipment → 5 devices` |
| `this` / `super` | constructor chaining in `Patient`, `super.getPriorityScore()` in `PediatricPatient`, `Allocatable.super.statusLabel()` |
| Access control | `public` API, `private` state, `protected` fields in `Person`, package-private constructors and `AllocationEngine` |
| Polymorphism | `getPriorityScore()`, `getHourlyCost()`, triage protocol chosen at runtime |
| Abstract class | `Person`, `Staff`, `MedicalEquipment`, `TriageProtocol` (Template Method) |
| Interface | `Prioritizable`, `Allocatable`, `Maintainable` (default-method conflict), `LifeSupport` (marker) |
| Generics | `PriorityQueueManager<T extends Prioritizable>`, `Inventory<T extends MedicalEquipment>` |
| Wildcards | `? extends`, `? super`, `?` in `EquipmentReports` and `PriorityQueueManager` (PECS) |
| Helper classes | inner (`DailyReport`, `InventoryIterator`), local (`Shortage`, `FilteredIterator`), anonymous, lambda, adapter (`WindowAdapter`, `HospitalEventAdapter`, `LegacyMonitorAdapter`) |
| Others | custom exceptions, enums with fields, Factory / Facade / Observer patterns |

UML diagrams are in [`HospitalTriage/docs/uml`](HospitalTriage/docs/uml).

## Author / ผู้พัฒนา

ณพัชรกัณฑ์ พัชญ์ชัยพงศา, designer and programmer.

## License

[MIT](LICENSE)
