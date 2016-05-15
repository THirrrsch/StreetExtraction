import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    public Utils() {
    }

    public static List<Point> getBresenhamPoints(int x1, int y1, int x2, int y2) {
        List<Point> result = new ArrayList<Point>();
        byte xIncrement = 1;
        byte yIncrement = 1;
        int dy = 2 * (y2 - y1);
        int dx = 2 * (x1 - x2);
        if(x1 > x2) {
            xIncrement = -1;
            dx = -dx;
        }

        if(y1 > y2) {
            yIncrement = -1;
            dy = -dy;
        }

        int e = 2 * dy + dx;
        int x = x1;
        int y = y1;
        if(dy < -dx) {
            while(x != x2 + 1) {
                e += dy;
                if(e > 0) {
                    e += dx;
                    y += yIncrement;
                }

                result.add(new Point(x, y));
                x += xIncrement;
            }
        } else {
            int tmp = -dx;
            dx = -dy;
            dy = tmp;

            for(e = 2 * tmp + dx; y != y2 + 1; y += yIncrement) {
                e += dy;
                if(e > 0) {
                    e += dx;
                    x += xIncrement;
                }

                result.add(new Point(x, y));
            }
        }

        List<Point> fourConnectedResult = new ArrayList<Point>();

        for(int i = 0; i < result.size() - 1; ++i) {
            Point current = result.get(i);
            Point next = result.get(i + 1);
            fourConnectedResult.add(current);
            if(next.x != current.x && next.y != current.y) {
                if((next.x >= current.x || next.y <= current.y) && (next.x <= current.x || next.y >= current.y)) {
                    fourConnectedResult.add(new Point(next.x, current.y));
                } else {
                    fourConnectedResult.add(new Point(current.x, next.y));
                }
            }

            fourConnectedResult.add(next);
        }

        return fourConnectedResult;
    }
}
