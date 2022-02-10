package org.t3.g11.proj2.peer.ui.swing.panels;

import net.miginfocom.swing.MigLayout;
import org.t3.g11.proj2.peer.ui.swing.SwingInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginPanel extends JPanel {

    public LoginPanel(SwingInterface swi, Runnable gotoAuthenticate) {
        super(new MigLayout("insets 20"));

        LoginPanel This = this;

        JLabel title = new JLabel("Welcome to Molater", JLabel.CENTER);
        title.setHorizontalAlignment(JLabel.CENTER);
        title.setFont( UIManager.getFont( "h0.font" ) );

        add(title, "span, wrap 15, growx");

        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont( UIManager.getFont( "large.font" ) );
        add(usernameLabel,   "span, wrap");
        JTextField username = createTextField();
        username.setText("");
        add(username, "span, wrap, pushx, growx");

        JLabel warningText = new JLabel("Error message here");
        warningText.setForeground(Color.red);
        warningText.setVisible(false);
        add(warningText, "span, wrap");

        JButton registerButton = new JButton("Register");
        registerButton.setFont( UIManager.getFont("h3.font") );
        JButton loginButton = new JButton("Login");
        loginButton.setFont( UIManager.getFont("h3.font") );

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String user = username.getText().trim();
                if (user.isEmpty()) {
                    warningText.setText("Username must not be empty");
                    warningText.setVisible(true);

                    username.setText("");

                    return;
                }

                registerButton.setEnabled(false);
                loginButton.setEnabled(false);

                if (swi.peer.register(user)) {
                    gotoAuthenticate.run();
                } else {
                    warningText.setText("Username already exists");
                    warningText.setVisible(true);

                    username.setText("");

                    registerButton.setEnabled(true);
                    loginButton.setEnabled(true);
                }
            }
        });
        add(registerButton, "gaptop 10");

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String user = username.getText().trim();
                if (user.isEmpty()) {
                    warningText.setText("Username must not be empty");
                    warningText.setVisible(true);

                    username.setText("");

                    return;
                }

                registerButton.setEnabled(false);
                loginButton.setEnabled(false);

                if (swi.peer.authenticate(user)) {
                    gotoAuthenticate.run();
                } else {
                    warningText.setText("Failed to load " + user + "'s key or database");
                    warningText.setVisible(true);

                    username.setText("");

                    registerButton.setEnabled(true);
                    loginButton.setEnabled(true);
                }
            }
        });
        add(loginButton);
    }

    private JTextField createTextField() {
        JTextField tf = new JTextField();
        tf.setPreferredSize(new Dimension(1, 27));
        return tf;
    }
}
