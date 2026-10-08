package com.cse2006.databasegui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point of the application.
 *
 * <p>Sets the operating system look and feel and then opens the main window
 * on the Swing event dispatch thread.</p>
 */
public class Main {

    public static void main(String[] args) {
        // Makes the text look smoother on screen.
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("Could not load the system look and feel: " + ex.getMessage());
        }

        // Swing windows must always be created on the event dispatch thread.
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
