import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.Binary;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class ColoredStreetsExtractor implements StreetsExtractor {
    private final ImagePlus _image;
    private final int _width;
    private final int _height;
    private final int _minHue;
    private final int _maxHue;
    private final int _minSat;
    private final int _maxSat;
    private final int _minBrght;
    private final int _maxBrght;
    private final int _streetWidth;

    public ColoredStreetsExtractor(ImagePlus image, int minHue, int maxHue, int minSat, int maxSat, int minBrght, int maxBrght, int streetWidth) {
        _image = image;
        _width = _image.getWidth();
        _height = _image.getHeight();
        _minHue = minHue;
        _maxHue = maxHue;
        _minSat = minSat;
        _maxSat = maxSat;
        _minBrght = minBrght;
        _maxBrght = maxBrght;
        _streetWidth = streetWidth;
    }

    public ImagePlus process() {
        ImagePlus segmentationImage = this.getSegmentationImage(_image);
        return this.getMorphologicalFilteredImage(segmentationImage);
    }

    private ImagePlus getSegmentationImage(ImagePlus input) {
        ImagePlus filtered = NewImage.createByteImage("segmented image", _width, _height, 1, 4);
        ImageProcessor filteredProcessor = filtered.getProcessor();
        byte[] filteredPixels = (byte[])filteredProcessor.getPixels();

        ColorProcessor colorProcessor = new ColorProcessor(input.getImage());
        byte[] h = new byte[_width * _height];
        byte[] s = new byte[_width * _height];
        byte[] b = new byte[_width * _height];
        colorProcessor.getHSB(h, s, b);

        for(int y = 0; y < _height; ++y) {
            for(int x = 0; x < _width; ++x) {
                int index = y * _width + x;
                int hue = h[index] & 255;
                int saturation = s[index] & 255;
                int brightness = b[index] & 255;
                if(hue >= _minHue && hue <= _maxHue && saturation >= _minSat && saturation <= _maxSat && brightness >= _minBrght && brightness <= _maxBrght) {
                    filteredPixels[index] = 0;
                }
            }
        }

        filtered.show();
        filtered.updateAndDraw();
        return filtered;
    }

    private ImagePlus getMorphologicalFilteredImage(ImagePlus segmentationImage) {
        ImagePlus morphologyImage = new ImagePlus("morphology image", segmentationImage.getImage());
        ImageProcessor morphologyProcessor = morphologyImage.getProcessor();


        //Fill holes in binary objects
        Binary binary = new Binary();
        binary.setup("fill", morphologyImage);
        morphologyProcessor.invertLut();
        binary.run(morphologyProcessor);

        //Remove objects bigger than street width
        morphologyProcessor.invertLut();
        for (int i = 0; i < _streetWidth / 2; i++) {
            morphologyProcessor.erode();
        }
        for (int i = 0; i < _streetWidth / 2; i++) {
            morphologyProcessor.dilate();
        }

        ImageCalculator calculator = new ImageCalculator();
        //calculator.run("Subtract", )

        morphologyImage.updateImage();
        return morphologyImage;

        //morphologyImage.show();
        //morphologyImage.updateAndDraw();
    }
}
