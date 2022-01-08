package org.t3.g11.proj2.peer.ui.swing;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        super();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Molater");

        setSize(400,500);//400 width and 500 height

        Toolkit toolKit = getToolkit();
        Dimension size = toolKit.getScreenSize();
        setLocation(size.width/2 - getWidth()/2, size.height/2 - getHeight()/2);

        // add components and stuff
//        add(this.basePanel());
        add(this.form());

        pack();
        setVisible(true);//making the frame visible
    }

    private JPanel basePanel() {
        MigLayout layout = new MigLayout("fillx", "[right]rel[grow,fill]", "[]10[]");
        JPanel panel = new JPanel(layout);

        panel.add(new JLabel("Enter size:"),   "");
        panel.add(new JTextField(""),          "wrap");
        panel.add(new JLabel("Enter weight:"), "");
        panel.add(new JTextField(""),          "");

        return panel;
    }

    private JPanel form() {
        MigLayout layout = new MigLayout("");
        JPanel panel = new JPanel(layout);

        JLabel title = new JLabel("Registration Form", JLabel.CENTER);
//        title.setOpaque(true);
//        title.setBackground(Color.black);
//        title.setForeground(Color.white);
        title.setPreferredSize(new Dimension(1, 27));

        panel.add(title, "span, wrap 15, growx");
        panel.add(new JLabel("Name"), "al right");
        panel.add(createTextField(), "wrap, pushx, growx");
        panel.add(new JLabel("Phone"), "al right");
        panel.add(createTextField(), "wrap, pushx, growx");
        panel.add(new JLabel("DOB"), "al right");
        panel.add(new JComboBox<>(new String[]{"1989"}), "split 3");
        panel.add(new JComboBox<>(new String[]{"01"}));
        panel.add(new JComboBox<>(new String[]{"01"}), "wrap");
        panel.add(new JLabel("Address"), "al right");
        panel.add(new JScrollPane(new JTextArea(3, 10)), "wrap, pushx, growx");
        panel.add(new JLabel("Resume"), "al right");
        panel.add(createTextField(), "pushx, growx, split 2");
        panel.add(new JButton("..."), "wrap 10");
        panel.add(new JLabel(""));
        panel.add(new JButton("Register"));

        panel.setPreferredSize(new Dimension(350, 300));

        return panel;
    }

    private JTextField createTextField() {
        JTextField tf = new JTextField();
        tf.setPreferredSize(new Dimension(1, 27));
        return tf;
    }

}
