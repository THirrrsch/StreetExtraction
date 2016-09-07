package Util;

import blob.Blob;
import ij.process.ImageProcessor;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    private Utils() {
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

        if (fourConnectedResult.size() > 0) {
            if (fourConnectedResult.get(0).x == x1 && fourConnectedResult.get(0).y == y1) {
                fourConnectedResult.remove(0);
            }
        }

        return fourConnectedResult;
    }

    public static void drawCentroidLine(List<Blob> currentLineBlobs, ImageProcessor processor) {
        for (int i = 0; i < currentLineBlobs.size() - 1; i++) {
            Point start = currentLineBlobs.get(i).getCentroid();
            Point end = currentLineBlobs.get(i + 1).getCentroid();
            processor.drawLine(start.x, start.y, end.x, end.y);
        }
    }

    public static int increaseConeOffset(int offset) {
        if (offset == 0) {
            return -1;
        } else if (offset < 0) {
            return (offset * -1);
        } else {
            ++offset;
            return (offset * -1);
        }
    }

    // 0 - 360
    public static double getAngle(int startX, int endX, int startY, int endY) {
        return getAngleDiff(endX - startX, endY - startY);
    }

    // 0 - 360
    public static double getAngle(int dX, int dY) {
        double angleRAD = Math.atan2((double)(dY), (double)(dX));
        return angleRAD * 180 / Math.PI;
    }

    public static double getAngleDiff(double alpha, double beta) {
        double diff = Math.abs(beta - alpha) % 360;
        return diff < 180 ? diff : 360 - diff;
    }
}