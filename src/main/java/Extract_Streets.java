import Util.*;
import blob.Blob;
import blob.FeatureEvaluator;
import blob.ManyBlobs;
import ij.*;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.gui.NonBlockingGenericDialog;
import ij.measure.ResultsTable;
import ij.plugin.EventListener;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyListener;

public class Extract_Streets implements PlugInFilter {
    private ImagePlus _image;
    private ImagePlus _resultImage;
    private FeatureEvaluator _evaluator;

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

        ResultsTable rt = new ResultsTable();
        for (Blob blob : preprocessedBlobs) {
            rt.incrementCounter();

            rt.addValue("Length", blob.getLength());
            rt.addValue("isInCluster", String.valueOf(blob.isInCluster()));
            rt.addValue("Parallel Coverage", blob.getParallelCoverage());
            rt.addValue("Line Following segments", blob.getLineFollowingElements());
        }
        rt.show("Features");

        _resultImage = NewImage.createByteImage("result image", _image.getWidth(), _image.getHeight(), 1, 4);
        _resultImage.show();

        _evaluator = new FeatureEvaluator(preprocessedBlobs, _resultImage.getProcessor());

        IJ.run("Images to Stack", "name=Stack title=[] use");

        this.showDialog();
    }

    private void showDialog() {
        NonBlockingGenericDialog dialog = new NonBlockingGenericDialog("Parameters");

        dialog.addMessage("Length");
        dialog.addNumericField("Infimum", _evaluator.getLengthValues().INFIMUM, 0);
        dialog.addNumericField("Supremum", _evaluator.getLengthValues().SUPREMUM, 0);
        dialog.addNumericField("Weight", _evaluator.getLengthValues().WEIGHT, 0);

        dialog.addMessage("Clustering");
        dialog.addNumericField("Weight", _evaluator.getClusteringValues().WEIGHT, 0);

        dialog.addMessage("Parallel Coverage");
        dialog.addNumericField("Infimum", _evaluator.getParallelCoverageValues().INFIMUM, 0);
        dialog.addNumericField("Supremum", _evaluator.getParallelCoverageValues().SUPREMUM, 0);
        dialog.addNumericField("Weight", _evaluator.getParallelCoverageValues().WEIGHT, 0);

        dialog.addMessage("Line Following Segments");
        dialog.addNumericField("Infimum", _evaluator.getLineFollowingValues().INFIMUM, 0);
        dialog.addNumericField("Supremum", _evaluator.getLineFollowingValues().SUPREMUM, 0);
        dialog.addNumericField("Weight", _evaluator.getLineFollowingValues().WEIGHT, 0);

        dialog.addPanel(new Panel());
        dialog.addNumericField("Overall Threshold", EvaluationConstants.THRESHOLD, 1);
        dialog.addCheckbox("Calculate preview", true);

        DialogListener listener = new DialogListener() {
            @Override
            public boolean dialogItemChanged(GenericDialog dialog, AWTEvent awtEvent) {
                if (awtEvent instanceof  ItemEvent) {
                    ItemEvent event = (ItemEvent) awtEvent;
                    if (event.getItem().equals("Calculate preview") && event.getStateChange() == 1) {
                        EvaluationValues lengthValues = new EvaluationValues(dialog.getNextNumber(), dialog.getNextNumber(), (int) dialog.getNextNumber());
                        EvaluationValues clusteringValues = new EvaluationValues(0, 0, (int) dialog.getNextNumber());
                        EvaluationValues parallelCoverageValues = new EvaluationValues(dialog.getNextNumber(), dialog.getNextNumber(), (int) dialog.getNextNumber());
                        EvaluationValues lineFollowingValues = new EvaluationValues(dialog.getNextNumber(), dialog.getNextNumber(), (int) dialog.getNextNumber());
                        double threshold = dialog.getNextNumber();

                        _evaluator.setLengthValues(lengthValues);
                        _evaluator.setClusteringValues(clusteringValues);
                        _evaluator.setParallelCoverageValues(parallelCoverageValues);
                        _evaluator.setLineFollowingValues(lineFollowingValues);
                        _evaluator.setThreshold(threshold);

                        _evaluator.evaluate();
                        _resultImage.updateAndDraw();
                    }
                }
                return true;
            }
        };
        dialog.addDialogListener(listener);

        dialog.showDialog();
    }

    private void calculateResult() {

    }

//    public void run(ImageProcessor ip) {
//        GenericDialog dialog = new GenericDialog("Options");
//        dialog.addChoice("Image type", new String[]{"pale", "colored"}, "pale");
//        dialog.addNumericField("Min hue colored", FeatureConstants.MIN_HUE_COLORED, 0);
//        dialog.addNumericField("Max hue colored", FeatureConstants.MAX_HUE_COLORED, 0);
//        dialog.addNumericField("Min saturation colored", FeatureConstants.MIN_SATURATION_COLORED, 0);
//        dialog.addNumericField("Max saturation colored", FeatureConstants.MAX_SATURATION_COLORED, 0);
//        dialog.addNumericField("Min brightness colored", FeatureConstants.MIN_BRIGHTNESS_COLORED, 0);
//        dialog.addNumericField("Max brightness colored", FeatureConstants.MAX_BRIGHTNESS_COLORED, 0);
//        dialog.addNumericField("Street Width", FeatureConstants.STREET_WIDTH, 0);
//        dialog.showDialog();
//
//        if(!dialog.wasCanceled()) {
//            int minHueColored = (int)dialog.getNextNumber();
//            int maxHueColored = (int)dialog.getNextNumber();
//            int minSatColored = (int)dialog.getNextNumber();
//            int maxSatColored = (int)dialog.getNextNumber();
//            int minBrightColored = (int)dialog.getNextNumber();
//            int maxBrightColored = (int)dialog.getNextNumber();
//            int streetWidth = (int)dialog.getNextNumber();
//            StreetsExtractor extractor;
//            if(dialog.getNextChoice().equals("pale")) {
//                //ImagePlus cannyImage = IJ.openImage();
//                extractor = new PaleStreetsExtractor(_image);
//            } else {
//                extractor = new ColoredStreetsExtractor(_image, minHueColored, maxHueColored, minSatColored, maxSatColored, minBrightColored, maxBrightColored, streetWidth);
//            }
//
//            ImagePlus filteredImage1 = extractor.process();
//            filteredImage1.show();
//            filteredImage1.updateAndDraw();
//            IJ.run("Images to Stack", "name=Stack title=[] use");
//        }
//    }

    public static void main(String[] args) {
        Class clazz = Extract_Streets.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.setProperty("plugins.dir", pluginsDir);
        new ImageJ();
        ImagePlus image = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\test.png");
        //ImagePlus image = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\test-pale\\canny\\manyStreets.png");
        //ImagePlus image = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\test.png");
        image.show();
        IJ.runPlugIn(clazz.getName(), "");
    }
}