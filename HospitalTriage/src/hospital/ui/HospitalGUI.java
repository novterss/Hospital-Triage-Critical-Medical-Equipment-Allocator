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
import hospital.generic.EquipmentReports;
import hospital.model.Consciousness;
import hospital.model.Patient;
import hospital.model.PediatricPatient;
import hospital.model.Staff;
import hospital.model.TriageLevel;
import hospital.model.VitalSigns;
import hospital.service.HospitalSystem;
import hospital.triage.TriageResult;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Main Swing UI for triage, treatment, equipment, and reports.
 */
public class HospitalGUI extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String THAI_SAMPLE = "ภาษาไทย";

    private final transient HospitalSystem system;
    private final transient VitalSignsSource bedsideMonitor =
            new LegacyMonitorAdapter(new LegacyVendorMonitor("LM-2049", 42));

    // tables
    private final QueueTableModel queueModel = new QueueTableModel();
    private final TreatmentTableModel treatmentModel = new TreatmentTableModel();
    private final EquipmentTableModel equipmentModel = new EquipmentTableModel();
    private final JTable queueTable = new JTable(queueModel);
    private final JTable treatmentTable = new JTable(treatmentModel);
    private final JTable equipmentTable = new JTable(equipmentModel);

    // shared controls
    private final JTabbedPane tabs = new JTabbedPane();
    private final JComboBox<Staff> staffBox = new JComboBox<>();
    private final JLabel clockLabel = new JLabel();
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel equipmentSummary = new JLabel(" ");
    private final JTextArea logArea = new JTextArea();
    private final JTextArea reportArea = new JTextArea();

    // admission form
    private final JTextField nameField = new JTextField(12);
    private final JSpinner ageSpinner = intSpinner(35, 0, 130);
    private final JTextField complaintField = new JTextField(12);
    private final JSpinner hrSpinner = intSpinner(80, 0, 300);
    private final JSpinner sbpSpinner = intSpinner(120, 0, 300);
    private final JSpinner rrSpinner = intSpinner(16, 0, 80);
    private final JSpinner spo2Spinner = intSpinner(98, 0, 100);
    private final JSpinner tempSpinner = new JSpinner(new SpinnerNumberModel(37.0, 25.0, 45.0, 0.1));
    private final JSpinner painSpinner = intSpinner(0, 0, 10);
    private final JComboBox<Consciousness> avpuBox = new JComboBox<>(Consciousness.values());
    private final JComboBox<String> resourcesBox = new JComboBox<>(new String[]{
        "0 - none / ไม่ใช้", "1 - one / 1 อย่าง", "2+ - many / 2+ อย่าง"});
    private final JCheckBox highRiskBox = new JCheckBox("High-risk (chest pain, stroke...) / เสี่ยงสูง");
    private final JComboBox<EquipmentType> allocTypeBox = new JComboBox<>(EquipmentType.values());
    private final JComboBox<EquipmentType> addTypeBox = new JComboBox<>(EquipmentType.values());

    public HospitalGUI(HospitalSystem system) {
        super("Hospital Triage & Critical Equipment Allocator - ระบบคัดกรองผู้ป่วยและจัดสรรอุปกรณ์วิกฤต");
        this.system = system;

        for (Staff s : system.getStaff()) {
            staffBox.addItem(s);
        }
        tempSpinner.setEditor(new JSpinner.NumberEditor(tempSpinner, "0.0"));
        resourcesBox.setSelectedIndex(1);
        staffBox.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Staff) {
                    setText(((Staff) value).getDisplayName());
                }
                return this;
            }
        });

        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        tabs.addTab("1. Triage & Queue / คัดกรองและคิว", buildTriageTab());
        tabs.addTab("2. Treatment / การรักษา", buildTreatmentTab());
        tabs.addTab("3. Equipment / อุปกรณ์", buildEquipmentTab());
        tabs.addTab("4. Reports & Log / รายงาน", buildReportTab());
        add(tabs, BorderLayout.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(statusLabel, BorderLayout.SOUTH);

        // load initial sample-data logs
        for (String line : system.getLog()) {
            logArea.append(line + "\n");
        }

        // keep log panel and status bar updated
        system.addListener(new HospitalEventAdapter() {
            @Override
            public void onLog(String message) {
                logArea.append(message + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }

            @Override
            public void onEquipmentShortage(EquipmentType type, Patient patient, String reason) {
                showStatus("SHORTAGE: " + type.getDisplayName() + " for " + patient.getName()
                        + " - " + reason, true);
            }
        });

        // confirm before closing
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int answer = JOptionPane.showConfirmDialog(HospitalGUI.this,
                        "Exit the program? Unsaved data will be lost.\nออกจากโปรแกรมหรือไม่? ข้อมูลจะไม่ถูกบันทึก",
                        "Exit / ออก", JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.YES_OPTION) {
                    dispose();
                    System.exit(0);
                }
            }
        });

        setMinimumSize(new Dimension(1120, 680));
        setSize(1340, 768);
        setLocationRelativeTo(null);
        refreshAll();
        showStatus("Ready. Double-click a patient row for details. / พร้อมใช้งาน ดับเบิลคลิกที่แถวเพื่อดูรายละเอียด", false);
    }

    // look & feel / fonts

    // use Nimbus and pick a font that supports Thai characters
    public static void installLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // keep default look and feel
        }
        Font font = pickThaiFont(13);
        UIManager.getLookAndFeelDefaults().put("defaultFont", new FontUIResource(font));
        for (Object key : new ArrayList<>(UIManager.getDefaults().keySet())) {
            if (key.toString().endsWith(".font")) {
                UIManager.put(key, new FontUIResource(font));
            }
        }
    }

    // use monospaced if it supports Thai, otherwise fallback to Thai UI font
    private static Font textAreaFont(int size) {
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, size);
        return mono.canDisplayUpTo(THAI_SAMPLE) == -1 ? mono : pickThaiFont(size);
    }

    private static Font pickThaiFont(int size) {
        List<String> available = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String family : new String[]{"Leelawadee UI", "Tahoma", "Noto Sans Thai", "Sukhumvit Set", "Thonburi"}) {
            if (available.contains(family)) {
                Font f = new Font(family, Font.PLAIN, size);
                if (f.canDisplayUpTo(THAI_SAMPLE) == -1) {
                    return f;
                }
            }
        }
        return new Font(Font.DIALOG, Font.PLAIN, size);
    }

    // tab layouts

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        header.setBackground(new Color(0x0D47A1));

        JLabel title = new JLabel("ER Triage & Critical Equipment Allocator");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JLabel subtitle = new JLabel(system.getName() + "  |  ระบบคัดกรองผู้ป่วยฉุกเฉินและจัดสรรอุปกรณ์การแพทย์วิกฤต");
        subtitle.setForeground(new Color(0xBBDEFB));
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(title);
        titles.add(subtitle);
        header.add(titles, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JLabel staffLabel = new JLabel("Acting staff / ผู้ปฏิบัติงาน:");
        staffLabel.setForeground(Color.WHITE);
        right.add(staffLabel);
        right.add(staffBox);
        clockLabel.setForeground(Color.WHITE);
        clockLabel.setFont(clockLabel.getFont().deriveFont(Font.BOLD, 16f));
        right.add(Box.createHorizontalStrut(12));
        right.add(clockLabel);
        right.add(button("+15 min", e -> advance(15)));
        right.add(button("+1 hour", e -> advance(60)));
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JComponent buildTriageTab() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("New patient / รับผู้ป่วยใหม่"));
        int row = 0;
        row = addRow(form, row, "Name / ชื่อ-สกุล", nameField);
        row = addRow(form, row, "Age (years) / อายุ", ageSpinner);
        row = addRow(form, row, "Chief complaint / อาการสำคัญ", complaintField);
        row = addRow(form, row, "Heart rate (bpm) / ชีพจร", hrSpinner);
        row = addRow(form, row, "Systolic BP (mmHg) / ความดันตัวบน", sbpSpinner);
        row = addRow(form, row, "Resp. rate (/min) / อัตราการหายใจ", rrSpinner);
        row = addRow(form, row, "SpO2 (%) / ออกซิเจนในเลือด", spo2Spinner);
        row = addRow(form, row, "Temperature (C) / อุณหภูมิ", tempSpinner);
        row = addRow(form, row, "Pain 0-10 / ระดับความปวด", painSpinner);
        row = addRow(form, row, "Consciousness (AVPU) / ความรู้สึกตัว", avpuBox);
        row = addRow(form, row, "Expected resources / ทรัพยากรที่ต้องใช้", resourcesBox);
        GridBagConstraints c = gbc(0, row);
        c.gridwidth = 2;
        form.add(highRiskBox, c);
        row++;

        JPanel formButtons = new JPanel(new java.awt.GridLayout(0, 1, 4, 4));
        formButtons.add(primary(button("Admit & triage / รับและคัดกรอง", e -> admitFromForm())));
        formButtons.add(button("Read bedside monitor / อ่านค่าจากเครื่อง", e -> readMonitor()));
        JButton clear = new JButton("Clear / ล้าง");
        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearForm();
            }
        });
        formButtons.add(clear);
        c = gbc(0, row);
        c.gridwidth = 2;
        form.add(formButtons, c);
        c = gbc(0, row + 1);
        c.weighty = 1;
        form.add(new JLabel(" "), c);

        JPanel queuePanel = new JPanel(new BorderLayout(4, 4));
        queuePanel.setBorder(BorderFactory.createTitledBorder(
                "Waiting queue - most urgent first / คิวรอตรวจ (เรียงตามความเร่งด่วน)"));
        setupTable(queueTable, new int[]{24, 60, 158, 36, 126, 178, 102, 188});
        installEsiRenderer(queueTable, 4);
        installWaitRenderer(queueTable, 6);
        addDoubleClick(queueTable, this::selectedWaitingPatient);
        queuePanel.add(new JScrollPane(queueTable), BorderLayout.CENTER);
        JPanel queueButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        queueButtons.add(primary(button("Call next patient / เรียกผู้ป่วยถัดไป", e -> callNext())));
        queueButtons.add(button("Re-triage with form vitals / คัดกรองใหม่", e -> retriageSelected()));
        queueButtons.add(button("Details / รายละเอียด", e -> showDetails(selectedWaitingPatient())));
        queuePanel.add(queueButtons, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(form), queuePanel);
        split.setDividerLocation(496);
        return split;
    }

    private JComponent buildTreatmentTab() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        setupTable(treatmentTable, new int[]{58, 145, 42, 138, 255, 72, 580});
        installEsiRenderer(treatmentTable, 3);
        addDoubleClick(treatmentTable, this::selectedTreatmentPatient);
        panel.add(new JScrollPane(treatmentTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(new JLabel("Equipment / อุปกรณ์:"));
        buttons.add(allocTypeBox);
        buttons.add(primary(button("Allocate / จัดสรร", e -> allocateSelected())));
        buttons.add(button("Release... / คืนอุปกรณ์", e -> releaseFromSelected()));
        buttons.add(button("Discharge / จำหน่าย", e -> dischargeSelected()));
        buttons.add(button("Details / รายละเอียด", e -> showDetails(selectedTreatmentPatient())));
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildEquipmentTab() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        setupTable(equipmentTable, new int[]{70, 130, 170, 80, 170, 200, 70, 80, 70});
        // color-code status column
        equipmentTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String text = String.valueOf(value);
                if (!isSelected) {
                    if (text.startsWith("Available")) {
                        setForeground(new Color(0x2E7D32));
                    } else if (text.startsWith("Under")) {
                        setForeground(new Color(0x6A1B9A));
                    } else {
                        setForeground(new Color(0xC62828));
                    }
                }
                return this;
            }
        });
        panel.add(new JScrollPane(equipmentTable), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addTypeBox);
        buttons.add(button("Add device / เพิ่มอุปกรณ์", e -> {
            MedicalEquipment added = system.addEquipment((EquipmentType) addTypeBox.getSelectedItem());
            refreshAll();
            showStatus("Added " + added, false);
        }));
        buttons.add(button("Start maintenance / ส่งซ่อมบำรุง", e -> maintenance(true)));
        buttons.add(button("Complete maintenance / ซ่อมเสร็จ", e -> maintenance(false)));
        buttons.add(button("Release from patient / คืนจากผู้ป่วย", e -> releaseSelectedDevice()));
        south.add(buttons, BorderLayout.NORTH);
        equipmentSummary.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        south.add(equipmentSummary, BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildReportTab() {
        logArea.setEditable(false);
        reportArea.setEditable(false);
        reportArea.setFont(textAreaFont(13));
        logArea.setFont(textAreaFont(12));

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Event log / บันทึกเหตุการณ์"));
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel reportPanel = new JPanel(new BorderLayout());
        reportPanel.setBorder(BorderFactory.createTitledBorder("Report output / ผลรายงาน"));
        reportPanel.add(new JScrollPane(reportArea), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(primary(button("Daily report / รายงานประจำวัน",
                e -> showReport(system.createDailyReport().getText()))));
        buttons.add(button("Shortage check / ตรวจอุปกรณ์ไม่พอ", e -> {
            List<String> shortages = system.checkShortages();
            showReport(shortages.isEmpty() ? "No shortages / อุปกรณ์เพียงพอ"
                    : "Shortages / อุปกรณ์ไม่พอ:\n" + String.join("\n", shortages));
        }));
        buttons.add(button("OOP concept demo / สาธิตแนวคิด OOP", e -> showReport(captureConceptDemo())));
        buttons.add(button("Clear log / ล้างบันทึก", e -> logArea.setText("")));
        reportPanel.add(buttons, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, logPanel, reportPanel);
        split.setDividerLocation(220);
        return split;
    }

    // UI actions

    private void admitFromForm() {
        try {
            VitalSigns v = readVitalsFromForm();
            Patient p = system.admitPatient(nameField.getText(), (Integer) ageSpinner.getValue(),
                    complaintField.getText(), v, (Integer) painSpinner.getValue(),
                    (Consciousness) avpuBox.getSelectedItem(), resourcesBox.getSelectedIndex(),
                    highRiskBox.isSelected());
            clearForm();
            refreshAll();
            selectPatient(queueTable, queueModel.rows, p);
            showStatus("Admitted " + p.getName() + " -> " + p.getTriageLevel()
                    + ", queue position " + system.queuePosition(p), false);
            if (p.getTriageLevel() == TriageLevel.ESI_1) {
                int answer = JOptionPane.showConfirmDialog(this,
                        p.getName() + " is ESI 1 (" + String.join(", ", p.getTriageResult().getReasons())
                                + ").\nCall to resuscitation now? / เรียกเข้าห้องกู้ชีพทันทีหรือไม่?",
                        "Critical patient / ผู้ป่วยวิกฤต", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (answer == JOptionPane.YES_OPTION) {
                    callNext();
                }
            }
        } catch (InvalidVitalSignsException ex) {
            error("Invalid vital signs / สัญญาณชีพไม่ถูกต้อง", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            error("Invalid input / ข้อมูลไม่ถูกต้อง", ex.getMessage());
        }
    }

    // read vitals from the bedside monitor
    private void readMonitor() {
        try {
            VitalSigns v = bedsideMonitor.readVitals();
            hrSpinner.setValue(v.getHeartRate());
            sbpSpinner.setValue(v.getSystolicBP());
            rrSpinner.setValue(v.getRespiratoryRate());
            spo2Spinner.setValue(v.getOxygenSaturation());
            tempSpinner.setValue(v.getTemperature());
            showStatus("Read from " + bedsideMonitor.getSourceName() + ": " + v, false);
        } catch (InvalidVitalSignsException ex) {
            error("Monitor error", ex.getMessage());
        }
    }

    private void callNext() {
        Patient p = system.callNextPatient(currentStaff());
        refreshAll();
        if (p == null) {
            showStatus("Nobody is waiting / ไม่มีผู้ป่วยรอ", false);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(p.getName()).append(" - ").append(p.getTriageLevel()).append("\n\n");
        sb.append("Recommended / อุปกรณ์ที่แนะนำ: ").append(p.getTriageResult().getRecommendedEquipment()).append("\n");
        sb.append("Allocated / ได้รับ: ");
        if (p.getAssignedEquipment().isEmpty()) {
            sb.append("none / ไม่มี");
        }
        for (MedicalEquipment e : p.getAssignedEquipment()) {
            sb.append("\n  - ").append(e);
        }
        Staff s = currentStaff();
        if (s != null && !s.canAuthorizeLifeSupport() && hasLifeSupportNeed(p)) {
            sb.append("\n\nNote: life-support equipment needs a doctor. Switch 'Acting staff' to a doctor and allocate it.")
              .append("\nหมายเหตุ: อุปกรณ์ช่วยชีวิตต้องให้แพทย์อนุมัติ");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Patient called / เรียกผู้ป่วยเข้ารักษา",
                JOptionPane.INFORMATION_MESSAGE);
        tabs.setSelectedIndex(1);
        selectPatient(treatmentTable, treatmentModel.rows, p);
    }

    private boolean hasLifeSupportNeed(Patient p) {
        for (EquipmentType t : p.getTriageResult().getRecommendedEquipment()) {
            if (t == EquipmentType.VENTILATOR || t == EquipmentType.DEFIBRILLATOR) {
                boolean has = false;
                for (MedicalEquipment e : p.getAssignedEquipment()) {
                    has |= e.getType() == t;
                }
                if (!has) {
                    return true;
                }
            }
        }
        return false;
    }

    private void retriageSelected() {
        Patient p = selectedWaitingPatient();
        if (p == null) {
            return;
        }
        try {
            VitalSigns v = readVitalsFromForm();
            int answer = JOptionPane.showConfirmDialog(this,
                    "Re-triage " + p.getName() + " with these vitals?\n" + v + "\nคัดกรองใหม่ด้วยค่าในแบบฟอร์ม?",
                    "Re-triage", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
            p.setPainScore((Integer) painSpinner.getValue());
            p.setConsciousness((Consciousness) avpuBox.getSelectedItem());
            TriageResult r = system.retriage(p, v);
            refreshAll();
            selectPatient(queueTable, queueModel.rows, p);
            showStatus(p.getName() + " is now " + r.getLevel(), false);
        } catch (InvalidVitalSignsException ex) {
            error("Invalid vital signs", ex.getMessage());
        }
    }

    private void allocateSelected() {
        Patient p = selectedTreatmentPatient();
        if (p == null) {
            return;
        }
        EquipmentType type = (EquipmentType) allocTypeBox.getSelectedItem();
        try {
            MedicalEquipment e = system.allocate(p, type, currentStaff());
            refreshAll();
            selectPatient(treatmentTable, treatmentModel.rows, p);
            showStatus("Allocated " + e.getId() + " to " + p.getName(), false);
        } catch (EquipmentUnavailableException ex) {
            error("Not available / ไม่มีอุปกรณ์ว่าง", ex.getMessage());
        } catch (UnauthorizedAllocationException ex) {
            error("Not authorized / ไม่มีสิทธิ์", ex.getMessage());
        }
    }

    private void releaseFromSelected() {
        Patient p = selectedTreatmentPatient();
        if (p == null) {
            return;
        }
        List<MedicalEquipment> owned = p.getAssignedEquipment();
        if (owned.isEmpty()) {
            showStatus(p.getName() + " has no equipment / ผู้ป่วยไม่มีอุปกรณ์", false);
            return;
        }
        MedicalEquipment choice = (MedicalEquipment) JOptionPane.showInputDialog(this,
                "Which device should be returned? / คืนอุปกรณ์ชิ้นใด?", "Release equipment",
                JOptionPane.QUESTION_MESSAGE, null, owned.toArray(), owned.get(0));
        if (choice != null) {
            system.release(choice, currentStaff());
            refreshAll();
            selectPatient(treatmentTable, treatmentModel.rows, p);
        }
    }

    private void dischargeSelected() {
        Patient p = selectedTreatmentPatient();
        if (p == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Discharge " + p.getName() + " and return all equipment?\nจำหน่ายผู้ป่วยและคืนอุปกรณ์ทั้งหมด?",
                "Discharge", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            system.discharge(p, currentStaff());
            refreshAll();
            showStatus("Discharged " + p.getName(), false);
        }
    }

    private void maintenance(boolean start) {
        MedicalEquipment e = selectedEquipment();
        if (e == null) {
            return;
        }
        try {
            if (start) {
                system.startMaintenance(e);
            } else {
                system.completeMaintenance(e);
            }
            refreshAll();
        } catch (IllegalStateException ex) {
            error("Cannot start maintenance", ex.getMessage());
        }
    }

    private void releaseSelectedDevice() {
        MedicalEquipment e = selectedEquipment();
        if (e != null && e.getAssignedPatient() != null) {
            system.release(e, currentStaff());
            refreshAll();
        }
    }

    private void advance(int minutes) {
        system.advanceClock(minutes);
        refreshAll();
    }

    private String captureConceptDemo() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8.name())) {
            ConceptDemo.runAll(out);
            return bytes.toString(StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException ex) {
            return "UTF-8 not supported: " + ex.getMessage();
        }
    }

    private void showReport(String text) {
        reportArea.setText(text);
        reportArea.setCaretPosition(0);
        tabs.setSelectedIndex(3);
    }

    private void showDetails(Patient p) {
        if (p == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(p.getId()).append("  ").append(p.getName()).append("\n");
        sb.append(p.getRole()).append(", ").append(p.getAge()).append(" years");
        if (p instanceof PediatricPatient) {
            sb.append(" (").append(((PediatricPatient) p).getAgeGroup()).append(")");
        }
        sb.append("\nStatus / สถานะ: ").append(p.getStatus());
        sb.append("\nComplaint / อาการ: ").append(p.getChiefComplaint());
        sb.append("\nVitals / สัญญาณชีพ: ").append(p.getVitals());
        sb.append("\nAVPU: ").append(p.getConsciousness()).append("   Pain / ปวด: ").append(p.getPainScore()).append("/10");
        sb.append("\nArrived / มาถึง: ").append(HospitalSystem.formatMinutes(p.getArrivalMinute()))
          .append("   Waited / รอ: ").append(system.getWaitingMinutes(p)).append(" min");
        TriageResult r = p.getTriageResult();
        sb.append("\n\n").append(r.getLevel()).append(" - ").append(r.getLevel().getNameTh())
          .append("   [").append(r.getProtocolName()).append("]");
        sb.append("\nPriority score / คะแนนความสำคัญ: ").append(p.getPriorityScore());
        sb.append("\nReasons / เหตุผล:");
        for (String reason : r.getReasons()) {
            sb.append("\n  - ").append(reason);
        }
        sb.append("\nRecommended equipment / อุปกรณ์ที่แนะนำ: ").append(r.getRecommendedEquipment());
        sb.append("\nAssigned equipment / อุปกรณ์ที่ได้รับ:");
        for (MedicalEquipment e : p.getAssignedEquipment()) {
            sb.append("\n  - ").append(e).append(" (").append(e.getSpecification()).append(")");
        }
        if (!p.getNotes().isEmpty()) {
            sb.append("\nNotes / บันทึก:");
            for (String n : p.getNotes()) {
                sb.append("\n  - ").append(n);
            }
        }
        JTextArea area = new JTextArea(sb.toString(), 18, 60);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Patient details / ข้อมูลผู้ป่วย",
                JOptionPane.PLAIN_MESSAGE);
    }

    // helpers

    private void refreshAll() {
        queueModel.reload();
        treatmentModel.reload();
        equipmentModel.reload();
        clockLabel.setText("Time " + HospitalSystem.formatMinutes(system.getClockMinutes()));
        tabs.setTitleAt(0, "1. Triage & Queue / คัดกรองและคิว (" + queueModel.rows.size() + ")");
        tabs.setTitleAt(1, "2. Treatment / การรักษา (" + treatmentModel.rows.size() + ")");
        List<MedicalEquipment> all = system.getEquipment();
        int available = 0;
        for (MedicalEquipment e : all) {
            if (e.isAvailable()) {
                available++;
            }
        }
        equipmentSummary.setText(String.format(
                "Devices %d | available %d | running cost of devices in use %,.0f THB/hour  /  อุปกรณ์ทั้งหมด %d ว่าง %d",
                all.size(), available, EquipmentReports.totalHourlyCostInUse(all), all.size(), available));
    }

    private VitalSigns readVitalsFromForm() throws InvalidVitalSignsException {
        return new VitalSigns((Integer) hrSpinner.getValue(), (Integer) sbpSpinner.getValue(),
                (Integer) rrSpinner.getValue(), (Integer) spo2Spinner.getValue(),
                ((Number) tempSpinner.getValue()).doubleValue());
    }

    private void clearForm() {
        nameField.setText("");
        complaintField.setText("");
        ageSpinner.setValue(35);
        hrSpinner.setValue(80);
        sbpSpinner.setValue(120);
        rrSpinner.setValue(16);
        spo2Spinner.setValue(98);
        tempSpinner.setValue(37.0);
        painSpinner.setValue(0);
        avpuBox.setSelectedIndex(0);
        resourcesBox.setSelectedIndex(1);
        highRiskBox.setSelected(false);
        nameField.requestFocusInWindow();
    }

    private Staff currentStaff() {
        return (Staff) staffBox.getSelectedItem();
    }

    private Patient selectedWaitingPatient() {
        return selectedRow(queueTable, queueModel.rows, "Select a waiting patient first / กรุณาเลือกผู้ป่วยในคิว");
    }

    private Patient selectedTreatmentPatient() {
        return selectedRow(treatmentTable, treatmentModel.rows, "Select a patient in treatment first / กรุณาเลือกผู้ป่วย");
    }

    private MedicalEquipment selectedEquipment() {
        return selectedRow(equipmentTable, equipmentModel.rows, "Select a device first / กรุณาเลือกอุปกรณ์");
    }

    private <T> T selectedRow(JTable table, List<T> rows, String messageIfNone) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showStatus(messageIfNone, true);
            return null;
        }
        return rows.get(table.convertRowIndexToModel(viewRow));
    }

    private static void selectPatient(JTable table, List<Patient> rows, Patient p) {
        int index = rows.indexOf(p);
        if (index >= 0) {
            table.setRowSelectionInterval(index, index);
            table.scrollRectToVisible(table.getCellRect(index, 0, true));
        }
    }

    private void showStatus(String message, boolean warning) {
        statusLabel.setForeground(warning ? new Color(0xC62828) : new Color(0x1B5E20));
        statusLabel.setText(message);
    }

    private void error(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
        showStatus(message, true);
    }

    private static JButton button(String text, ActionListener action) {
        JButton b = new JButton(text);
        b.addActionListener(action);
        return b;
    }

    private static JButton primary(JButton b) {
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        return b;
    }

    private static JSpinner intSpinner(int value, int min, int max) {
        return new JSpinner(new SpinnerNumberModel(value, min, max, 1));
    }

    private static GridBagConstraints gbc(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.insets = new Insets(3, 6, 3, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = x == 1 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        c.weightx = x == 1 ? 1 : 0;
        return c;
    }

    private static int addRow(JPanel panel, int row, String label, JComponent field) {
        panel.add(new JLabel(label), gbc(0, row));
        panel.add(field, gbc(1, row));
        return row + 1;
    }

    private static void setupTable(JTable table, int[] widths) {
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    // paint ESI badge background color
    private static void installEsiRenderer(JTable table, int column) {
        table.getColumnModel().getColumn(column).setCellRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (value instanceof TriageLevel) {
                    TriageLevel level = (TriageLevel) value;
                    setText("ESI " + level.getLevel() + " " + level.getNameTh());
                    setBackground(new Color(level.getRgb()));
                    setForeground(level == TriageLevel.ESI_3 ? Color.BLACK : Color.WHITE);
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                return this;
            }
        });
    }

    private static void installWaitRenderer(JTable table, int column) {
        table.getColumnModel().getColumn(column).setCellRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                boolean late = String.valueOf(value).contains("LATE");
                if (!isSelected) {
                    setForeground(late ? new Color(0xC62828) : Color.DARK_GRAY);
                }
                return this;
            }
        });
    }

    // double-click table row to open patient details
    private void addDoubleClick(JTable table, Supplier<Patient> selected) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showDetails(selected.get());
                }
            }
        });
    }

    // table models

    private class QueueTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final String[] columns = {"#", "ID", "Name / ชื่อ", "Age", "ESI",
            "Complaint / อาการ", "Wait / รอ (min)", "Needs / อุปกรณ์ที่ต้องใช้"};
        private transient List<Patient> rows = new ArrayList<>();

        void reload() {
            rows = HospitalGUI.this.system.getWaitingPatients();
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            Patient p = rows.get(row);
            switch (column) {
                case 0:
                    return row + 1;
                case 1:
                    return p.getId();
                case 2:
                    return p.getName() + (p.isPediatric() ? " (child)" : "");
                case 3:
                    return p.getAge();
                case 4:
                    return p.getTriageLevel();
                case 5:
                    return p.getChiefComplaint();
                case 6:
                    int waited = HospitalGUI.this.system.getWaitingMinutes(p);
                    int target = p.getTriageLevel().getTargetWaitMinutes();
                    return waited + " / " + target + (waited > target ? "  LATE" : "");
                default:
                    return typesText(p.getTriageResult().getRecommendedEquipment());
            }
        }
    }

    private class TreatmentTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final String[] columns = {"ID", "Name / ชื่อ", "Age", "ESI",
            "Equipment / อุปกรณ์ที่ได้รับ", "THB/hour", "Notes / บันทึก"};
        private transient List<Patient> rows = new ArrayList<>();

        void reload() {
            rows = HospitalGUI.this.system.getPatientsInTreatment();
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            Patient p = rows.get(row);
            switch (column) {
                case 0:
                    return p.getId();
                case 1:
                    return p.getName();
                case 2:
                    return p.getAge();
                case 3:
                    return p.getTriageLevel();
                case 4:
                    StringBuilder sb = new StringBuilder();
                    for (MedicalEquipment e : p.getAssignedEquipment()) {
                        sb.append(sb.length() == 0 ? "" : ", ").append(e.getId());
                    }
                    return sb.length() == 0 ? "-" : sb.toString();
                case 5:
                    return String.format("%,.0f", EquipmentReports.totalHourlyCostInUse(p.getAssignedEquipment()));
                default:
                    List<String> notes = p.getNotes();
                    return notes.isEmpty() ? "" : notes.get(notes.size() - 1);
            }
        }
    }

    private class EquipmentTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final String[] columns = {"ID", "Type / ประเภท", "Model / รุ่น", "Life support",
            "Status / สถานะ", "Assigned to / ผู้ใช้", "Usage h", "Service at h", "THB/h"};
        private transient List<MedicalEquipment> rows = new ArrayList<>();

        void reload() {
            rows = new ArrayList<>(HospitalGUI.this.system.getEquipment());
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            MedicalEquipment e = rows.get(row);
            switch (column) {
                case 0:
                    return e.getId();
                case 1:
                    return e.getType().getDisplayName();
                case 2:
                    return e.getModel();
                case 3:
                    return e.isLifeSupport() ? "YES" : "";
                case 4:
                    return e.statusLabel();
                case 5:
                    Patient p = e.getAssignedPatient();
                    return p == null ? "" : p.getId() + " " + p.getName();
                case 6:
                    return String.format("%.1f", e.getUsageHours());
                case 7:
                    return String.format("%.0f", e.getServiceIntervalHours());
                default:
                    return String.format("%,.0f", e.getHourlyCost());
            }
        }
    }

    private static String typesText(java.util.Set<EquipmentType> types) {
        if (types.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (EquipmentType t : types) {
            sb.append(sb.length() == 0 ? "" : ", ").append(t.getDisplayName());
        }
        return sb.toString();
    }
}
