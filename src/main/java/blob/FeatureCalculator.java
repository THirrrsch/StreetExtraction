package blob;

import Util.BlobMapper;
import Util.Constants;
import Util.LineMapper;
import Util.Utils;
import com.sun.tools.internal.jxc.ap.Const;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ImageProcessor;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class FeatureCalculator {

    private ManyBlobs _blobs;

    public FeatureCalculator(ManyBlobs blobs) {
        _blobs = blobs;
    }

    public void calculateFeatures() {
        this.checkClustering();
        this.calculateParallelCoverage();
        this.calculateLineFollowingSegments();
    }

    private void checkClustering() {
        double epsilon = Constants.DBSCAN_EPSILON;
        int minPts = Constants.DBSCAN_MINPTS;

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

        int sampleRate = Constants.SAMPLE_RATE;
        int minStreetWidth = Constants.MIN_STREET_WIDTH;
        int maxStreetWidth = Constants.MAX_STREET_WIDTH;

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

                List<Point> pointsClock = Utils.getBresenhamPoints(baseX + vecXStart, baseY + vecYStart, baseX + vecXEnd, baseY + vecYEnd);
                List<Point> pointsCounterclock = Utils.getBresenhamPoints(baseX - vecXStart, baseY - vecYStart, baseX - vecXEnd, baseY - vecYEnd);

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
                blob.setParallelCoverage(maxParallelCount / maxSegments);
            } else {
                blob.setParallelCoverage(0);
            }
        }
    }

    private boolean doesParallelLineExist(double currentAngle, List<Point> points, LineMapper mapper) {
        Map<Point, List<Line>> intersectedLines = mapper.getIntersectedLines(points);
        int maxAngleDiff = Constants.MAX_ANGLE_DIFF;

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
        int lineFollowingSampleRate = Constants.CONTOUR_FOLLOW_SAMPLE_RATE;
        int coneAngle = Constants.CONE_ANGLE;
        int coneLength = Constants.CONE_LENGTH;

        for (Blob blob : _blobs) {
            boolean foundFirstPoint = true;
            int segmentCount = 1;

            for (int i = 0; i < 2; i++) {
                Blob lastBlobAdded = blob;

                while (lastBlobAdded != null) {
                    int[] contourX = lastBlobAdded.getLineX();
                    int[] contourY = lastBlobAdded.getLineY();
                    int end = lastBlobAdded.getLength();

                    int x1 = foundFirstPoint ? contourX[lineFollowingSampleRate] : contourX[end - lineFollowingSampleRate];
                    int y1 = foundFirstPoint ? contourY[lineFollowingSampleRate] : contourY[end - lineFollowingSampleRate];
                    int startX = foundFirstPoint ? contourX[0] : contourX[end];
                    int startY = foundFirstPoint ? contourY[0] : contourY[end];
                    double baseAngle = Utils.getAngle(x1, startX, y1, startY);
                    int offset = 0;

                    while (offset < coneAngle) {
                        double currentAngle = baseAngle + (double) offset;
                        double currentAngleRAD = Math.PI * currentAngle / 180;
                        int endX = (int) ((double) startX + (double) coneLength * Math.cos(currentAngleRAD));
                        int endY = (int) ((double) startY + (double) coneLength * Math.sin(currentAngleRAD));
                        List<Point> points = Utils.getBresenhamPoints(startX, startY, endX, endY);

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
        int maxAngleDiffCones = Constants.MAX_ANGLE_DIFF_CONES;
        int lineFollowingSampleRate = Constants.CONTOUR_FOLLOW_SAMPLE_RATE;

        for (Point p : points) {
            Blob candidate = lookForFirstPoint ? mapper.getBlobWithGivenFirstPoint(p) : mapper.getBlobWithGivenLastPoint(p);
            if (candidate != null) {
                int[] contourX = candidate.getLineX();
                int[] contourY = candidate.getLineY();
                int end = candidate.getLength();
                double angle = lookForFirstPoint
                        ? Utils.getAngle(contourX[0], contourX[lineFollowingSampleRate], contourY[0], contourY[lineFollowingSampleRate])
                        : Utils.getAngle(contourX[end], contourX[end - lineFollowingSampleRate], contourY[end], contourY[end - lineFollowingSampleRate]);

                if (Utils.getAngleDiff(baseAngle, angle) < (double) maxAngleDiffCones) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
