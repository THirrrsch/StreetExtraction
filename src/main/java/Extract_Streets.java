import Util.Utils;
import blob.*;
import calculation.*;
import gui.ImageResultsTableSelector;
import gui.StreetExtrationDialog;
import ij.*;
import ij.gui.*;
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

        Utils.printBlobsToCSV(preprocessedBlobs);

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
        evaluate();

//        Class clazz = Extract_Streets.class;
//        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
//        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
//        System.setProperty("plugins.dir", pluginsDir);
//        new ImageJ();
//        ImagePlus image = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\" + EvaluationConstants.COLORED + "\\LoG\\" + EvaluationConstants.FILE_NAME + ".png");
//        //ImagePlus image = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\new_pale_data_log.png");
//        image.show();
//        IJ.runPlugIn(clazz.getName(), "");
    }

    private static void evaluate() {
        new ImageJ();

        ImagePlus inputImage = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\" + EvaluationConstants.COLORED + "\\LoG_preprocessed\\" + EvaluationConstants.FILE_NAME + ".png");
        ImagePlus resultImage = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\" + EvaluationConstants.COLORED + "\\result_old\\" + EvaluationConstants.FILE_NAME + ".png");
        ImagePlus groundTruthImage = IJ.openImage("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\" + EvaluationConstants.COLORED + "\\ground truth\\" + EvaluationConstants.FILE_NAME + ".png");

        ImagePlus evaluationImage = NewImage.createRGBImage("Evaluation Image", resultImage.getWidth(), resultImage.getHeight(), 1, 4);
        ImagePlus streetsDetectedImage = NewImage.createRGBImage("streets detected", resultImage.getWidth(), resultImage.getHeight(), 1, 4);

        ManyBlobs resultBlobs = new ManyBlobs(resultImage);
        resultBlobs.findConnectedComponents();
        resultBlobs.createLineOrdering();

        double[] precisionRecall = calculatePrecisionRecall(resultBlobs, inputImage, groundTruthImage, evaluationImage);
        double[] streetsDetected = calculateStreetsDetected(resultBlobs, resultImage, groundTruthImage, streetsDetectedImage);

        System.out.println("Precision: " + precisionRecall[0]);
        System.out.println("Recall: " + precisionRecall[1]);
        System.out.println("Detected street coverage: " + streetsDetected[0]);
        System.out.println("Detected streets: " + streetsDetected[1]);

        evaluationImage.show();
        evaluationImage.updateAndDraw();

        streetsDetectedImage.show();
        streetsDetectedImage.updateAndDraw();

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("C:\\Users\\Hirsch\\Desktop\\Forschungsprojekt\\" + EvaluationConstants.COLORED + "\\quantity_new.txt", true)));
            out.println(EvaluationConstants.FILE_NAME + ".png");
            out.println("precision: " + precisionRecall[0]);
            out.println("recall: " + precisionRecall[1]);
            out.println("Detected street coverage: " + streetsDetected[0]);
            out.println("Detected streets: " + streetsDetected[1]);
            out.println("--------------------------");
            out.close();
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }

    private static double[] calculatePrecisionRecall(ManyBlobs resultBlobs, ImagePlus inputImage, ImagePlus groundTruthImage, ImagePlus evaluationImage) {
        ImageProcessor groundTruthProcessor = groundTruthImage.getProcessor();

        ManyBlobs inputBlobs = new ManyBlobs(inputImage);
        inputBlobs.findConnectedComponents();
        inputBlobs.createLineOrdering();

        ImageProcessor evaluationProcessor = evaluationImage.getProcessor();
        for (Blob blob : inputBlobs) {
            blob.draw(evaluationProcessor, 0, Color.BLACK);
        }

        double allRelevant = getRelevantBlobs(inputBlobs, groundTruthProcessor, evaluationImage, false);
        double right = getRelevantBlobs(resultBlobs, groundTruthProcessor, evaluationImage, true);

        double[] result = new double[2];
        result[0] = right / resultBlobs.size() * 100;
        result[1] = right / allRelevant * 100;

        return result;
    }

    private static double getRelevantBlobs(ManyBlobs inputBlobs, ImageProcessor groundTruthProcessor, ImagePlus evaluationImage, boolean isResultBlob) {
        ImageProcessor processor = evaluationImage.getProcessor();

        double relevant = 0;
        for (Blob blob : inputBlobs) {
            int[] contourX = blob.getLineX();
            int[] contourY = blob.getLineY();
            double rightPixels = 0;
            Color color;

            for (int i = 0; i < blob.getLength(); i++) {
                if (groundTruthProcessor.getPixel(contourX[i], contourY[i]) == -65316) {
                    rightPixels++;
                }
            }

            if (rightPixels / blob.getLength() > 0.5) {
                relevant++;
                color = isResultBlob ? Color.GREEN : Color.ORANGE;
            } else {
                color = isResultBlob ? Color.RED : Color.LIGHT_GRAY;
            }

            blob.draw(processor, 0, color);
        }

        return relevant;
    }

    private static double[] calculateStreetsDetected(ManyBlobs resultBlobs, ImagePlus resultImage, ImagePlus groundTruthImage, ImagePlus evaluationImage) {
        int width = resultImage.getWidth();
        int height = resultImage.getHeight();

        ImageProcessor evaluationImageProcessor = evaluationImage.getProcessor();
        ImageProcessor groundTruthProcessor = groundTruthImage.getProcessor();

        ImagePlus skelettonImage = NewImage.createByteImage("skelettonize Image", width, height, 1, 4);
        ByteProcessor skelettonProcessor = (ByteProcessor) skelettonImage.getProcessor();

        byte[] skelettonPixels = (byte[]) skelettonProcessor.getPixels();
        byte[] resultPixels = (byte[]) resultImage.getProcessor().getPixels();

        for (Blob blob : resultBlobs) {
            blob.draw(evaluationImageProcessor, 0, Color.BLACK);
        }

        //make manually drawn pixels black
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (groundTruthProcessor.getPixel(x, y) == -65316) {
                    skelettonPixels[y * width + x] = 0;
                }
            }
        }

        //skeletonize manually drawn streets
        BinaryProcessor binaryProcessor = new BinaryProcessor(skelettonProcessor);
        binaryProcessor.skeletonize();
        byte[] outputPixels = new byte[width * height];

        //disconnect crossed lines
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int index = y * width + x;
                if (skelettonPixels[index] == 0) {
                    if (getNeighborCount(x, y, skelettonPixels, width, height) > 2) {
                        outputPixels[index] = (byte) 255;
                    } else {
                        outputPixels[index] = (byte) 0;
                    }
                } else {
                    outputPixels[index] = (byte) 255;
                }
            }
        }

        skelettonProcessor.setPixels(outputPixels);
        skelettonImage.updateImage();

        ManyBlobs groundTruthBlobs = new ManyBlobs(skelettonImage);
        groundTruthBlobs.findConnectedComponents();
        groundTruthBlobs.createLineOrdering();

        int sampleRate = 5;
        int maxStreetWidth = 28;

        double pointsDetected = 0;
        double allPoints = 0;
        double streetsDetected = 0;
        double allStreets = 0;

        for (Blob blob : groundTruthBlobs) {
            if (blob.getLength() > 60) {
                allStreets++;
                int[] contourX = blob.getLineX();
                int[] contourY = blob.getLineY();
                int max = blob.getLength() - sampleRate;
                int foundPixels = 0;

                for (int i = 0; i <= max; i++) {
                    int dX = contourX[i + sampleRate - 1] - contourX[i];
                    int dY = contourY[i + sampleRate - 1] - contourY[i];

                    double length = Math.sqrt(dX * dX + dY * dY);
                    double vecXNorm = dX * (1 / length);
                    double vecYNorm = dY * (1 / length);

                    int vecXEnd = (int) (-vecYNorm * maxStreetWidth);
                    int vecYEnd = (int) (vecXNorm * maxStreetWidth);

                    int baseX = contourX[i + sampleRate / 2];
                    int baseY = contourY[i + sampleRate / 2];

                    if (resultPixels[baseY * width + baseX] == 0) {
                        foundPixels++;
                    } else {
                        java.util.List<Point> points = Utils.getBresenhamPoints4Connected(baseX, baseY, baseX + vecXEnd, baseY + vecYEnd);
                        points.addAll(Utils.getBresenhamPoints4Connected(baseX, baseY, baseX - vecXEnd, baseY - vecYEnd));

                        for (Point p : points) {
                            if (isInImageRange(p.x, p.y, width, height) && resultPixels[p.y * width + p.x] == 0) {
                                foundPixels++;
                                break;
                            }
                        }
                    }
                }

                pointsDetected += foundPixels;
                allPoints += max;
                if ((double)foundPixels / max >= 0.5) {
                    streetsDetected++;
                    blob.draw(evaluationImageProcessor, 0, Color.GREEN);
                } else {
                    blob.draw(evaluationImageProcessor, 0, Color.RED);
                }
            }
        }

        double[] result = new double[2];
        result[0] = pointsDetected / allPoints * 100;
        result[1] = streetsDetected / allStreets * 100;

        return result;
    }

    private static int getNeighborCount(int x, int y, byte[] pixels, int width, int height) {
        int result = 0;
        int newX;
        int newY;

        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (i != 0 || j != 0) {
                    newX = x + i;
                    newY = y + j;
                    if (isInImageRange(newX, newY, width, height) && pixels[newY * width + newX] == 0) {
                        result++;
                    }
                }
            }
        }

        return result;
    }

    private static boolean isInImageRange(int x, int y, int width, int height) {
        return x > -1 && x < width && y > -1 && y < height;
    }
}