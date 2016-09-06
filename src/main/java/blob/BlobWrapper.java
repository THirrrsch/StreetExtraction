package blob;

import blob.Blob;
import org.apache.commons.math3.ml.clustering.Clusterable;

public class BlobWrapper implements Clusterable {
    private double[] _points;
    private Blob _blob;

    public BlobWrapper(Blob blob) {
        this._blob = blob;
        double angle = this.getAngle180Positive(blob.getLineX()[0], blob.getLineX()[blob.getLength()], blob.getLineY()[0], blob.getLineY()[blob.getLength()]);
        this._points = new double[] {blob.getLength(), angle};
    }

    public Blob getBlob() {
        return _blob;
    }

    public double[] getPoint() {
        return _points;
    }

    private double getAngle180Positive(int startX, int endX, int startY, int endY) {
        double angleRAD = Math.atan2((double)(endY - startY), (double)(endX - startX));
        double angleDegree = angleRAD * 180 / Math.PI;
        return angleDegree > 0 ? angleDegree : 180 + angleDegree;
    }
}
