package util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class RTSettings {

    private GlobalVariables gv = new GlobalVariables();

    public void applyFileData(String path) {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            System.out.println("[DEBUG] Attempting to load file located at " + path);
            input = new FileInputStream(path);

            prop.load(input);

            System.out.println("[DEBUG] File loaded, reading config data and applying to GlobalVariables.java");
            gv.setRoborioIPAddress(prop.getProperty("roborioIPAddress"));
            gv.setNtName(prop.getProperty("ntName"));
            gv.setHeadless(Boolean.parseBoolean(prop.getProperty("headless")));
            gv.setCaptureDevice(Integer.parseInt(prop.getProperty("captureDevice")));
            gv.setVIDEO_WIDTH(Double.parseDouble(prop.getProperty("VIDEO_WIDTH")));
            gv.setVIDEO_HEIGHT(Double.parseDouble(prop.getProperty("VIDEO_HEIGHT")));
            gv.setCAPTURE_STATUS_STREAM(prop.getProperty("CAPTURE_STATUS_STREAM"));
            gv.setCAPTURE_STATUS_RESTART(prop.getProperty("CAPTURE_STATUS_RESTART"));
            gv.setCAPTURE_STATUS_STOP(prop.getProperty("CAPTURE_STATUS_STOP"));
            gv.setMIN_ACCEPTED_SCORE(Integer.parseInt(prop.getProperty("MIN_ACCEPTED_SCORE")));
            gv.setHSVShown(Boolean.parseBoolean(prop.getProperty("HSVShown")));
            gv.setExposure(Double.parseDouble(prop.getProperty("exposure")));
            gv.setMinHue(Double.parseDouble(prop.getProperty("minHue")));
            gv.setMaxHue(Double.parseDouble(prop.getProperty("maxHue")));
            gv.setMinSat(Double.parseDouble(prop.getProperty("minSat")));
            gv.setMaxSat(Double.parseDouble(prop.getProperty("maxSat")));
            gv.setMinVal(Double.parseDouble(prop.getProperty("minVal")));
            gv.setMaxVal(Double.parseDouble(prop.getProperty("maxVal")));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
