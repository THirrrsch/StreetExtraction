import Util.BlobMapper;
import Util.LineMapper;
import Util.Utils;
import blob.Blob;
import blob.BlobWrapper;
import blob.Line;
import blob.ManyBlobs;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ImageProcessor;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class PaleStreetsExtractorOld implements StreetsExtractor {
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

    public PaleStreetsExtractorOld(ImagePlus cannyImage,
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
        //ImagePlus parallelImage = getParallelBlobs(clusterFilteredBlobs);
        //return parallelImage;
        return this.getStreetImageByFollowingLines(clusterFilteredBlobs);
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

    }

    private ImagePlus getParallelBlobs(ManyBlobs inputBlobs) {

    }








    private ImagePlus getStreetImageByFollowingLines(ManyBlobs inputBlobs) {

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
                    List<Point> points = Util.Utils.getBresenhamPoints(startX, startY, endX, endY);

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
