package Util;

import blob.Blob;
import blob.ManyBlobs;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class BlobMapper {

    private Map<Point, Blob> _blobMap;

    public BlobMapper(ManyBlobs blobs) {
        _blobMap = new HashMap<Point, Blob>();

        for (Blob blob : blobs) {
            int[] contourX = blob.getLineX();
            int[] contourY = blob.getLineY();
            int end = blob.getLength();

            _blobMap.put(new Point(contourX[0], contourY[0]), blob);
            _blobMap.put(new Point(contourX[end], contourY[end]), blob);
        }
    }

    public Blob getBlobWithGivenFirstPoint(Point p) {
        Blob result = _blobMap.get(p);
        if (result != null && result.getLineX()[0] == p.x && result.getLineY()[0] == p.y) {
            return result;
        }
        return null;
    }

    public Blob getBlobWithGivenLastPoint(Point p) {
        Blob result = _blobMap.get(p);
        if (result != null) {
            int end = result.getLength();
            if (result.getLineX()[end] == p.x && result.getLineY()[end] == p.y) {
                return result;
            }
        }
        return null;
    }
}