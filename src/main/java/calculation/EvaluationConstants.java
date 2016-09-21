package calculation;

public interface EvaluationConstants {
    double MIN_PROBABILITY = 0.2;
    double EVEN_PROBABILITY = 0.5;
    double MAX_PROBABILITY = 0.8;

    double THRESHOLD = 0.4;
    EvaluationValues lengthValues = new EvaluationValues(30, 80, 1);
    EvaluationValues clusteringValues = new EvaluationValues(0, 0, 0);
    EvaluationValues parallelCoverageValues = new EvaluationValues(0.2, 0.8, 1);
    EvaluationValues lineFollowingValues = new EvaluationValues(2, 5, 1);
}