package hospital;

import hospital.demo.ConceptDemo;
import hospital.service.HospitalSystem;
import hospital.tests.SystemTest;
import hospital.ui.ConsoleApp;
import hospital.ui.HospitalGUI;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.Scanner;

/**
 * Program entry point.
 *
 * CSC250 Object-Oriented Programming - Capstone Project
 * Group "chill until final"
 *   ณพัชรกัณฑ์ พัชญ์ชัยพงศา - designer / programmer
 *
 * Args: (none) Swing GUI | --console text menu | --demo OOP demo | --test run tests
 */
public class Main {

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0].toLowerCase() : "";
        switch (mode) {
            case "--console":
                new ConsoleApp(HospitalSystem.createWithSampleData(), new Scanner(System.in)).run();
                break;
            case "--demo":
                ConceptDemo.runAll(System.out);
                break;
            case "--test":
                System.exit(SystemTest.runAll() ? 0 : 1);
                break;
            default:
                if (GraphicsEnvironment.isHeadless()) {
                    System.out.println("No display found - starting console mode.");
                    new ConsoleApp(HospitalSystem.createWithSampleData(), new Scanner(System.in)).run();
                } else {
                    SwingUtilities.invokeLater(() -> {
                        HospitalGUI.installLookAndFeel();
                        new HospitalGUI(HospitalSystem.createWithSampleData()).setVisible(true);
                    });
                }
        }
    }
}
