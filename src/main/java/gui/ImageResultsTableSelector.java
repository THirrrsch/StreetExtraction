package gui;

import blob.Blob;
import blob.ManyBlobs;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;

/**
 * This class implements an listener for interactive select a blob in an image and the corresponding row in the results table.
 **/
public class ImageResultsTableSelector implements MouseListener {

    private ImagePlus _image;
    private ManyBlobs _blobs;
    private Map<Point, Integer> _blobMap;
    public static boolean isParticleSelected;

    public ImageResultsTableSelector(ImagePlus imp, ManyBlobs blobs) {
        _image = imp;
        _blobs = blobs;

        _blobMap = new HashMap<Point, Integer>();
        int i = 0;
        for (Blob blob : _blobs) {
            int[] contourX = blob.getLineX();
            int[] contourY = blob.getLineY();

            for (int j = 0; j < blob.getLength(); j++) {
                _blobMap.put(new Point(contourX[j], contourY[j]), i);
            }
            i++;
        }
        isParticleSelected = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Point location = _image.getCanvas().getCursorLoc();

        Overlay ov = _image.getOverlay();
        if (ov == null) {
            ov = new Overlay();
            _image.setOverlay(ov);
        } else {
            ov.clear();
        }

        isParticleSelected = false;
        Integer index = this.getNearestBlobIndex(location);
        Blob b = _blobs.get(index);

        IJ.getTextPanel().setSelection(index, index);

        PolygonRoi pr = new PolygonRoi(b.getOuterContour().xpoints.clone(), b.getOuterContour().ypoints.clone(), b.getOuterContour().npoints, Roi.TRACED_ROI);
        pr.setStrokeWidth(2);
        pr.setPosition(1);
        ov.add(pr);
        Point[] mer = b.getMinimumBoundingRectangle();
        int[] xpoints = new int[mer.length];
        int[] ypoints = new int[mer.length];
        for (int j = 0; j < mer.length; j++) {
            xpoints[j] = mer[j].x;
            ypoints[j] = mer[j].y;
        }
        PolygonRoi pr2 = new PolygonRoi(xpoints, ypoints, mer.length, Roi.POLYGON);
        pr2.setStrokeWidth(1);
        pr2.setStrokeColor(Color.red);
        pr2.setPosition(1);
        ov.add(pr2);
        IJ.getImage().repaintWindow();
        isParticleSelected = true;
    }

    private Integer getNearestBlobIndex(Point p) {
        int x = p.x;
        int y = p.y;

        int offset = 1;

        while (true) {
            for (int j = -offset; j <= offset; j++) {
                p.setLocation(x + j, y - offset);
                if (_blobMap.containsKey(p)) {
                    return _blobMap.get(p);
                }
            }
            for (int j = -offset; j <= offset; j++) {
                p.setLocation(x + j, y + offset);
                if (_blobMap.containsKey(p)) {
                    return _blobMap.get(p);
                }
            }
            for (int j = -offset; j <= offset; j++) {
                p.setLocation(x - offset, y + j);
                if (_blobMap.containsKey(p)) {
                    return _blobMap.get(p);
                }
            }
            for (int j = -offset; j <= offset; j++) {
                p.setLocation(x + offset, y + j);
                if (_blobMap.containsKey(p)) {
                    return _blobMap.get(p);
                }
            }

            offset++;
        }
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mousePressed(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }
}