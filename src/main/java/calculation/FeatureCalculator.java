package calculation;

import Util.BlobMapper;
import Util.LineMapper;
import Util.Utils;
import blob.*;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeatureCalculator {

    private ManyBlobs _blobs;
    private ImagePlus _binaryImage;
    private int _width;
    private int _height;

    public FeatureCalculator(ManyBlobs blobs) {
        _blobs = blobs;
    }

    public FeatureCalculator(ManyBlobs blobs, ImagePlus binaryImage) {
        _blobs = blobs;
        _binaryImage = binaryImage;
        _width = binaryImage.getWidth();
        _height = binaryImage.getHeight();
    }

    public void calculateFeatures() {
        this.checkClustering();
        this.calculateParallelCoverage();
        this.calculateLineFollowingSegments();

        this.fillFeatureTable();
    }

    private void checkClustering() {
        double epsilon = FeatureConstants.DBSCAN_EPSILON;
        int minPts = FeatureConstants.DBSCAN_MINPTS;

        List<BlobWrapper> clusterInput = new ArrayList<BlobWrapper>(_blobs.size());
        for (Blob blob : _blobs) {
            clusterInput.add(new BlobWrapper(blob));
        }

        DBSCANClusterer<BlobWrapper> clusterer = new DBSCANClusterer<BlobWrapper>(epsilon, minPts);
        List<Cluster<BlobWrapper>> clusterResults = clusterer.cluster(clusterInput);

        for (Cluster<BlobWrapper> cluster : clusterResults) {
            for (BlobWrapper wrapper : cluster.getPoints()) {
                wrapper.getBlob().setInCluster(true);
            }
        }
    }

    private void calculateParallelCoverage() {
        List<Line> inputLines = new ArrayList<Line>();
        inputLines.addAll(_blobs.getDottedLines());
        for (Blob blob : _blobs) {
            inputLines.add(new Line(blob));
        }
        LineMapper mapper = new LineMapper(inputLines);

        int sampleRate = FeatureConstants.SAMPLE_RATE;
        int minStreetWidth = FeatureConstants.MIN_STREET_WIDTH;
        int maxStreetWidth = FeatureConstants.MAX_STREET_WIDTH;

        for (Blob blob : _blobs) {
            int parallelCountClock = 0;
            int parallelCountCounterClock = 0;
            int[] contourX = blob.getLineX();
            int[] contourY = blob.getLineY();
            int max = blob.getLength() - sampleRate;

            for (int i = 0; i <= max; i += sampleRate) {
                int dX = contourX[i + sampleRate - 1] - contourX[i];
                int dY = contourY[i + sampleRate - 1] - contourY[i];
                double currentAngle = Utils.getAngle(dX, dY);

                double length = Math.sqrt(dX * dX + dY * dY);
                double vecXNorm = dX * (1 / length);
                double vecYNorm = dY * (1 / length);

                int vecXStart = (int) (-vecYNorm * minStreetWidth);
                int vecYStart = (int) (vecXNorm * minStreetWidth);
                int vecXEnd = (int) (-vecYNorm * maxStreetWidth);
                int vecYEnd = (int) (vecXNorm * maxStreetWidth);

                int baseX = contourX[i + sampleRate / 2];
                int baseY = contourY[i + sampleRate / 2];

                List<Point> pointsClock = Utils.getBresenhamPoints4Connected(baseX + vecXStart, baseY + vecYStart, baseX + vecXEnd, baseY + vecYEnd);
                List<Point> pointsCounterclock = Utils.getBresenhamPoints4Connected(baseX - vecXStart, baseY - vecYStart, baseX - vecXEnd, baseY - vecYEnd);

                if (this.doesParallelLineExist(currentAngle, pointsClock, mapper)) {
                    parallelCountClock++;
                }
                if (this.doesParallelLineExist(currentAngle, pointsCounterclock, mapper)) {
                    parallelCountCounterClock++;
                }
            }

            int maxParallelCount = parallelCountClock > parallelCountCounterClock ? parallelCountClock : parallelCountCounterClock;
            if (maxParallelCount > 0) {
                int maxSegments = blob.getLength() / sampleRate;
                blob.setParallelCoverage((double)maxParallelCount / maxSegments);
            } else {
                blob.setParallelCoverage(0);
            }
        }
    }

    private boolean doesParallelLineExist(double currentAngle, List<Point> points, LineMapper mapper) {
        Map<Point, List<Line>> intersectedLines = mapper.getIntersectedLines(points);
        int maxAngleDiff = FeatureConstants.MAX_ANGLE_DIFF;

        for (Map.Entry<Point, List<Line>> entry : intersectedLines.entrySet()) {
            Point p = entry.getKey();
            List<Line> lines = entry.getValue();
            for (Line line : lines) {
                double intersectedLineAngle = line.getAngleAt(p);
                if (Utils.getAngleDiff(currentAngle, intersectedLineAngle) <= maxAngleDiff) {
                    return true;
                }
            }
        }
        return false;
    }

    private void calculateLineFollowingSegments() {
        BlobMapper mapper = new BlobMapper(_blobs);
        int lineFollowingSampleRate = FeatureConstants.CONTOUR_FOLLOW_SAMPLE_RATE;
        int coneAngle = FeatureConstants.CONE_ANGLE;
        int coneLength = FeatureConstants.CONE_LENGTH;

        for (Blob blob : _blobs) {
            boolean foundFirstPoint = true;
            int segmentCount = 1;

            for (int i = 0; i < 2; i++) {
                Blob lastBlobAdded = blob;

                while (lastBlobAdded != null) {
                    int[] contourX = lastBlobAdded.getLineX();
                    int[] contourY = lastBlobAdded.getLineY();
                    int end = lastBlobAdded.getLength();
                    int sampleRate = end >= lineFollowingSampleRate ? lineFollowingSampleRate : end;

                    int x1 = foundFirstPoint ? contourX[end - sampleRate] : contourX[sampleRate];
                    int y1 = foundFirstPoint ? contourY[end - sampleRate] : contourY[sampleRate];
                    int startX = foundFirstPoint ? contourX[end] : contourX[0];
                    int startY = foundFirstPoint ? contourY[end] : contourY[0];
                    double baseAngle = Utils.getAngle(x1, startX, y1, startY);
                    int offset = 0;

                    while (offset < coneAngle) {
                        double currentAngle = baseAngle + (double) offset;
                        double currentAngleRAD = Math.PI * currentAngle / 180;
                        int endX = (int) ((double) startX + (double) coneLength * Math.cos(currentAngleRAD));
                        int endY = (int) ((double) startY + (double) coneLength * Math.sin(currentAngleRAD));
                        List<Point> points = Utils.getBresenhamPoints4Connected(startX, startY, endX, endY);

                        lastBlobAdded = this.getLineFollowingBlob(baseAngle, points, mapper, true);
                        if (lastBlobAdded != null) {
                            foundFirstPoint = true;
                            segmentCount++;
                            break;
                        } else {
                            lastBlobAdded = this.getLineFollowingBlob(baseAngle, points, mapper, false);
                            if (lastBlobAdded != null) {
                                foundFirstPoint = false;
                                segmentCount++;
                                break;
                            }
                        }

                        offset = Utils.increaseConeOffset(offset);
                    }
                }
                foundFirstPoint = false;
            }
            blob.setLineFollowingElements(segmentCount);
        }
    }

    private Blob getLineFollowingBlob(double baseAngle, List<Point> points, BlobMapper mapper, boolean lookForFirstPoint) {
        int maxAngleDiffCones = FeatureConstants.MAX_ANGLE_DIFF_CONES;
        int lineFollowingSampleRate = FeatureConstants.CONTOUR_FOLLOW_SAMPLE_RATE;

        for (Point p : points) {
            Blob candidate = lookForFirstPoint ? mapper.getBlobWithGivenFirstPoint(p) : mapper.getBlobWithGivenLastPoint(p);
            if (candidate != null) {
                int[] contourX = candidate.getLineX();
                int[] contourY = candidate.getLineY();
                int end = candidate.getLength();
                int sampleRate = end >= lineFollowingSampleRate ? lineFollowingSampleRate : end;

                double angle = lookForFirstPoint
                        ? Utils.getAngle(contourX[0], contourX[sampleRate], contourY[0], contourY[sampleRate])
                        : Utils.getAngle(contourX[end], contourX[end - sampleRate], contourY[end], contourY[end - sampleRate]);

                if (Utils.getAngleDiff(baseAngle, angle) < (double) maxAngleDiffCones) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private void fillFeatureTable() {
        ResultsTable rt = Analyzer.getResultsTable();
        if (rt == null) {
            rt = new ResultsTable();
            Analyzer.setResultsTable(rt);
        }

        for (Blob blob : _blobs) {
            rt.incrementCounter();

            rt.addValue("Length", blob.getLength());
            rt.addValue("Length %", 0);
            rt.addValue("isInCluster", String.valueOf(blob.isInCluster()));
            rt.addValue("Cluster %", 0);
            rt.addValue("Parallel Coverage", blob.getParallelCoverage());
            rt.addValue("Parallel %", 0);
            rt.addValue("Line Following segments", blob.getLineFollowingElements());
            rt.addValue("Line Following %", 0);
            rt.addValue("Overall %", 0);
        }
        rt.show("Results");
    }

    private boolean isInImageRange(int x, int y) {
        return x > -1 && x < _width && y > -1 && y < _height;
    }
}
