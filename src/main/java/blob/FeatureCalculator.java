package blob;

import Util.Constants;
import Util.LineMapper;
import Util.Utils;
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
        int maxAngleDiff = Constants.MAX_ANGLE_DIFF;

        for (Blob blob : _blobs) {
            int parallelSegmentCount = 0;
            int[] contourX = blob.getLineX();
            int[] contourY = blob.getLineY();
            int max = blob.getLength() - sampleRate;

            for(int i = 0; i <= max; i += sampleRate) {
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

                Map<Point, List<Line>> intersectedLines = mapper.getIntersectedLines(pointsClock);
                intersectedLines.putAll(mapper.getIntersectedLines(pointsCounterclock));

                boolean parallelLineFound = false;
                for (Map.Entry<Point, List<Line>> entry : intersectedLines.entrySet()) {
                    if (!parallelLineFound) {
                        Point p = entry.getKey();
                        List<Line> lines = entry.getValue();
                        for (Line line : lines) {
                            double intersectedLineAngle = line.getAngleAt(p);
                            if (Utils.getAngleDiff(currentAngle, intersectedLineAngle) <= maxAngleDiff) {
                                parallelLineFound = true;
                            }
                        }
                    }
                }

                if (parallelLineFound) {
                    parallelSegmentCount++;
                }
            }

            if (parallelSegmentCount > 0) {
                int maxSegments = blob.getLength() / sampleRate;
                blob.setParallelCoverage(parallelSegmentCount / maxSegments);
            } else {
                blob.setParallelCoverage(0);
            }
        }
    }

    private void calculateLineFollowingSegments() {

    }
}
