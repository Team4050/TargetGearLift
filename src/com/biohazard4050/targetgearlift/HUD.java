package com.biohazard4050.targetgearlift;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class HUD extends Mat {

    public HUD(double videoWidth, double videoHeight) {
        super((int) videoHeight, (int) videoWidth, CvType.CV_8UC3);
        this.setTo(new Scalar(0));
        
        double frameCenterX = (videoWidth / 2.0);
        Imgproc.line(this, new Point(frameCenterX, 0), new Point(frameCenterX, videoHeight-1), new Scalar(0, 0, 255), 1);
    }

    public HUD(double videoWidth, double videoHeight, Rect rectL, Rect rectR, double targetCenterX, double targetOffset,
               double heightRatioLvR, double heightRatioRvL, double distance) {
        super((int) videoHeight, (int) videoWidth, CvType.CV_8UC3);
        this.setTo(new Scalar(0));

        double frameCenterX = (videoWidth / 2.0);
        
        // Draw target rectangles
        Imgproc.rectangle(this, rectL.tl(), rectL.br(), new Scalar(0, 255, 0), 2);
        Imgproc.rectangle(this, rectR.tl(), rectR.br(), new Scalar(0, 255, 0), 2);

        // Draw line for center of target
        Imgproc.line(this, new Point(targetCenterX, 0), new Point(targetCenterX, videoHeight-1), new Scalar(255, 255, 255), 1);

        // See if need to move left
        if (heightRatioLvR < 0.95) {
            Imgproc.putText(this, "<- L", new Point(15, 15), Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(255, 255, 255), 2);
        }
        
        // See if need to move right
        if (heightRatioRvL < 0.95) {
            Imgproc.putText(this, "R ->", new Point(videoWidth - 50, 15), Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(255, 255, 255), 2);
        }
        
        // Display pixel heights of rectangles: (1) left (2) average (3) right
        int averageHeight = (rectL.height + rectR.height) / 2;
        Imgproc.putText(this, rectL.height + " ", new Point(15, videoHeight - 30), Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(255, 255, 255));
        Imgproc.putText(this, rectR.height + " ", new Point(videoWidth - 50, videoHeight - 30), Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(255, 255, 255));
        Imgproc.putText(this, averageHeight + " ", new Point(frameCenterX + 10, videoHeight - 30), Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(255, 255, 255));

        // Display distance
        Imgproc.putText(this, String.format ("%.1f", distance), new Point(15, (videoHeight/2)), Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(255, 255, 255), 2);

        // Set color of center line based on distance of target center from center line
        Scalar centerLineColor = NearnessColor(targetOffset);
        
        // Draw color-coordinated vertical line in middle of frame
        Imgproc.line(this, new Point(frameCenterX, 0), new Point(frameCenterX, videoHeight-1), centerLineColor, 1);
    }

    private Scalar NearnessColor(double targetOffset) {
        Scalar tempScalar;
        
        if (targetOffset > 20) {
            tempScalar = new Scalar(0, 0, 255);
        } else if (targetOffset > 5) {
            tempScalar = new Scalar(0, 216, 255);
        } else {
            tempScalar = new Scalar(0, 255, 0);
        }
        
        return tempScalar;
    }
}
