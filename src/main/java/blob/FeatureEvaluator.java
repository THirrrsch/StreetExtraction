package blob;

import Util.EvaluationConstants;
import Util.EvaluationValues;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeatureEvaluator {

    private final ManyBlobs _inputBlobs;
    private final int _width;
    private final int _height;

    EvaluationValues _lengthValues;
    EvaluationValues _clusteringValues;
    EvaluationValues _parallelCoverageValues;
    EvaluationValues _lineFollowingValues;

    private int _weightSum;
    private double _threshold;

    public FeatureEvaluator(ManyBlobs blobs, int width, int height) {
        _inputBlobs = blobs;
        _width = width;
        _height = height;

        _lengthValues = EvaluationConstants.lengthValues;
        _clusteringValues = EvaluationConstants.clusteringValues;
        _parallelCoverageValues = EvaluationConstants.parallelCoverageValues;
        _lineFollowingValues = EvaluationConstants.lineFollowingValues;

        _weightSum = _lengthValues.WEIGHT + _clusteringValues.WEIGHT + _parallelCoverageValues.WEIGHT + _lineFollowingValues.WEIGHT;
        _threshold = EvaluationConstants.THRESHOLD;
    }

    public ImagePlus getEvaluatedResult() {
        Map<Blob, Double> lengthGrades = this.evaluateLength();
        Map<Blob, Double> clusteringGrades = this.evalauteClusteringResults();
        Map<Blob, Double> parallelCoverageGrades = this.evaluateParallelCoverage();
        Map<Blob, Double> lineFollowingGrades = this.evaluateLineFollowingSegments();

        Map<Blob, Double> overallGrades = this.evaluateGrades(lengthGrades, clusteringGrades, parallelCoverageGrades, lineFollowingGrades);

        List<Blob> resultBlobs = this.evaluateOverallResult(overallGrades);

        ImagePlus resultImage = NewImage.createByteImage("result image", _width, _height, 1, 4);
        ImageProcessor resultProcessor = resultImage.getProcessor();
        for (Blob blob : resultBlobs) {
            blob.draw(resultProcessor);
        }

        return resultImage;
    }

    private Map<Blob, Double> evaluateLength() {
        Map<Blob, Double> result = new HashMap<Blob, Double>();

        for (Blob blob : _inputBlobs) {
            int length = blob.getLength();
            double prohability;
            if (length <= _lengthValues.INFIMUM) {
                prohability = EvaluationConstants.MIN_PROHABILITY;
            } else if (length <= _lengthValues.SUPREMUM) {
                prohability = EvaluationConstants.MID_PROHABILITY;
            } else {
                prohability = EvaluationConstants.MAX_PROHABILITY;
            }

            result.put(blob, prohability);
        }

        return result;
    }

    private Map<Blob, Double> evalauteClusteringResults() {
        Map<Blob, Double> result = new HashMap<Blob, Double>();

        for (Blob blob : _inputBlobs) {
            if (blob.isInCluster()) {
                result.put(blob, EvaluationConstants.MIN_PROHABILITY);
            } else {
                result.put(blob, EvaluationConstants.MID_PROHABILITY);
            }
        }

        return result;
    }

    private Map<Blob, Double> evaluateParallelCoverage() {
        Map<Blob, Double> result = new HashMap<Blob, Double>();

        for (Blob blob : _inputBlobs) {
            double parallelCoverage = blob.getParallelCoverage();
            double probability;
            if (parallelCoverage <= _parallelCoverageValues.INFIMUM) {
                probability = EvaluationConstants.MIN_PROHABILITY;
            } else if (parallelCoverage <= _parallelCoverageValues.SUPREMUM) {
                probability = EvaluationConstants.MID_PROHABILITY;
            } else {
                probability = EvaluationConstants.MAX_PROHABILITY;
            }

            result.put(blob, probability);
        }

        return result;
    }

    private Map<Blob, Double> evaluateLineFollowingSegments() {
        Map<Blob, Double> result = new HashMap<Blob, Double>();

        for (Blob blob : _inputBlobs) {
            int lineFollowingSegmentCount = blob.getLineFollowingElements();
            double probability;
            if (lineFollowingSegmentCount <= _lineFollowingValues.INFIMUM) {
                probability = EvaluationConstants.MIN_PROHABILITY;
            } else if (lineFollowingSegmentCount <= _lineFollowingValues.SUPREMUM) {
                probability = EvaluationConstants.MID_PROHABILITY;
            } else {
                probability = EvaluationConstants.MAX_PROHABILITY;
            }

            result.put(blob, probability);
        }

        return result;
    }

    private Map<Blob, Double> evaluateGrades(Map<Blob, Double> lengthGrades, Map<Blob, Double> clusteringGrades, Map<Blob, Double> parallelCoverageGrades, Map<Blob, Double> lineFollowingGrades) {
        Map<Blob, Double> result = new HashMap<Blob, Double>();

        for (Blob blob : _inputBlobs) {
            double lengthGrade = lengthGrades.get(blob);
            double clusteringGrade = clusteringGrades.get(blob);
            double parallelCoverageGrade = parallelCoverageGrades.get(blob);
            double lineFollowingGrade = lineFollowingGrades.get(blob);

            lengthGrade *= _lengthValues.WEIGHT;
            clusteringGrade *= _clusteringValues.WEIGHT;
            parallelCoverageGrade *= _parallelCoverageValues.WEIGHT;
            lineFollowingGrade *= _lineFollowingValues.WEIGHT;

            double overAllGrade = (lengthGrade + clusteringGrade + parallelCoverageGrade + lineFollowingGrade) / _weightSum;
            result.put(blob, overAllGrade);
        }

        return result;
    }

    private List<Blob> evaluateOverallResult(Map<Blob, Double> grades) {
        List<Blob> result = new ArrayList<Blob>();

        for (Blob blob : _inputBlobs) {
            if (grades.get(blob) > _threshold) {
                result.add(blob);
            }
        }

        return result;
    }










}
