package calculation;

import blob.Blob;
import blob.ManyBlobs;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeatureEvaluator {

    private final ManyBlobs _inputBlobs;

    private ImagePlus _resultStack;
    private ImageProcessor _resultProcessor;

    private EvaluationValues _lengthValues;
    private EvaluationValues _clusteringValues;
    private EvaluationValues _parallelCoverageValues;
    private EvaluationValues _lineFollowingValues;

    private double _threshold;

    public FeatureEvaluator(ManyBlobs blobs, ImagePlus resultStack) {
        _inputBlobs = blobs;
        _resultStack = resultStack;
        _resultProcessor = resultStack.getStack().getProcessor(2);

        _lengthValues = EvaluationConstants.lengthValues;
        _clusteringValues = EvaluationConstants.clusteringValues;
        _parallelCoverageValues = EvaluationConstants.parallelCoverageValues;
        _lineFollowingValues = EvaluationConstants.lineFollowingValues;
        _threshold = EvaluationConstants.THRESHOLD;
    }

    public EvaluationValues getLengthValues() {
        return _lengthValues;
    }

    public EvaluationValues getClusteringValues() {
        return _clusteringValues;
    }

    public EvaluationValues getParallelCoverageValues() {
        return _parallelCoverageValues;
    }

    public EvaluationValues getLineFollowingValues() {
        return _lineFollowingValues;
    }

    public double getThreshold() {
        return _threshold;
    }

    public void setLengthValues(EvaluationValues _lengthValues) {
        this._lengthValues = _lengthValues;
    }

    public void setClusteringValues(EvaluationValues _clusteringValues) {
        this._clusteringValues = _clusteringValues;
    }

    public void setParallelCoverageValues(EvaluationValues _parallelCoverageValues) {
        this._parallelCoverageValues = _parallelCoverageValues;
    }

    public void setLineFollowingValues(EvaluationValues _lineFollowingValues) {
        this._lineFollowingValues = _lineFollowingValues;
    }

    public void setThreshold(double _threshold) {
        this._threshold = _threshold;
    }

    public void evaluate() {
        Map<Blob, Double> lengthGrades = this.evaluateLength();
        Map<Blob, Double> clusteringGrades = this.evalauteClusteringResults();
        Map<Blob, Double> parallelCoverageGrades = this.evaluateParallelCoverage();
        Map<Blob, Double> lineFollowingGrades = this.evaluateLineFollowingSegments();

        Map<Blob, Double> overallGrades = this.evaluateGrades(lengthGrades, clusteringGrades, parallelCoverageGrades, lineFollowingGrades);

        List<Blob> resultBlobs = this.evaluateOverallResult(overallGrades);

        int width = _resultProcessor.getWidth();
        int height = _resultProcessor.getHeight();
        byte[] pixels = (byte[]) _resultProcessor.getPixels();

        for (int x = 0; x < width - 1; x++) {
            for (int y = 0; y < height - 1; y++) {
                pixels[y * width + x] = (byte)255;
            }
        }

        for (Blob blob : _inputBlobs) {
            blob.draw(_resultProcessor, 1, Color.LIGHT_GRAY);
        }

        for (Blob blob : resultBlobs) {
            blob.draw(_resultProcessor);
        }

        _resultStack.updateAndDraw();
    }

    private Map<Blob, Double> evaluateLength() {
        Map<Blob, Double> result = new HashMap<Blob, Double>();

        for (Blob blob : _inputBlobs) {
            int length = blob.getLength();
            double prohability;
            if (length <= _lengthValues.INFIMUM) {
                prohability = EvaluationConstants.MIN_PROBABILITY;
            } else if (length <= _lengthValues.SUPREMUM) {
                prohability = EvaluationConstants.EVEN_PROBABILITY;
            } else {
                prohability = EvaluationConstants.MAX_PROBABILITY;
            }

            result.put(blob, prohability);
        }

        return result;
    }

    private Map<Blob, Double> evalauteClusteringResults() {
        Map<Blob, Double> result = new HashMap<Blob, Double>();

        for (Blob blob : _inputBlobs) {
            if (blob.isInCluster()) {
                result.put(blob, EvaluationConstants.MIN_PROBABILITY);
            } else {
                result.put(blob, EvaluationConstants.EVEN_PROBABILITY);
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
                probability = EvaluationConstants.MIN_PROBABILITY;
            } else if (parallelCoverage <= _parallelCoverageValues.SUPREMUM) {
                probability = EvaluationConstants.EVEN_PROBABILITY;
            } else {
                probability = EvaluationConstants.MAX_PROBABILITY;
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
                probability = EvaluationConstants.MIN_PROBABILITY;
            } else if (lineFollowingSegmentCount <= _lineFollowingValues.SUPREMUM) {
                probability = EvaluationConstants.EVEN_PROBABILITY;
            } else {
                probability = EvaluationConstants.MAX_PROBABILITY;
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

            int weightSum = _lengthValues.WEIGHT + _clusteringValues.WEIGHT + _parallelCoverageValues.WEIGHT + _lineFollowingValues.WEIGHT;
            double overAllGrade = (lengthGrade + clusteringGrade + parallelCoverageGrade + lineFollowingGrade) / weightSum;
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
