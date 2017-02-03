import java.awt.Image;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class TargetGearLift {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    //TODO:  Take relative area of rectangle into account. Larger score higher.
    
    //private static final double VIDEO_WIDTH = 1280;
    //private static final double VIDEO_HEIGHT = 720;
    
    private static final double VIDEO_WIDTH  = 640;
    private static final double VIDEO_HEIGHT = 360;
    
    private static final int    NUM_OF_SCORES         = 5;
    private static final int    MIN_ACCEPTED_SCORE    = 250;
    private static final double GAP_TO_TARGET_RATIO_W = 0.609756; // 6.25" / 10.25"
    private static final double HEIGHT_TO_WIDTH_RATIO = 0.4;      // 2.0"  /  5.0"

    private static final double TEST_WEIGHT_1 = 100.0; // Horizontal alignment
    private static final double TEST_WEIGHT_2 =  60.0; // Widths are very similar
    private static final double TEST_WEIGHT_3 =  40.0; // Heights are very similar
    private static final double TEST_WEIGHT_4 = 100.0; // Target width vs. gap width
    private static final double TEST_WEIGHT_5 =  40.0; // Width-to-Height ratio (average both contours)
        
    private static TargetGearLift a = new TargetGearLift();
    private ImageProcessor imp = new ImageProcessor();
    private Mat webcamMat = new Mat();
    private GripPipeline imagePipeline = new GripPipeline();

    private GUI augmentedFrame;

    private JLabel augmentedImageLabel;

    public static void main(String[] args) {
        a.runMainLoop();
    }

    private void runMainLoop() {
        augmentedImageLabel = new JLabel();

        augmentedFrame = new GUI("Augmented Frame", 400, 400, true, false);

        augmentedFrame.add(augmentedImageLabel);

        augmentedFrame.setLocation(~-320, ~0);

        Image augmentedImage;

        VideoCapture capture = new VideoCapture(1);

        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, VIDEO_WIDTH);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, VIDEO_HEIGHT);

        capture.set(Videoio.CAP_PROP_EXPOSURE, 0.0); // Set Lifecam for painters
        //capture.set(Videoio.CAP_PROP_EXPOSURE, -10.0); // Set Lifecam for retro
        
        if (capture.isOpened()) {
            while (true) {
                capture.read(webcamMat);
                if (!webcamMat.empty()) {

                    // Perform processing on image
                    imagePipeline.process(webcamMat);

                    // Get contours to score
                    ArrayList<MatOfPoint> contourArray = imagePipeline.findContoursOutput();

                    Mat overlayedImage = webcamMat.clone();

                    int contourCount = contourArray.size();
                    
                    Rect[] rect = new Rect[contourCount];
                    
                    // Create bounding rectangle for each contour
                    
                    for (int i = 0; i < contourCount; i++) {
                        //Convert contours(i) from MatOfPoint to MatOfPoint2f
                        MatOfPoint2f contour2f = new MatOfPoint2f(contourArray.get(i).toArray() );

                        MatOfPoint2f approxCurve = new MatOfPoint2f();
                        
                        //Processing on mMOP2f1 which is in type MatOfPoint2f
                        double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
                        Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

                        //Convert back to MatOfPoint
                        MatOfPoint points = new MatOfPoint(approxCurve.toArray());

                        // Get bounding rectangle of contour
                        rect[i] = Imgproc.boundingRect(points);
                    }
                    
                    // Calculate the number of pair combinations
                    
                    int numOfPairs = ( (contourCount - 1) * contourCount) / 2;
                   
                    int[][] contourScores = new int[numOfPairs][NUM_OF_SCORES + 2];
                    
                    int scoreIndex = 0; 

                    /******************************
                     * 0 : Index of contour 1
                     * 1 : Index of contour 2
                     * 2 : Score #1
                     *   :
                     * x : Score #(x-1)
                     ******************************/
                    
                    // Score each pair combination
                    for (int i = 0; i < (contourCount - 1); i++) {
                        for (int j = i+1; j < contourCount; j++) {
                            double ratio = 0.0;
                            int testIndex = 2;
                            
                            int leftIndex;
                            int rightIndex;
                           
                            if (rect[i].x <= rect[j].x) {
                                leftIndex = i;
                                rightIndex = j;
                            } else {
                                leftIndex = j;
                                rightIndex = i;
                            }
                            
                            contourScores[scoreIndex][0] = leftIndex;
                            contourScores[scoreIndex][1] = rightIndex;
                            
                            double leftX = rect[leftIndex].x;
                            double leftY = rect[leftIndex].y;
                            double leftHeight = rect[leftIndex].height;
                            double leftWidth = rect[leftIndex].width;
                            
                            double rightX = rect[rightIndex].x;
                            double rightY = rect[rightIndex].y;
                            double rightHeight = rect[rightIndex].height;
                            double rightWidth = rect[rightIndex].width;
                            
                            /****************************************
                             * Test 1 : Horizontal alignment
                             ****************************************/
                            
                            contourScores[scoreIndex][testIndex++] = (int) (Math.max(0.0, TEST_WEIGHT_1 - ((Math.abs(leftY - rightY) / leftHeight) * TEST_WEIGHT_1)));

                            /****************************************
                             * Test 2 : Widths are very similar
                             ****************************************/
                            
                            ratio = leftWidth / rightWidth;
                            
                            if (ratio > 1.0) {
                                ratio = 1.0 / ratio;
                            }
                            
                            contourScores[scoreIndex][testIndex++] = (int) (ratio * TEST_WEIGHT_2);

                            /****************************************
                             * Test 3 : Heights are very similar
                             ****************************************/
                            
                            ratio = leftHeight / rightHeight;
                            
                            if (ratio > 1.0) {
                                ratio = 1.0 / ratio;
                            }
                            
                            contourScores[scoreIndex][testIndex++] = (int) (ratio * TEST_WEIGHT_3);
                            
                            /****************************************
                             * Test 4 : Target width vs. gap width
                             ****************************************/
                            
                            double targetWidth = (rightX + rightWidth) - leftX;
                            double gapWidth = rightX - (leftX + leftWidth);

                            ratio = (gapWidth / targetWidth) / GAP_TO_TARGET_RATIO_W;
                            
                            if (ratio > 1.0) {
                                ratio = 1.0 / ratio;
                            }
                            
                            contourScores[scoreIndex][testIndex++] = (int) (ratio * TEST_WEIGHT_4);

                            /****************************************
                             * Test 5 : Width-to-Height ratio
                             * (average both contours)
                             ****************************************/
                            
                            double leftRatio = (leftWidth / leftHeight) / HEIGHT_TO_WIDTH_RATIO;
                            
                            if (leftRatio > 1.0) {
                                leftRatio = 1.0 / leftRatio;
                            }
                            
                            double rightRatio = (rightWidth / rightHeight) / HEIGHT_TO_WIDTH_RATIO;
                            
                            if (rightRatio > 1.0) {
                                rightRatio = 1.0 / rightRatio;
                            }
                            
                            ratio = (leftRatio + rightRatio) / 2.0;
                    
                            contourScores[scoreIndex][testIndex++] = (int) (ratio * TEST_WEIGHT_5);

                            scoreIndex++;
                        }
                    }
                    
                    int highestScore = MIN_ACCEPTED_SCORE; 
                    int bestPairIndex = -1;
                    int targetIndex1 = -1;
                    int targetIndex2 = -1;
                    
                    for (int i = 0; i < numOfPairs; i++) {
                        int tempScore = contourScores[i][2] + contourScores[i][3] + contourScores[i][4] + 
                                        contourScores[i][5] + contourScores[i][6];
                        
                        if (tempScore > highestScore) {
                            highestScore = tempScore; 
                            bestPairIndex = i;
                            targetIndex1 = contourScores[i][0];
                            targetIndex2 = contourScores[i][1];
                        }
                    }
                    
                    double frameCenterX = (VIDEO_WIDTH / 2.0);
                    
                    Scalar centerLineColor;
                    
                    if (bestPairIndex == -1) {
                        // No target found
                        centerLineColor = new Scalar(0, 0, 255);
                    } else {
                        // Found a target
                        double leftRectRightX = rect[targetIndex1].x + rect[targetIndex1].width;
                        double rightRectLeftX = rect[targetIndex2].x;
                        double targetCenterX = ( (rightRectLeftX - leftRectRightX) / 2.0) + leftRectRightX;
                        double targetOffset = Math.abs(frameCenterX - targetCenterX);

                        // Set color of center line based on distance of
                        // target center from center line
                        if (targetOffset > 20) {
                            centerLineColor = new Scalar(0, 0, 255);
                        } else if (targetOffset > 5) {
                            centerLineColor = new Scalar(0, 216, 255);
                        } else {
                            centerLineColor = new Scalar(0, 255, 0);
                        }

                        // Draw line for center of target
                        Imgproc.line(overlayedImage, new Point(targetCenterX, 0), new Point(targetCenterX, VIDEO_HEIGHT-1), new Scalar(255, 255, 255), 1);

                        // Calculate height comparison ratios
                        double heightRatioLvR = (double) (rect[targetIndex1].height) / (double) (rect[targetIndex2].height);
                        double heightRatioRvL = (double) (rect[targetIndex2].height) / (double) (rect[targetIndex1].height);
                        
                        // See if need to move left
                        if (heightRatioLvR < 0.95) {
                            Imgproc.putText(overlayedImage, "<- L", new Point(15, 15), Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(255, 255, 255), 2);
                        }
                        
                        // See if need to move right
                        if (heightRatioRvL < 0.95) {
                            Imgproc.putText(overlayedImage, "R ->", new Point(VIDEO_WIDTH - 50, 15), Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(255, 255, 255), 2);
                        }
                        
                        // Display pixel heights of rectangles: (1) left (2) average (3) right
                        int averageHeight = (rect[targetIndex1].height + rect[targetIndex2].height) / 2;
                        Imgproc.putText(overlayedImage, rect[targetIndex1].height + " ", new Point(15, VIDEO_HEIGHT - 30), Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(255, 255, 255));
                        Imgproc.putText(overlayedImage, rect[targetIndex2].height + " ", new Point(VIDEO_WIDTH - 50, VIDEO_HEIGHT - 30), Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(255, 255, 255));
                        Imgproc.putText(overlayedImage, averageHeight + " ", new Point(frameCenterX + 10, VIDEO_HEIGHT - 30), Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(255, 255, 255));

                        /*    
                        // Print scoring data to console                    
                        System.out.println(contourScores[bestPairIndex][0] + " | " + contourScores[bestPairIndex][1] + " | " +
                                           (contourScores[bestPairIndex][2] + contourScores[bestPairIndex][3] +
                                            contourScores[bestPairIndex][4] + contourScores[bestPairIndex][5] +
                                            contourScores[bestPairIndex][6]) + " | " +
                                           contourScores[bestPairIndex][2] + " | " + contourScores[bestPairIndex][3] + " | " +
                                           contourScores[bestPairIndex][4] + " | " + contourScores[bestPairIndex][5] + " | " +
                                           contourScores[bestPairIndex][6]);
                        */
                    }
                    
                    // Draw color-coordinated vertical line in middle of frame
                    Imgproc.line(overlayedImage, new Point(frameCenterX, 0), new Point(frameCenterX, VIDEO_HEIGHT-1), centerLineColor, 1);

                    /******************************************************************
                     * Draw pair of contours that are target candidate.
                     * 1. Draw non-target rectangles in red
                     * 2. Draw target rectangles in green
                     ******************************************************************/
                    
                    for (int i = 0; i < contourArray.size(); i++) {
                        if ( (i == targetIndex1) || (i == targetIndex2) ) {
                            Imgproc.rectangle(overlayedImage, rect[i].tl(), rect[i].br(), new Scalar(0, 255, 0), 2);
                        } else {
                            Imgproc.rectangle(overlayedImage, rect[i].tl(), rect[i].br(), new Scalar(0, 0, 255), 1);
                        }
                    }

                    // Convert enhanced raw footage matrix to image to display in JLabel
                    augmentedImage = imp.toBufferedImage(overlayedImage);
                    ImageIcon augmentedImageIcon = new ImageIcon(augmentedImage, "Augmented Image");
                    augmentedImageLabel.setIcon(augmentedImageIcon);

                    // Resize the windows to fit the image
                    augmentedFrame.pack();
                } else {
                    System.out.println(" -- Frame not captured -- Break!");
                    break;
                }
            }
        }
    }
}
