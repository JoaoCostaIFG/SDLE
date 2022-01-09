package org.t3.g11.proj2.peer.ui.swing.menu;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class MenuItemListener implements ActionListener {

    JFrame frame;

    public MenuItemListener(JFrame frame) {
        this.frame = frame;
    }

    public void actionPerformed(ActionEvent e) {
        final JOptionPane optionPane = new JOptionPane(
                "The only way to close this dialog is by\n"
                        + "pressing one of the following buttons.\n"
                        + "Do you understand?",
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_OPTION);

        final JDialog dialog = new JDialog(frame,
                "Click a button",
                true);
        dialog.setContentPane(optionPane);
        dialog.setDefaultCloseOperation(
                JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.out.println("Thwarted user attempt to close window.");
            }
        });
        optionPane.addPropertyChangeListener(
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent e) {
                        String prop = e.getPropertyName();

                        if (dialog.isVisible()
                                && (e.getSource() == optionPane)
                                && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                            //If you were going to check something before closing the window, you'd do it here.
                            dialog.setVisible(false);
                        }
                    }
                });
        dialog.pack();
        dialog.setVisible(true);

        int value = ((Integer)optionPane.getValue()).intValue();
        if (value == JOptionPane.YES_OPTION) {
            // Yes option
            System.out.println("Good.");
        } else if (value == JOptionPane.NO_OPTION) {
            // No option
            System.out.println("Try using the window decorations "
                    + "to close the non-auto-closing dialog. "
                    + "You can't!");
        }
    }
}
