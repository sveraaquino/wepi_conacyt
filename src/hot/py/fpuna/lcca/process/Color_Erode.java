package py.fpuna.lcca.process;

import java.util.ArrayList;
import java.util.Iterator;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class Color_Erode implements PlugInFilter {
	private ImagePlus imagePl;
	ArrayList<Weight> weightList = new ArrayList<Weight>(); // Array of weight
	private ByteProcessor[] channel; // RGB channels
	int n; // Roi coordinate x
	int m; // Roi coordinate y

	// AUXILIARY CLASSES

	class Weight {
		Roi roi;
		int[] sum;
		int totalPixels;

		public Weight(Roi r, int[] s, int t) {
			this.roi = r;
			this.sum = s;
			this.totalPixels = t;
		}
	}

	class Pixel {

		public int u;
		public int v;

		// CONSTRUCTOR
		public Pixel(int u, int v) {
			this.u = u;
			this.v = v;
		}
	}

	// METHODS

	public void setWeightList() {

		Roi roi;
		int totalPixels;
		int width = imagePl.getWidth() / n;
		int height = imagePl.getHeight() / m;
		int xStart = 0, yStart = 0, rWidth = width, rHeight = height;

		// ITERATE THROUGH THE IMAGINARY MATRIX OF ROI'S

		for (int yr = 0; yr < m; yr++) { // Y
			// SET X VALUES TO DEFAULT
			xStart = 0;
			rWidth = width;

			if (yr == m - 1) // IF Y IS THE LAST ONE
				rHeight = imagePl.getHeight() - yStart;

			for (int xr = 0; xr < n; xr++) { // X

				if (xr == n - 1) // IF X IS THE LAST ONE
					rWidth = imagePl.getWidth() - xStart;

				// CREATE ROI AND ADD TO THE WEIGHTLIST
				System.out.println("Roi(" + xStart + "," + yStart + "," + rWidth + "," + rHeight + ")");
				roi = new Roi(xStart, yStart, rWidth, rHeight);
				totalPixels = rWidth * rHeight;
				weightList.add(new Weight(roi, getRGBSum(roi), totalPixels));
				xStart += width; // SHIFT STARTING X

			}

			yStart += height; // SHIFT STARTING Y
		}
	}

	private float[] getRealWeight(Pixel p, Pixel[] se) {

		// VARIABLES

		Pixel pixel = new Pixel(0, 0);
		Iterator<Weight> iterator = weightList.iterator();
		int numPixels = 0;
		Weight weight;
		float[] sum = new float[channel.length];

		// SET ALL SUMS TO 0

		for (int i = 0; i < channel.length; i++)
			sum[i] = 0;

		// ITERATE STRUCTURE ELEMENT

		for (int i = 0; i < se.length; i++) {

			pixel.u = p.u + se[i].u;
			pixel.v = p.v + se[i].v;

			// ADDITION OF NUMPIXELS AND SUM VECTOR

			while (iterator.hasNext()) {

				weight = iterator.next();

				if (weight.roi.contains(pixel.u, pixel.v)) {

					numPixels += weight.totalPixels;

					for (int j = 0; j < channel.length; j++)
						sum[j] += weight.sum[j];

				}
			}
		}

		// DIVIDE SUM BY NUMPIXELS

		for (int i = 0; i < channel.length; i++)
			sum[i] /= numPixels;

		return sum;
	} // end of getRegions();

	public int[] getRGBSum(Roi roi) {

		int cSize = channel.length;
		int hSize = channel[0].getHistogramSize();

		int[] sum = new int[cSize];

		int[][] channelHistogram = new int[cSize][hSize];

		for (int i = 0; i < cSize; i++) {
			channel[i].setRoi(roi);
			channelHistogram[i] = channel[i].getHistogram();
		}

		for (int i = 0; i < hSize; i++) {
			for (int k = 0; k < cSize; k++) {
				sum[k] = (sum[k] + (i + 1) * channelHistogram[k][i]);
			}
		}

		return sum;
	}

	public int[] min(float[] weight, Pixel[] SE, Pixel p) {

		int u, v;
		float t = 0.0f, min = 999999.9f;
		int cLength = channel.length;
		int[] idx = new int[cLength];
		int[] pixel = new int[cLength];
		int[] minP = new int[cLength];
		boolean findMinPixel = true;
		int j = 0;

		for (int i = 0; i < SE.length; i++) {

			u = p.u + SE[i].u;
			v = p.v + SE[i].v;

			// Se carga el pixel y su valor T(p).
			for (int k = 0; k < cLength; k++) {
				pixel[k] = channel[k].get(u, v);
				t = t + weight[k] * pixel[k];
			}

			if (t == min && !iqualPixels(minP, pixel)) {

				System.out.println("here");

				idx = maxWeight(weight);

				while (findMinPixel) {

					if (pixel[idx[j]] < minP[idx[j]]) {
						min = t;
						minP = getRGB(pixel);
						findMinPixel = false;
					} else {
						j++;
					}
				}
				findMinPixel = true;

			} else if (t < min) {
				min = t;
				minP = getRGB(pixel);
			}

			t = 0;
		}
		return minP;
	}

	public int[] maxWeight(float[] w) {

		int aux;
		int[] idx = new int[w.length];
		float actualW, nextW;

		for (int i = 0; i < w.length; i++) {
			idx[i] = i;
		}

		for (int j = 1; j < w.length; j++) {
			for (int i = 0; i < w.length - j; i++) {

				actualW = w[idx[i]];
				nextW = w[idx[i + 1]];

				if (actualW < nextW) {

					aux = idx[i];
					idx[i] = idx[i + 1];
					idx[i + 1] = aux;
				}
			}
		}

		// Prioridad en caso de que los pesos sean iguales en el formato RGB.
		if (w[0] == w[1] && idx[1] == 1) {
			aux = idx[0];
			idx[0] = idx[1];
			idx[1] = aux;
		}

		return idx;
	}

	public int[] getRGB(int[] pixel) {

		int[] p = new int[pixel.length];

		for (int i = 0; i < pixel.length; i++) {
			p[i] = pixel[i];
		}

		return p;
	}

	public boolean iqualPixels(int[] p1, int[] p2) {

		for (int i = 0; i < p1.length; i++) {
			if (p1[i] != p2[i]) {
				return false;
			}
		}
		return true;
	}

	public int setup(String arg, ImagePlus imp) {
		if (imp == null) {
			return DONE;
		}
		if (!showDialog()) {
			return DONE;
		}
		return DOES_ALL;

	}

	public boolean showDialog() {
		GenericDialog gd = new GenericDialog("ICIP");
		gd.addNumericField("M value:", 0, 0);
		gd.addNumericField("N value:", 0, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		m = (int) gd.getNextNumber();
		n = (int) gd.getNextNumber();
		return true;
	}

	public void run(ImageProcessor ip) {
		imagePl = new ImagePlus();
		imagePl.setProcessor(ip);
		ColorProcessor cp = (ColorProcessor) ip;
		channel = new ByteProcessor[cp.getNChannels()];

		for (int i = 0; i < channel.length; i++) {
			channel[i] = cp.getChannel(i + 1, channel[i]);
		}
		setWeightList();
		// Create 4N Structuring Element.

		int[] rFour = { -1, 0, 0, 0, 1 };
		int[] cFour = { 0, -1, 0, 1, 0 };
		Pixel[] seFour = new Pixel[rFour.length];

		for (int i = 0; i < rFour.length; i++) {
			seFour[i] = new Pixel(rFour[i], cFour[i]);
		}

		// Create 8N Structuring Element.

		int[] rEight = { -1, -1, -1, 0, 0, 0, 1, 1, 1 };
		int[] cEight = { -1, 0, 1, -1, 0, 1, -1, 0, 1 };
		Pixel[] seEight = new Pixel[rEight.length];

		for (int i = 0; i < rEight.length; i++) {
			seEight[i] = new Pixel(rEight[i], cEight[i]);
		}

		// RUN MAIN

		ColorProcessor iPr = cp;
		// Auxiliary variables

		int[] minP;
		float[] realWeight;

		// NOTA: CAMBIAR EL "-1"
		// Roams through the image

		for (int u = 1; u < iPr.getWidth() - 1; u++) {
			for (int v = 1; v < iPr.getHeight() - 1; v++) {
				realWeight = getRealWeight(new Pixel(u, v), seFour);
				minP = min(realWeight, seFour, new Pixel(u, v));
				iPr.putPixel(u, v, minP);
			}
		}
	}

	public void setParams(int m, int n) {
		this.m = m;
		this.n = n;
	}
}
