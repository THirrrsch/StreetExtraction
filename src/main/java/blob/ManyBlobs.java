/*
    IJBlob is a ImageJ library for extracting connected components in binary Images
    Copyright (C) 2012  Thorsten Wagner wagner@biomedical-imaging.de

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MER
TABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package blob;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageStatistics;

import java.util.ArrayList;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Represents the result-set of all detected blobs as ArrayList
 * @author Thorsten Wagner
 */

public class ManyBlobs extends ArrayList<Blob> {

	private static final long serialVersionUID = 1L;
	private ImagePlus binaryImage = null;

	public ManyBlobs() {

	}

	public ManyBlobs(ImagePlus binaryImage) {
		setImage(binaryImage);
	}
	
	private void setImage(ImagePlus imp) {
		this.binaryImage = imp;
		ImageStatistics stats = imp.getStatistics();
		
		boolean notBinary = (stats.histogram[0] + stats.histogram[255]) != stats.pixelCount;
		boolean toManyChannels = (imp.getNChannels()>1);
		boolean wrongBitDepth = (imp.getBitDepth()!=8);
		if (notBinary | toManyChannels | wrongBitDepth) {
			throw new java.lang.IllegalArgumentException("Wrong Image Format. IJ Blob only supports 8-bit, single-channel binary images");
		}
	}

	public void findConnectedComponents() {
		if(binaryImage==null){
			throw new RuntimeException("Cannot run findConnectedComponents: No input image specified");
		}
		ConnectedComponentLabeler labeler = new ConnectedComponentLabeler(this, binaryImage, 255, 0);
		labeler.doConnectedComponents();
	}

	public void createLineOrdering() {
		for (Blob blob : this) {
			blob.createLineOrdering();
		}
	}

}
