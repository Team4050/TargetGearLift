package org.frc4050.targetgearlift.util;

public class GlobalVariables {

    private static String roborioIPAddress;
    private static String ntName;

    private static int CaptureDevice;

    private static double VIDEO_WIDTH;
    private static double VIDEO_HEIGHT;

    private static int MIN_ACCEPTED_SCORE;

    private static boolean HSVShown;
    private static double minHue;
    private static double minSat;
    private static double minVal;
    private static double maxHue;
    private static double maxSat;
    private static double maxVal;

    private static boolean headless;
    private static boolean streaming;
    private static int streamingPort;

    public int getStreamingPort() {
        return streamingPort;
    }

    public void setStreamingPort(int streamingPort) {
        GlobalVariables.streamingPort = streamingPort;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public void setStreaming(boolean runStreamServer) {
        GlobalVariables.streaming = runStreamServer;
    }

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

    public int getMIN_ACCEPTED_SCORE() {
        return MIN_ACCEPTED_SCORE;
    }

    public void setMIN_ACCEPTED_SCORE(int MIN_ACCEPTED_SCORE) {
        this.MIN_ACCEPTED_SCORE = MIN_ACCEPTED_SCORE;
    }

    public boolean isHSVShown() {
        return HSVShown;
    }

    public void setHSVShown(boolean HSVShown) {
        this.HSVShown = HSVShown;
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

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public int getCaptureDevice() {
        return CaptureDevice;
    }

    public void setCaptureDevice(int captureDevice) {
        CaptureDevice = captureDevice;
    }
}
