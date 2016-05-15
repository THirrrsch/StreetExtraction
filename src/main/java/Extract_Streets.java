import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import java.awt.Panel;

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
        dialog.addNumericField("Line following sample rate", Constants.CONTOUR_FOLLOW_SAMPLE_RATE, 0);
        dialog.addNumericField("Cone angle", Constants.CONE_ANGLE, 0);
        dialog.addNumericField("Cone length", Constants.CONE_LENGTH, 0);
        dialog.addNumericField("Max angle diff for following lines", Constants.MAX_ANGLE_DIFF_CONES, 0);
        dialog.addCheckbox("Draw cones", false);
        dialog.addPanel(new Panel());
        dialog.addNumericField("Min hue", Constants.MIN_HUE, 0);
        dialog.addNumericField("Max hue", Constants.MAX_HUE, 0);
        dialog.addNumericField("Min saturation", Constants.MIN_SATURATION, 0);
        dialog.addNumericField("Max saturation", Constants.MAX_SATURATION, 0);
        dialog.addNumericField("Min brightness", Constants.MIN_BRIGHTNESS, 0);
        dialog.addNumericField("Max brightness", Constants.MAX_BRIGHTNESS, 0);
        dialog.showDialog();

        if(!dialog.wasCanceled()) {
            int filteredImage = (int)dialog.getNextNumber();
            int maxAngleDiff = (int)dialog.getNextNumber();
            int minContourLength = (int)dialog.getNextNumber();
            int lineFollowingSampleRate = (int)dialog.getNextNumber();
            int coneAngle = (int)dialog.getNextNumber();
            int coneLength = (int)dialog.getNextNumber();
            int maxAngleDiff2 = (int)dialog.getNextNumber();
            boolean drawCones = dialog.getNextBoolean();
            int minHue = (int)dialog.getNextNumber();
            int maxHue = (int)dialog.getNextNumber();
            int minSat = (int)dialog.getNextNumber();
            int maxSat = (int)dialog.getNextNumber();
            int minBrght = (int)dialog.getNextNumber();
            int maxBrght = (int)dialog.getNextNumber();
            StreetsExtractor extractor;
            if(dialog.getNextChoice().equals("pale")) {
                extractor = new PaleStreetsExtractor(this._image, filteredImage, maxAngleDiff, minContourLength, lineFollowingSampleRate, coneAngle, coneLength, maxAngleDiff2, drawCones);
            } else {
                extractor = new ColoredStreetsExtractor(this._image, minHue, maxHue, minSat, maxSat, minBrght, maxBrght);
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
        ImagePlus image = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\paleStreets\\cannysharpened.png");
        image.show();
        IJ.runPlugIn(clazz.getName(), "");
    }
}
