package com.biohazard4050.targetgearlift;

import java.awt.Image;
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

    public static void main(String[] args) {
        a.runMainLoop();
    }

    private void runMainLoop() {
        hudImageLabel = new JLabel();

        hudFrame = new GUI("HUD Frame", 400, 400, true, true); //false);
        hudFrame.add(hudImageLabel);
        hudFrame.setLocation(~-320, ~0);

        VideoCapture capture = new VideoCapture(1);

        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, VIDEO_WIDTH);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, VIDEO_HEIGHT);

        //capture.set(Videoio.CAP_PROP_EXPOSURE, 0.0); // Set Lifecam for painters
        capture.set(Videoio.CAP_PROP_EXPOSURE, -10.0); // Set Lifecam for retro
        
        int lowestAcceptedScore = 400;
        
        if (capture.isOpened()) {
            while (true) {
                /**********************************************
                 * Used for estimating FPS/
                 **********************************************
                double t = (double)Core.getTickCount();  
                /**********************************************/
                capture.read(webcamMat);
                if (!webcamMat.empty()) {
                    // Perform processing on image
                    imagePipeline.process(webcamMat);

                    // Get contours to score
                    ArrayList<MatOfPoint> contourArray = imagePipeline.findContoursOutput();

                    int contourCount = contourArray.size();
                    
                    Rect[] rect = new Rect[contourCount];
                    
                    // Create bounding rectangle for each contour
                    for (int i = 0; i < contourCount; i++) {
                        MatOfPoint points = new MatOfPoint(contourArray.get(i));
                        rect[i] = Imgproc.boundingRect(points);
                    }
                    
                    // Calculate the number of pair combinations
                    int numOfPairs = ( (contourCount - 1) * contourCount) / 2;
                   
                    TargetCandidate[] rectCandidates = new TargetCandidate[numOfPairs];
                    
                    int scoreIndex = 0; 

                    // Score each pair combination
                    for (int i = 0; i < (contourCount - 1); i++) {
                        for (int j = i+1; j < contourCount; j++) {
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
                    } else { // Found a target
                        if (rectCandidates[bestPairIndex].getTotalScore() < lowestAcceptedScore) {
                            lowestAcceptedScore = rectCandidates[bestPairIndex].getTotalScore(); 
                            System.out.println("Score = " + lowestAcceptedScore);
                        }

                        double frameCenterX = (VIDEO_WIDTH / 2.0);
                        double leftRectRightX = rect[targetIndex1].x + rect[targetIndex1].width;
                        double rightRectLeftX = rect[targetIndex2].x;

                        double targetCenterX = ( (rightRectLeftX - leftRectRightX) / 2.0) + leftRectRightX;
                        double targetOffset = Math.abs(frameCenterX - targetCenterX);
                        double distance = estimatedDistance(rect[targetIndex1].height, rect[targetIndex2].height);
                        double heightRatioLvR = (double) (rect[targetIndex1].height) / (double) (rect[targetIndex2].height);
                        double heightRatioRvL = (double) (rect[targetIndex2].height) / (double) (rect[targetIndex1].height);

                        hudMat = new HUD(VIDEO_WIDTH, VIDEO_HEIGHT, rect[targetIndex1], rect[targetIndex2],
                                         targetCenterX, targetOffset, heightRatioLvR, heightRatioRvL, distance);
                    }
                    
                    Mat augmentedImage = webcamMat.clone();

                    // Overlay HUD on video frame
                    Core.bitwise_or(augmentedImage, hudMat, augmentedImage);
                    
                    // Convert HUD to image to display in JLabel
                    Image hudImage = imp.toBufferedImage(augmentedImage);
                    ImageIcon hudImageIcon = new ImageIcon(hudImage, "Augmented Image");
                    hudImageLabel.setIcon(hudImageIcon);

                    hudFrame.pack(); // Resize the windows to fit the image
                } else {
                    System.out.println(" -- Frame not captured -- Break!");
                    break;
                }
                /**********************************************
                 * Used for estimating FPS/
                 **********************************************
                double tickCount = (double)Core.getTickCount();
                double tickFreq = Core.getTickFrequency();
                t = (tickCount - t) / tickFreq;
                System.out.println("FPS "+ 1.0/t);
                /**********************************************/
            }
        }
    }
    
    private double estimatedDistance(int heightL, int heightR){
        int averageHeight = (heightL + heightR) / 2;
        double distance = 0.0;

        if (averageHeight < 60) {
            distance = (0.0000555453*Math.pow(averageHeight,3)) - (0.00290273*Math.pow(averageHeight,2)) - (0.219377*averageHeight) + 15.6542;
        } else {
            distance = (-0.00000710617*Math.pow(averageHeight,3)) + (0.00212286*Math.pow(averageHeight,2)) - (0.239151*averageHeight) + 12.2385;
        }

        return distance;
    }
}
