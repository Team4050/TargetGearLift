package com.biohazard4050.targetgearlift;

public class GlobalVariables {

    private String roborioIPAddress = "10.40.50.4";
    private String ntName = "Vision";

    private double VIDEO_WIDTH  = 640.0;
    private double VIDEO_HEIGHT = 360.0;

    private String CAPTURE_STATUS_STREAM  = "STREAM";
    private String CAPTURE_STATUS_RESTART = "RESTART";
    private String CAPTURE_STATUS_STOP    = "STOP";

    private int MIN_ACCEPTED_SCORE = 250;

    private boolean HSV = false;
    private double exposure = 0.0;
    private double minHue = 0.0;
    private double minSat = 0.0;
    private double minVal = 0.0;
    private double maxHue = 0.0;
    private double maxSat = 0.0;
    private double maxVal = 0.0;
    private String streamStatus;

    private boolean headless = false;


    public String getRoborioIPAddress() {
        return roborioIPAddress;
    }

    public void setRoborioIPAddress(String roborioIPAddress) {
        this.roborioIPAddress = roborioIPAddress;
    }

    public String getNtName() {
        return ntName;
    }

    public void setNtName(String ntName) {
        this.ntName = ntName;
    }

    public double getVIDEO_WIDTH() {
        return VIDEO_WIDTH;
    }

    public void setVIDEO_WIDTH(double VIDEO_WIDTH) {
        this.VIDEO_WIDTH = VIDEO_WIDTH;
    }

    public double getVIDEO_HEIGHT() {
        return VIDEO_HEIGHT;
    }

    public void setVIDEO_HEIGHT(double VIDEO_HEIGHT) {
        this.VIDEO_HEIGHT = VIDEO_HEIGHT;
    }

    public String getCAPTURE_STATUS_STREAM() {
        return CAPTURE_STATUS_STREAM;
    }

    public void setCAPTURE_STATUS_STREAM(String CAPTURE_STATUS_STREAM) {
        this.CAPTURE_STATUS_STREAM = CAPTURE_STATUS_STREAM;
    }

    public String getCAPTURE_STATUS_RESTART() {
        return CAPTURE_STATUS_RESTART;
    }

    public void setCAPTURE_STATUS_RESTART(String CAPTURE_STATUS_RESTART) {
        this.CAPTURE_STATUS_RESTART = CAPTURE_STATUS_RESTART;
    }

    public String getCAPTURE_STATUS_STOP() {
        return CAPTURE_STATUS_STOP;
    }

    public void setCAPTURE_STATUS_STOP(String CAPTURE_STATUS_STOP) {
        this.CAPTURE_STATUS_STOP = CAPTURE_STATUS_STOP;
    }

    public int getMIN_ACCEPTED_SCORE() {
        return MIN_ACCEPTED_SCORE;
    }

    public void setMIN_ACCEPTED_SCORE(int MIN_ACCEPTED_SCORE) {
        this.MIN_ACCEPTED_SCORE = MIN_ACCEPTED_SCORE;
    }

    public boolean isHSV() {
        return HSV;
    }

    public void setHSV(boolean HSV) {
        this.HSV = HSV;
    }

    public double getExposure() {
        return exposure;
    }

    public void setExposure(double exposure) {
        this.exposure = exposure;
    }

    public double getMinHue() {
        return minHue;
    }

    public void setMinHue(double minHue) {
        this.minHue = minHue;
    }

    public double getMinSat() {
        return minSat;
    }

    public void setMinSat(double minSat) {
        this.minSat = minSat;
    }

    public double getMinVal() {
        return minVal;
    }

    public void setMinVal(double minVal) {
        this.minVal = minVal;
    }

    public double getMaxHue() {
        return maxHue;
    }

    public void setMaxHue(double maxHue) {
        this.maxHue = maxHue;
    }

    public double getMaxSat() {
        return maxSat;
    }

    public void setMaxSat(double maxSat) {
        this.maxSat = maxSat;
    }

    public double getMaxVal() {
        return maxVal;
    }

    public void setMaxVal(double maxVal) {
        this.maxVal = maxVal;
    }

    public String getStreamStatus() {
        return streamStatus;
    }

    public void setStreamStatus(String streamStatus) {
        this.streamStatus = streamStatus;
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }
}
