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
import java.util.List;

public class PaleStreetsExtractor implements StreetsExtractor {
    private final ManyBlobs _allBlobs;
    private final int _width;
    private final int _height;
    private final int _sampleRate;
    private final int _maxAngleDiff;
    private final int _minContourLength;
    private final int _epsilon;
    private final int _minPts;
    private final int _lineFollowingSampleRate;
    private final int _coneAngle;
    private final int _coneLength;
    private final int _maxAngleDiffCone;

    ImagePlus _clusteredImage;

    public PaleStreetsExtractor(ImagePlus cannyImage, int sampleRate, int maxAngleDiff, int minContourLength, int epsilon, int minPts, int lineFollowingSampleRate, int coneAngle, int coneLength, int maxAngleDiffCone) {
        _sampleRate = sampleRate;
        _maxAngleDiff = maxAngleDiff;
        _minContourLength = minContourLength;
        _epsilon = epsilon;
        _minPts = minPts;
        _lineFollowingSampleRate = lineFollowingSampleRate;
        _coneAngle = coneAngle;
        _coneLength = coneLength;
        _maxAngleDiffCone = maxAngleDiffCone;
        _width = cannyImage.getWidth();
        _height = cannyImage.getHeight();

        _allBlobs = new ManyBlobs(cannyImage);
        _allBlobs.findConnectedComponents();
    }

    public ImagePlus process() {
        ManyBlobs straighLineBlobs = this.getStraightLineBlobs(_allBlobs);
        ManyBlobs longBlobs = this.getLongBlobs(straighLineBlobs);

        //this.printBlobsToCSV(longBlobs);

        ManyBlobs clusterFilteredBlobs = this.getClusterFilteredBlobs(longBlobs);
        return this.getStreetImageByFollowingLines(clusterFilteredBlobs);
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
                        angleCurrent = this.getAngle(startX, endX, startY, endY);
                    } else {
                        angleOld = angleCurrent;
                        angleCurrent = this.getAngle(startX, endX, startY, endY);
                        angleDiff = this.getAngleDiff(angleCurrent, angleOld);

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
        _clusteredImage = NewImage.createByteImage("clustered image", _width, _height, 1, 4);
        ImageProcessor clusteredProcessor = _clusteredImage.getProcessor();

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

        _clusteredImage.show();
        _clusteredImage.updateAndDraw();
        return result;
    }

    private ImagePlus getStreetImageByFollowingLines(ManyBlobs inputBlobs) {
        ImagePlus streetImage = NewImage.createByteImage("street image", _width, _height, 1, 4);
        ImageProcessor streetImageProcessor = streetImage.getProcessor();
        byte[] clusteredPixels = (byte[]) _clusteredImage.getProcessor().getPixels();

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
                double baseAngle = this.getAngle(x1, startX, y1, startY);
                int offset = 0;

                while (offset < _coneAngle) {
                    double currentAngle = baseAngle + (double) offset;
                    double currentAngleRAD = Math.PI * currentAngle / 180;
                    int endX = (int) ((double) startX + (double) _coneLength * Math.cos(currentAngleRAD));
                    int endY = (int) ((double) startY + (double) _coneLength * Math.sin(currentAngleRAD));
                    List<Point> points = Utils.getBresenhamPoints(startX, startY, endX, endY);

//                    for (Point p : points) {
//                        if (this.isInImageRange(p.x, p.y) && clusteredPixels[p.y * _width + p.x] != 0) {
//                            clusteredPixels[p.y * _width + p.x] = -56;
//                        }
//                    }

                    if (this.couldFollow(points, inputBlobs, baseAngle, streetImageProcessor)) {
                        blob.draw(streetImageProcessor);
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

        streetImage.updateImage();
        return streetImage;
    }

    private boolean couldFollow(List<Point> points, ManyBlobs longBlobs, double baseAngle, ImageProcessor streetImageProcessor) {
        for (Blob blob : longBlobs) {
            int[] contourX = blob.getLineX();
            int[] contourY = blob.getLineY();
            int end = blob.getOuterContour().npoints / 2;

            for (Point p : points) {
                double angle;
                if (contourX[0] == p.x && contourY[0] == p.y) {
                    angle = this.getAngle(contourX[0], contourX[_lineFollowingSampleRate], contourY[0], contourY[_lineFollowingSampleRate]);
                    if (this.getAngleDiff(baseAngle, angle) < (double) _maxAngleDiffCone) {
                        streetImageProcessor.drawLine(points.get(0).x, points.get(0).y, p.x, p.y);
                        return true;
                    }
                }

                if (contourX[end] == p.x && contourY[end] == p.y) {
                    angle = this.getAngle(contourX[end], contourX[end - _lineFollowingSampleRate], contourY[end], contourY[end - _lineFollowingSampleRate]);
                    if (this.getAngleDiff(baseAngle, angle) < (double) _maxAngleDiffCone) {
                        streetImageProcessor.drawLine(points.get(0).x, points.get(0).y, p.x, p.y);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private double getAngle(int startX, int endX, int startY, int endY) {
        double angleRAD = Math.atan2((double)(endY - startY), (double)(endX - startX));
        return angleRAD * 180 / Math.PI;
    }

    private double getAngle180Positive(int startX, int endX, int startY, int endY) {
        double angleRAD = Math.atan2((double)(endY - startY), (double)(endX - startX));
        double angleDegree = angleRAD * 180 / Math.PI;
        return angleDegree > 0 ? angleDegree : 180 + angleDegree;
    }

    private double getAngleDiff(double alpha, double beta) {
        double diff = Math.abs(beta - alpha) % 360;
        return diff < 180 ? diff : 360 - diff;
    }

    private void printBlobsToCSV(ManyBlobs blobs) {
        try
        {
            FileWriter writer = new FileWriter("C:\\Users\\Hirsch\\Desktop\\test.csv");

            for (Blob blob : blobs) {
                int end = blob.getOuterContour().npoints / 2;
                int[] contourX = blob.getLineX();
                int[] contourY = blob.getLineY();
                double angle = this.getAngle180Positive(contourX[0], contourX[end], contourY[0], contourY[end]);

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

    /*
    private ImagePlus getColoredNeighborhoodImage(ManyBlobs inputBlobs) {
        inputBlobs.createLineOrdering();

        ImagePlus coloredBlobsImage = NewImage.createByteImage("colored blobs image", _originalImage.getWidth(), _height, 1, 4);
        ImageProcessor coloredBlobsImageProcessor = coloredBlobsImage.getProcessor();

        for (Blob blob : inputBlobs) {
            //int[] contourX = blob.getOuterContour().xpoints;
            //int[] contourY = blob.getOuterContour().ypoints;
            //int max = blob.getOuterContour().npoints - _sampleRate;

            int[] contourX = blob.getLineX();
            int[] contourY = blob.getLineY();
            int max = (blob.getOuterContour().npoints / 2) - _sampleRate;
            double backgroundRatio = 0;
            int count = 0;

            for(int i = 0; i <= max; i += _sampleRate) {
                int vecX = contourX[i + _sampleRate - 1] - contourX[i];
                int vecY = contourY[i + _sampleRate - 1] - contourY[i];

                double length = Math.sqrt(vecX*vecX + vecY*vecY);
                double vecXNorm = vecX * (1 / length);
                double vecYNorm = vecY * (1 / length);

                double vecXClock = -vecYNorm * _colorLookupRadius;
                double vecYClock = vecXNorm * _colorLookupRadius;
                double vecXCounterclock = vecYNorm * _colorLookupRadius;
                double vecYCounterclock = -vecXNorm * _colorLookupRadius;

                int startX = contourX[i + _sampleRate / 2];
                int startY = contourY[i + _sampleRate / 2];

                int endXClock = (int) (startX + vecXClock);
                int endYClock = (int) (startY + vecYClock);
                int endXCounterclock = (int) (startX + vecXCounterclock);
                int endYCounterclock = (int) (startY + vecYCounterclock);

                List<Point> pointsClock = Utils.getBresenhamPoints(startX, startY, endXClock, endYClock);
                List<Point> pointsCounterclock = Utils.getBresenhamPoints(startX, startY, endXCounterclock, endYCounterclock);

                double clockwiseRatio = getBackgroundRatio(pointsClock);
                double counterclockwiseRatio = getBackgroundRatio(pointsCounterclock);

                backgroundRatio += ((clockwiseRatio + counterclockwiseRatio) / 2);
                count++;
            }

            backgroundRatio /= count;
            if (backgroundRatio >= _colorLookupRatio) {
                blob.draw(coloredBlobsImageProcessor);
            }
        }

        coloredBlobsImage.updateImage();
        return coloredBlobsImage;
    }
    */

    /*
    private ManyBlobs getColoredBlobs(ManyBlobs inputBlobs) {
        ImagePlus coloredBlobsImage = NewImage.createByteImage("colored blobs image", _originalImage.getWidth(), _height, 1, 4);
        ImageProcessor coloredBlobsImageProcessor = coloredBlobsImage.getProcessor();
        ManyBlobs result = new ManyBlobs();

        for (Blob blob : inputBlobs) {
            int[] contourX = blob.getLineX();
            int[] contourY = blob.getLineY();
            int max = (blob.getOuterContour().npoints / 2) - _sampleRate;
            double backgroundRatio = 0;
            int count = 0;

            for(int i = 0; i <= max; i += _sampleRate) {
                int vecX = contourX[i + _sampleRate - 1] - contourX[i];
                int vecY = contourY[i + _sampleRate - 1] - contourY[i];

                double length = Math.sqrt(vecX*vecX + vecY*vecY);
                double vecXNorm = vecX * (1 / length);
                double vecYNorm = vecY * (1 / length);

                double vecXClock = -vecYNorm * _colorLookupRadius;
                double vecYClock = vecXNorm * _colorLookupRadius;
                double vecXCounterclock = vecYNorm * _colorLookupRadius;
                double vecYCounterclock = -vecXNorm * _colorLookupRadius;

                int startX = contourX[i + _sampleRate / 2];
                int startY = contourY[i + _sampleRate / 2];

                int endXClock = (int) (startX + vecXClock);
                int endYClock = (int) (startY + vecYClock);
                int endXCounterclock = (int) (startX + vecXCounterclock);
                int endYCounterclock = (int) (startY + vecYCounterclock);

                List<Point> pointsClock = Utils.getBresenhamPoints(startX, startY, endXClock, endYClock);
                List<Point> pointsCounterclock = Utils.getBresenhamPoints(startX, startY, endXCounterclock, endYCounterclock);

                double clockwiseRatio = getBackgroundRatio(pointsClock);
                double counterclockwiseRatio = getBackgroundRatio(pointsCounterclock);

                backgroundRatio += ((clockwiseRatio + counterclockwiseRatio) / 2);
                count++;
            }

            backgroundRatio /= count;
            if (backgroundRatio >= _colorLookupRatio) {
                blob.draw(coloredBlobsImageProcessor);
                result.add(blob);
            }
        }

        coloredBlobsImage.show();
        coloredBlobsImage.updateAndDraw();

        return result;
    }
    */

        /*
    private double getBackgroundRatio(List<Point> points) {
        double pointsCount = 0;
        double backgroundCount = 0;

        for (Point p : points) {
            if (this.isInImageRange(p.x, p.y)) {
                pointsCount++;
                int index = p.y * _width + p.x;
                int hue = _hue[index] & 255;
                int saturation = _saturation[index] & 255;
                int brightness = _brightness[index] & 255;

                if(hue >= _minHue && hue <= _maxHue && saturation >= _minSat && saturation <= _maxSat && brightness >= _minBright && brightness <= _maxBright) {
                    backgroundCount++;
                }
            }
        }

        return backgroundCount / pointsCount;
    }
    */

    private boolean isInImageRange(int x, int y) {
        return x > -1 && x < _width && y > -1 && y < _height;
    }
}
