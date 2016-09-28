package calculation;

public class EvaluationResult {
    private double lengthResult;
    private double clusteringResult;
    private double parallelResult;
    private double lineFollowingResult;

    private double overallResult;

    public EvaluationResult(double lengthResult, double clusteringResult, double parallelResult, double lineFollowingResult, double overallResult) {
        this.lengthResult = lengthResult;
        this.clusteringResult = clusteringResult;
        this.parallelResult = parallelResult;
        this.lineFollowingResult = lineFollowingResult;
        this.overallResult = overallResult;
    }

    public double getLengthResult() {
        return lengthResult;
    }

    public double getClusteringResult() {
        return clusteringResult;
    }

    public double getParallelResult() {
        return parallelResult;
    }

    public double getLineFollowingResult() {
        return lineFollowingResult;
    }

    public double getOverallResult() {
        return overallResult;
    }

}
