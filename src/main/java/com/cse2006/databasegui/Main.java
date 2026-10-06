package com.cse2006.databasegui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point for the CSE2006 database GUI demonstration.
 *
 * <p>Loads the look and feel of the operating system and then opens
 * {@link MainFrame} on the Swing event dispatch thread.</p>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("Could not load the system look and feel: " + ex.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
