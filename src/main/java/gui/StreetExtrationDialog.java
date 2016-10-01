package gui;

import calculation.EvaluationConstants;
import calculation.EvaluationValues;
import calculation.FeatureEvaluator;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.gui.NonBlockingGenericDialog;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class StreetExtrationDialog implements DialogListener {

    private final FeatureEvaluator _evaluator;
    private final ImagePlus _resultStack;

    public StreetExtrationDialog(FeatureEvaluator evaluator, ImagePlus resultStack) {
        _evaluator = evaluator;
        _resultStack = resultStack;
    }

    public void show() {
        NonBlockingGenericDialog dialog = new NonBlockingGenericDialog("Parameters");

        dialog.addMessage("Length");
        dialog.addSlider("Infimum", 0, 100, _evaluator.getLengthValues().INFIMUM);
        dialog.addSlider("Supremum", 0, 100, _evaluator.getLengthValues().SUPREMUM);
        dialog.addSlider("Weight", 0, 10, _evaluator.getLengthValues().WEIGHT);

        dialog.addMessage("Clustering");
        dialog.addSlider("Weight", 0, 10, _evaluator.getClusteringValues().WEIGHT);

        dialog.addMessage("Parallel Coverage");
        dialog.addSlider("Infimum", 0, 1, _evaluator.getParallelCoverageValues().INFIMUM);
        dialog.addSlider("Supremum", 0, 1, _evaluator.getParallelCoverageValues().SUPREMUM);
        dialog.addSlider("Weight", 0, 10, _evaluator.getParallelCoverageValues().WEIGHT);

        dialog.addMessage("Line Following Segments");
        dialog.addSlider("Infimum", 1, 20, _evaluator.getLineFollowingValues().INFIMUM);
        dialog.addSlider("Supremum", 1, 20, _evaluator.getLineFollowingValues().SUPREMUM);
        dialog.addSlider("Weight", 0, 10, _evaluator.getLineFollowingValues().WEIGHT);

        dialog.addPanel(new Panel());
        dialog.addSlider("Overall Threshold", 0, 1, EvaluationConstants.THRESHOLD);

        dialog.addCheckbox("Preview", true);

        dialog.addDialogListener(this);

        dialog.showDialog();

        //------------------------------------------------------------------------------------------------
        //----------------------------------------EVALUATION----------------------------------------------
        //------------------------------------------------------------------------------------------------
//        if (!dialog.wasCanceled()) {
//            ImageProcessor resultProcessor = _resultStack.getStack().getProcessor(_resultStack.getStack().getSize());
//            byte[] resultPixels = (byte[]) resultProcessor.getPixels();
//
//            ImagePlus groundTruthImage = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\" + EvaluationConstants.COLORED + "\\ground truth\\" + EvaluationConstants.FILE_NAME + ".png");
//            ImageProcessor groundTruthProcessor = groundTruthImage.getProcessor();
//
//            int width = resultProcessor.getWidth();
//            int height = resultProcessor.getHeight();
//
//            int found = 0;
//            int right = 0;
//            int wrong = 0;
//
//            for (int x = 0; x < width; x++) {
//                for (int y = 0; y < height; y++) {
//                    if (resultPixels[y * width + x] == 0) {
//                        found++;
//                        if (groundTruthProcessor.getPixel(x, y) == -65316) {
//                            right++;
//                        } else {
//                            wrong++;
//                        }
//                    }
//                }
//            }
//
//            double rightPercent = (double) right / (double) found * 100;
//            double wrongPercent = (double) wrong / (double) found * 100;
//            System.out.println("Right: " + rightPercent + "%");
//            System.out.println("Wrong: " + wrongPercent + "%");
//
//            try {
//                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\" + EvaluationConstants.COLORED + "\\quantity_new.txt", true)));
//                out.println(EvaluationConstants.FILE_NAME + ".png");
//                out.println("found: " + found);
//                out.println("right: " + right);
//                out.println("wrong: " + wrong);
//                out.println("right %: " + rightPercent);
//                out.println("wrong %: " + wrongPercent);
//                out.println("--------------------------");
//                out.close();
//            } catch (IOException e) {
//                //exception handling left as an exercise for the reader
//            }
//        }
    }


    @Override
    public boolean dialogItemChanged(GenericDialog dialog, AWTEvent awtEvent) {
        EvaluationValues lengthValues = new EvaluationValues(dialog.getNextNumber(), dialog.getNextNumber(), (int) dialog.getNextNumber());
        EvaluationValues clusteringValues = new EvaluationValues(0, 0, (int) dialog.getNextNumber());
        EvaluationValues parallelCoverageValues = new EvaluationValues(dialog.getNextNumber(), dialog.getNextNumber(), (int) dialog.getNextNumber());
        EvaluationValues lineFollowingValues = new EvaluationValues(dialog.getNextNumber(), dialog.getNextNumber(), (int) dialog.getNextNumber());
        double threshold = dialog.getNextNumber();
        boolean isPreviewEnabled = dialog.getNextBoolean();

        _evaluator.setLengthValues(lengthValues);
        _evaluator.setClusteringValues(clusteringValues);
        _evaluator.setParallelCoverageValues(parallelCoverageValues);
        _evaluator.setLineFollowingValues(lineFollowingValues);
        _evaluator.setThreshold(threshold);

        if (isPreviewEnabled) {
            _evaluator.evaluate();
        }
        return true;
    }


}
