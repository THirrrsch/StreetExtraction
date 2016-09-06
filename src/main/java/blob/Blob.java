/*
    IJBlob is a ImageJ library for extracting connected components in binary Images
    Copyright (C) 2012  Thorsten Wagner wagner@biomedical-imaging.de

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package blob;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.PolygonFiller;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Blob {
	
	public final static int DRAW_HOLES = 1;
	public final static int DRAW_CONVEX_HULL = 2;
	public final static int DRAW_LABEL = 4;
	
	private static Color defaultColor = Color.black;

	int[] lineX;
	int[] lineY;

	private Polygon outerContour;
	private ArrayList<Polygon> innerContours;
	private int label;
	
	//Features
	private Point2D  centerOfGrafity = null;
	private Point centroid = null;
	private double enclosedArea = -1;
	private int length = -1;
	private boolean isInCluster = false;
	private double parallelCoverage = -1;
	private int lineFollowingElements = -1;

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public boolean isInCluster() {
		return isInCluster;
	}

	public void setInCluster(boolean inCluster) {
		isInCluster = inCluster;
	}

	public double getParallelCoverage() {
		return parallelCoverage;
	}

	public void setParallelCoverage(double parallelCoverage) {
		this.parallelCoverage = parallelCoverage;
	}

	public int getLineFollowingElements() {
		return lineFollowingElements;
	}

	public void setLineFollowingElements(int lineFollowingElements) {
		this.lineFollowingElements = lineFollowingElements;
	}

	private Calibration cal = new Calibration();

	public int[] getLineX() {
		return this.lineX;
	}

	public int[] getLineY() {
		return this.lineY;
	}

	public Blob(Polygon outerContour, int label, Calibration cal) {
		this.outerContour = outerContour;
		this.label = label;
		innerContours = new ArrayList<Polygon>();
		this.cal = cal;
	}
	
	void draw(ImageProcessor ip, int options, Color col){
		ip.setColor(col);
		fillPolygon(ip, outerContour, false);
		
		
		if((options&DRAW_HOLES)>0){
			for (Polygon innerContour : innerContours) {
				if (defaultColor == Color.white) {
					ip.setColor(Color.BLACK);
				} else {
					ip.setColor(Color.white);
				}
				fillPolygon(ip, innerContour, true);
				if (defaultColor == Color.white) {
					ip.setColor(Color.white);
				} else {
					ip.setColor(Color.black);
				}
				ip.drawPolygon(innerContour);
			}
		}
		
		if((options&DRAW_CONVEX_HULL)>0){
			ip.setColor(Color.RED);
			ip.drawPolygon(getConvexHull());
		}
		
		if((options&DRAW_LABEL)>0){
			Point2D cog = getCenterOfGravity();
			ip.setColor(Color.MAGENTA);
			ip.drawString(""+getLabel(), (int)cog.getX(), (int)cog.getX());
		}
	}

	public void draw(ImageProcessor ip, int options){
		draw(ip, options, defaultColor);
	}
	
	void draw(ImageProcessor ip, int options, int deltax, int deltay){
		ip.setColor(Color.BLACK);
		Polygon p = new Polygon(outerContour.xpoints,outerContour.ypoints,outerContour.npoints);
		p.translate(deltax, deltay);
		fillPolygon(ip, p, false);
		
		
		if((options&DRAW_HOLES)>0){
			for (Polygon innerContour : innerContours) {
				ip.setColor(Color.WHITE);
				p = new Polygon(innerContour.xpoints, innerContour.ypoints, innerContour.npoints);
				p.translate(deltax, deltay);
				fillPolygon(ip, p, true);
			}
		}
		if((options&DRAW_CONVEX_HULL)>0){
			ip.setColor(Color.RED);
			ip.drawPolygon(getConvexHull());
		}
		
		if((options&DRAW_LABEL)>0){
			Point2D cog = getCenterOfGravity();
			ip.setColor(Color.MAGENTA);
			ip.drawString(""+getLabel(), (int)cog.getX(), (int)cog.getY());
		}
	}

	public void draw(ImageProcessor ip){
		draw(ip,DRAW_HOLES);
	}

	public Point2D getCenterOfGravity() {
		
		if(centerOfGrafity != null){
			return centerOfGrafity;
		}
		centerOfGrafity = new Point2D.Float();
	    
	    int[] x = outerContour.xpoints;
	    int[] y = outerContour.ypoints;
	    int sumx = 0;
	    int sumy = 0;
	    double A = 0;

	    for(int i = 0; i < outerContour.npoints-1; i++){
	    	int cross = (x[i]*y[i+1]-x[i+1]*y[i]);
	    	sumx = sumx + (x[i]+x[i+1])*cross;
	    	sumy = sumy + (y[i]+y[i+1])*cross;
	    	A = A + x[i]*y[i+1]-x[i+1]*y[i];
	    }
	    A = 0.5*A;
	    
	    centerOfGrafity.setLocation(cal.getX(sumx/(6*A)),cal.getY(sumy/(6*A)));
		if(getEnclosedArea()==1){
			centerOfGrafity.setLocation(cal.getX(x[0]),cal.getY(y[0]));
		}

		return centerOfGrafity;
	}

	public Point getCentroid() {

		if (centroid != null) {
			return centroid;
		}

		int[] x = outerContour.xpoints;
		int[] y = outerContour.ypoints;
		double sumx = 0;
		double sumy = 0;

		for (int i = 0; i < outerContour.npoints - 1; i++) {
			sumx += x[i];
			sumy += y[i];
		}

		centroid = new Point((int) (sumx / (outerContour.npoints - 1)), (int) (sumy / (outerContour.npoints - 1)));
		return centroid;
	}

	private void fillPolygon(ImageProcessor ip, Polygon p, boolean internContour) {
		PolygonRoi proi = new PolygonRoi(p, PolygonRoi.POLYGON);
		Rectangle r = proi.getBounds();
		PolygonFiller pf = new PolygonFiller();
		pf.setPolygon(proi.getXCoordinates(), proi.getYCoordinates(), proi.getNCoordinates());
		ip.setRoi(r);
		ImageProcessor objectMask = pf.getMask(r.width, r.height);
		ip.fill(objectMask);
		if(!internContour){
			ip.drawPolygon(p);
		}
	}

	public Polygon getOuterContour() {
		return outerContour;
	}

	void addInnerContour(Polygon contour) {
		innerContours.add(contour);
	}

	public int getLabel() {
		return label;
	}
	
	public Polygon getConvexHull() {
		PolygonRoi roi = new PolygonRoi(outerContour, Roi.POLYGON);
		Polygon hull = roi.getConvexHull();
		if(hull==null){
			return getOuterContour();
		}
		return hull;
	}

	public double getEnclosedArea() {
		if(enclosedArea!=-1){
			return enclosedArea;
		}
		/*
		int[] cc = contourToChainCode(getOuterContour());
		enclosedArea = getAreaOfChainCode(cc)*cal.pixelHeight*cal.pixelWidth;
		*/
		
		//enclosedArea = getArea(getOuterContour())*cal.pixelHeight*cal.pixelWidth;
		
		ImagePlus imp = generateBlobImage(this);
		enclosedArea = imp.getStatistics().histogram[0]*cal.pixelHeight*cal.pixelWidth;
		
		return enclosedArea;
	}

	public int getNumberofHoles() {
		return innerContours.size();
	}
	
	public static ImagePlus generateBlobImage(Blob b){
		Rectangle r = b.getOuterContour().getBounds();
		r.setBounds(r.x, r.y, (int)r.getWidth()+1, (int)r.getHeight()+1);
		ImagePlus help = NewImage.createByteImage("", r.width+2, r.height+2, 1, NewImage.FILL_WHITE);
		ImageProcessor ip = help.getProcessor();
		b.draw(ip, Blob.DRAW_HOLES, -(r.x-1), -(r.y-1));
		help.setProcessor(ip);
		return help;
	}

	public void createLineOrdering() {
		int[] contourX = this.outerContour.xpoints;
		int[] contourY = this.outerContour.ypoints;
		int minXIndex = 0;
		int maxXIndex = 0;
		int maxYIndex = 0;
		int minYIndex = 0;

		int minIndex;
		for(minIndex = 0; minIndex < this.outerContour.npoints; ++minIndex) {
			if(contourX[minIndex] <= contourX[minXIndex]) {
				minXIndex = minIndex;
			} else if(contourX[minIndex] >= contourX[maxXIndex]) {
				maxXIndex = minIndex;
			}

			if(contourY[minIndex] <= contourY[minYIndex]) {
				minYIndex = minIndex;
			} else if(contourY[minIndex] >= contourY[maxYIndex]) {
				maxYIndex = minIndex;
			}
		}

		minIndex = contourX[maxXIndex] - contourX[minXIndex] > contourY[maxYIndex] - contourY[minYIndex]?minXIndex:minYIndex;
		this.lineX = this.doReordering(contourX, minIndex);
		this.lineY = this.doReordering(contourY, minIndex);
	}

	private int[] doReordering(int[] contour, int minIndex) {
		int[] tmp = new int[this.outerContour.npoints];
		System.arraycopy(contour, 0, tmp, 0, this.outerContour.npoints);
		if(minIndex == 0) {
			return tmp;
		} else {
			System.arraycopy(tmp, 1, tmp, 0, minIndex);
			return this.circularShiftSingle(tmp, minIndex);
		}
	}

	private int[] circularShiftSingle(int[] array, int shift) {
		int[] array2 = new int[shift];
		System.arraycopy(array, 0, array2, 0, shift);
		System.arraycopy(array, shift, array, 0, array.length - shift);
		System.arraycopy(array2, shift + array.length - shift - array.length, array, array.length - shift, array.length - (array.length - shift));
		return array;
	}

	public boolean contourContains(Point point) {
		int[] contourX = outerContour.xpoints;
		int[] contourY = outerContour.ypoints;

		for (int i = 0; i < outerContour.npoints; i++) {
			if (contourX[i] == point.x && contourY[i] == point.y) {
				return true;
			}
		}

		return false;
	}

	public void drawCentroid(ImageProcessor processor) {
		processor.drawLine(centroid.x - 2, centroid.y, centroid.x + 2, centroid.y);
		processor.drawLine(centroid.x, centroid.y - 2, centroid.x, centroid.y + 2);
		processor.drawLine(centroid.x - 2, centroid.y, centroid.x + 2, centroid.y);
		processor.drawLine(centroid.x, centroid.y - 2, centroid.x, centroid.y + 2);
	}
}
