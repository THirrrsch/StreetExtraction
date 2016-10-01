package blob;

import Util.Utils;
import blob.Blob;
import org.apache.commons.math3.ml.clustering.Clusterable;

public class BlobWrapper implements Clusterable {
    private double[] _points;
    private Blob _blob;

    public BlobWrapper(Blob blob) {
        this._blob = blob;
        double angle = Utils.getAngle180Positive(blob.getLineX()[0], blob.getLineX()[blob.getLength()], blob.getLineY()[0], blob.getLineY()[blob.getLength()]);
        //this._points = new double[] {blob.getLength(), angle};
        this._points = new double[] {blob.getLength(), angle, blob.getCentroid().getX(), blob.getCentroid().getY()};
    }

    public Blob getBlob() {
        return _blob;
    }

    public double[] getPoint() {
        return _points;
    }
}
