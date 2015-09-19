package py.fpuna.lcca.process;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class GrayConverter implements PlugInFilter {
	private ImageProcessor ip;
	private int choice;
	private String method;
	private int intParam;
	private double weights[];
	private static String[] methods = { "Intensity", "Gleam", "Luminance", "Luma", "Lightness", "Value", "Luster", "MinDecomposition", "Single color channel", "Custom # of gray shades",
			"Custom # of gray shades with dithering", "Custom weighting" };

	public ImageProcessor convertToGray() {
		int w = ip.getWidth();
		int h = ip.getHeight();
		int[] p = new int[3]; // rgb pixel values
		int gray;
		GrayValue g = grayValue();
		ImageProcessor bp = new ByteProcessor(w, h);
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				ip.getPixel(x, y, p);
				gray = g.val(p);
				bp.putPixel(x, y, gray);
			}
		}
		return bp;
	}

	public ImageProcessor singleChannel(int channel) {
		int w = ip.getWidth();
		int h = ip.getHeight();
		int[] p = new int[3]; // rgb pixel values
		int gray;
		ImageProcessor bp = new ByteProcessor(w, h);

		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				ip.getPixel(x, y, p);
				gray = p[channel];
				bp.putPixel(x, y, gray);
			}
		}
		return bp;
	}

	public ImageProcessor customGrayShades(int shades) {
		int ConversionFactor = 255 / (shades - 1);
		int w = ip.getWidth();
		int h = ip.getHeight();
		int[] p = new int[3]; // rgb pixel values
		int gray;
		ImageProcessor bp = new ByteProcessor(w, h);

		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				ip.getPixel(x, y, p);
				int AverageValue = (p[0] + p[1] + p[2]) / 3;
				gray = (int) ((AverageValue / ConversionFactor) + 0.5) * ConversionFactor;
				bp.putPixel(x, y, gray);
			}
		}
		return bp;
	}

	public ImageProcessor customWeighting() {
		ColorProcessor.setWeightingFactors(weights[0], weights[1], weights[2]);
		return ip.convertToByte(false);
	}

	private GrayValue grayValue() {
		switch (choice) {
		case 0:
			return new GrayValue() {
				public int val(int[] p) {
					return Methods.intensity(p);
				}
			};
		case 1:
			return new GrayValue() {
				public int val(int[] p) {
					return Methods.gleam(p);
				}
			};
		case 2:
			return new GrayValue() {
				public int val(int[] p) {
					return Methods.luminance(p);
				}
			};
		case 3:
			return new GrayValue() {
				public int val(int[] p) {
					return Methods.luma(p);
				}
			};

		case 4:
			return new GrayValue() {
				public int val(int[] p) {
					return Methods.lightness(p);
				}
			};
		case 5:
			return new GrayValue() {
				public int val(int[] p) {
					return Methods.value(p);
				}
			};
		case 6:
			return new GrayValue() {
				public int val(int[] p) {
					return Methods.luster(p);
				}
			};
		case 7:
			return new GrayValue() {
				public int val(int[] p) {
					return Methods.minDecomposition(p);
				}
			};
		}
		return null;
	}

	public ImageProcessor methodChooser() {
		switch (choice) {
		case 8:
			return showSingleChannelDialog();
		case 9:
			return showCustomGrayShadesDialog();
		case 10:
			System.out.println("No implementado");
			return null;
		case 11:
			return showWeightingDialog();
		default:
			return convertToGray();
		}
	}

	public void run(ImageProcessor arg0) {
		if (!showDialog())
			return;
		ip = arg0.duplicate();
		ImageProcessor bp = methodChooser();
		if (bp == null)
			return;
		ImagePlus imp = new ImagePlus(method, bp);
		imp.show();
	}

	public int setup(String arg0, ImagePlus arg1) {
		if (arg1.getBitDepth() == 24) {
			return DOES_RGB;
		}
		return DONE;
	}

	private ImageProcessor showCustomGrayShadesDialog() {
		GenericDialog gd = new GenericDialog("Custom Shades of Gray");
		gd.addSlider("# Shades", 2, 256, 2);
		gd.showDialog();
		if (gd.wasCanceled())
			return null;
		return customGrayShades((int) gd.getNextNumber());
	}

	public boolean showDialog() {
		// create a dialog
		GenericDialog dialog = new GenericDialog("Color-to-Grayscale");
		dialog.addChoice("Method", methods, "Averaging");

		// show the dialog, quit if the user clicks "cancel"
		dialog.showDialog();
		if (dialog.wasCanceled())
			return false;

		// get options
		choice = dialog.getNextChoiceIndex();
		method = methods[choice];
		return true;
	}

	private ImageProcessor showSingleChannelDialog() {
		GenericDialog gd = new GenericDialog("Select color channel");
		String[] channels = { "Red", "Green", "Blue" };
		// gd.addRadioButtonGroup("Channels", channels, 3, 3, channels[0]);
		gd.addChoice("Channel", channels, "Red");
		gd.showDialog();
		if (gd.wasCanceled())
			return null;

		// String channel = gd.getNextRadioButton();
		String channel = gd.getNextChoice();
		int index = channel.equals("Red") ? 0 : (channel.equals("Blue") ? 1 : 2);
		method = method + "-" + channel;
		return singleChannel(index);
	}

	public ImageProcessor showWeightingDialog() {
		weights = ColorProcessor.getWeightingFactors();
		GenericDialog gd = new GenericDialog("Set weights");
		gd.addNumericField("Red", weights[0], 2, 5, "");
		gd.addNumericField("Green", weights[1], 2, 5, "");
		gd.addNumericField("Blue", weights[2], 2, 5, "");
		gd.showDialog();
		if (gd.wasCanceled())
			return null;
		for (int i = 0; i < 3; i++)
			weights[i] = (double) gd.getNextNumber();

		return customWeighting();
	}

	public ImageProcessor wepi_run(ImageProcessor arg0) {
		ip = arg0.duplicate();
		switch (choice) {
		case 8:
			return singleChannel(intParam);
		case 9:
			return customGrayShades(intParam);
		case 10:
			System.out.println("No implementado");
			return null;
		case 11:
			return customWeighting();
		default:
			return convertToGray();
		}
	}

	public boolean wepi_setup(Object... objects) {
		choice = (Integer) objects[0];
		method = methods[choice];

		if (objects.length == 2)
			intParam = (Integer) objects[1];
		// Parametros del customWeighting
		else if (objects.length == 4)
			weights = new double[] { (Double) objects[1], (Double) objects[2], (Double) objects[3] };
		return true;
	}
}

interface GrayValue {
	int val(int[] p);
}

class Methods {
	private static int gamma(int t) {
		double g = Math.pow((double) t, (double) 1);
		return (int) g;
	}

	public static int gleam(int[] p) {
		double g = ((gamma(p[0]) + gamma(p[1]) + gamma(p[2])) / 3);
		return (int) g;
	}

	public static int intensity(int[] p) {
		return (int) (0.3 * (p[0] + p[1] + p[2]));
	}

	public static int lightness(int[] p) {
		double y = 0.2126 * p[0] + 0.7152 * p[1] + 0.0722 * p[2];
		double f = y > Math.pow(6 / 29, 3) ? Math.pow(y, 1 / 3) : 1 / 3 * (29 / 6) * (29 / 6) * y + 4 / 29;
		double g = 1 / 100 * (116 * f - 16);
		return (int) g;
	}

	public static int luma(int[] p) {
		return (int) (0.2126 * gamma(p[0]) + 0.7152 * gamma(p[1]) + 0.0722 * gamma(p[2]));
	}

	public static int luminance(int[] p) {
		return (int) (0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]);
	}

	public static int luster(int[] p) {
		int max = Math.max(Math.max(p[0], p[1]), p[2]);
		int min = Math.min(Math.min(p[0], p[1]), p[2]);
		double g = 0.5 * (max + min);
		return (int) g;
	}

	public static int minDecomposition(int[] p) {
		return Math.min(Math.min(p[0], p[1]), p[2]);
	}

	public static int value(int[] p) {
		return Math.max(Math.max(p[0], p[1]), p[2]);
	}
}
