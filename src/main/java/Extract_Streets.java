import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.awt.*;

public class Extract_Streets implements PlugInFilter {
    private ImagePlus _image;

    public Extract_Streets() {
    }

    public int setup(String arg, ImagePlus imp) {
        this._image = imp;
        return 31;
    }

    public void run(ImageProcessor ip) {
        GenericDialog dialog = new GenericDialog("Options");
        dialog.addChoice("Image type", new String[]{"pale", "colored"}, "pale");
        dialog.addNumericField("Sample rate", Constants.SAMPLE_RATE, 0);
        dialog.addNumericField("Max angle diff for straight lines", Constants.MAX_ANGLE_DIFF, 0);
        dialog.addNumericField("Min contour length", Constants.MIN_CONTOUR_LENGTH, 0);
        dialog.addNumericField("DBSCAN epsilon", Constants.DBSCAN_EPSILON, 0);
        dialog.addNumericField("DBSCAN minPts", Constants.DBSCAN_MINPTS, 0);
        dialog.addNumericField("Line following sample rate", Constants.CONTOUR_FOLLOW_SAMPLE_RATE, 0);
        dialog.addNumericField("Cone angle", Constants.CONE_ANGLE, 0);
        dialog.addNumericField("Cone length", Constants.CONE_LENGTH, 0);
        dialog.addNumericField("Max angle diff for following lines", Constants.MAX_ANGLE_DIFF_CONES, 0);
        dialog.addPanel(new Panel());
        dialog.addNumericField("Min hue colored", Constants.MIN_HUE_COLORED, 0);
        dialog.addNumericField("Max hue colored", Constants.MAX_HUE_COLORED, 0);
        dialog.addNumericField("Min saturation colored", Constants.MIN_SATURATION_COLORED, 0);
        dialog.addNumericField("Max saturation colored", Constants.MAX_SATURATION_COLORED, 0);
        dialog.addNumericField("Min brightness colored", Constants.MIN_BRIGHTNESS_COLORED, 0);
        dialog.addNumericField("Max brightness colored", Constants.MAX_BRIGHTNESS_COLORED, 0);
        dialog.showDialog();

        if(!dialog.wasCanceled()) {
            int sampleRate = (int)dialog.getNextNumber();
            int maxAngleDiff = (int)dialog.getNextNumber();
            int minContourLength = (int)dialog.getNextNumber();
            int dbscanEpsilon = (int) dialog.getNextNumber();
            int dbscanMinPts = (int) dialog.getNextNumber();
            int lineFollowingSampleRate = (int)dialog.getNextNumber();
            int coneAngle = (int)dialog.getNextNumber();
            int coneLength = (int)dialog.getNextNumber();
            int maxAngleDiff2 = (int)dialog.getNextNumber();
            int minHueColored = (int)dialog.getNextNumber();
            int maxHueColored = (int)dialog.getNextNumber();
            int minSatColored = (int)dialog.getNextNumber();
            int maxSatColored = (int)dialog.getNextNumber();
            int minBrightColored = (int)dialog.getNextNumber();
            int maxBrightColored = (int)dialog.getNextNumber();
            StreetsExtractor extractor;
            if(dialog.getNextChoice().equals("pale")) {
                //ImagePlus cannyImage = IJ.openImage();
                extractor = new PaleStreetsExtractor(this._image, sampleRate, maxAngleDiff, minContourLength, dbscanEpsilon, dbscanMinPts, lineFollowingSampleRate, coneAngle, coneLength, maxAngleDiff2);
            } else {
                extractor = new ColoredStreetsExtractor(this._image, minHueColored, maxHueColored, minSatColored, maxSatColored, minBrightColored, maxBrightColored);
            }

            ImagePlus filteredImage1 = extractor.process();
            filteredImage1.show();
            filteredImage1.updateAndDraw();
            IJ.run("Images to Stack", "name=Stack title=[] use");
        }
    }

    public static void main(String[] args) {
        Class clazz = Extract_Streets.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.setProperty("plugins.dir", pluginsDir);
        new ImageJ();
        ImagePlus image = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\test-pale\\canny\\wood2.png");
        image.show();
        IJ.runPlugIn(clazz.getName(), "");
    }
}
