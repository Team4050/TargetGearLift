package com.biohazard4050.targetgearlift.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Used strictly for writing config files yo
 */
public class WriteFile {
    public static void main(String[] args) {
        Properties prop = new Properties();
        OutputStream output = null;

        try {

            output = new FileOutputStream("config.biohazard");

            prop.setProperty("roborioIPAddress", "127.0.0.1");
            prop.setProperty("ntName", "\\Vision");
            prop.setProperty("headless", "false");
            prop.setProperty("captureDevice", "0");
            prop.setProperty("VIDEO_WIDTH", "640.0");
            prop.setProperty("VIDEO_HEIGHT", "360.0");
            prop.setProperty("CAPTURE_STATUS_STREAM", "STREAM");
            prop.setProperty("CAPTURE_STATUS_RESTART", "RESTART");
            prop.setProperty("CAPTURE_STATUS_STOP", "STOP");
            prop.setProperty("MIN_ACCEPTED_SCORE", "250");
            prop.setProperty("HSVShown", "false");
            prop.setProperty("exposure", "0.0");
            prop.setProperty("minHue", "0.0");
            prop.setProperty("maxHue", "0.0");
            prop.setProperty("minSat", "0.0");
            prop.setProperty("maxSat", "0.0");
            prop.setProperty("minVal", "0.0");
            prop.setProperty("maxVal", "0.0");

            // save properties to project root folder
            prop.store(output, null);

        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

}
