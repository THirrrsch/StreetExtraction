import blob.Blob;
import blob.ManyBlobs;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ImageProcessor;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaleStreetsExtractor implements StreetsExtractor {
    private final ManyBlobs _allBlobs;

    private final ImagePlus _cannyImage;

    private final int _width;
    private final int _height;
    private final int _sampleRate;
    private final int _maxAngleDiff;
    private final int _minContourLength;
    private final int _epsilon;
    private final int _minPts;
    private final int _dottedLineBlobSize;
    private final int _dottedLineConeAngle;
    private final int _dottedLineConeLength;
    private final int _minStreetWidth;
    private final int _maxStreetWidth;
    private final int _lineFollowingSampleRate;
    private final int _coneAngle;
    private final int _coneLength;
    private final int _maxAngleDiffCone;

    private ImagePlus combinedImage;

    public PaleStreetsExtractor(ImagePlus cannyImage,
                                int sampleRate, int maxAngleDiff,
                                int minContourLength,
                                int epsilon, int minPts,
                                int dottedLineBlobSize, int dottedLineConeAngle, int dottedLineConeLength,
                                int minStreetWidth, int maxStreetWidth,
                                int lineFollowingSampleRate, int coneAngle, int coneLength, int maxAngleDiffCone) {
        _sampleRate = sampleRate;
        _maxAngleDiff = maxAngleDiff;
        _minContourLength = minContourLength;
        _epsilon = epsilon;
        _minPts = minPts;
        _dottedLineBlobSize = dottedLineBlobSize;
        _dottedLineConeAngle = dottedLineConeAngle;
        _dottedLineConeLength = dottedLineConeLength;
        _minStreetWidth = minStreetWidth;
        _maxStreetWidth = maxStreetWidth;
        _lineFollowingSampleRate = lineFollowingSampleRate;
        _coneAngle = coneAngle;
        _coneLength = coneLength;
        _maxAngleDiffCone = maxAngleDiffCone;
        _width = cannyImage.getWidth();
        _height = cannyImage.getHeight();

        _cannyImage = cannyImage;

        _allBlobs = new ManyBlobs(_cannyImage);
        _allBlobs.findConnectedComponents();
    }

    public ImagePlus process() {
        ManyBlobs straighLineBlobs = this.getStraightLineBlobs(_allBlobs);
        ManyBlobs longBlobs = this.getLongBlobs(straighLineBlobs);

        //this.printBlobsToCSV(longBlobs);

        ManyBlobs clusterFilteredBlobs = this.getClusterFilteredBlobs(longBlobs);
        ImagePlus parallelImage = getParallelBlobs(clusterFilteredBlobs);
        return parallelImage;
        //return this.getStreetImageByFollowingLines(clusterFilteredBlobs);
    }

    private ManyBlobs getStraightLineBlobs(ManyBlobs inputBlobs) {
        double angleCurrent = 0;
        double angleOld;
        double angleDiff;
        ImagePlus straightLineImage = NewImage.createByteImage("straight line image", _width, _height, 1, 4);
        ImageProcessor straightLineProcessor = straightLineImage.getProcessor();
        byte[] straightLinePixels = (byte[]) straightLineProcessor.getPixels();

        for (Blob blob : inputBlobs) {
            if (blob.getOuterContour().npoints >= _sampleRate) {
                int[] contourX = blob.getOuterContour().xpoints;
                int[] contourY = blob.getOuterContour().ypoints;
                int max = blob.getOuterContour().npoints - _sampleRate;

                for(int i = 0; i < max; i += _sampleRate) {
                    int startX = contourX[i];
                    int startY = contourY[i];
                    int endX = contourX[i + _sampleRate];
                    int endY = contourY[i + _sampleRate];

                    if(i == 0) {
                        angleCurrent = Utils.getAngle(startX, endX, startY, endY);
                    } else {
                        angleOld = angleCurrent;
                        angleCurrent = Utils.getAngle(startX, endX, startY, endY);
                        angleDiff = Utils.getAngleDiff(angleCurrent, angleOld);

                        if(angleDiff < _maxAngleDiff) {
                            for(int j = 0; j < _sampleRate; ++j) {
                                straightLinePixels[contourY[i + j] * _width + contourX[i + j]] = 0;
                            }
                        }
                    }
                }
            }
        }

        straightLineImage.show();
        straightLineImage.updateAndDraw();
        ManyBlobs result = new ManyBlobs(straightLineImage);
        result.findConnectedComponents();
        return result;
    }

    private ManyBlobs getLongBlobs(ManyBlobs inputBlobs) {
        ImagePlus longLineImage = NewImage.createByteImage("long line image", _width, _height, 1, 4);
        ImageProcessor longLineProcessor = longLineImage.getProcessor();
        ManyBlobs result = new ManyBlobs();

        for (Blob blob : inputBlobs) {
            if (blob.getOuterContour().npoints / 2 >= _minContourLength) {
                blob.draw(longLineProcessor);
                result.add(blob);
            }
        }

        longLineImage.show();
        longLineImage.updateAndDraw();
        result.createLineOrdering();
        return result;
    }

    private ManyBlobs getClusterFilteredBlobs(ManyBlobs longBlobs) {
        ManyBlobs result = new ManyBlobs();
        ImagePlus clusteredImage = NewImage.createByteImage("clustered image", _width, _height, 1, 4);
        ImageProcessor clusteredProcessor = clusteredImage.getProcessor();

        List<BlobWrapper> clusterInput = new ArrayList<BlobWrapper>(longBlobs.size());
        for (Blob blob : longBlobs) {
            clusterInput.add(new BlobWrapper(blob));
        }

        DBSCANClusterer<BlobWrapper> clusterer = new DBSCANClusterer<BlobWrapper>(_epsilon, _minPts);
        List<Cluster<BlobWrapper>> clusterResults = clusterer.cluster(clusterInput);

        for (Cluster<BlobWrapper> cluster : clusterResults) {
            for (BlobWrapper wrapper : cluster.getPoints()) {
                clusterInput.remove(wrapper);
            }
        }

        for (BlobWrapper resultWrapper : clusterInput) {
            Blob resultBlob = resultWrapper.getBlob();
            result.add(resultBlob);
            resultBlob.draw(clusteredProcessor);
        }

        clusteredImage.show();
        clusteredImage.updateAndDraw();
        return result;
    }

    private ImagePlus getParallelBlobs(ManyBlobs inputBlobs) {
        ImagePlus parallelImage = NewImage.createByteImage("Parallel Image", _width, _height, 1, 4);
        ImageProcessor parallelProcessor = parallelImage.getProcessor();

        List<Line> lines = this.getDottedLines(inputBlobs);
        for (Blob blob : inputBlobs) {
            lines.add(new Line(blob));
        }

        for (Blob blob : inputBlobs) {
            List<Line> parallelLines = this.findParallelLines(blob, lines);

            if (parallelLines.size() > 0) {
                blob.draw(parallelProcessor);
                for (Line parallelLine : parallelLines) {
                    parallelLine.draw(parallelProcessor);
                }
            }
        }

        parallelImage.updateImage();
        return parallelImage;
    }

    private List<Line> findParallelLines(Blob blob, List<Line> lines) {
        byte[] pixels = (byte[]) combinedImage.getProcessor().getPixels();

        List<Line> result = new ArrayList<Line>();
        int[] contourX = blob.getLineX();
        int[] contourY = blob.getLineY();
        int max = (blob.getOuterContour().npoints / 2) - _sampleRate;
        Line currentLine = new Line(blob);

        for(int i = 0; i <= max; i += _sampleRate) {
            int dX = contourX[i + _sampleRate - 1] - contourX[i];
            int dY = contourY[i + _sampleRate - 1] - contourY[i];

            double length = Math.sqrt(dX * dX + dY * dY);
            double vecXNorm = dX * (1 / length);
            double vecYNorm = dY * (1 / length);

            int vecXStart = (int) (-vecYNorm * _minStreetWidth);
            int vecYStart = (int) (vecXNorm * _minStreetWidth);
            int vecXEnd = (int) (-vecYNorm * _maxStreetWidth);
            int vecYEnd = (int) (vecXNorm * _maxStreetWidth);

            int baseX = contourX[i + _sampleRate / 2];
            int baseY = contourY[i + _sampleRate / 2];

            List<Point> pointsClock = Utils.getBresenhamPoints(baseX + vecXStart, baseY + vecYStart, baseX + vecXEnd, baseY + vecYEnd);
            List<Point> pointsCounterclock = Utils.getBresenhamPoints(baseX - vecXStart, baseY - vecYStart, baseX - vecXEnd, baseY - vecYEnd);

//            for (Point p : pointsClock) {
//                if (isInImageRange(p.x, p.y)) {
//                    pixels[p.y * _width + p.x] = -56;
//                }
//            }

            for (Line line : lines) {
                if (!result.contains(line) && line.containsAny(pointsClock)) {
                    if (Utils.getAngleDiff(line.getAngle(), currentLine.getAngle()) < _maxAngleDiff) {
                        result.add(line);
                    }
                }
            }

            for (Line line : lines) {
                if (!result.contains(line) && line.containsAny(pointsCounterclock)) {
                    if (Utils.getAngleDiff(line.getAngle(), currentLine.getAngle()) < _maxAngleDiff) {
                        result.add(line);
                    }
                }
            }
        }

        combinedImage.updateImage();

        return result;
    }

    private List<Line> getDottedLines(ManyBlobs inputBlobs) {
        combinedImage = NewImage.createByteImage("combined image", _width, _height, 1, 4);
        ImageProcessor combinedProcessor = combinedImage.getProcessor();

        ManyBlobs smallBlobs = new ManyBlobs();
        Map<Point, Blob> centroids = new HashMap<Point, Blob>();
        List<Blob> dottedLineBlobs = new ArrayList<Blob>();
        List<Line> result = new ArrayList<Line>();

        for (Blob blob : _allBlobs) {
            if (blob.getEnclosedArea() < _dottedLineBlobSize) {
                Point centroid = blob.getCentroid();
                smallBlobs.add(blob);
                centroids.put(centroid, blob);
            }
        }

        for (Blob blob : smallBlobs) {
            if (!dottedLineBlobs.contains(blob)) {
                List<Blob> currentLineBlobs = this.buildLine(blob, centroids, dottedLineBlobs);

                if (currentLineBlobs.size() > 2) {
                    dottedLineBlobs.addAll(currentLineBlobs);
                    result.add(new Line(currentLineBlobs));
                    Utils.drawCentroidLine(currentLineBlobs, combinedProcessor);
                }
            }
        }

        for (Blob blob : inputBlobs) {
            blob.draw(combinedProcessor);
        }

        combinedImage.show();
        combinedImage.updateAndDraw();

        return result;
    }

    private List<Blob> buildLine(Blob blob, Map<Point, Blob> centroids, List<Blob> alreadyUsedBlobs) {
        List<Blob> result = new ArrayList<Blob>();
        Blob neighbor = findNearestBlobByCentroid(blob, centroids);

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

                while(offset < _dottedLineConeAngle) {
                    double currentAngle = baseAngle + (double) offset;
                    double currentAngleRAD = Math.PI * currentAngle / 180;
                    int endX = (int) (endCentroid.getX() + (_dottedLineConeLength * Math.cos(currentAngleRAD)));
                    int endY = (int) ( endCentroid.getY() +  (_dottedLineConeLength* Math.sin(currentAngleRAD)));
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
                                offset = -_dottedLineConeAngle;
                                break;
                            }
                        }
                    }

                    offset = increaseConeOffset(offset);
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
        int x = (int) blob.getCentroid().getX();
        int y = (int) blob.getCentroid().getY();
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

    private ImagePlus getStreetImageByFollowingLines(ManyBlobs inputBlobs) {
        ImagePlus streetImage = NewImage.createByteImage("street image", _width, _height, 1, 4);
        ImageProcessor streetImageProcessor = streetImage.getProcessor();

        for (Blob blob : inputBlobs) {
            int[] contourX = blob.getLineX();
            int[] contourY = blob.getLineY();
            int end = blob.getOuterContour().npoints / 2;
            boolean firstPart = true;

            for (int i = 0; i < 2; ++i) {
                int x1 = firstPart ? contourX[_lineFollowingSampleRate] : contourX[end - _lineFollowingSampleRate];
                int y1 = firstPart ? contourY[_lineFollowingSampleRate] : contourY[end - _lineFollowingSampleRate];
                int startX = firstPart ? contourX[0] : contourX[end];
                int startY = firstPart ? contourY[0] : contourY[end];
                double baseAngle = Utils.getAngle(x1, startX, y1, startY);
                int offset = 0;

                while (offset < _coneAngle) {
                    double currentAngle = baseAngle + (double) offset;
                    double currentAngleRAD = Math.PI * currentAngle / 180;
                    int endX = (int) ((double) startX + (double) _coneLength * Math.cos(currentAngleRAD));
                    int endY = (int) ((double) startY + (double) _coneLength * Math.sin(currentAngleRAD));
                    List<Point> points = Utils.getBresenhamPoints(startX, startY, endX, endY);

                    if (this.couldFollow(points, inputBlobs, baseAngle, streetImageProcessor)) {
                        blob.draw(streetImageProcessor);
                        break;
                    }

                    offset = this.increaseConeOffset(offset);
                }

                firstPart = !firstPart;
            }
        }

        streetImage.updateImage();
        return streetImage;
    }

    private boolean couldFollow(List<Point> points, ManyBlobs inputBlobs, double baseAngle, ImageProcessor streetImageProcessor) {
        for (Blob blob : inputBlobs) {
            int[] contourX = blob.getLineX();
            int[] contourY = blob.getLineY();
            int end = blob.getOuterContour().npoints / 2;

            for (Point p : points) {
                double angle;
                if (contourX[0] == p.x && contourY[0] == p.y) {
                    angle = Utils.getAngle(contourX[0], contourX[_lineFollowingSampleRate], contourY[0], contourY[_lineFollowingSampleRate]);
                    if (Utils.getAngleDiff(baseAngle, angle) < (double) _maxAngleDiffCone) {
                        streetImageProcessor.drawLine(points.get(0).x, points.get(0).y, p.x, p.y);
                        return true;
                    }
                }

                if (contourX[end] == p.x && contourY[end] == p.y) {
                    angle = Utils.getAngle(contourX[end], contourX[end - _lineFollowingSampleRate], contourY[end], contourY[end - _lineFollowingSampleRate]);
                    if (Utils.getAngleDiff(baseAngle, angle) < (double) _maxAngleDiffCone) {
                        streetImageProcessor.drawLine(points.get(0).x, points.get(0).y, p.x, p.y);
                        return true;
                    }
                }
            }
        }

        return false;
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

    private void printBlobsToCSV(ManyBlobs blobs) {
        try
        {
            FileWriter writer = new FileWriter("C:\\Users\\Hirsch\\Desktop\\test.csv");

            for (Blob blob : blobs) {
                int end = blob.getOuterContour().npoints / 2;
                int[] contourX = blob.getLineX();
                int[] contourY = blob.getLineY();
                double angle = Utils.getAngle(contourX[0], contourX[end], contourY[0], contourY[end]);

                writer.append(String.valueOf(end));
                writer.append(' ');
                writer.append(String.valueOf(angle));
                writer.append('\n');
            }

            writer.flush();
            writer.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private boolean isInImageRange(int x, int y) {
        return x > -1 && x < _width && y > -1 && y < _height;
    }

    /*
    private ManyBlobs getConeFollowingBlobs(ManyBlobs longBlobs) {
        ImagePlus coneFollowingImage = NewImage.createByteImage("cone following image", _width, _height, 1, 4);
        ImageProcessor coneFollowingProcessor = coneFollowingImage.getProcessor();
        //ManyBlobs result = new ManyBlobs();

        for (Blob blob : longBlobs) {
            int[] contourX = blob.getLineX();
            int[] contourY = blob.getLineY();
            int end = blob.getOuterContour().npoints / 2;
            boolean firstPart = true;

            for (int i = 0; i < 2; ++i) {
                int x1 = firstPart ? contourX[_lineFollowingSampleRate] : contourX[end - _lineFollowingSampleRate];
                int y1 = firstPart ? contourY[_lineFollowingSampleRate] : contourY[end - _lineFollowingSampleRate];
                int startX = firstPart ? contourX[0] : contourX[end];
                int startY = firstPart ? contourY[0] : contourY[end];
                double baseAngle = this.getAngle(x1, startX, y1, startY);
                int offset = 0;

                while (offset < _coneAngle) {
                    double currentAngle = baseAngle + (double) offset;
                    double currentAngleRAD = Math.PI * currentAngle / 180;
                    int endX = (int) ((double) startX + (double) _coneLength * Math.cos(currentAngleRAD));
                    int endY = (int) ((double) startY + (double) _coneLength * Math.sin(currentAngleRAD));
                    List<Point> points = Utils.getBresenhamPoints(startX, startY, endX, endY);

                    if (this.couldFollow(points, longBlobs, baseAngle, coneFollowingProcessor)) {
                        blob.draw(coneFollowingProcessor);

                        //TODO DELETE THIS LINE IF CONES ARE CONNECTED
                        //result.add(blob);
                        break;
                    }

                    if (offset == 0) {
                        offset = -1;
                    } else if (offset < 0) {
                        offset *= -1;
                    } else {
                        ++offset;
                        offset *= -1;
                    }
                }

                firstPart = !firstPart;
            }
        }

        coneFollowingImage.show();
        coneFollowingImage.updateAndDraw();

        ManyBlobs result = new ManyBlobs(coneFollowingImage);
        result.findConnectedComponents();

        return result;
    }
    */


}
