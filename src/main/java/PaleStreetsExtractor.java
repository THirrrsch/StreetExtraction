import calculation.Preprocessor;
import blob.Blob;
import blob.ManyBlobs;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;

public class PaleStreetsExtractor implements StreetsExtractor {

    private final ImagePlus _cannyImage;
    //private final FeatureEvaluator _evaluator;

    private ImagePlus combinedImage;

    public PaleStreetsExtractor(ImagePlus cannyImage) {
        _cannyImage = cannyImage;

        // find initial blobs
        ManyBlobs rawBlobs = new ManyBlobs(_cannyImage);
        rawBlobs.findConnectedComponents();

        // 1 cut off high curvature parts
        // 2 remove super short lines
        // 3 calculate dotted lines for the calculation of parallel coverage
        Preprocessor preprocessor = new Preprocessor(_cannyImage);
        ManyBlobs preprocessedBlobs = preprocessor.process();

        ImagePlus preprocessedImage = NewImage.createByteImage("preprocessed image", _cannyImage.getWidth(), _cannyImage.getHeight(), 1, 4);
        ImageProcessor preprocessedProcessor = preprocessedImage.getProcessor();

        // 1 get the (preprocessed) blobs to work with
        // 2 draw them on a new image
        // 3 calculate necessary features
        for (Blob blob : preprocessedBlobs) {
            blob.draw(preprocessedProcessor);
        }
        preprocessedImage.show();
        preprocessedImage.updateAndDraw();
        preprocessedBlobs.computeFeatures();

        // 1 show features of each blob in ResultsTable
        ResultsTable rt = new ResultsTable();
        for (Blob blob : preprocessedBlobs) {
            rt.incrementCounter();

            //rt.addValue("Index", rt.getCounter());
            rt.addValue("Length", blob.getLength());
            rt.addValue("isInCluster", String.valueOf(blob.isInCluster()));
            rt.addValue("Parallel Coverage", blob.getParallelCoverage());
            rt.addValue("Line Following segments", blob.getLineFollowingElements());
        }
        rt.show("Features");

        //IJ.getTextPanel().addMouseListener(new ResultsTableSelectionDrawer(preprocessedImage, preprocessedBlobs, rt));

        //_evaluator = new FeatureEvaluator(preprocessedBlobs, _cannyImage.getWidth(), _cannyImage.getHeight());
    }

    public ImagePlus process() {
        //return _evaluator.evaluate();
        return null;
    }
}
