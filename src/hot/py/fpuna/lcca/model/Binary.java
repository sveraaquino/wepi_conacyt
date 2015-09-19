package py.fpuna.lcca.model;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;

import javax.imageio.ImageIO;
import java.io.IOException;

import ij.*;
import ij.process.*;

import py.fpuna.lcca.process.Canny_Edge_Detector;
import py.fpuna.lcca.process.Deinterlace;
import py.fpuna.lcca.process.FFT;
import py.fpuna.lcca.process.Watershed_Algorithm;
import py.fpuna.lcca.util.Image;
import py.fpuna.lcca.util.MyException;
import py.fpuna.lcca.util.parse.StructureElement;

/**
 * Binary representa las imágenes binarias.
 */
public class Binary extends Image implements Serializable {

	private static final long serialVersionUID = 1L;

	private BinaryProcessor bp;

	/** El elemento estructurante */
	private int[][] se;

	private int xCenter;

	private int yCenter;

	public Binary(BinaryProcessor a) {
		bp = a;
	}

	@Override
	public int[] histogram() {
		return bp.getHistogram();
	}

	/**
	 * Invierte la imagen.
	 * 
	 * @return imagen binaria
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	@Override
	public Binary invertirImagen() throws IOException {
		Binary out = this.duplicate();
		out.bp.invert();
		out.copyAtribute("InvertBinary", this);
		return out;
	}

	/**
	 * Genera el reflejo de una matriz.
	 * 
	 * @param se
	 *            Matriz a reflejar.
	 * @return int[][]
	 */
	@Override
	public int[][] reflect(int[][] se) {

		int N = se.length; // number of rows
		int M = se[0].length; // number of columns
		int[][] fse = new int[N][M];
		for (int j = 0; j < N; j++) {
			for (int i = 0; i < M; i++) {
				fse[j][i] = se[N - j - 1][M - i - 1];
			}
		}
		return fse;
	}

	/**
	 * Convierte una matriz de tipo String a int.
	 * 
	 * @param matrix
	 *            Matriz a convertir
	 * @return int [][]
	 */
	private int[][] parser(String matrix) {
		StructureElement se = new StructureElement(matrix);

		int[] mask = se.getMask();

		int we, he, x = 0;
		we = se.getWidth();
		he = se.getHeight();
		int[][] maskMatrix = new int[we][he];
		for (int i = 0; i < mask.length; i++)
			System.out.print(mask[i] + " ");
		System.out.println();
		for (int i = 0; i < we; i++) {
			for (int j = 0; j < he; j++) {
				maskMatrix[i][j] = mask[x++];
				System.out.print(maskMatrix[i][j] + " ");
			}
			System.out.println();
		}

		return maskMatrix;
	}

	/**
	 * Metodo que invoca a morphology.
	 * 
	 * @param a
	 *            Indica el tipo de operacion a realizar
	 * @param element
	 *            Elemento estructurante
	 * @param xc
	 *            El centro del elemento estructurante en el eje x
	 * @param yc
	 *            El centro del elemento estructurante en el eje y
	 * @param iterations
	 *            La cantidad de iteraciones a realizar
	 * @return imagen binaria
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Binary morphoBinaryInvoke(String a, String element, int xc, int yc, int iterations) throws IOException {

		int[][] matriz = parser(element);
		System.out.println(matriz[0][0] + " luego de conversion");
		if (xc < 0 || xc > matriz.length || yc < 0 || yc > matriz[0].length)
			throw new MyException("morphology.outOfRange");
		return morphology(a, matriz, xc, yc, iterations);

	}

	/**
	 * Dilata la imagen.
	 * 
	 * @param H
	 *            Elemento estructurante.
	 * @param xCenter
	 *            Centro del elemento estructurante en el eje x.
	 * @param yCenter
	 *            Centro del elemento estrucutrante en el eje y.
	 * @return imagen binaria
	 */
	@Override
	public Binary dilate(int[][] H, int xCenter, int yCenter) {
		if (H == null) {
			IJ.error("no structuring element");
			throw new MyException();
		}
		int ic = xCenter;
		int jc = yCenter;
		int i, j;
		BinaryProcessor tmp = new BinaryProcessor((ByteProcessor) bp.duplicate());

		for (j = 0; j < H.length; j++) {
			for (i = 0; i < H[j].length; i++) {
				if (H[j][i] > 0) { // this pixel is set
					// copy image into position (u-ch,v-cv)
					tmp.copyBits(bp, i - ic, j - jc, Blitter.MAX);
				}
			}
		}
		return new Binary(tmp);
	}

	/**
	 * Erosiona la imagen.
	 * 
	 * @param H
	 *            Elemento estructurante.
	 * @param xCenter
	 *            Centro del elemento estructurante en el eje x.
	 * @param yCenter
	 *            Centro del elemento estrucutrante en el eje y.
	 * @return imagen binaria
	 * @throws MyException
	 *             the my exception
	 */
	@Override
	public Binary erode(int[][] H, int xCenter, int yCenter) throws MyException {
		// dilates the background
		Binary ip = new Binary(bp);
		ip.bp.invert();
		// try{
		ip = ip.dilate(reflect(H), xCenter, yCenter);
		// }
		// catch (MyException exc) {
		// throw exc;
		// }
		ip.bp.invert();
		return ip;
	}

	/**
	 * Recupera la informacion de la imagen.
	 * 
	 * @return byte[]
	 * @throws MyException
	 *             the my exception
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	@Override
	public byte[] getData() throws MyException, IOException {
		BufferedImage img = this.bp.getBufferedImage();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(img, this.getTipo(), baos);
		baos.flush();
		byte[] res = baos.toByteArray();
		baos.close();
		return res;
	}

	/**
	 * Devuelve un duplicado de la imagen original.
	 * 
	 * @return imagen binaria
	 */
	public Binary duplicate() {

		Binary out = new Binary(new BinaryProcessor((ByteProcessor) bp.duplicate()));

		out.se = this.se;
		out.xCenter = this.xCenter;
		out.yCenter = this.yCenter;

		return out;
	}

	/**
	 * Realiza la transformada de fourier
	 * 
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public Gray fft() throws IOException {
		FFT f = new FFT();
		ImageProcessor ip = f.wepi_run(null, this.bp);
		Gray out = new Gray((ByteProcessor) ip);
		out.copyAtribute("FFT_", this);
		return out;
	}

	/**
	 * Realiza una operacion logica o aritmetica entre dos imagenes.
	 * 
	 * @param im2
	 *            La segunda imagen utilizada para la operacion.
	 * @param op
	 *            Indica la operacion a efectuar puede ser "AND", "OR", "XOR", "MAX", "MIN", "DIFFERENCE", "MULTIPLY", "DIVIDE".
	 * @return imagen binaria
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Binary operationBetween(Binary im2, String op) throws IOException {
		Binary out = this.duplicate();

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
		else
			throw new MyException("200");

		out.copyAtribute(im2.getName() + "_" + op, this);
		return out;
	}

	/**
	 * Devuelve el ancho de la imagen.
	 * 
	 * @return int
	 * @throws MyException
	 *             the my exception
	 */
	@Override
	public int getWidth() throws MyException {

		return bp.getWidth();

	}

	/**
	 * Devuelve el alto de la imagen.
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
	 * Aplica una operacion morfológica a una imagen.
	 * 
	 * @param a
	 *            El tipo de operacion morfologica que se aplica a la imagen, puede ser "dilate", "erode", "open", "close", "externalBound", "internalBound", "externalInternalBound", "BinaryOutline","Skeletonize".
	 * @param H
	 *            Elemento estructurante.
	 * @param xCenter
	 *            Centro en el eje x del elemento estructurante.
	 * @param yCenter
	 *            Centro en el eje y del elemento estructurante.
	 * @param iterations
	 *            Cantidad de iteraciones a realizar
	 * @return imagen binaria
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Binary morphology(String a, int[][] H, int xCenter, int yCenter, int iterations) throws IOException {
		this.se = H;
		this.xCenter = xCenter;
		this.yCenter = yCenter;
		Binary out = this.duplicate();

		if (a.equals("dilate")) {
			for (int i = 0; i < iterations; i++)
				out.dilate();
		} else if (a.equals("erode")) {
			for (int i = 0; i < iterations; i++)
				out.erode();
		} else if (a.equals("open")) {
			out.open(iterations);
		} else if (a.equals("close")) {
			out.close(iterations);
		} else if (a.equals("externalBound"))
			out.externalBoundary();
		else if (a.equals("internalBound"))
			out.internalBoundary();
		else if (a.equals("externalInternalBound"))
			out.internalExternalBoundary();
		else if (a.equals("BinaryOutline"))
			out.bp.outline();
		else if (a.equals("Skeletonize"))
			out.bp.outline();
		else
			throw new MyException("100");

		out.copyAtribute(a, this);
		return out;
	}

	/**
	 * Metodo privado que dilata la imagen.
	 * 
	 * @return void
	 */

	private void dilate() {
		int[][] H = se;
		int w = bp.getWidth();
		int h = bp.getHeight();
		int wH = H.length;
		int hH = H[0].length;

		if (xCenter >= wH || yCenter >= hH) {
			throw new MyException("Centros fuera de rango");
		}

		int[][] copy = bp.getIntArray();
		BinaryProcessor out = new BinaryProcessor((ByteProcessor) bp.duplicate());
		int v, u, k, fil, col, i, j;
		for (v = 0; v <= h - 1; v++) {
			for (u = 0; u <= w - 1; u++) {

				if (copy[u][v] == 0) {
					for (j = 0; j < hH; j++) {
						for (i = 0; i < wH; i++) {
							fil = v + j - xCenter;
							col = u + i - yCenter;

							if (fil >= 0 && fil < h && col >= 0 && col < w) {
								if (H[i][j] >= 1)

									out.set(col, fil, 0);
							}
						}
					}
				}
			}
		}
		bp = out;
	}

	/**
	 * Metodo privado que erosiona la imagen.
	 * 
	 * @return void
	 */
	private void erode() {
		bp.invert();
		this.dilate();
		bp.invert();
	}

	/**
	 * Metodo privado que realiza la apertura de la imagen.
	 * 
	 * @param iterations
	 *            the iterations
	 * @return void
	 */

	private void open(int iterations) {
		for (int i = 0; i < iterations; i++)
			this.erode();
		for (int i = 0; i < iterations; i++)
			this.dilate();
	}

	/**
	 * Metodo privado que realiza la clausura de la imagen.
	 * 
	 * @param iterations
	 *            the iterations
	 * @return void
	 */
	private void close(int iterations) {
		for (int i = 0; i < iterations; i++)
			this.dilate();
		for (int i = 0; i < iterations; i++)
			this.erode();
	}

	/**
	 * Metodo privado que realiza la deteccion de bordes interna de la imagen.
	 * 
	 * @return void
	 */
	private void internalBoundary() {
		Binary foreground = this.duplicate();
		foreground.erode();
		bp.copyBits(foreground.bp, 0, 0, Blitter.DIFFERENCE);
	}

	/**
	 * Metodo privado que realiza la deteccion de bordes externa de la imagen.
	 * 
	 * @return void
	 */
	private void externalBoundary() {
		// ByteProcessor foreground2 = (ByteProcessor)ip.duplicate();
		Binary foreground = this.duplicate();
		foreground.dilate();
		bp.copyBits(foreground.bp, 0, 0, Blitter.DIFFERENCE);
	}

	/**
	 * Metodo privado que realiza la deteccion de bordes interna y externa de la imagen.
	 * 
	 * @return void
	 */
	private void internalExternalBoundary() {

		Binary ip1 = this.duplicate();
		Binary ip2 = this.duplicate();
		ip1.dilate();
		ip2.erode();
		ip1.bp.copyBits(ip2.bp, 0, 0, Blitter.DIFFERENCE);
		bp.copyBits(ip1.bp, 0, 0, Blitter.COPY);
	}

	/**
	 * 
	 * Crea un marcador binario para reconstruir la imagen a partir de este.
	 * 
	 * @return imagen binaria
	 * 
	 * @throws IOException
	 */

	public Binary createMarker() throws IOException {
		int w = bp.getWidth();
		int h = bp.getHeight();
		int i, j;
		ByteProcessor ip2 = new ByteProcessor(w, h);
		BinaryProcessor bp2 = new BinaryProcessor(ip2);
		// BinaryProcessor bp2 = new
		// BinaryProcessor((ByteProcessor)bp.createProcessor(w, h));

		for (i = 0; i < h; i++) {
			for (j = 0; j < w; j++) {
				if (i == 0 || j == 0 || j == w - 1 || i == h - 1)
					bp2.set(j, i, 0);
				else
					bp2.set(j, i, 255);
			}
		}
		// if (background == 0) bp2.invert();
		Binary out = new Binary(bp2);
		out.copyAtribute("Marker", this);
		return out;
	}

	/**
	 * Metodo intermedio para realizar la reconstruccion.
	 * 
	 * @param toReconstruct
	 *            Una imagen binaria a partir del cual se reconstruye
	 * @return imagen binaria
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */

	public Binary preReconstruction(Binary toReconstruct) throws IOException {
		Binary out = toReconstruct.reconstruction(this);
		out.copyAtribute("Reconstruction", this);
		return out;
	}

	/**
	 * Realiza al reconstruccion de la imagen a partir de un marcador.
	 * 
	 * @param marker
	 *            Una imagen binaria a partir del cual se reconstruye la imagen original.
	 * @return imagen binaria
	 * 
	 */
	public Binary reconstruction(Binary marker) {

		ImageProcessor aux;
		Binary marker2 = marker.duplicate();
		int matriz[][] = { { 0, 1, 0 }, { 1, 1, 1 }, { 0, 1, 0 } };
		marker2.se = matriz;
		marker2.yCenter = 1;
		marker2.xCenter = 1;
		boolean idem = false;
		while (!idem) {
			aux = marker2.bp.duplicate();
			marker2.dilate();
			marker2.bp.copyBits(this.bp, 0, 0, Blitter.MAX);
			if (equals(aux, marker2.bp))
				idem = true;
		}
		return marker2;
	}

	/**
	 * Metodo equals. Devuelte true si las imagenes son iguales. False en caso contrario
	 * 
	 * @param ip1
	 *            Primera imagen.
	 * @param ip2
	 *            Segunda imagen.
	 * @return boolean
	 */
	private boolean equals(ImageProcessor ip1, ImageProcessor ip2) {
		int i, j;
		int w = ip1.getWidth();
		int h = ip1.getHeight();
		for (i = 0; i < h; i++) {
			for (j = 0; j < w; j++) {
				if (ip1.getPixel(i, j) != ip2.getPixel(i, j))
					return false;
			}
		}
		return true;
	}

	/**
	 * Desentrelaza la imagen
	 * 
	 * @param choice
	 *            metodo de desentrelazado
	 * @return imagen binaria
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public Binary deinterlace(int choice) throws IOException {
		Deinterlace d = new Deinterlace();
		d.wepi_setup(choice);
		ImageProcessor ip = d.wepi_run(this.bp);
		Binary out = new Binary(new BinaryProcessor((ByteProcessor) ip));
		out.copyAtribute("Deinterlace" + (choice == 0 ? "Even" : "Odd") + "_", this);
		return out;
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
	 *             Signals that an I/O exception has occurred.
	 */
	public Binary cannyEdgeDetector(double radius, double low, double high, boolean normalize) throws IOException {
		Canny_Edge_Detector c = new Canny_Edge_Detector();
		ImageProcessor ip = c.wepi_run(this.bp, (float) radius, (float) low, (float) high, normalize);
		Binary out = new Binary((BinaryProcessor) ip);
		out.copyAtribute("CannyEdge_", this);
		return out;
	}

	/**
	 * Realiza la inmersión de watershed - Vincent and Soille (1991)
	 * 
	 * @return imagen binaria
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Binary watershed() throws IOException {
		Watershed_Algorithm w = new Watershed_Algorithm();
		ImageProcessor ip = w.wepi_run(this.bp);
		Binary out = new Binary((BinaryProcessor) ip);
		out.copyAtribute("Watershed_", this);
		return out;
	}

	/**
	 * Difumina la imagen.
	 * 
	 * @return imagen binaria
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public Binary blur() throws IOException {
		Binary out = this.duplicate();
		int[] BLUR1 = { 1, 2, 1, 2, 4, 2, 1, 2, 1 };
		out.bp.convolve3x3(BLUR1);

		out.copyAtribute("Blur_", this);
		return out;
	}

	/**
	 * Aumenta la nitidez de la imagen
	 * 
	 * @return imagen binaria
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public Binary sharpen() throws IOException {
		Binary out = this.duplicate();
		int[] SHARPEN = { 0, -1, 0, -1, 5, -1, 0, -1, 0 };
		out.bp.convolve3x3(SHARPEN);

		out.copyAtribute("Sharpen_", this);
		return out;
	}
}
