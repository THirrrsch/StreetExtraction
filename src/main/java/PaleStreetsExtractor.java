import Util.Preprocessor;
import blob.ManyBlobs;
import ij.ImagePlus;

public class PaleStreetsExtractor implements StreetsExtractor {

    private final ImagePlus _cannyImage;
    private final ManyBlobs _allBlobs;
    private ImagePlus combinedImage;

    public PaleStreetsExtractor(ImagePlus cannyImage) {

        _cannyImage = cannyImage;

        // find initial blobs
        ManyBlobs rawBlobs = new ManyBlobs(_cannyImage);
        rawBlobs.findConnectedComponents();

        // 1 cut off high curvature parts
        // 2 remove super short lines
        // 3 calculate dotted lines for the calculation of parallel coverage
        Preprocessor preprocessor = new Preprocessor(rawBlobs, _cannyImage);
        preprocessor.process();

        // 1 get the (preprocessed) blobs to work with
        // 2 calculate necessary features
        _allBlobs = preprocessor.getProcessedBlobs();
        _allBlobs.computeFeatures();
    }

    public ImagePlus process() {
        return null;
    }

}
