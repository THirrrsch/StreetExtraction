import blob.*;
import calculation.EvaluationConstants;
import calculation.EvaluationValues;
import calculation.FeatureEvaluator;
import calculation.Preprocessor;
import gui.ImageResultsTableSelector;
import gui.StreetExtrationDialog;
import ij.*;
import ij.gui.*;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.PlugInFilter;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Extract_Streets implements PlugInFilter {
    private ImagePlus _image;

    public Extract_Streets() {
    }

    public int setup(String arg, ImagePlus imp) {
        this._image = imp;
        return 31;
    }

    public void run(ImageProcessor ip) {
        Preprocessor preprocessor = new Preprocessor(_image);
        ManyBlobs preprocessedBlobs = preprocessor.process();
        preprocessedBlobs.computeFeatures();

        ImagePlus resultStack = this.createResultStack(preprocessor);

        resultStack.getCanvas().addMouseListener(new ImageResultsTableSelector(resultStack, preprocessedBlobs));

        FeatureEvaluator evaluator = new FeatureEvaluator(preprocessedBlobs, resultStack);
        StreetExtrationDialog dialog = new StreetExtrationDialog(evaluator, resultStack);

        dialog.show();
    }

    private ImagePlus createResultStack(Preprocessor preprocessor) {
        ImageStack stack = _image.getStack();
        stack.addSlice(preprocessor.getCentroidImage().getProcessor());
        stack.addSlice(preprocessor.getLineImage().getProcessor());
        stack.addSlice(NewImage.createByteImage("result image", _image.getWidth(), _image.getHeight(), 1, 4).getProcessor());
        ImagePlus stackedImage = new ImagePlus("Stack", stack);
        stackedImage.setSlice(stackedImage.getStack().getSize());
        stackedImage.show();
        _image.close();

        return stackedImage;
    }

    public static void main(String[] args) {
        Class clazz = Extract_Streets.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.setProperty("plugins.dir", pluginsDir);
        new ImageJ();
        ImagePlus image = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\" + EvaluationConstants.COLORED + "\\LoG\\" + EvaluationConstants.FILE_NAME + ".png");
        //ImagePlus image = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\new_pale_data_log.png");
        image.show();
        IJ.runPlugIn(clazz.getName(), "");
    }
}