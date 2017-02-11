package com.biohazard4050.targetgearlift;

import javax.swing.JFrame;

public class GUI extends JFrame {

    public GUI(String windowTitle, int width, int height, boolean setVisible, boolean setResizable) {
        setTitle(windowTitle);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(width, height);
        setVisible(setVisible);
        setResizable(setResizable);
    }
}
