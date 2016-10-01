package calculation;

import blob.Blob;
import blob.ManyBlobs;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.*;
import java.util.List;

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
        _resultProcessor = resultStack.getStack().getProcessor(resultStack.getStack().getSize());

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
        Map<Blob, EvaluationResult> results = this.evaluateFeatures();
        List<Blob> resultBlobs = this.EvaluateResults(results);

        ResultsTable rt = Analyzer.getResultsTable();

        int width = _resultProcessor.getWidth();
        int height = _resultProcessor.getHeight();
        byte[] pixels = (byte[]) _resultProcessor.getPixels();

        for (int x = 0; x < width - 1; x++) {
            for (int y = 0; y < height - 1; y++) {
                pixels[y * width + x] = (byte) 255;
            }
        }

        for (int i = 0; i < _inputBlobs.size(); i++) {
            Blob blob = _inputBlobs.get(i);
            EvaluationResult blobResult = results.get(blob);
            //blob.draw(_resultProcessor, 1, Color.LIGHT_GRAY);

            rt.setValue(1, i, blobResult.getLengthResult());
            rt.setValue(3, i, blobResult.getClusteringResult());
            rt.setValue(5, i, blobResult.getParallelResult());
            rt.setValue(7, i, blobResult.getLineFollowingResult());
            rt.setValue(8, i, blobResult.getOverallResult());
        }

        for (Blob blob : resultBlobs) {
            blob.draw(_resultProcessor);
            AbstractMap.SimpleEntry<Blob, Point> leftConnection = blob.getLeftConnection();
            AbstractMap.SimpleEntry<Blob, Point> rightConnection = blob.getRightConnection();

            if (leftConnection != null && resultBlobs.contains(leftConnection.getKey())) {
                _resultProcessor.drawLine(blob.getLineX()[0], blob.getLineY()[0], leftConnection.getValue().x, leftConnection.getValue().y);
            }

            if (rightConnection != null && resultBlobs.contains(rightConnection.getKey())) {
                _resultProcessor.drawLine(blob.getLineX()[blob.getLength()], blob.getLineY()[blob.getLength()], rightConnection.getValue().x, rightConnection.getValue().y);
            }
        }

        rt.show("Results");
        _resultStack.updateAndDraw();
    }

    private Map<Blob, EvaluationResult> evaluateFeatures() {
        Map<Blob, EvaluationResult> result = new HashMap<Blob, EvaluationResult>();

        for (Blob blob : _inputBlobs) {
            double lengthResult = this.getLengthResult(blob);
            double clusteringResult = this.getClusteringResult(blob);
            double parallelResult = this.getParallelResult(blob);
            double lineFollowingResult = this.getLineFollowingResult(blob);

            double overallResult = this.getOverallResult(lengthResult, clusteringResult, parallelResult, lineFollowingResult);
            EvaluationResult currentResult = new EvaluationResult(lengthResult, clusteringResult, parallelResult, lineFollowingResult, overallResult);

            result.put(blob, currentResult);
        }

        return result;
    }

    private double getLengthResult(Blob blob) {
        int length = blob.getLength();
        if (length < _lengthValues.INFIMUM) {
            return EvaluationConstants.MIN_PROBABILITY;
        } else if (length < _lengthValues.SUPREMUM) {
            return EvaluationConstants.EVEN_PROBABILITY;
        } else {
            return EvaluationConstants.MAX_PROBABILITY;
        }
    }

    private double getClusteringResult(Blob blob) {
        if (blob.isInCluster()) {
            return EvaluationConstants.MIN_PROBABILITY;
        } else {
            return EvaluationConstants.EVEN_PROBABILITY;
        }

    }

    private double getParallelResult(Blob blob) {
        double parallelCoverage = blob.getParallelCoverage();
        if (parallelCoverage < _parallelCoverageValues.INFIMUM) {
            return EvaluationConstants.MIN_PROBABILITY;
        } else if (parallelCoverage < _parallelCoverageValues.SUPREMUM) {
            return EvaluationConstants.EVEN_PROBABILITY;
        } else {
            return EvaluationConstants.MAX_PROBABILITY;
        }
    }

    private double getLineFollowingResult(Blob blob) {
        int lineFollowingSegmentCount = blob.getLineFollowingElements();
        if (lineFollowingSegmentCount < _lineFollowingValues.INFIMUM) {
            return EvaluationConstants.MIN_PROBABILITY;
        } else if (lineFollowingSegmentCount < _lineFollowingValues.SUPREMUM) {
            return EvaluationConstants.EVEN_PROBABILITY;
        } else {
            return EvaluationConstants.MAX_PROBABILITY;
        }
    }

    private double getOverallResult(double lengthResult, double clusteringResult, double parallelResult, double lineFollowingResult) {
        lengthResult *= _lengthValues.WEIGHT;
        clusteringResult *= _clusteringValues.WEIGHT;
        parallelResult *= _parallelCoverageValues.WEIGHT;
        lineFollowingResult *= _lineFollowingValues.WEIGHT;

        int weightSum = _lengthValues.WEIGHT + _clusteringValues.WEIGHT + _parallelCoverageValues.WEIGHT + _lineFollowingValues.WEIGHT;
        return ((lengthResult + clusteringResult + parallelResult + lineFollowingResult) / weightSum);
    }

    private List<Blob> EvaluateResults(Map<Blob, EvaluationResult> results) {
        List<Blob> result = new ArrayList<Blob>();

        for (Blob blob : _inputBlobs) {
            if (results.get(blob).getOverallResult() > _threshold) {
                result.add(blob);
            }
        }

        return result;
    }


}
