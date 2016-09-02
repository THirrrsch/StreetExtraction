package blob;

import Util.Utils;
import blob.Blob;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Line {

    private List<Point> _points;
    private double _angle;

    public double getAngle() {
        return _angle;
    }

    public List<Point> getPoints() {
        return _points;
    }

    //assume that a blob is already line-ish and create a line out of its contour
    public Line(Blob blob) {
        int[] xPoints = blob.getLineX();
        int[] yPoints = blob.getLineY();
        int end = blob.getOuterContour().npoints / 2;
        _points = new ArrayList<Point>();

        for (int i = 0; i < end; i++) {
            _points.add(new Point(xPoints[i], yPoints[i]));
        }

        _angle = Utils.getAngle(_points.get(0).x, _points.get(end - 1).x, _points.get(0).y, _points.get(end - 1).y);
    }

    //connect the centroids of blobs to a line
    public Line(List<Blob> lineBlobs) {
        _points = new ArrayList<Point>();

        for (int i = 0; i < lineBlobs.size() - 2; i++) {
            Point centroidA = lineBlobs.get(i).getCentroid();
            Point centroidB = lineBlobs.get(i + 1).getCentroid();

            _points.addAll(Utils.getBresenhamPoints(centroidA.x, centroidA.y, centroidB.x, centroidB.y));
        }

        _angle = Utils.getAngle(_points.get(0).x, _points.get(_points.size()-1).x, _points.get(0).y, _points.get(_points.size() - 1).y);
    }

    public void draw(ImageProcessor parallelProcessor) {
        int width = parallelProcessor.getWidth();
        byte[] pixels = (byte[]) parallelProcessor.getPixels();
        for (Point p : _points) {
            pixels[p.y * width + p.x] = 0;
        }
    }

    public boolean containsAny(List<Point> points) {
        return !Collections.disjoint(_points, points);
    }
}