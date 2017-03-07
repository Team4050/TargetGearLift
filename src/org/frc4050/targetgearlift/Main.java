package org.frc4050.targetgearlift;

import com.github.lalyos.jfiglet.FigletFont;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import org.frc4050.targetgearlift.processing.GripPipeline;
import org.frc4050.targetgearlift.processing.ImageProcessor;
import org.frc4050.targetgearlift.util.GUI;
import org.frc4050.targetgearlift.util.GlobalVariables;
import org.frc4050.targetgearlift.util.RTSettings;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class Main {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static Main a = new Main();
    private static RTSettings s = new RTSettings();
    private ImageProcessor imp = new ImageProcessor();
    private Mat webcamMat = new Mat();
    private GripPipeline imagePipeline = new GripPipeline();
    private GlobalVariables gv = new GlobalVariables();

    private GUI hudFrame;
    private JLabel hudImageLabel;

    //Global Vars
    private String roborioIPAddress;
    private String ntName;

    private int captureDevice;

    private double VIDEO_WIDTH;
    private double VIDEO_HEIGHT;

    private int MIN_ACCEPTED_SCORE;

    private boolean showHSV;
    private double minHue;
    private double minSat;
    private double minVal;
    private double maxHue;
    private double maxSat;
    private double maxVal;

    private boolean headless;
    private boolean streaming;
    private int streamingPort;

    private double resizeWidth;
    private double resizeHeight;

    public static void main(String[] args) {
        String asciiArt1 = null;
        String asciiArt2 = null;
        try {
            asciiArt1 = FigletFont.convertOneLine("BIOHAZARD VISION");
            asciiArt2 = FigletFont.convertOneLine("Eye See You     o.O");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(asciiArt1);
        System.out.println(asciiArt2);
        if(args.length > 0) {
            if(args[0].equals("-config")) {
                System.out.println("[DEBUG] Initiating Read from Config");
                s.applyFileData(args[1]);
            }
        }
        a.initVar();
        a.runMainLoop();
    }

    private void initVar() {
        roborioIPAddress = gv.getRoborioIPAddress();
        ntName = gv.getNtName();

        captureDevice = gv.getCaptureDevice();

        VIDEO_WIDTH = gv.getVIDEO_WIDTH();
        VIDEO_HEIGHT = gv.getVIDEO_HEIGHT();

        MIN_ACCEPTED_SCORE = gv.getMIN_ACCEPTED_SCORE();

        showHSV = gv.isHSVShown();
        minHue = gv.getMinHue();
        minSat = gv.getMinSat();
        minVal = gv.getMinVal();
        maxHue = gv.getMaxHue();
        maxSat = gv.getMaxSat();
        maxVal = gv.getMaxVal();

        headless = gv.isHeadless();
        streaming = gv.isStreaming();
        streamingPort = gv.getStreamingPort();

        resizeWidth = gv.getResizeWidth();
        resizeHeight = gv.getResizeHeight();

        System.out.println("[INFO] Prepare to be slammed with some of the most hard hitting facts you'll see all day!");
        System.out.println("       RoboRIO IP Address = " + roborioIPAddress);
        System.out.println("       Network Table Name = " + ntName);
        System.out.println("       Headless Mode Enabled = " + Boolean.toString(headless));
        System.out.println("       Is Streaming? = " + Boolean.toString(streaming));
        System.out.println("       If so, on what port? = " + Integer.toString(streamingPort));
        System.out.println("       HSV Overlay Enabled = " + Boolean.toString(showHSV));
        System.out.println("       Capture Device = " + Integer.toString(captureDevice));
        System.out.println("       Resized Width = " + Double.toString(resizeWidth));
        System.out.println("       Resized Height = " + Double.toString(resizeHeight));
        System.out.println("       Minimum Accepted Score = " + Integer.toString(MIN_ACCEPTED_SCORE));
        System.out.println("       Minimum Hue = " + Double.toString(minHue));
        System.out.println("       Maximum Hue = " + Double.toString(maxHue));
        System.out.println("       Minimum Saturation = " + Double.toString(minSat));
        System.out.println("       Maximum Saturation = " + Double.toString(maxSat));
        System.out.println("       Minimum Value = " + Double.toString(minVal));
        System.out.println("       Maximum Value = " + Double.toString(maxVal));
        System.out.println("[INFO] Fact slamming over");

    }

    private void runMainLoop() {
        // Set up NetworkTable
        NetworkTable.setIPAddress(roborioIPAddress);
        NetworkTable.setClientMode();
        NetworkTable table = NetworkTable.getTable(ntName);

        try {

            VideoCapture capture = new VideoCapture(captureDevice);
            capture.set(Videoio.CAP_PROP_FRAME_WIDTH, VIDEO_WIDTH);
            capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, VIDEO_HEIGHT);
            capture.set(Videoio.CAP_PROP_FPS, 30.0);

            int lowestAcceptedScore = 400;

            int matSize = (int) (VIDEO_WIDTH * VIDEO_HEIGHT * 3);
            byte[] matBuffer = new byte[matSize];

            imagePipeline.setSize(resizeWidth, resizeHeight);
            imagePipeline.setHSV(minHue, minSat, minVal, maxHue, maxSat, maxVal);

            if(!headless) {
                hudImageLabel = new JLabel();
                hudFrame = new GUI("org.frc4050.targetgearlift.HUD Frame", 400, 400, true, false);
                hudFrame.add(hudImageLabel);
                hudFrame.setLocation(~-320, ~0);
            }

            int highestScore = MIN_ACCEPTED_SCORE;
            int bestPairIndex = -1;
            int targetIndex1 = -1;
            int targetIndex2 = -1;


            while(true) {
                if (capture.isOpened()) {

                    capture.read(webcamMat);

                    if (!webcamMat.empty()) {

                        imagePipeline.process(webcamMat);

                        // Get contours to score
                        ArrayList<MatOfPoint> contourArray = imagePipeline.findContoursOutput();

                        int contourCount = contourArray.size();

                        Rect[] rect = new Rect[contourCount];

                        int rectCount = createBoundingRects(contourArray, rect, contourCount);

                        // Calculate the number of pair combinations
                        int numOfPairs = ( (rectCount - 1) * rectCount) / 2;

                        TargetCandidate[] rectCandidates = new TargetCandidate[numOfPairs];

                        int scoreIndex = 0;

                        // Score each pair combination
                        for (int i = 0; i < (rectCount - 1); i++) {
                            for (int j = i+1; j < rectCount; j++) {
                                rectCandidates[scoreIndex] = new TargetCandidate(rect[i], rect[j], i, j);
                                scoreIndex++;
                            }
                        }

                        for (int i = 0; i < numOfPairs; i++) {
                            int tempScore = rectCandidates[i].getTotalScore();

                            if (tempScore > highestScore) {
                                highestScore = tempScore;
                                bestPairIndex = i;
                                targetIndex1 = rectCandidates[i].getRectL();
                                targetIndex2 = rectCandidates[i].getRectR();
                            }
                        }

                        HUD hudMat;

                        if (bestPairIndex == -1) { // No target found
                            hudMat = new HUD(VIDEO_WIDTH, VIDEO_HEIGHT);
                            sendTargetingData(table, false, 0, 0, 0);
                        } else { // Found a target
                            if (rectCandidates[bestPairIndex].getTotalScore() < lowestAcceptedScore) {
                                lowestAcceptedScore = rectCandidates[bestPairIndex].getTotalScore();
                                System.out.println("Score = " + lowestAcceptedScore);
                            }

                            double frameCenterX = (VIDEO_WIDTH / 2.0);
                            double leftRectRightX = rect[targetIndex1].x + rect[targetIndex1].width;
                            double rightRectLeftX = rect[targetIndex2].x;

                            // These are the values to show in the org.frc4050.targetgearlift.HUD and to send to NetworkTable
                            double targetCenterX = ( (rightRectLeftX - leftRectRightX) / 2.0) + leftRectRightX;
                            double targetOffset = frameCenterX - targetCenterX;
                            double distance = estimatedDistance(rect[targetIndex1].height, rect[targetIndex2].height);
                            double heightRatioLvR = (double) (rect[targetIndex1].height) / (double) (rect[targetIndex2].height);
                            double heightRatioRvL = (double) (rect[targetIndex2].height) / (double) (rect[targetIndex1].height);

                            hudMat = new HUD(VIDEO_WIDTH, VIDEO_HEIGHT, rect[targetIndex1], rect[targetIndex2],
                                             targetCenterX, Math.abs(targetOffset), heightRatioLvR, heightRatioRvL, distance);

                            sendTargetingData(table, true, (targetOffset / targetCenterX), (1.0 - heightRatioRvL) * 2.0, distance);
                        }

                        Mat augmentedImage;

                        if (showHSV) {
                            augmentedImage = imagePipeline.hsvThresholdOutput();
                        } else {
                            augmentedImage = webcamMat.clone();
                            Core.bitwise_or(augmentedImage, hudMat, augmentedImage);
                        }

                        if (!headless) {
                            Image hudImage = imp.toBufferedImage(augmentedImage);
                            ImageIcon hudImageIcon = new ImageIcon(hudImage, "Augmented Image");
                            hudImageLabel.setIcon(hudImageIcon);

                            hudFrame.pack(); // Resize the windows to fit the image
                        }

                        augmentedImage.get(0, 0, matBuffer);
                    } else {
                        System.out.println(" -- Frame not captured -- Break!");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    private double estimatedDistance(int heightL, int heightR){
        int averageHeight = (heightL + heightR) / 2;
        double distance = 0.0;

        if (averageHeight < 60) {
            distance = (0.0000555453*Math.pow(averageHeight,3)) - (0.00290273*Math.pow(averageHeight,2)) -
                       (0.219377*averageHeight) + 15.6542;
        } else {
            distance = (-0.00000710617*Math.pow(averageHeight,3)) + (0.00212286*Math.pow(averageHeight,2)) -
                       (0.239151*averageHeight) + 12.2385;
        }

        return distance;
    }

    private void sendTargetingData(NetworkTable table, Boolean haveTarget, double pivot, 
                                   double lateral, double distance) {
        table.putBoolean("rpiHaveTarget", haveTarget);
        table.putString("rpiDistance", (new DecimalFormat("#0.00")).format(distance));
        table.putString("rpiPivot", (new DecimalFormat("#0.00")).format(pivot));
        table.putString("rpiLateral", (new DecimalFormat("#0.00")).format(lateral));
    }
    
    private int createBoundingRects(ArrayList<MatOfPoint> contourArray, Rect[] rect, int contourCount) {
        int rectCount = 0;
        
        // Create bounding rectangle for each contour
        for (int i = 0; i < contourCount; i++) {
            MatOfPoint points = new MatOfPoint(contourArray.get(i));
            Rect tempRect = Imgproc.boundingRect(points);
   
            // Only include rectangles that are at least partially
            // in the bottom half of the frame
            if (tempRect.br().y > (VIDEO_HEIGHT / 2.0)) {
                rect[rectCount] = tempRect;
                rectCount += 1;
            }
        }

        return rectCount;
    }
}
