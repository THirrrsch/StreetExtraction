import Util.Constants;
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
        dialog.addNumericField("Min hue colored", Constants.MIN_HUE_COLORED, 0);
        dialog.addNumericField("Max hue colored", Constants.MAX_HUE_COLORED, 0);
        dialog.addNumericField("Min saturation colored", Constants.MIN_SATURATION_COLORED, 0);
        dialog.addNumericField("Max saturation colored", Constants.MAX_SATURATION_COLORED, 0);
        dialog.addNumericField("Min brightness colored", Constants.MIN_BRIGHTNESS_COLORED, 0);
        dialog.addNumericField("Max brightness colored", Constants.MAX_BRIGHTNESS_COLORED, 0);
        dialog.addNumericField("Street Width", Constants.STREET_WIDTH, 0);
        dialog.showDialog();

        if(!dialog.wasCanceled()) {
            int minHueColored = (int)dialog.getNextNumber();
            int maxHueColored = (int)dialog.getNextNumber();
            int minSatColored = (int)dialog.getNextNumber();
            int maxSatColored = (int)dialog.getNextNumber();
            int minBrightColored = (int)dialog.getNextNumber();
            int maxBrightColored = (int)dialog.getNextNumber();
            int streetWidth = (int)dialog.getNextNumber();
            StreetsExtractor extractor;
            if(dialog.getNextChoice().equals("pale")) {
                //ImagePlus cannyImage = IJ.openImage();
                extractor = new PaleStreetsExtractor(_image);
            } else {
                extractor = new ColoredStreetsExtractor(_image, minHueColored, maxHueColored, minSatColored, maxSatColored, minBrightColored, maxBrightColored, streetWidth);
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
        //ImagePlus image = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\test-pale\\canny\\manyStreets.png");
        ImagePlus image = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\test.png");
        image.show();
        IJ.runPlugIn(clazz.getName(), "");
    }
}