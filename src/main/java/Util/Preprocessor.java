package Util;

import blob.Blob;
import blob.Line;
import blob.ManyBlobs;
import com.sun.tools.classfile.ConstantPool;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Preprocessor {

    private ManyBlobs _rawBlobs;
    private ManyBlobs _processedBlobs;
    private ImagePlus _binaryImage;
    private int _width;
    private int _height;

    public Preprocessor(ManyBlobs blobs, ImagePlus binaryImage) {
        _rawBlobs = blobs;
        _binaryImage = binaryImage;
        _width = _binaryImage.getWidth();
        _height = _binaryImage.getHeight();
    }

    public void process() {
        ManyBlobs tmpBlobs = this.getStraightLineBlobs(_rawBlobs);
        _processedBlobs = this.getLongBlobs(tmpBlobs);

        List<Line> dottedLines = this.getDottedLines(_rawBlobs);
        _processedBlobs.setDottedLines(dottedLines);
    }

    public ManyBlobs getProcessedBlobs() {
        return _processedBlobs;
    }

    private ManyBlobs getStraightLineBlobs(ManyBlobs inputBlobs) {
        double angleCurrent = 0;
        double angleOld;
        double angleDiff;
        int width = inputBlobs.getBinaryImage().getWidth();
        int height = inputBlobs.getBinaryImage().getHeight();
        int sampleRate = Constants.SAMPLE_RATE;
        int maxAngleDiff = Constants.MAX_ANGLE_DIFF;

        ImagePlus straightLineImage = NewImage.createByteImage("straight line image", width, height, 1, 4);
        ImageProcessor straightLineProcessor = straightLineImage.getProcessor();
        byte[] straightLinePixels = (byte[]) straightLineProcessor.getPixels();

        for (Blob blob : inputBlobs) {
            if (blob.getOuterContour().npoints >= sampleRate) {
                int[] contourX = blob.getOuterContour().xpoints;
                int[] contourY = blob.getOuterContour().ypoints;
                int max = blob.getOuterContour().npoints - sampleRate;

                for(int i = 0; i < max; i += sampleRate) {
                    int startX = contourX[i];
                    int startY = contourY[i];
                    int endX = contourX[i + sampleRate];
                    int endY = contourY[i + sampleRate];

                    if(i == 0) {
                        angleCurrent = Utils.getAngle(startX, endX, startY, endY);
                    } else {
                        angleOld = angleCurrent;
                        angleCurrent = Utils.getAngle(startX, endX, startY, endY);
                        angleDiff = Utils.getAngleDiff(angleCurrent, angleOld);

                        if(angleDiff < maxAngleDiff) {
                            for(int j = 0; j < sampleRate; ++j) {
                                straightLinePixels[contourY[i + j] * width + contourX[i + j]] = 0;
                            }
                        }
                    }
                }
            }
        }

        straightLineImage.updateImage();
        ManyBlobs result = new ManyBlobs(straightLineImage);
        result.findConnectedComponents();
        return result;
    }

    private ManyBlobs getLongBlobs(ManyBlobs inputBlobs) {
        ManyBlobs result = new ManyBlobs();
        int minLength = Constants.MIN_CONTOUR_LENGTH;
        for (Blob blob : inputBlobs) {
            int length = blob.getOuterContour().npoints / 2;
            if (length > minLength) {
                blob.setLength(length);
                result.add(blob);
            }
        }
        result.createLineOrdering();
        return result;
    }

    private List<Line> getDottedLines(ManyBlobs inputBlobs) {
        ManyBlobs smallBlobs = new ManyBlobs();
        Map<Point, Blob> centroids = new HashMap<Point, Blob>();
        List<Blob> dottedLineBlobs = new ArrayList<Blob>();
        List<Line> result = new ArrayList<Line>();
        int dottedLineMaxBlobSize = Constants.DOTTED_LINE_BLOB_SIZE;

        for (Blob blob : inputBlobs) {
            if (blob.getEnclosedArea() < dottedLineMaxBlobSize) {
                Point centroid = blob.getCentroid();
                smallBlobs.add(blob);
                centroids.put(centroid, blob);
            }
        }

        // TODO re-use already used centroids or not?
        for (Blob blob : smallBlobs) {
            //if (!dottedLineBlobs.contains(blob)) {   // if yes, comment
                List<Blob> currentLineBlobs = this.buildLine(blob, centroids, dottedLineBlobs);
                if (currentLineBlobs.size() > 2) {
                    //dottedLineBlobs.addAll(currentLineBlobs);  // if yes, comment
                    result.add(new Line(currentLineBlobs));
                }
            //}  // if yes, comment
        }

        return result;
    }

    private List<Blob> buildLine(Blob blob, Map<Point, Blob> centroids, List<Blob> alreadyUsedBlobs) {
        List<Blob> result = new ArrayList<Blob>();
        Blob neighbor = this.findNearestBlobByCentroid(blob, centroids);
        int dottedLineConeAngle = Constants.DOTTED_LINE_CONE_ANGLE;
        int dottedLineConeLength = Constants.DOTTED_LINE_CONE_LENGTH;

        result.add(blob);
        result.add(neighbor);

        for (int i = 0; i < 2; i++) {
            Point startCentroid = i == 0 ? blob.getCentroid() : neighbor.getCentroid();
            Point endCentroid = i == 0 ? neighbor.getCentroid() : blob.getCentroid();

            double baseAngle;
            int offset;
            Blob lastBlobAdded = result.get(result.size() - 1);

            while (lastBlobAdded != null) {
                lastBlobAdded = null;
                offset = 0;
                baseAngle = Utils.getAngle(startCentroid.x, endCentroid.x, startCentroid.y, endCentroid.y);

                while(offset < dottedLineConeAngle) {
                    double currentAngle = baseAngle + (double) offset;
                    double currentAngleRAD = Math.PI * currentAngle / 180;
                    int endX = (int) (endCentroid.getX() + (dottedLineConeLength * Math.cos(currentAngleRAD)));
                    int endY = (int) ( endCentroid.getY() +  (dottedLineConeLength* Math.sin(currentAngleRAD)));
                    List<Point> points = Utils.getBresenhamPoints(endCentroid.x, endCentroid.y, endX, endY);

                    for (Point p : points) {
                        if (this.isInImageRange(p.x, p.y) && centroids.containsKey(p)) {
                            Blob candidate = centroids.get(p);
                            if (!alreadyUsedBlobs.contains(candidate)) {
                                lastBlobAdded = candidate;
                                if (i == 0) {
                                    result.add(lastBlobAdded);
                                } else {
                                    result.add(0, lastBlobAdded);
                                }
                                offset = -dottedLineConeAngle;
                                break;
                            }
                        }
                    }

                    offset = this.increaseConeOffset(offset);
                }

                if (lastBlobAdded != null) {
                    startCentroid = endCentroid;
                    endCentroid = lastBlobAdded.getCentroid();
                }
            }
        }

        return result;
    }

    private Blob findNearestBlobByCentroid(Blob blob, Map<Point, Blob> centroids) {
        int offset = 1;
        int x = blob.getCentroid().x;
        int y = blob.getCentroid().y;
        Point p = new Point();

        while (true) {
            for (int j = -offset; j <= offset; j++) {
                p.setLocation(x + j, y - offset);
                if (this.isInImageRange(x + j, y - offset) && centroids.containsKey(p)) {
                    return centroids.get(p);
                }
            }
            for (int j = -offset; j <= offset; j++) {
                p.setLocation(x + j, y + offset);
                if (this.isInImageRange(x + j, y + offset) && centroids.containsKey(p)) {
                    return centroids.get(p);
                }
            }
            for (int j = -offset; j <= offset; j++) {
                p.setLocation(x - offset, y + j);
                if (this.isInImageRange(x - offset, y + j) && centroids.containsKey(p)) {
                    return centroids.get(p);
                }
            }
            for (int j = -offset; j <= offset; j++) {
                p.setLocation(x + offset, y + j);
                if (this.isInImageRange(x + offset, y + j) && centroids.containsKey(p)) {
                    return centroids.get(p);
                }
            }

            offset++;
        }
    }

    private int increaseConeOffset(int offset) {
        if (offset == 0) {
            return -1;
        } else if (offset < 0) {
            return (offset * -1);
        } else {
            ++offset;
            return (offset * -1);
        }
    }

    private boolean isInImageRange(int x, int y) {
        return x > -1 && x < _width && y > -1 && y < _height;
    }
}
