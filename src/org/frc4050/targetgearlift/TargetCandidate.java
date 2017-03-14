package org.frc4050.targetgearlift;

import org.opencv.core.Rect;

public class TargetCandidate {

    // "Gettable" properties
    private int rectL;
    private int rectR;
    private int totalScore;
    private int maxScore;
    
    private int[] score; // Holds individual test scores

    private static final double GAP_TO_TARGET_RATIO_W = 0.609756; // 6.25" / 10.25"
    //private static final double HEIGHT_TO_WIDTH_RATIO = 0.4;      // 2.0"  /  5.0"

    private static final int    NUM_OF_SCORES = 5;

    private static final double TEST_WEIGHT_1 = 100.0; // Horizontal alignment
    private static final double TEST_WEIGHT_2 =  60.0; // Widths are very similar
    //private static final double TEST_WEIGHT_3 =  40.0; // Heights are very similar
    private static final double TEST_WEIGHT_4 = 100.0; // Target width vs. gap width
    //private static final double TEST_WEIGHT_5 =  40.0; // Width-to-Height ratio (average both contours)
    
    private double leftX;
    private double leftTopY;
    private double leftBottomY;
    private double leftHeight;
    private double leftWidth;
    
    private double rightX;
    private double rightTopY;
    private double rightBottomY;
    private double rightHeight;
    private double rightWidth;

    public TargetCandidate(Rect candidate1, Rect candidate2, int index1, int index2) {
        //maxScore = (int) (TEST_WEIGHT_1 + TEST_WEIGHT_2 + TEST_WEIGHT_3 + TEST_WEIGHT_4 + TEST_WEIGHT_5);
        maxScore = (int) (TEST_WEIGHT_1 + TEST_WEIGHT_2 + TEST_WEIGHT_4);
        scoreCandidatePair(candidate1, candidate2, index1, index2);
    }

    public int getRectL(){
        return rectL;
    }
    
    public int getRectR(){
        return rectR;
    }
    
    public int getTotalScore(){
        return totalScore;
    }
    
    public int getMaxScore(){
        return maxScore;
    }
    
    private void scoreCandidatePair(Rect rect1, Rect rect2, int index1, int index2) {
        setLeftAndRight(rect1, rect2, index1, index2);
        
        score = new int[NUM_OF_SCORES];
        
        score[0] = TestScore1();
        score[1] = TestScore2();
        score[2] = 0; //TestScore3();
        score[3] = TestScore4();
        score[4] = 0; //TestScore5();

        totalScore = 0;
        
        for (int i  = 0; i < score.length; i++) {
            totalScore += score[i];
        }
    }
    
    private void setLeftAndRight(Rect rect1, Rect rect2, int index1, int index2) {
        if (rect1.x <= rect2.x) {
            rectL = index1;
            rectR = index2;

            leftX = rect1.x;
            leftTopY = rect1.y;
            leftBottomY = rect1.br().y;
            leftHeight = rect1.height;
            leftWidth = rect1.width;

            rightX = rect2.x;
            rightTopY = rect2.y;
            rightBottomY = rect2.br().y;
            rightHeight = rect2.height;
            rightWidth = rect2.width;
        } else {
            rectL = index2;
            rectR = index1;

            leftX = rect2.x;
            leftTopY = rect2.y;
            leftBottomY = rect2.br().y;
            leftHeight = rect2.height;
            leftWidth = rect2.width;

            rightX = rect1.x;
            rightTopY = rect1.y;
            rightBottomY = rect1.br().y;
            rightHeight = rect1.height;
            rightWidth = rect1.width;
        }
    }
    
    private int TestScore1() { // Horizontal alignment
        double maxHeight = (leftHeight >= rightHeight) ? leftHeight : rightHeight;
        int score1Top = (int) (Math.max(0.0, TEST_WEIGHT_1 - ((Math.abs(leftTopY - rightTopY) / maxHeight) * TEST_WEIGHT_1)));
        int score1Bottom = (int) (Math.max(0.0, TEST_WEIGHT_1 - ((Math.abs(leftBottomY - rightBottomY) / maxHeight) * TEST_WEIGHT_1)));
        
        return (score1Top >= score1Bottom) ? score1Top : score1Bottom;
    }

    private int TestScore2() { // Widths are very similar
        double ratio = leftWidth / rightWidth;
    
        if (ratio > 1.0) {
            ratio = 1.0 / ratio;
        }
    
        return (int) (ratio * TEST_WEIGHT_2);
    }
/*
    private int TestScore3() { // Heights are very similar
        double ratio = leftHeight / rightHeight;
    
        if (ratio > 1.0) {
            ratio = 1.0 / ratio;
        }
    
        return  (int) (ratio * TEST_WEIGHT_3);
    }
*/
    private int TestScore4() { // Target width vs. gap width
        double targetWidth = (rightX + rightWidth) - leftX;
        double gapWidth = rightX - (leftX + leftWidth);
        double ratio = (gapWidth / targetWidth) / GAP_TO_TARGET_RATIO_W;
        
        if (ratio > 1.0) {
            ratio = 1.0 / ratio;
        }
        
        return (int) (ratio * TEST_WEIGHT_4);
        
    }
/*
    private int TestScore5() { // Width-to-Height ratio (average both rects)
        double leftRatio = (leftWidth / leftHeight) / HEIGHT_TO_WIDTH_RATIO;
        
        if (leftRatio > 1.0) {
            leftRatio = 1.0 / leftRatio;
        }
        
        double rightRatio = (rightWidth / rightHeight) / HEIGHT_TO_WIDTH_RATIO;
        
        if (rightRatio > 1.0) {
            rightRatio = 1.0 / rightRatio;
        }
        
        double ratio = (leftRatio + rightRatio) / 2.0;

        return (int) (ratio * TEST_WEIGHT_5);
    }
*/
}
