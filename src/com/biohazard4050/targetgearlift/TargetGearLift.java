package com.biohazard4050.targetgearlift;

import java.awt.Image;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import edu.wpi.first.wpilibj.networktables.NetworkTable;

//TODO: Ignore targets more than X feet away.

public class TargetGearLift {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static final double VIDEO_WIDTH  = 640.0;
    private static final double VIDEO_HEIGHT = 360.0;
    
    private static final int MIN_ACCEPTED_SCORE = 250;

    private static TargetGearLift a = new TargetGearLift();
    private ImageProcessor imp = new ImageProcessor();
    private Mat webcamMat = new Mat();
    private GripPipeline imagePipeline = new GripPipeline();

    private GUI hudFrame;

    private JLabel hudImageLabel;

    private boolean showHSV = false;
    private double exposure = 0.0;
    private double minHue = 0.0;
    private double minSat = 0.0;
    private double minVal = 0.0;
    private double maxHue = 0.0;
    private double maxSat = 0.0;
    private double maxVal = 0.0;

    public static void main(String[] args) {
        a.runMainLoop();
    }

    private void runMainLoop() {
        // Set up NetworkTable
        NetworkTable.setIPAddress("localhost");
        NetworkTable.setClientMode();
        NetworkTable table = NetworkTable.getTable("/RPiVision");

        /************************************************************
         * Prime the read values. These will come from RoboRIO later.
         ************************************************************/
        table.putBoolean("rioShowHSV", false);
        table.putNumber("rioExposure", -10.0);
        table.putNumber("rioMinHue",  89.0287);
        table.putNumber("rioMinSat", 178.8669);
        table.putNumber("rioMinVal",  59.6223);
        table.putNumber("rioMaxHue", 129.3174);
        table.putNumber("rioMaxSat", 255.0);
        table.putNumber("rioMaxVal", 255.0);
        /************************************************************/

        hudImageLabel = new JLabel();

        hudFrame = new GUI("HUD Frame", 400, 400, true, false);
        hudFrame.add(hudImageLabel);
        hudFrame.setLocation(~-320, ~0);

        VideoCapture capture = new VideoCapture(1);

        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, VIDEO_WIDTH);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, VIDEO_HEIGHT);
        capture.set(Videoio.CAP_PROP_FPS, 30.0);

        /**********************************************
         * Exposure settings for Lifecam:
         *  -5.0 : Normal lighting
         * -10.0 : Retro tape lighting
         **********************************************/

        exposure = table.getNumber("rioExposure", -10.0);
        capture.set(Videoio.CAP_PROP_EXPOSURE, exposure);
        
        int lowestAcceptedScore = 400;
        
        if (capture.isOpened()) {
            /*==========================================================*
             * SERVER CODE
             *==========================================================*/
            ServerSocket matSocketServer;
            
            try {
                matSocketServer = new ServerSocket(5805);  
                Socket matSocket = matSocketServer.accept(); //establishes connection   
                DataOutputStream matOutputStream = new DataOutputStream(matSocket.getOutputStream());  

            int matSize = (int) (VIDEO_WIDTH * VIDEO_HEIGHT * 3);
            int matWidth = (int) VIDEO_WIDTH;
            int matHeight = (int) VIDEO_HEIGHT;
        
            byte[] matBuffer = new byte[matSize];
            /*==========================================================*/

            //while (true) {
            while (minHue >= 0.0) {
                /**********************************************
                 * Used for estimating FPS/
                 **********************************************
                double t = (double)Core.getTickCount();  
                /**********************************************/

                // Read NetworkTable values from RoboRIO
                readFromNetworkTable(table);
                
                capture.read(webcamMat);

                if (!webcamMat.empty()) {
                    // Set HSV values for GripPipeline.java
                    imagePipeline.setHSV(minHue, minSat, minVal, maxHue, maxSat, maxVal);
                    
                    // Perform processing on image
                    imagePipeline.process(webcamMat);

                    // Get contours to score
                    ArrayList<MatOfPoint> contourArray = imagePipeline.findContoursOutput();

                    int contourCount = contourArray.size();
                    
                    Rect[] rect = new Rect[contourCount];
                    
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
                    
                    int highestScore = MIN_ACCEPTED_SCORE; 
                    int bestPairIndex = -1;
                    int targetIndex1 = -1;
                    int targetIndex2 = -1;
                    
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
                        WriteToNetworkTable(table, false, 0, 0, 0);
                    } else { // Found a target
                        if (rectCandidates[bestPairIndex].getTotalScore() < lowestAcceptedScore) {
                            lowestAcceptedScore = rectCandidates[bestPairIndex].getTotalScore(); 
                            System.out.println("Score = " + lowestAcceptedScore);
                        }

                        double frameCenterX = (VIDEO_WIDTH / 2.0);
                        double leftRectRightX = rect[targetIndex1].x + rect[targetIndex1].width;
                        double rightRectLeftX = rect[targetIndex2].x;

                        // These are the values to show in the HUD and to send to NetworkTable
                        double targetCenterX = ( (rightRectLeftX - leftRectRightX) / 2.0) + leftRectRightX;
                        double targetOffset = frameCenterX - targetCenterX;
                        double distance = estimatedDistance(rect[targetIndex1].height, rect[targetIndex2].height);
                        double heightRatioLvR = (double) (rect[targetIndex1].height) / (double) (rect[targetIndex2].height);
                        double heightRatioRvL = (double) (rect[targetIndex2].height) / (double) (rect[targetIndex1].height);

                        hudMat = new HUD(VIDEO_WIDTH, VIDEO_HEIGHT, rect[targetIndex1], rect[targetIndex2],
                                         targetCenterX, Math.abs(targetOffset), heightRatioLvR, heightRatioRvL, distance);

                        WriteToNetworkTable(table, true, (targetOffset / targetCenterX), (1.0 - heightRatioRvL) * 2.0, distance);
                    }
                    
                    Mat augmentedImage;

                    if (showHSV) {
                        augmentedImage = imagePipeline.hsvThresholdOutput();
                    } else {
                        augmentedImage = webcamMat.clone();
                        Core.bitwise_or(augmentedImage, hudMat, augmentedImage);
                    }
                    
                    // Convert HUD to image to display in JLabel
                    Image hudImage = imp.toBufferedImage(augmentedImage);
                    ImageIcon hudImageIcon = new ImageIcon(hudImage, "Augmented Image");
                    hudImageLabel.setIcon(hudImageIcon);

                    hudFrame.pack(); // Resize the windows to fit the image
                    
                    /*==========================================================*
                     * SERVER CODE
                     *==========================================================*/

                    augmentedImage.get(0, 0, matBuffer);

                    matOutputStream.writeBoolean(showHSV);
                    matOutputStream.writeInt(matSize);
                    matOutputStream.writeInt(matWidth);
                    matOutputStream.writeInt(matHeight);
                    matOutputStream.write(matBuffer);
                    matOutputStream.flush();  
                    /*==========================================================*/
                } else {
                    System.out.println(" -- Frame not captured -- Break!");
                    break;
                }
            }
            /*==========================================================*
             * SERVER CODE
             *==========================================================*/
            } catch (Exception e) {
                System.out.println(e);
            }  
            /*==========================================================*/
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
    
    private void readFromNetworkTable(NetworkTable table) {
        showHSV  = table.getBoolean("rioShowHSV", false);
        minHue   = table.getNumber("rioMinHue", 100.2158);
        minSat   = table.getNumber("rioMinSat",  64.4172);
        minVal   = table.getNumber("rioMinVal",  67.0);
        maxHue   = table.getNumber("rioMaxHue", 158.9965);
        maxSat   = table.getNumber("rioMaxSat", 255.0);
        maxVal   = table.getNumber("rioMaxVal", 255.0);
    }

    private void WriteToNetworkTable(NetworkTable table, Boolean haveTarget, double pivot, 
                                     double lateral, double distance) {
        table.putBoolean("rpiHaveTarget", haveTarget);
        table.putString("rpiDistance", (new DecimalFormat("#0.00")).format(distance));
        table.putString("rpiPivot", (new DecimalFormat("#0.00")).format(pivot));
        table.putString("rpiLateral", (new DecimalFormat("#0.00")).format(lateral));
    }
}
