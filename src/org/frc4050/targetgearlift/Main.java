package org.frc4050.targetgearlift;

import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

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

import com.github.lalyos.jfiglet.FigletFont;

import edu.wpi.first.wpilibj.networktables.NetworkTable;

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

    private int exposure;
    private int brightness;
    private int contrast;

    private double closeNufDist;
    
    private static String systemType;

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
                s.applyFileData(args[1]);
            }

            if(args[2].equals("-os")) {
                systemType = args[3];
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

        exposure = gv.getExposure();
        brightness = gv.getBrightness();
        contrast = gv.getContrast();

        closeNufDist = gv.getCloseNufDist();
        
        System.out.println("[INFO] Prepare to be slammed with some of the most hard hitting facts you'll see all day!");
        System.out.println("       RoboRIO IP Address = " + roborioIPAddress);
        System.out.println("       Network Table Name = " + ntName);
        System.out.println("       Headless Mode Enabled = " + Boolean.toString(headless));
        System.out.println("       HSV Overlay Enabled = " + Boolean.toString(showHSV));
        System.out.println("       Capture Device = " + Integer.toString(captureDevice));
        System.out.println("       Exposure = " + Integer.toString(exposure));
        System.out.println("       Brightness = " + Integer.toString(brightness));
        System.out.println("       Contrast = " + Integer.toString(contrast));
        System.out.println("       Resized Width = " + Double.toString(VIDEO_WIDTH));
        System.out.println("       Resized Height = " + Double.toString(VIDEO_HEIGHT));
        System.out.println("       Minimum Accepted Score = " + Integer.toString(MIN_ACCEPTED_SCORE));
        System.out.println("       Minimum Hue = " + Double.toString(minHue));
        System.out.println("       Maximum Hue = " + Double.toString(maxHue));
        System.out.println("       Minimum Saturation = " + Double.toString(minSat));
        System.out.println("       Maximum Saturation = " + Double.toString(maxSat));
        System.out.println("       Minimum Value = " + Double.toString(minVal));
        System.out.println("       Maximum Value = " + Double.toString(maxVal));
        System.out.println("       Distance to stop vision = " + Double.toString(closeNufDist));
        System.out.println("[INFO] Fact slamming over");
        System.out.println();
    }

    private void runMainLoop() {
        // Set up NetworkTable
        NetworkTable.setIPAddress(roborioIPAddress);
        NetworkTable.setClientMode();
        NetworkTable table = NetworkTable.getTable(ntName);

        VideoCapture capture = new VideoCapture(captureDevice);
        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, VIDEO_WIDTH);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, VIDEO_HEIGHT);
        capture.set(Videoio.CAP_PROP_FPS, 30.0);

        if(systemType.matches("linux|Linux|L|l")) {
            try {
                Runtime.getRuntime().exec("v4l2-ctl -d /dev/video" + captureDevice + " -c exposure_auto=1 -c exposure_absolute=" + exposure + " -c brightness=" + brightness + " -c contrast=" + contrast);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            capture.set(Videoio.CAP_PROP_EXPOSURE, exposure);
        }

        imagePipeline.setHSV(minHue, minSat, minVal, maxHue, maxSat, maxVal);

        int highestScore = MIN_ACCEPTED_SCORE;
        int bestPairIndex = -1;
        int targetIndex1 = -1;
        int targetIndex2 = -1;

        ArrayList<MatOfPoint> contourArray;

        int contourCount;

        Rect[] rect;

        int rectCount;
        int numOfPairs;

        TargetCandidate[] rectCandidates;

        int scoreIndex = 0;

        double frameCenterX = (VIDEO_WIDTH / 2.0);

        double leftRectRightX = 0.0;
        double rightRectLeftX = 0.0;
        double targetCenterX = 0.0;
        double targetOffset = 0.0;
        double heightRatioLvR = 0.0;
        double heightRatioRvL = 0.0;

        Boolean haveTarget = false;
        String pivot = "";
        String lateral = "";
        String distance = "";

        Boolean haveTargetPrev = true;
        String pivotPrev = "";
        String lateralPrev = "";
        String distancePrev = "";
        
        int leftRectW = 0;
        int leftRectH = 0;
        int rightRectH = 0;

        try {
            if(!headless) {
                hudImageLabel = new JLabel();
                hudFrame = new GUI("HUD Frame", 400, 400, true, false);
                hudFrame.add(hudImageLabel);
                hudFrame.setLocation(~-320, ~0);
            }
                     
            boolean readingFromCamera = false;

            if (capture.isOpened()) {
                readingFromCamera = true;
                System.out.println("Camera is opened.");
            } else {
                System.out.println("ERROR: Camera failed to open.");
            }

            boolean stillGoing = true;
            
            int frameNumber = 1;
            long frameStart = 0;
            long currTimeStamp = 0;
            long prevTimeStamp = 0;
            long begininng = System.nanoTime();
        
            writeInitialNTValues(table);
            
            while(readingFromCamera) {
                //if (capture.isOpened()) { /* Moved isOpened() logic outside of loop. */

                    capture.read(webcamMat);

                    if (!webcamMat.empty()) {
                        frameStart = System.nanoTime();
                        
                        imagePipeline.process(webcamMat);
                        /*
                        currTimeStamp = System.nanoTime();
                        System.out.println("01: " + ((currTimeStamp - frameStart) / 1e6));
                        prevTimeStamp = currTimeStamp;
                        */
                        // Get contours to score
                        contourArray = imagePipeline.findContoursOutput();
                        contourCount = contourArray.size();
                        rect = new Rect[contourCount];
                        rectCount = createBoundingRects(contourArray, rect, contourCount);
                        /*
                        currTimeStamp = System.nanoTime();
                        System.out.println("02: " + ((currTimeStamp - prevTimeStamp) / 1e6));
                        prevTimeStamp = currTimeStamp;
                        */
                        // Calculate the number of pair combinations
                        numOfPairs = ( (rectCount - 1) * rectCount) / 2;
                        rectCandidates = new TargetCandidate[numOfPairs];
                        /*
                        currTimeStamp = System.nanoTime();
                        System.out.println("03: " + ((currTimeStamp - prevTimeStamp) / 1e6));
                        prevTimeStamp = currTimeStamp;
                        */
                        // Reset for current frame.
                        scoreIndex = 0; 

                        // Score each pair combination
                        for (int i = 0; i < (rectCount - 1); i++) {
                            for (int j = i+1; j < rectCount; j++) {
                                rectCandidates[scoreIndex] = new TargetCandidate(rect[i], rect[j], i, j);
                                scoreIndex++;
                            }
                        }
                        /*
                        currTimeStamp = System.nanoTime();
                        System.out.println("04: " + ((currTimeStamp - prevTimeStamp) / 1e6));
                        prevTimeStamp = currTimeStamp;
                        */
                        // Reset for current frame.
                        highestScore = MIN_ACCEPTED_SCORE; 
                        bestPairIndex = -1;
                        targetIndex1 = -1;
                        targetIndex2 = -1;
                
                        for (int i = 0; i < numOfPairs; i++) {
                            int tempScore = rectCandidates[i].getTotalScore();

                            if (tempScore > highestScore) {
                                highestScore = tempScore;
                                bestPairIndex = i;
                                targetIndex1 = rectCandidates[i].getRectL();
                                targetIndex2 = rectCandidates[i].getRectR();
                            }
                        }
                        /*
                        currTimeStamp = System.nanoTime();
                        System.out.println("05: " + ((currTimeStamp - prevTimeStamp) / 1e6));
                        prevTimeStamp = currTimeStamp;
                        */
                        if (bestPairIndex == -1) { // No target found
                            if (haveTargetPrev) {
                                sendTargetingData(table, false, "0.00", "0.00", "0.00", false);
                                //System.out.println("[DEBUG] Wrote to NetworkTable");

                                haveTargetPrev = false;
                                pivotPrev = "0.00";
                                lateralPrev = "0.00";
                                distancePrev = "0.00";
                            }
                        } else { // Found a target
                            leftRectW = rect[targetIndex1].width;
                            leftRectH = rect[targetIndex1].height;
                            rightRectH = rect[targetIndex2].height;
                                    
                            leftRectRightX = rect[targetIndex1].x + leftRectW;
                            rightRectLeftX = rect[targetIndex2].x;

                            // These are the values to show in the HUD and to send to NetworkTable
                            targetCenterX = ( (rightRectLeftX - leftRectRightX) / 2.0) + leftRectRightX;
                            targetOffset = frameCenterX - targetCenterX;
                            heightRatioLvR = (double) leftRectH / (double) rightRectH;
                            heightRatioRvL = (double) rightRectH / (double) leftRectH;

                            haveTarget = true;
                            distance = estimatedDistance(leftRectH, rightRectH);
                            pivot = String.format("%1$,.2f", (targetOffset / targetCenterX));
                            lateral = String.format("%1$,.2f", (1.0 - heightRatioRvL) * 2.0);

                            if (stillGoing && Double.parseDouble(distance) < closeNufDist) {
                                table.putBoolean("rpiCloseEnough", true);
                                stillGoing = false;
                            }
                            
                            if ( (haveTarget != haveTargetPrev) || (distance != distancePrev) ||
                                 (pivot != pivotPrev) || (lateral != lateralPrev) ) {
                                sendTargetingData(table, haveTarget, pivot, lateral, distance, !stillGoing);
                                //System.out.println("[DEBUG] Wrote to NetworkTable");
                            }
                               
                            haveTargetPrev = haveTarget;
                            distancePrev = distance;
                            pivotPrev = pivot;
                            lateralPrev = lateral;
                        }
                        /*
                        currTimeStamp = System.nanoTime();
                        System.out.println("06: " + ((currTimeStamp - prevTimeStamp) / 1e6));
                        prevTimeStamp = currTimeStamp;
                        */
                        if (!headless) {
                            HUD hudMat;
                            Mat augmentedImage;
                            
                            if (bestPairIndex == -1) { // No target found
                                hudMat = new HUD(VIDEO_WIDTH, VIDEO_HEIGHT);
                            } else { // Found a target
                                hudMat = new HUD(VIDEO_WIDTH, VIDEO_HEIGHT, rect[targetIndex1], rect[targetIndex2],
                                                 targetCenterX, Math.abs(targetOffset), heightRatioLvR, heightRatioRvL, Double.parseDouble(distance));
                            }

                            if (showHSV) {
                                augmentedImage = imagePipeline.hsvThresholdOutput();
                            } else {
                                augmentedImage = webcamMat.clone();
                                Core.bitwise_or(augmentedImage, hudMat, augmentedImage);
                            }

                            Image hudImage = imp.toBufferedImage(augmentedImage);
                            ImageIcon hudImageIcon = new ImageIcon(hudImage, "Augmented Image");
                            hudImageLabel.setIcon(hudImageIcon);

                            hudFrame.pack(); // Resize the windows to fit the image
                        }
                        /*
                        currTimeStamp = System.nanoTime();
                        System.out.println("07: " + ((currTimeStamp - prevTimeStamp) / 1e6));
                        prevTimeStamp = currTimeStamp;

                        System.out.println("Frame: " + frameNumber++ + " - Time: " + ((System.nanoTime() - frameStart) / 1e6) +
                                           " - Total: " + ((System.nanoTime() - begininng) / 1e6));
                        */
                    } else {
                        System.out.println(" -- Frame not captured -- Break!");
                        break;
                    }
                //} /* Moved isOpened() logic outside of loop. */
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    private String estimatedDistance(int heightL, int heightR){
        final int DIST_4FT_H = 48; 

        int maxHeight = (heightL >= heightR) ? heightL : heightR;
        double distance = 0.0;

        if (maxHeight < DIST_4FT_H) {
            distance = (-0.001675*Math.pow(maxHeight,3)) + (0.232*Math.pow(maxHeight,2)) - (11.767*maxHeight) + 262.0;
        } else {
            distance = (-0.0000628*Math.pow(maxHeight,3)) + (0.02*Math.pow(maxHeight,2)) - (2.36*maxHeight) + 122.0;
        }

        return String.format("%1$,.2f", distance / 12.0);
    }

    private void writeInitialNTValues(NetworkTable table) {
        table.putBoolean("rpiCloseEnough", false);
        table.putBoolean("rpiHaveTarget", false);
        table.putString("rpiDistance", "0.00");
        table.putString("rpiPivot", "0.00");
        table.putString("rpiLateral", "0.00");
    }
    
    private void sendTargetingData(NetworkTable table, Boolean haveTarget, String pivot, 
                                   String lateral, String distance, Boolean closeNuf) {
        table.putBoolean("rpiHaveTarget", haveTarget);
        table.putString("rpiDistance", distance);
        table.putString("rpiPivot", pivot);
        table.putString("rpiLateral", lateral);
        
        System.out.print("   T: " + ((haveTarget) ? "true " : "false") +
                         " | D: " + ((Double.parseDouble(distance) < 10.0) ? " " : "") + distance +
                         " | P: " + ((Double.parseDouble(pivot) >= 0.0) ? " " : "") + pivot +
                         " | L: " + ((Double.parseDouble(lateral) >= 0.0) ? " " : "") + lateral +
                         " | C: " + ((closeNuf) ? "true " : "false") + "\r");
    }
    
    private int createBoundingRects(ArrayList<MatOfPoint> contourArray, Rect[] rect, int contourCount) {
        int rectCount = 0;
        
        // Create bounding rectangle for each contour
        for (int i = 0; i < contourCount; i++) {
            MatOfPoint points = new MatOfPoint(contourArray.get(i));
            Rect tempRect = Imgproc.boundingRect(points);
            /*
            // Only include rectangles that are at least partially
            // in the bottom half of the frame
            if (tempRect.br().y > (VIDEO_HEIGHT / 2.0)) {
                rect[rectCount] = tempRect;
                rectCount += 1;
            }
            */
            rect[rectCount] = tempRect;
            rectCount += 1;
        }

        return rectCount;
    }
}
