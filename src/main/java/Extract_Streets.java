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
        Preprocessor preprocessor = new Preprocessor(_image);
        ManyBlobs preprocessedBlobs = preprocessor.process();
        preprocessedBlobs.computeFeatures();

        ImagePlus resultImage = NewImage.createByteImage("result image", _image.getWidth(), _image.getHeight(), 1, 4);
        resultImage.show();

        ImageStack stack = _image.getStack();
        stack.addSlice("result slice", resultImage.getProcessor());

        ImagePlus stackedImage = new ImagePlus("Stack", stack);
        stackedImage.show();
        stackedImage.updateAndDraw();

        this.fillFeatureTable(preprocessedBlobs);
        resultImage.getCanvas().addMouseListener(new ImageResultsTableSelector(resultImage, preprocessedBlobs));

        FeatureEvaluator evaluator = new FeatureEvaluator(preprocessedBlobs, resultImage);
        StreetExtrationDialog dialog = new StreetExtrationDialog(evaluator);

        dialog.show();

        //IJ.run("Images to Stack", "name=Stack title=[] use");
    }

    private void fillFeatureTable(ManyBlobs blobs) {
        ResultsTable rt = Analyzer.getResultsTable();
        if(rt==null)
        {
            rt = new ResultsTable();
            Analyzer.setResultsTable(rt);
        }

        for (Blob blob : blobs) {
            rt.incrementCounter();

            rt.addValue("Length", blob.getLength());
            rt.addValue("isInCluster", String.valueOf(blob.isInCluster()));
            rt.addValue("Parallel Coverage", blob.getParallelCoverage());
            rt.addValue("Line Following segments", blob.getLineFollowingElements());
        }
        rt.show("Results");
    }

    public static void main(String[] args) {
        Class clazz = Extract_Streets.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.setProperty("plugins.dir", pluginsDir);
        new ImageJ();
        ImagePlus image = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\test.png");
        //ImagePlus image = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\new_pale_data_log.png");
        //ImagePlus image = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\test-pale\\canny\\manyStreets.png");
        //ImagePlus image = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\test.png");
        image.show();
        IJ.runPlugIn(clazz.getName(), "");
    }
}