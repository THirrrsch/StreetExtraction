import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class ColoredStreetsExtractor implements StreetsExtractor {
    private final ImagePlus _image;
    private final int _minHue;
    private final int _maxHue;
    private final int _minSat;
    private final int _maxSat;
    private final int _minBrght;
    private final int _maxBrght;

    public ColoredStreetsExtractor(ImagePlus image, int minHue, int maxHue, int minSat, int maxSat, int minBrght, int maxBrght) {
        this._image = image;
        this._minHue = minHue;
        this._maxHue = maxHue;
        this._minSat = minSat;
        this._maxSat = maxSat;
        this._minBrght = minBrght;
        this._maxBrght = maxBrght;
    }

    public ImagePlus process() {
        ColorProcessor colorProcessor = new ColorProcessor(this._image.getImage());
        int width = this._image.getWidth();
        int height = this._image.getHeight();
        byte[] h = new byte[width * height];
        byte[] s = new byte[width * height];
        byte[] b = new byte[width * height];
        colorProcessor.getHSB(h, s, b);
        ImagePlus filtered = NewImage.createByteImage("corrected image", width, height, 1, 4);
        ImageProcessor filteredProcessor = filtered.getProcessor();
        byte[] filteredPixels = (byte[])filteredProcessor.getPixels();

        for(int y = 0; y < height; ++y) {
            for(int x = 0; x < width; ++x) {
                int index = y * width + x;
                int hue = h[index] & 255;
                int saturation = s[index] & 255;
                int brightness = b[index] & 255;
                if(hue >= this._minHue && hue <= this._maxHue && saturation >= this._minSat && saturation <= this._maxSat && brightness >= this._minBrght && brightness <= this._maxBrght) {
                    filteredPixels[index] = 0;
                }
            }
        }

        filteredProcessor.erode();
        filteredProcessor.dilate();
        filtered.updateImage();
        return filtered;
    }
}
