package Util;

public interface EvaluationConstants {
    double MIN_PROHABILITY = 0.2;
    double MAX_PROHABILITY = 0.8;
    double MID_PROHABILITY = 0.5;

    EvaluationValues lengthValues = new EvaluationValues(30, 80, 1);
    EvaluationValues clusteringValues = new EvaluationValues(0, 0, 1);
    EvaluationValues parallelCoverageValues = new EvaluationValues(0.2, 0.8, 1);
    EvaluationValues lineFollowingValues = new EvaluationValues(2, 5, 1);

    double THRESHOLD = 0.4;
}