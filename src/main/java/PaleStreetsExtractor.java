import blob.Blob;
import blob.ManyBlobs;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ImageProcessor;
import java.awt.Point;
import java.util.Iterator;
import java.util.List;

public class PaleStreetsExtractor implements StreetsExtractor {
    private final boolean _drawCones;
    private ImagePlus _image;
    private ImagePlus _longLineImage;
    private final ManyBlobs _allBlobs;
    private final int _width;
    private final int _height;
    private final int _sampleRate;
    private final int _maxAngleDiff;
    private final int _minContourLength;
    private final int _lineFollowingSampleRate;
    private final int _coneAngle;
    private final int _coneLength;
    private final int _maxAngleDiffCone;

    public PaleStreetsExtractor(ImagePlus image, int sampleRate, int maxAngleDiff, int minContourLength, int lineFollowingSampleRate, int coneAngle, int coneLength, int maxAngleDiffCone, boolean drawCones) {
        this._image = image;
        this._sampleRate = sampleRate;
        this._maxAngleDiff = maxAngleDiff;
        this._minContourLength = minContourLength;
        this._lineFollowingSampleRate = lineFollowingSampleRate;
        this._coneAngle = coneAngle;
        this._coneLength = coneLength;
        this._maxAngleDiffCone = maxAngleDiffCone;
        this._drawCones = drawCones;
        this._width = this._image.getWidth();
        this._height = this._image.getHeight();
        this._allBlobs = new ManyBlobs(this._image);
        this._allBlobs.findConnectedComponents();
    }

    public ImagePlus process() {
        ManyBlobs straighLineBlobs = this.getStraightLineBlobs();
        ManyBlobs longBlobs = this.getLongBlobs(straighLineBlobs);
        return this.getStreetImageByFollowingLines(longBlobs);
    }

    private ManyBlobs getStraightLineBlobs() {
        double angleCurrent = 0;
        double angleOld;
        double angleDiff;
        ImagePlus straightLineImage = NewImage.createByteImage("straight line image", this._image.getWidth(), this._image.getHeight(), 1, 4);
        ImageProcessor straightLineProcessor = straightLineImage.getProcessor();
        byte[] straightLinePixels = (byte[]) straightLineProcessor.getPixels();

        for (Blob blob : _allBlobs) {
            if (blob.getOuterContour().npoints >= this._sampleRate) {
                int[] contourX = blob.getOuterContour().xpoints;
                int[] contourY = blob.getOuterContour().ypoints;
                int max = blob.getOuterContour().npoints - this._sampleRate;

                for(int i = 0; i <= max; i += this._sampleRate) {
                    int startX = contourX[i];
                    int startY = contourY[i];
                    int endX = contourX[i + this._sampleRate - 1];
                    int endY = contourY[i + this._sampleRate - 1];

                    if(i == 0) {
                        angleCurrent = this.getAngle(startX, endX, startY, endY);
                    } else {
                        angleOld = angleCurrent;
                        angleCurrent = this.getAngle(startX, endX, startY, endY);
                        angleDiff = this.getAngleDiff(angleCurrent, angleOld);

                        if(angleDiff < this._maxAngleDiff) {
                            for(int j = 0; j < this._sampleRate; ++j) {
                                straightLinePixels[contourY[i + j] * this._width + contourX[i + j]] = 0;
                            }
                        }
                    }
                }
            }
        }

        straightLineImage.show();
        straightLineImage.updateAndDraw();
        ManyBlobs result = new ManyBlobs(straightLineImage);
        result.findConnectedComponents();
        return result;
    }

    private ManyBlobs getLongBlobs(ManyBlobs inputBlobs) {
        this._longLineImage = NewImage.createByteImage("long line image", this._image.getWidth(), this._image.getHeight(), 1, 4);
        ImageProcessor longLineProcessor = this._longLineImage.getProcessor();
        ManyBlobs result = new ManyBlobs();

        for (Blob blob : inputBlobs) {
            if (blob.getOuterContour().npoints / 2 >= this._minContourLength) {
                blob.draw(longLineProcessor);
                result.add(blob);
            }
        }

        this._longLineImage.show();
        this._longLineImage.updateAndDraw();
        result.createLineOrdering();
        return result;
    }

    private ImagePlus getStreetImageByFollowingLines(ManyBlobs longBlobs) {
        ImagePlus streetImage = NewImage.createByteImage("street image", this._image.getWidth(), this._image.getHeight(), 1, 4);
        ImageProcessor streetImageProcessor = streetImage.getProcessor();
        byte[] longLinePixels = (byte[]) this._longLineImage.getProcessor().getPixels();

        for (Blob blob : longBlobs) {
            int[] contourX = blob.getLineX();
            int[] contourY = blob.getLineY();
            int end = blob.getOuterContour().npoints / 2;
            boolean firstPart = true;

            for (int i = 0; i < 2; ++i) {
                int x1 = firstPart ? contourX[this._lineFollowingSampleRate] : contourX[end - this._lineFollowingSampleRate];
                int y1 = firstPart ? contourY[this._lineFollowingSampleRate] : contourY[end - this._lineFollowingSampleRate];
                int startX = firstPart ? contourX[0] : contourX[end];
                int startY = firstPart ? contourY[0] : contourY[end];
                longLinePixels[startY * this._width + startX] = -106;
                double baseAngle = this.getAngle(x1, startX, y1, startY);
                int offset = 0;

                while (offset < this._coneAngle) {
                    double currentAngle = baseAngle + (double) offset;
                    double currentAngleRAD = Math.PI * currentAngle / 180;
                    int endX = (int) ((double) startX + (double) this._coneLength * Math.cos(currentAngleRAD));
                    int endY = (int) ((double) startY + (double) this._coneLength * Math.sin(currentAngleRAD));
                    List<Point> points = Utils.getBresenhamPoints(startX, startY, endX, endY);
                    if (this._drawCones) {

                        for (Point p : points) {
                            if (this.isInImageRange(p.x, p.y) && longLinePixels[p.y * this._width + p.x] != 0) {
                                longLinePixels[p.y * this._width + p.x] = -56;
                            }
                        }
                    }

                    if (this.couldFollow(points, longBlobs, baseAngle, streetImageProcessor)) {
                        blob.draw(streetImageProcessor);
                        break;
                    }

                    if (offset == 0) {
                        offset = -1;
                    } else if (offset < 0) {
                        offset *= -1;
                    } else {
                        ++offset;
                        offset *= -1;
                    }
                }

                firstPart = !firstPart;
            }
        }

        this._longLineImage.updateAndDraw();
        streetImage.updateImage();
        return streetImage;
    }

    private boolean couldFollow(List<Point> points, ManyBlobs longBlobs, double baseAngle, ImageProcessor streetImageProcessor) {

        for (Blob blob : longBlobs) {
            int[] contourX = blob.getLineX();
            int[] contourY = blob.getLineY();
            int end = blob.getOuterContour().npoints / 2;

            for (Point p : points) {
                double angle;
                if (contourX[0] == p.x && contourY[0] == p.y) {
                    angle = this.getAngle(contourX[0], contourX[this._lineFollowingSampleRate], contourY[0], contourY[this._lineFollowingSampleRate]);
                    if (this.getAngleDiff(baseAngle, angle) < (double) this._maxAngleDiffCone) {
                        streetImageProcessor.drawLine(points.get(0).x, points.get(0).y, p.x, p.y);
                        return true;
                    }
                }

                if (contourX[end] == p.x && contourY[end] == p.y) {
                    angle = this.getAngle(contourX[end], contourX[end - this._lineFollowingSampleRate], contourY[end], contourY[end - this._lineFollowingSampleRate]);
                    if (this.getAngleDiff(baseAngle, angle) < (double) this._maxAngleDiffCone) {
                        streetImageProcessor.drawLine(points.get(0).x, points.get(0).y, p.x, p.y);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private double getAngle(int startX, int endX, int startY, int endY) {
        double angleRAD = Math.atan2((double)(endY - startY), (double)(endX - startX));
        return angleRAD * 180 / Math.PI;
    }

    private double getAngleDiff(double alpha, double beta) {
        double diff = Math.abs(beta - alpha) % 360;
        return diff < 180 ? diff : 360 - diff;
    }

    private boolean isInImageRange(int x, int y) {
        return x > -1 && x < this._width && y > -1 && y < this._height;
    }
}
