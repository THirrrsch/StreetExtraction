package gui;

import blob.Blob;
import blob.ManyBlobs;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * This class implements an listener for interactively select a row in the results table and the corresponding blob in an image.
 **/
public class ResultsTableSelectionDrawer implements MouseListener {

    public int selectionStart = -1;
    public int selectionStop = -1;
    public ImagePlus imp;
    private ManyBlobs blobs;
    private ResultsTable rt;

    public ResultsTableSelectionDrawer(ImagePlus imp, ManyBlobs blobs, ResultsTable rt) {
        this.imp = imp;
        this.blobs = blobs;
        this.rt = rt;
    }

    @Override
    public void mouseClicked(MouseEvent e) {


    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseReleased(MouseEvent e) {

        update(IJ.getTextPanel().getSelectionStart(), IJ.getTextPanel().getSelectionEnd());
    }

    public void update(int start, int end) {

        if ((selectionStart != start || selectionStop != end) && imp.isVisible()) {
            selectionStart = start;
            selectionStop = end;
            if (selectionStart >= 0) {
                showAsOverlay(selectionStart, selectionStop);

            }
        }
    }

    public void showAsOverlay(int start, int end) {
        Overlay ov = imp.getOverlay();
        IJ.selectWindow(imp.getTitle());

        if (ov == null) {
            ov = new Overlay();
            IJ.getImage().setOverlay(ov);
        } else {
            ov.clear();
        }
        int firstSlice = -1;

        for (int i = start; i <= end; i++) {
            int index = (int) rt.getValueAsDouble(0, i);
            Blob b = blobs.get(index - 1);

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
            if (firstSlice == -1) {
                firstSlice = 1;
            }
        }

        imp.setSlice(firstSlice);
        imp.repaintWindow();

        // setOverlay(ov);
    }
}