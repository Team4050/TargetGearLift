package org.frc4050.targetgearlift.util;

import javax.swing.*;

public class GUI extends JFrame {

    public GUI(String windowTitle, int width, int height, boolean setVisible, boolean setResizable) {
        setTitle(windowTitle);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(width, height);
        setVisible(setVisible);
        setResizable(setResizable);
    }
}
