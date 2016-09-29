package calculation;

public interface EvaluationConstants {
    double MIN_PROBABILITY = 0.2;
    double EVEN_PROBABILITY = 0.5;
    double MAX_PROBABILITY = 0.8;

    double THRESHOLD = 0.6;
    EvaluationValues lengthValues = new EvaluationValues(30, 80, 1);
    EvaluationValues clusteringValues = new EvaluationValues(0, 0, 0);
    EvaluationValues parallelCoverageValues = new EvaluationValues(0.2, 0.5, 3);
    EvaluationValues lineFollowingValues = new EvaluationValues(2, 4, 3);

    String COLORED = "test-pale";
    String FILE_NAME = "basic2";
}