package org.t3.g11.proj2.peer.ui.swing;

import com.formdev.flatlaf.FlatDarkLaf;
import net.miginfocom.swing.MigLayout;
import org.t3.g11.proj2.peer.Peer;
import org.t3.g11.proj2.peer.ui.PeerInterface;
import org.t3.g11.proj2.peer.ui.swing.menu.MenuBar;
import org.t3.g11.proj2.peer.ui.swing.panels.LoginPanel;
import org.t3.g11.proj2.peer.ui.swing.panels.MainPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;

class MainFrame extends JFrame {

    private final SwingInterface swi;

    private final LoginPanel loginPanel;

    public MainFrame(SwingInterface swi) {
        super();

        this.swi = swi;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Molater");

        // add components and stuff
        this.loginPanel = new LoginPanel(swi, this::gotoAuthenticated);
        add(this.loginPanel);

        pack();
        setVisible(true);//making the frame visible

        // Center on screen
        Toolkit toolKit = getToolkit();
        Dimension size = toolKit.getScreenSize();
        setLocation(size.width/2 - getWidth()/2, size.height/2 - getHeight()/2);

        URL iconURL = MainFrame.class.getClassLoader().getResource("tordo_square_512.png");
        Image icon = Toolkit.getDefaultToolkit().getImage(iconURL);
        setIconImage(icon);
    }

    private void gotoAuthenticated() {
        remove(this.loginPanel);
        add(new MainPanel(this.swi));
        setJMenuBar(new MenuBar(swi));
        pack();
        setSize(600, 400);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        revalidate();
    }

}

public class SwingInterface extends JFrame implements PeerInterface {

    public final Peer peer;

    public SwingInterface(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void setup() {
        // https://stackoverflow.com/questions/18976990/best-practice-to-start-a-swing-application
        SwingUtilities.invokeLater(() -> {
            // https://www.formdev.com/flatlaf
            FlatDarkLaf.setup();

            MainFrame mf = new MainFrame(this);
        });
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    public ImageIcon createImageIcon(String path,
                                        String description) {
        java.net.URL imgURL = getClass().getClassLoader().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
}
