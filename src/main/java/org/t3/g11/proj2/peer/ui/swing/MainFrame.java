package org.t3.g11.proj2.peer.ui.swing;

import com.sun.tools.jconsole.JConsoleContext;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainFrame extends JFrame {

    private final SwingInterface swi;

    public MainFrame(SwingInterface swi) {
        super();

        this.swi = swi;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Molater");

//        setMinimumSize(new Dimension(500, 700));

        Toolkit toolKit = getToolkit();
        Dimension size = toolKit.getScreenSize();
        setLocation(size.width/2 - getWidth()/2, size.height/2 - getHeight()/2);

        // add components and stuff
        add(this.basePanel());
//        add(this.form());

        pack();
        setVisible(true);//making the frame visible
    }

    private JPanel basePanel() {
        MigLayout layout = new MigLayout("insets 20");
        JPanel panel = new JPanel(layout);

        JLabel title = new JLabel("Welcome to Molater", JLabel.CENTER);
        title.setPreferredSize(new Dimension(1, 27));

        panel.add(title, "span, wrap 15, growx");

        panel.add(new JLabel("Username"),   "span, wrap");
        JTextField username = createTextField();
        panel.add(username, "span, wrap, pushx, growx");

        JLabel warningText = new JLabel("Error message here");
        warningText.setForeground(Color.red);
        warningText.setVisible(false);
        panel.add(warningText, "span, wrap");

        JButton registerButton = new JButton("Register");
        JButton loginButton = new JButton("Login");

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String user = username.getText().trim();
                if (user.isEmpty()) {
                    warningText.setText("Username must not be empty");
                    warningText.setVisible(true);

                    username.setText("");

                    pack();
                    return;
                }

                registerButton.setEnabled(false);
                loginButton.setEnabled(false);

                if (swi.peer.register(user)) {
                    System.out.println("hell yeah brotha");
                } else {
                    warningText.setText("Username already exists");
                    warningText.setVisible(true);

                    username.setText("");

                    registerButton.setEnabled(true);
                    loginButton.setEnabled(true);

                    pack();
                }
            }
        });
        panel.add(registerButton, "gaptop 10");

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String user = username.getText().trim();
                if (user.isEmpty()) {
                    warningText.setText("Username must not be empty");
                    warningText.setVisible(true);

                    username.setText("");

                    pack();
                    return;
                }

                registerButton.setEnabled(false);
                loginButton.setEnabled(false);

                if (swi.peer.authenticate(user)) {
                    System.out.println("hell yeah brotha");
                } else {
                    warningText.setText("Failed to load " + user + "'s key or database");
                    warningText.setVisible(true);

                    username.setText("");

                    registerButton.setEnabled(true);
                    loginButton.setEnabled(true);

                    pack();
                }
            }
        });
        panel.add(new JButton("Login"), "gapleft 30");

        return panel;
    }

    private JTextField createTextField() {
        JTextField tf = new JTextField();
        tf.setPreferredSize(new Dimension(1, 27));
        return tf;
    }

}
