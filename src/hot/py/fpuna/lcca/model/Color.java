package py.fpuna.lcca.model;

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.BinaryProcessor;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

import javax.imageio.ImageIO;

import py.fpuna.lcca.process.Canny_Edge_Detector;
import py.fpuna.lcca.process.ColorClosing;
import py.fpuna.lcca.process.ColorDilate;
import py.fpuna.lcca.process.ColorErode;
import py.fpuna.lcca.process.ColorOpening;
import py.fpuna.lcca.process.Color_Dilate;
import py.fpuna.lcca.process.Color_Erode;
import py.fpuna.lcca.process.Color_Space_Converter;
import py.fpuna.lcca.process.Deinterlace;
import py.fpuna.lcca.process.FFT;
import py.fpuna.lcca.process.GrayConverter;
import py.fpuna.lcca.process.Kuwahara_Filter;
import py.fpuna.lcca.process.Lipschitz;
import py.fpuna.lcca.util.Image;
import py.fpuna.lcca.util.MyException;

/**
 * Clase color. Representa las Imagenes a color (RGB)
 */
public class Color extends Image implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	private ImageProcessor bp;

	public Color(ImageProcessor a) {
		bp = a;

	}

	/**
	 * Aplica contraste automaticamente a la imagen.
	 * 
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color autoContrat() throws IOException {
		ImageProcessor bp = this.bp.duplicate();
		int low, high, value;
		low = (int) bp.getMin();
		high = (int) bp.getMax();
		if (low == high)
			throw new MyException("200");
		System.out.println(low + "-" + high);
		int h = bp.getHeight();
		int w = bp.getWidth();
		int y, x;
		for (y = 0; y < h; y++) {
			for (x = 0; x < w; x++) {
				value = (bp.get(x, y) - low) * (255 / (high - low));
				bp.set(x, y, value);
			}
		}
		Color result = new Color(bp);
		result.copyAtribute("autoContrat", this);

		return result;
	}

	/**
	 * Difumina la imagen.
	 * 
	 * @return imagen a color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color blur() throws IOException {
		Color out = this.duplicate();
		int[] BLUR1 = { 1, 2, 1, 2, 4, 2, 1, 2, 1 };
		out.bp.convolve3x3(BLUR1);

		out.copyAtribute("Blur_", this);
		return out;
	}

	/**
	 * Realiza la transformada bottom hat.
	 * 
	 * @param iteration
	 *            número de iteraciones
	 * @return imagen a color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color bottomHat(int iterations) throws IOException {
		Color c = this.colorClosing(iterations);
		Color th = c.operationBetween(this, "DIFFERENCE");
		th.copyAtribute("ColorBottomHat_", this);
		return th;
	}

	/**
	 * Aplica brillo a la imagen de acuerdo a un valor recibido.
	 * 
	 * @param p
	 *            Cantidad de brillo a aplicar.
	 * @return imagen a color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color brightness(int p) throws IOException {
		ImageProcessor bp = this.bp.duplicate();
		bp.add(p);
		Color result = new Color(bp);
		result.copyAtribute("brightness", this);

		return result;
	}

	/**
	 * Canny edge detector. Realiza la deteccion de bordes de Canny
	 * 
	 * 
	 * @param radius
	 *            radio del kernel
	 * @param low
	 *            umbral inferior
	 * @param high
	 *            umbral superior
	 * @param normalize
	 *            normalizar la imagen
	 * @return imagen binaria
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Binary cannyEdgeDetector(double radius, double low, double high, boolean normalize) throws IOException {
		Canny_Edge_Detector c = new Canny_Edge_Detector();
		ImageProcessor ip = c.wepi_run(this.bp, (float) radius, (float) low, (float) high, normalize);
		Binary out = new Binary((BinaryProcessor) ip);
		out.copyAtribute("CannyEdge_", this);
		return out;
	}

	/**
	 * Realiza la clausura
	 * 
	 * @param iteration
	 *            número de iteraciones
	 * @return imagen a color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color colorClosing(int iterations) throws IOException {
		ColorClosing c = new ColorClosing();
		ImageProcessor ip = c.wepi_run(this.bp, iterations);
		Color out = new Color(ip);
		out.copyAtribute("ColorClosing_", this);
		return out;
	}

	/**
	 * 
	 * Realiza la dilatación implementada en el ImageJ
	 * 
	 * @param iterations
	 *            numero de iteraciones
	 * @return imagen a color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color colorDilate(int iterations) throws IOException {
		ColorDilate c = new ColorDilate();
		ImageProcessor ip = c.wepi_run(this.bp, iterations);
		Color out = new Color(ip);
		out.copyAtribute("ColorDilate_", this);
		return out;
	}

	/**
	 * Realiza la erosión del ImageJ.
	 * 
	 * @param iteration
	 *            número de iteraciones
	 * @return imagen a color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color colorErode(int iterations) throws IOException {
		ColorErode c = new ColorErode();
		ImageProcessor ip = c.wepi_run(this.bp, iterations);
		Color out = new Color(ip);
		out.copyAtribute("ColorErode_", this);
		return out;
	}

	/**
	 * 
	 * Realiza la apertura
	 * 
	 * @param iteration
	 *            número de iteraciones
	 * @return imagen a color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color colorOpening(int iterations) throws IOException {
		ColorOpening c = new ColorOpening();
		ImageProcessor ip = c.wepi_run(this.bp, iterations);
		Color out = new Color(ip);
		out.copyAtribute("ColorOpening_", this);
		return out;
	}

	/**
	 * Aplica contraste a la imagen de acuerdo a un valor recibido.
	 * 
	 * @param p
	 *            Cantidad de contraste a aplicar.
	 * @return imagen a color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color constrast(double p) throws IOException {
		ImageProcessor bp = this.bp.duplicate();
		bp.multiply(p + 1);
		Color result = new Color(bp);
		result.copyAtribute("constrast", this);

		return result;
	}

	/**
	 * Convierte una imagen a grises.
	 * 
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Gray convertToGray() throws IOException {
		ByteProcessor ret = (ByteProcessor) bp.convertToByte(false);

		Gray result = new Gray(ret);
		result.copyAtribute("Gray", this);
		return result;
	}

	/**
	 * Desentrelaza la imagen
	 * 
	 * @param choice
	 *            metodo de desentrelazado
	 * @return imagen a color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color deinterlace(int choice) throws IOException {
		Deinterlace d = new Deinterlace();
		d.wepi_setup(choice);
		ImageProcessor ip = d.wepi_run(this.bp);
		Color out = new Color(ip);
		out.copyAtribute("Deinterlace" + (choice == 0 ? "Even" : "Odd") + "_", this);
		return out;
	}

	/**
	 * Realiza la dilatación.
	 * 
	 * @param m
	 *            Ancho de la ventana.
	 * @param n
	 *            Alto de la ventana.
	 * @return imagen a color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color dilate(int m, int n) throws IOException {
		if (m == 0 && n == 0)
			throw new MyException("validator.greaterThanZero");// error parametro

		ImageProcessor ip = this.bp.duplicate();
		Color_Dilate dilate = new Color_Dilate();

		dilate.setParams(m, n);
		dilate.run(ip);

		Color result = new Color(ip);
		result.copyAtribute("colorDilate" + m + "x" + n, this);
		return result;
	}

	/**
	 * Devuelve una copia de la imagen a color.
	 * 
	 * @return la copia
	 */
	public Color duplicate() {
		Color out = new Color(this.bp.duplicate());
		return out;
	}

	/**
	 * Realiza la erosión.
	 * 
	 * @param m
	 *            Ancho de la ventana.
	 * @param n
	 *            Alto de la ventana.
	 * @return imagen a color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color erode(int m, int n) throws IOException {
		Color_Erode erode = new Color_Erode();
		erode.setParams(m, n);

		ImageProcessor ip = this.bp.duplicate();
		erode.run(ip);

		Color result = new Color(ip);
		result.copyAtribute("colorErode" + m + "x" + n, this);
		return result;
	}

	/**
	 * Realiza la transformada de fourier
	 * 
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Gray fft() throws IOException {
		FFT f = new FFT();
		ImageProcessor ip = f.wepi_run(null, this.bp);
		Gray out = new Gray((ByteProcessor) ip);
		out.copyAtribute("FFT_", this);
		return out;
	}

	/**
	 * Aplica un filtro de Kuwahara a la imagen.
	 * 
	 * @param size
	 *            tamaño de la venatana
	 * @param filterRGB
	 *            filtrar cada canal por separado
	 * @return imagen a color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */

	public Color filterKuwahara(int size, boolean filterRGB) throws IOException {
		if (size >= 3 && size % 2 == 0)
			throw new MyException("validator.impar");

		Kuwahara_Filter k = new Kuwahara_Filter();
		k.setIsRGB(true);
		k.setSize(size);
		k.setfilterRGB(filterRGB);

		ImageProcessor ip = this.bp.duplicate();
		k.run(ip);

		Color out = new Color(ip);
		out.copyAtribute("filterKuwahara" + "-size:" + size + (filterRGB ? "-filterRGB" : ""), this);

		return out;
	}

	/**
	 * Aplica el filtro Lipschitz
	 * 
	 * @param slope
	 *            pendiente
	 * @param down
	 *            realizar bottomHat
	 * @param hat
	 *            realizar topHat
	 * @return the color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color filterLipschitz(double slope, boolean down, boolean hat) throws IOException {
		Lipschitz l = new Lipschitz();
		ImagePlus imp = l.wepi_run(this.bp, slope, down, hat);

		Color result = new Color(imp.getProcessor());
		result.copyAtribute("filterLipschitz" + "-slope:" + slope + (down ? "-down" : "") + (hat ? "-hat" : ""), this);
		return result;
	}

	/**
	 * Recupera la informacion de una imagen.
	 * 
	 * @return byte[]
	 * @throws MyException
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */

	@Override
	public byte[] getData() throws MyException, IOException {
		BufferedImage img = this.bp.getBufferedImage();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(img, "jpg", baos);
		baos.flush();
		byte[] res = baos.toByteArray();
		baos.close();
		return res;
	}

	/**
	 * Devuelve el alto de una imagen.
	 * 
	 * @return int
	 * @throws MyException
	 *             the my exception
	 */

	@Override
	public int getHeight() throws MyException {

		return bp.getHeight();
	}

	/**
	 * Devuelve el histograma de una imagen.
	 * 
	 * @return int[][]
	 */

	public int[][] getHistogram() {
		int[][] histogram = new int[3][256];
		int pixel, r, g, b;
		for (int i = 0; i < bp.getWidth(); i++) {
			for (int j = 0; j < bp.getHeight(); j++) {
				pixel = bp.getPixel(i, j);
				r = (pixel & 0xff0000) >> 16;
				g = (pixel & 0x00ff00) >> 8;
				b = (pixel & 0x0000ff);
				histogram[0][r]++;
				histogram[1][g]++;
				histogram[2][b]++;
			}
		}
		return histogram;
	}

	/**
	 * Devuelve el ancho de una imagen.
	 * 
	 * @return int
	 * @throws MyException
	 */

	@Override
	public int getWidth() throws MyException {
		return bp.getWidth();

	}

	/**
	 * Convierte a escala de grises. G(x,y) = (R' + G' + B')/3
	 * 
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Gray gleam() throws IOException {
		GrayConverter gc = new GrayConverter();

		gc.wepi_setup(1);
		ByteProcessor ret = (ByteProcessor) gc.wepi_run(this.bp);
		Gray result = new Gray(ret);
		result.copyAtribute("Gray_Gleam", this);
		return result;
	}

	/**
	 * Convierte a escala de gris con n sombras
	 * 
	 * @param shades
	 *            numero de sombras de gris
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Gray grayShades(int shades) throws IOException {
		GrayConverter gc = new GrayConverter();

		gc.wepi_setup(9, shades);
		ByteProcessor ret = (ByteProcessor) gc.wepi_run(this.bp);
		Gray result = new Gray(ret);
		result.copyAtribute("Gray_" + shades + "shades", this);
		return result;
	}

	@Override
	public int[] histogram() {
		return bp.getHistogram();
	}

	/**
	 * Convierte a escala de grises. G(x,y) = (R+G+B)/3
	 * 
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Gray intensity() throws IOException {
		GrayConverter gc = new GrayConverter();

		gc.wepi_setup(0);
		ImageProcessor ret = gc.wepi_run(this.bp);
		Gray result = new Gray((ByteProcessor) ret);
		result.copyAtribute("Gray", this);
		return result;
	}

	/**
	 * Invierte la imagen.
	 * 
	 * @return imagen a color
	 * @throws MyException
	 *             the my exception
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */

	@Override
	public Color invertirImagen() throws MyException, IOException {

		ImageProcessor out = this.bp.duplicate();
		out.invert();

		Color result = new Color(out);
		result.copyAtribute("invertirImagen", this);

		return result;
	}

	/**
	 * Convierte a escala de grises. y = 0.2126 * p[0] + 0.7152 * p[1] + 0.0722 * p[2]; f = y > (6 / 29)^ 3 ? (y, 1) ^ 3 : 1 / 3 * (29 / 6)^2 * y + 4 / 29; G(x,y) = 1 / 100 * (116 * f - 16);
	 * 
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Gray lightness() throws IOException {
		GrayConverter gc = new GrayConverter();

		gc.wepi_setup(4);
		ByteProcessor ret = (ByteProcessor) gc.wepi_run(this.bp);
		Gray result = new Gray(ret);
		result.copyAtribute("Gray_lightness", this);
		return result;
	}

	/**
	 * Convierte a escala de grises. G(x,y) = 0.2126 * R' + 0.7152 * G' + 0.0722 * B'
	 * 
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Gray luma() throws IOException {
		GrayConverter gc = new GrayConverter();

		gc.wepi_setup(3);
		ByteProcessor ret = (ByteProcessor) gc.wepi_run(this.bp);
		Gray result = new Gray(ret);
		result.copyAtribute("Gray_luma", this);
		return result;
	}

	/**
	 * Convierte a escala de grises. G(x,y) = 0.3 * R + 0.59 * G + 0.11 * B
	 * 
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Gray luminance() throws IOException {
		GrayConverter gc = new GrayConverter();

		gc.wepi_setup(2);
		ByteProcessor ret = (ByteProcessor) gc.wepi_run(this.bp);
		Gray result = new Gray(ret);
		result.copyAtribute("Gray_luminance", this);
		return result;
	}

	/**
	 * Convierte a escala de grises. G(x,y) = (max(R,G,B) + min(R,G,B))/2
	 * 
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Gray luster() throws IOException {
		GrayConverter gc = new GrayConverter();

		gc.wepi_setup(6);
		ByteProcessor ret = (ByteProcessor) gc.wepi_run(this.bp);
		Gray result = new Gray(ret);
		result.copyAtribute("Gray_luster", this);
		return result;
	}

	/**
	 * Corre automaticamente un metodo (No implementado).
	 * 
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 * @return the color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	private Color methodChooser(String method, Object... params) throws IOException {
		PlugInFilter p = null;
		if (method.equals("Lipschitz_"))
			p = new Lipschitz();

		ImageProcessor ip = this.bp.duplicate();
		p.run(ip);

		Color result = new Color(ip);
		result.copyAtribute(method, this);
		return result;
	}

	/**
	 * Convierte a escala de grises.
	 * 
	 * G(x,y) = min (R,G,B)
	 * 
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Gray minDecomposition() throws IOException {
		GrayConverter gc = new GrayConverter();

		gc.wepi_setup(7);
		ByteProcessor ret = (ByteProcessor) gc.wepi_run(this.bp);
		Gray result = new Gray(ret);
		result.copyAtribute("Gray_minDecomposition", this);
		return result;
	}

	/**
	 * Realiza una operacion logica o aritmetica entre dos imagenes.
	 * 
	 * @param im2
	 *            La segunda imagen utilizada para la operacion.
	 * @param op
	 *            Indica la operacion a efectuar puede ser "AND", "OR", "XOR", "MAX", "MIN", "DIFFERENCE", "MULTIPLY", "DIVIDE".
	 * @return imagen a color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color operationBetween(Color im2, String op) throws IOException {
		Color out = this.duplicate();

		if (op.equals("AND"))
			out.bp.copyBits(im2.bp, 0, 0, Blitter.AND);
		else if (op.equals("OR"))
			out.bp.copyBits(im2.bp, 0, 0, Blitter.OR);
		else if (op.equals("XOR"))
			out.bp.copyBits(im2.bp, 0, 0, Blitter.XOR);
		else if (op.equals("MAX"))
			out.bp.copyBits(im2.bp, 0, 0, Blitter.MAX);
		else if (op.equals("MIN"))
			out.bp.copyBits(im2.bp, 0, 0, Blitter.MIN);
		else if (op.equals("DIFFERENCE"))
			out.bp.copyBits(im2.bp, 0, 0, Blitter.DIFFERENCE);
		else if (op.equals("MULTIPLY"))
			out.bp.copyBits(im2.bp, 0, 0, Blitter.MULTIPLY);
		else if (op.equals("DIVIDE"))
			out.bp.copyBits(im2.bp, 0, 0, Blitter.DIVIDE);
		else if (op.equals("SUBTRACT"))
			out.bp.copyBits(im2.bp, 0, 0, Blitter.SUBTRACT);
		else
			throw new MyException("Not an operation");

		out.copyAtribute(im2.getName() + "_" + op, this);

		return out;
	}

	/**
	 * Aumenta la nitidez de la imagen
	 * 
	 * @return imagen a color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color sharpen() throws IOException {
		Color out = this.duplicate();
		int[] SHARPEN = { 0, -1, 0, -1, 5, -1, 0, -1, 0 };
		out.bp.convolve3x3(SHARPEN);

		out.copyAtribute("Sharpen_", this);
		return out;
	}

	/**
	 * Conviertea escala de grises usando un solo canal. G(x,y) = p[canal]
	 * 
	 * @param channel
	 *            canal de colores
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Gray singleChannel(int channel) throws IOException {
		GrayConverter gc = new GrayConverter();

		gc.wepi_setup(8, channel);
		ByteProcessor ret = (ByteProcessor) gc.wepi_run(this.bp);
		Gray result = new Gray(ret);
		result.copyAtribute("Gray_" + (channel == 0 ? "Red" : (channel == 1 ? "Green" : "Blue")), this);
		return result;
	}

	/**
	 * Convierte de un espacio de color a otro.
	 * 
	 * @param from
	 *            espacio actual
	 * @param to
	 *            espacio destino
	 * @param whitepoint
	 *            whitepoint
	 * @return imagen a color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color spaceConverter(String from, String to, String whitepoint) throws IOException {
		Color_Space_Converter c = new Color_Space_Converter();

		ImageProcessor ip = this.bp.duplicate();
		c.wepi_run(ip, from, to, whitepoint);

		Color result = new Color(ip);
		result.copyAtribute("spaceConvert (" + to + ")", this);
		return result;
	}

	/**
	 * Realiza la transformada top hat
	 * 
	 * @param iteration
	 *            número de iteraciones
	 * @return imagen a color
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Color topHat(int iterations) throws IOException {
		ImageProcessor copy = this.bp.duplicate();
		Color o = this.colorOpening(iterations);
		copy.copyBits(o.bp, 0, 0, Blitter.DIFFERENCE);
		Color th = new Color(copy);
		th.copyAtribute("ColorTopHat_", this);
		return th;
	}

	/**
	 * Convierte a escala de grises. G(x,y) = max(R,G,B)
	 * 
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Gray value() throws IOException {
		GrayConverter gc = new GrayConverter();

		gc.wepi_setup(5);
		ByteProcessor ret = (ByteProcessor) gc.wepi_run(this.bp);
		Gray result = new Gray(ret);
		result.copyAtribute("Gray_value", this);
		return result;
	}

	/**
	 * Convierte a escala de gris con pesos personalizados. g(x,y) = r*R + g*G + b*B
	 * 
	 * @param r
	 *            peso del canal rojo
	 * @param g
	 *            peso del canal verde
	 * @param b
	 *            peso del canal azul
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Gray weighting(double r, double g, double b) throws IOException {
		GrayConverter gc = new GrayConverter();

		gc.wepi_setup(11, r, g, b);
		ByteProcessor ret = (ByteProcessor) gc.wepi_run(this.bp);
		Gray result = new Gray(ret);
		result.copyAtribute("Gray_" + r + "r" + g + "g" + b + "b", this);
		return result;
	}

	/**
	 * Realiza la segmentacion de acuerdo al parametro recibido.
	 * 
	 * @param seleccion
	 *            Indica que tipo de segmentacion realizar.
	 * 
	 * @return imagen a color
	 * 
	 * @throws IOException
	 */
	public Color segmentationSelection(int seleccion) throws IOException {
		ImageProcessor copy = this.bp.duplicate();
		Color out = new Color(copy);
		switch (seleccion) {
		case (1):
			int[] PwX = { -1, 0, 1, -1, 0, 1, -1, 0, 1 };
			out.bp.convolve3x3(PwX);
			out.copyAtribute("PrewittX_", this);
			return out;

		case (2):
			int[] PwY = { 1, 1, 1, 0, 0, 0, -1, -1, -1 };
			out.bp.convolve3x3(PwY);
			out.copyAtribute("PrewittY_", this);
			return out;

		case (4):
			int[] SbX = { -1, 0, 1, -2, 0, 2, -1, 0, 1 };
			out.bp.convolve3x3(SbX);
			out.copyAtribute("SobelX_", this);
			return out;

		case (5):
			int[] SbY = { -1, -2, -1, 0, 0, 0, 1, 2, 1 };
			out.bp.convolve3x3(SbY);
			out.copyAtribute("SobelY_", this);
			return out;

		case (7):
			int[] laplace = { 1, 1, 1, 1, -8, 1, 1, 1, 1 };
			out.bp.convolve3x3(laplace);
			out.copyAtribute("Laplace_", this);
			return out;

		default:
			out.copyAtribute("", this);
			return out;

		}

	}
}
