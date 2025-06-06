package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainGUI extends JFrame {
    private JCheckBox startBotCheckBox;
    private JButton saveButton;

    public MainGUI() {
        setTitle("EliteBot Configuration");
        setSize(300, 150);
        setLayout(new FlowLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        startBotCheckBox = new JCheckBox("Start bot after saving");
        saveButton = new JButton("Save and Close");

        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Save settings logic placeholder
                dispose();
            }
        });

        add(startBotCheckBox);
        add(saveButton);
    }

    public boolean shouldStartBot() {
        return startBotCheckBox.isSelected();
    }
}