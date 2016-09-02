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

    public Set<Line> getIntersectedLines(List<Point> points) {
        Set<Line> result = new HashSet<Line>();
        List<Line> linesAtCurrentPosition;

        for (Point p : points) {
            linesAtCurrentPosition = _lineMap.get(p);
            if (linesAtCurrentPosition != null) {
                result.addAll(linesAtCurrentPosition);
            }
        }

        return result;
    }
}
