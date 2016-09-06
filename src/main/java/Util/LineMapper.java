package Util;

import blob.Line;

import java.awt.*;
import java.util.*;
import java.util.List;

public class LineMapper {

    private Map<Point, List<Line>> _lineMap;

    public LineMapper(List<Line> lines) {
        _lineMap = new HashMap<Point, List<Line>>();

        List<Line> linesAtCurrentPosition;
        for (Line line : lines) {
            List<Point> points = line.getPoints();
            for (Point p : points) {
                linesAtCurrentPosition = _lineMap.get(p);
                if (linesAtCurrentPosition == null) {
                    linesAtCurrentPosition = new ArrayList<Line>();
                }
                linesAtCurrentPosition.add(line);
                _lineMap.put(p, linesAtCurrentPosition);
            }
        }
    }

    public Map<Point, List<Line>> getIntersectedLines(List<Point> points) {
        Map<Point, List<Line>> result = new HashMap<Point, List<Line>>();
        List<Line> intersectedLines;

        for (Point p : points) {
            intersectedLines = _lineMap.get(p);
            if (intersectedLines != null) {
                result.put(p, intersectedLines);
            }
        }

        return result;
    }
}
