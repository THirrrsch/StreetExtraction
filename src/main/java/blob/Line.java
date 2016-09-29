package blob;

import Util.Utils;
import calculation.FeatureConstants;
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
        int end = blob.getLength();
        _points = new ArrayList<Point>();

        for (int i = 0; i < end; i++) {
            _points.add(new Point(xPoints[i], yPoints[i]));
        }

        _angle = Utils.getAngle(_points.get(0).x, _points.get(end - 1).x, _points.get(0).y, _points.get(end - 1).y);
    }

    //connect the centroids of blobs to a line
    public Line(List<Blob> lineBlobs) {
        _points = new ArrayList<Point>();

        for (int i = 0; i < lineBlobs.size() - 1; i++) {
            Point centroidA = lineBlobs.get(i).getCentroid();
            Point centroidB = lineBlobs.get(i + 1).getCentroid();

            _points.addAll(Utils.getBresenhamPoints8Connected(centroidA.x, centroidA.y, centroidB.x, centroidB.y));
        }

        _angle = Utils.getAngle(_points.get(0).x, _points.get(_points.size() - 1).x, _points.get(0).y, _points.get(_points.size() - 1).y);
    }

    public void draw(ImageProcessor parallelProcessor) {
        int width = parallelProcessor.getWidth();
        int height = parallelProcessor.getHeight();
        byte[] pixels = (byte[]) parallelProcessor.getPixels();
        for (Point p : _points) {
            if (p.x >= 0 && p.x < width - 1 && p.y >= 0 && p.y < height - 1)
                pixels[p.y * width + p.x] = 0;
        }
    }

    public boolean containsAny(List<Point> points) {
        return !Collections.disjoint(_points, points);
    }

    public double getAngleAt(Point p) {
        int i = _points.indexOf(p);
        if (i == -1) {
            throw new IndexOutOfBoundsException(); // shouldn't happen
        } else {
            int sampleRate = FeatureConstants.SAMPLE_RATE;
            int halfSampleRate = sampleRate / 2;
            Point pStart;
            Point pEnd;

            if (sampleRate > _points.size()) {
                pStart = _points.get(0);
                pEnd = _points.get(_points.size() - 1);
            } else if (i - halfSampleRate < 0) {
                pStart = _points.get(0);
                pEnd = _points.get(sampleRate - 1);
            } else if (i + halfSampleRate >= _points.size()) {
                pStart = _points.get(_points.size() - sampleRate);
                pEnd = _points.get(_points.size() - 1);
            } else {
                pStart = _points.get(i - halfSampleRate);
                pEnd = _points.get(i + halfSampleRate);
            }

            return Utils.getAngle(pStart.x, pEnd.x, pStart.y, pEnd.y);
        }
    }
}