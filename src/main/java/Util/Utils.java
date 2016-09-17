package Util;

import blob.Blob;
import blob.ManyBlobs;
import ij.process.ImageProcessor;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    private Utils() {
    }

    public static List<Point> getBresenhamPoints8Connected(int x1, int y1, int x2, int y2) {
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

        if (result.size() > 0) {
            if (result.get(0).x == x1 && result.get(0).y == y1) {
                result.remove(0);
            }
        }

        return result;
    }

    public static List<Point> getBresenhamPoints4Connected(int x1, int y1, int x2, int y2) {
        List<Point> eightConnectedResult = getBresenhamPoints8Connected(x1, y1, x2, y2);

        List<Point> result = new ArrayList<Point>();

        for(int i = 0; i < eightConnectedResult.size() - 1; ++i) {
            Point current = eightConnectedResult.get(i);
            Point next = eightConnectedResult.get(i + 1);
            result.add(current);
            if(next.x != current.x && next.y != current.y) {
                if((next.x >= current.x || next.y <= current.y) && (next.x <= current.x || next.y >= current.y)) {
                    result.add(new Point(next.x, current.y));
                } else {
                    result.add(new Point(current.x, next.y));
                }
            }

            result.add(next);
        }

        if (result.size() > 0) {
            if (result.get(0).x == x1 && result.get(0).y == y1) {
                result.remove(0);
            }
        }

        return result;
    }

    public static void drawCentroidLine(List<Blob> currentLineBlobs, ImageProcessor processor) {
        for (int i = 0; i < currentLineBlobs.size() - 1; i++) {
            Point start = currentLineBlobs.get(i).getCentroid();
            Point end = currentLineBlobs.get(i + 1).getCentroid();
            processor.drawLine(start.x, start.y, end.x, end.y);
        }
    }

    public static void printBlobsToCSV(ManyBlobs blobs) {
        try
        {
            FileWriter writer = new FileWriter("C:\\Users\\Hirsch\\Desktop\\test.csv");

            for (Blob blob : blobs) {
                int end = blob.getOuterContour().npoints / 2;
                int[] contourX = blob.getLineX();
                int[] contourY = blob.getLineY();
                double angle = Utils.getAngle(contourX[0], contourX[end], contourY[0], contourY[end]);

                writer.append(String.valueOf(end));
                writer.append(' ');
                writer.append(String.valueOf(angle));
                writer.append('\n');
            }

            writer.flush();
            writer.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
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
        return getAngle(endX - startX, endY - startY);
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