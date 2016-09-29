package calculation;

import Util.Utils;
import blob.Blob;
import blob.Line;
import blob.ManyBlobs;
import calculation.FeatureConstants;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Preprocessor {

    private ImagePlus _inputImage;
    private ImagePlus _centroidImage;
    private ImagePlus _lineImage;
    private int _width;
    private int _height;

    public Preprocessor(ImagePlus binaryImage) {
        _inputImage = binaryImage;
        _width = _inputImage.getWidth();
        _height = _inputImage.getHeight();
        _lineImage = NewImage.createByteImage("artificial parallel image", _width, _height, 1, 4);
        _centroidImage = NewImage.createByteImage("centroid image", _width, _height, 1, 4);
    }

    public ImagePlus getLineImage() {
        return _lineImage;
    }

    public ImagePlus getCentroidImage() {
        return _centroidImage;
    }

    public ManyBlobs process() {
        ManyBlobs inputBlobs = this.getInitialBlobs();
        ManyBlobs straightLineBlobs = this.getStraightLineBlobs(inputBlobs);
        ManyBlobs result = this.removeShortLines(straightLineBlobs);

        List<Line> dottedLines = this.getDottedLines(inputBlobs);
        result.setDottedLines(dottedLines);

        return result;
    }

    // removes crosses
    private ManyBlobs getInitialBlobs() {
        ImagePlus outputImage = NewImage.createByteImage("straight line image 1", _width, _height, 1, 4);

        byte[] inputPixels = (byte[]) _inputImage.getProcessor().getPixels();
        byte[] outputPixels = (byte[]) outputImage.getProcessor().getPixels();

        for (int x = 0; x < _width - 1; x++) {
            for (int y = 0; y < _height - 1; y++) {
                int index = y * _width + x;
                if (inputPixels[index] == 0) {
                    if (this.getNeighborCount(x, y, inputPixels) > 2) {
                        outputPixels[index] = (byte) 255;
                    } else {
                        outputPixels[index] = 0;
                    }
                } else {
                    outputPixels[index] = (byte) 255;
                }
            }
        }

        ManyBlobs result = new ManyBlobs(outputImage);
        result.findConnectedComponents();

        return result;
    }

    private ManyBlobs getStraightLineBlobs(ManyBlobs inputBlobs) {
        double angleCurrent = 0;
        double angleOld;
        double angleDiff;
        int sampleRate = FeatureConstants.SAMPLE_RATE;
        int maxAngleDiff = FeatureConstants.MAX_ANGLE_DIFF;
        ImagePlus straightLineImage = NewImage.createByteImage("straight line image", _width, _height, 1, 4);
        byte[] straightLinePixels = (byte[]) straightLineImage.getProcessor().getPixels();

        for (Blob blob : inputBlobs) {
            if (blob.getOuterContour().npoints >= sampleRate) {
                int[] contourX = blob.getOuterContour().xpoints;
                int[] contourY = blob.getOuterContour().ypoints;
                int max = blob.getOuterContour().npoints - sampleRate;

                for (int i = 0; i < max; i += sampleRate) {
                    int startX = contourX[i];
                    int startY = contourY[i];
                    int endX = contourX[i + sampleRate];
                    int endY = contourY[i + sampleRate];

                    if (i == 0) {
                        angleCurrent = Utils.getAngle(startX, endX, startY, endY);
                        System.out.println("jo");
                    } else {
                        angleOld = angleCurrent;
                        angleCurrent = Utils.getAngle(startX, endX, startY, endY);
                        angleDiff = Utils.getAngleDiff(angleCurrent, angleOld);

                        if (angleDiff < maxAngleDiff) {
                            for (int j = 0; j < sampleRate; ++j) {
                                straightLinePixels[contourY[i + j] * _width + contourX[i + j]] = 0;
                            }
                        }
                    }
                }
            }
        }

        ManyBlobs result = new ManyBlobs(straightLineImage);
        result.findConnectedComponents();

        return result;
    }

    private ManyBlobs removeShortLines(ManyBlobs inputBlobs) {
        ImagePlus longLineImage = NewImage.createByteImage("preprocessed image", _width, _height, 1, 4);
        ImageProcessor longLineProcessor = longLineImage.getProcessor();
        int _minContourLength = FeatureConstants.MIN_CONTOUR_LENGTH;

        ManyBlobs result = new ManyBlobs();

        for (Blob blob : inputBlobs) {
            int length = blob.getOuterContour().npoints / 2;
            if (length >= _minContourLength) {
                blob.draw(longLineProcessor);
                result.add(blob);
                blob.setLength(length);
            }
        }

        //longLineImage.show();
        //longLineImage.updateAndDraw();

        result.setBinaryImage(longLineImage);
        result.createLineOrdering();
        return result;
    }

    private int getNeighborCount(int x, int y, byte[] pixels) {
        int result = 0;
        int newX;
        int newY;

        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (i != 0 || j != 0) {
                    newX = x + i;
                    newY = y + j;
                    if (this.isInImageRange(newX, newY) && pixels[newY * _width + newX] == 0) {
                        result++;
                    }
                }
            }
        }

        return result;
    }

    private List<Line> getDottedLines(ManyBlobs inputBlobs) {
        ManyBlobs smallBlobs = new ManyBlobs();
        Map<Point, Blob> centroids = new HashMap<Point, Blob>();
        List<Blob> dottedLineBlobs = new ArrayList<Blob>();
        List<Line> result = new ArrayList<Line>();
        int dottedLineMinLength = FeatureConstants.DOTTED_LINE_MIN_LENGTH;
        int dottedLineMaxLength = FeatureConstants.DOTTED_LINE_MAX_LENGTH;

        ImageProcessor centroidProcessor = _centroidImage.getProcessor();
        ImageProcessor lineProcessor = _lineImage.getProcessor();

        for (Blob blob : inputBlobs) {
            int length = blob.getOuterContour().npoints / 2;
            if (length < dottedLineMaxLength) {
                Point centroid = blob.getCentroid();
                smallBlobs.add(blob);
                centroids.put(centroid, blob);
                blob.drawCentroid(centroidProcessor);
                blob.drawCentroid(lineProcessor);
            } else {
                blob.draw(lineProcessor);
                blob.draw(centroidProcessor);
            }
        }

        if (smallBlobs.size() == 1) {
            return result;
        }

        // TODO re-use already used centroids or not?
        for (Blob blob : smallBlobs) {
            //if (!dottedLineBlobs.contains(blob)) {   // if yes, comment
            List<Blob> currentLineBlobs = this.buildLine(blob, centroids, dottedLineBlobs);
            if (currentLineBlobs.size() > 2) {
                //dottedLineBlobs.addAll(currentLineBlobs);  // if yes, comment
                Line line = new Line(currentLineBlobs);
                result.add(line);
                line.draw(lineProcessor);
            }
            //}  // if yes, comment
        }

        _centroidImage.updateAndDraw();
        _lineImage.updateAndDraw();

        return result;
    }

    private List<Blob> buildLine(Blob blob, Map<Point, Blob> centroids, List<Blob> alreadyUsedBlobs) {
        List<Blob> result = new ArrayList<Blob>();
        Blob neighbor = this.findNearestBlobByCentroid(blob, centroids);
        int dottedLineConeAngle = FeatureConstants.DOTTED_LINE_CONE_ANGLE;
        int dottedLineConeLength = FeatureConstants.DOTTED_LINE_CONE_LENGTH;

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

                while (offset < dottedLineConeAngle) {
                    double currentAngle = baseAngle + (double) offset;
                    double currentAngleRAD = Math.PI * currentAngle / 180;
                    int endX = (int) (endCentroid.getX() + (dottedLineConeLength * Math.cos(currentAngleRAD)));
                    int endY = (int) (endCentroid.getY() + (dottedLineConeLength * Math.sin(currentAngleRAD)));
                    List<Point> points = Utils.getBresenhamPoints4Connected(endCentroid.x, endCentroid.y, endX, endY);

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

                    offset = Utils.increaseConeOffset(offset);
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
                p.setLocation(x - offset, y + j);
                if (centroids.containsKey(p)) {
                    return centroids.get(p);
                }
            }
            for (int j = -offset; j <= offset; j++) {
                p.setLocation(x + offset, y + j);
                if (centroids.containsKey(p)) {
                    return centroids.get(p);
                }
            }
            for (int j = -offset; j <= offset; j++) {
                p.setLocation(x + j, y - offset);
                if (centroids.containsKey(p)) {
                    return centroids.get(p);
                }
            }
            for (int j = -offset; j <= offset; j++) {
                p.setLocation(x + j, y + offset);
                if (centroids.containsKey(p)) {
                    return centroids.get(p);
                }
            }
            offset++;
        }
    }

    private boolean isInImageRange(int x, int y) {
        return x > -1 && x < _width && y > -1 && y < _height;
    }
}
