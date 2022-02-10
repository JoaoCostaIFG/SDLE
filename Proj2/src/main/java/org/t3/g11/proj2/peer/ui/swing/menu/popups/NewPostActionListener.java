package org.t3.g11.proj2.peer.ui.swing.menu.popups;

import net.miginfocom.swing.MigLayout;
import org.t3.g11.proj2.peer.ui.swing.SwingInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NewPostActionListener implements ActionListener {

    private final SwingInterface swi;

    public NewPostActionListener(SwingInterface swi) {
        this.swi = swi;
    }

    public void actionPerformed(ActionEvent e) {

        NewPostPanel postPanel = new NewPostPanel();
        Object[] options = { "POST", "CANCEL" };
        int selected = JOptionPane.showOptionDialog(null, postPanel, "New post",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, null);

        if (selected != 0)
            return;

        String content = postPanel.input.getText().trim();
        if (content.isBlank()) {
            JOptionPane.showMessageDialog(null, "Post must not be empty", "Alert", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (swi.peer.newPost(content)) {
            JOptionPane.showMessageDialog(null, "Post successful!", "Alert", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, "Failed to create post", "Alert", JOptionPane.ERROR_MESSAGE);
        }
    }
}

class NewPostPanel extends JPanel {

    public final JTextArea input;

    NewPostPanel() {
        super(new MigLayout("wrap, inset 5"));

        JLabel label = new JLabel("Content");
        add(label);

        this.input = new JTextArea();
        input.setLineWrap(true);
        input.setPreferredSize(new Dimension(400, 200));
        add(input);

        setPreferredSize(new Dimension(400, 240));
    }
}
