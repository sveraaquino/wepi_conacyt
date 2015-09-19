package py.fpuna.lcca.model;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import py.fpuna.lcca.process.*;

import javax.imageio.ImageIO;

import org.eclipse.jdt.core.dom.ThrowStatement;

import ij.*;
import ij.plugin.ImageCalculator;
import ij.process.*;
import imagescience.random.BinomialGenerator;
import imagescience.random.ExponentialGenerator;
import imagescience.random.GammaGenerator;
import imagescience.random.GaussianGenerator;
import imagescience.random.PoissonGenerator;
import imagescience.random.RandomGenerator;
import imagescience.random.UniformGenerator;
import fiji.threshold.Auto_Local_Threshold;

import py.fpuna.lcca.util.Image;
import py.fpuna.lcca.util.MyException;
import py.fpuna.lcca.util.parse.StructureElement;

/**
 * Clase gray. Representa las Imagenes en escala de grises (8-bit)
 */
public class Gray extends Image implements Serializable {

	private static final long serialVersionUID = 1L;
	private ByteProcessor bp;
	private int[][] se;
	private int xCenter;
	private int yCenter;

	public Gray(ByteProcessor a) {
		bp = a;
	}

	@Override
	public int[] histogram() {
		return bp.getHistogram();
	}

	public Gray(ByteProcessor a, int[][] structuringElement, int xCenter, int yCenter) {
		bp = a;
		se = (int[][]) structuringElement.clone();
		this.xCenter = xCenter;
		this.yCenter = yCenter;
	}

	/**
	 * Metodo utilizado internamente por otros procedimientos. Devuelve el pixel con el valor minimo dentro de su vecindad de acuerdo al elemento estructurante que posee.
	 * 
	 * @param m
	 *            Fila del pixel.
	 * @param n
	 *            Columna del pixel.
	 * @return int
	 */

	private int min(int m, int n) {

		int i, j, f, c, xlen, ylen, w, h, min;
		f = m - xCenter;
		c = n - yCenter;
		xlen = se.length;
		ylen = se[0].length;
		w = bp.getWidth();
		h = bp.getHeight();
		int[][] sum = new int[xlen][ylen];
		for (i = 0; i < xlen; i++) {
			for (j = 0; j < ylen; j++) {
				// System.out.println("ip["+f+"]["+c+"]");
				if (f >= 0 && c >= 0 && c < h && f < w) {
					sum[i][j] = se[i][j] + bp.getPixel(f, c);
					// System.out.println("ip["+f+"]["+c+"]="+ip[f][c]);
				} else {
					sum[i][j] = 0;
				}
				c++;
			}
			f++;
			c = n - yCenter;
		}
		min = sum[0][0];
		for (i = 0; i < xlen; i++) {
			for (j = 0; j < ylen; j++) {
				if (sum[i][j] < min) {
					min = sum[i][j];
				}
			}
		}
		return min;
	}

	/**
	 * Metodo utilizado internamente por otros procedimientos. Devuelve el pixel con el valor maximo dentro de su vecindad de acuerdo al elemento estructurante que posee.
	 * 
	 * @param m
	 *            Fila del pixel.
	 * @param n
	 *            Columna del pixel.
	 * @return int
	 */

	private int max(int m, int n) {

		int i, j, f, c, xlen, ylen, w, h, max;
		f = m - xCenter;
		c = n - yCenter;
		xlen = se.length;
		ylen = se[0].length;
		w = bp.getWidth();
		h = bp.getHeight();
		int[][] sum = new int[xlen][ylen];
		for (i = 0; i < xlen; i++) {
			for (j = 0; j < ylen; j++) {
				// System.out.println("ip["+f+"]["+c+"]");
				if (f >= 0 && c >= 0 && c < h && f < w) {// controla que los
															// indices no
															// superen
					sum[i][j] = se[i][j] + bp.getPixel(f, c);
					// System.out.println("ip["+f+"]["+c+"]="+ip[f][c]);
				} else {
					sum[i][j] = 0;
				}
				c++;
			}
			f++;
			c = n - yCenter;
		}
		max = sum[0][0];
		for (i = 0; i < xlen; i++) {
			for (j = 0; j < ylen; j++) {
				if (sum[i][j] > max) {
					max = sum[i][j];
				}
			}
		}
		return max;
	}

	/**
	 * Invierte la imagen
	 * 
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */
	@Override
	public Gray invertirImagen() throws MyException, IOException {

		ByteProcessor out = (ByteProcessor) this.bp.duplicate();
		out.invert();

		Gray result = new Gray(out);
		result.copyAtribute("invertirImagen", this);

		return result;
	}

	/**
	 * Genera el reflejo de un elemento estructurante.
	 * 
	 * @param se
	 *            Elemento Estructurante.
	 * @return int [][]
	 */
	@Override
	public int[][] reflect(int[][] se) {
		// mirrors the structuring element around the center (hot spot)
		// used to implement erosion by a dilation
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
	 * Metodo utilizado internamento por otros procedimientos. Devuelve la información de una imagen.
	 * 
	 * @return byte[]
	 * 
	 * @throws IOException
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
	 * Metodo utilizado para binarizacion de una imagen en escala de gris de forma local. Se ultiza el plugin de imagej Auto_Threshold.jar de Gabriel Landini, plus others.
	 * 
	 * @param op
	 *            Puede tomar los valores "Try all", "Bernsen", "Mean", "Median", "MidGrey", "Niblack", "Sauvola".
	 * @return imagen binaria
	 * 
	 * @throws IOException
	 */

	public Binary localThresholding(String op) throws IOException, NullPointerException {

		ByteProcessor out = (ByteProcessor) bp.duplicate();
		Auto_Local_Threshold alt = new Auto_Local_Threshold();
		ImagePlus imap = new ImagePlus("in", out);

		alt.exec(imap, op, 15, 0, 0, true);
		ImageProcessor aux = imap.getProcessor();
		ByteProcessor aux2 = (ByteProcessor) aux;

		// ImagePlus imp = new ImagePlus(op, out);
		// imp.show();
		Binary out2 = new Binary(new BinaryProcessor(aux2));
		out2.copyAtribute("LocalThresholding_" + op, this);
		System.out.println("Operacion es: " + op);
		return out2;
	}

	/**
	 * Metodo utilizado para binarizacion de una imagen en escala de gris de forma global. Se ultiza el plugin de imagej Auto_Threshold.jar de Gabriel Landini, plus others.
	 * 
	 * @param op
	 *            Puede tomar los valores "Try all", "Default","Huang", "Intermodes", "IsoData", "Li", "MaxEntropy","Mean", "MinError(I)", "Minimum", "Moments", "Otsu", "Percentile", "RenyiEntropy", "Shanbhag" ,
	 *            "Triangle", "Yen".
	 * @return imagen binaria
	 * 
	 * @throws IOException
	 */
	public Binary globalThresholding(String op) throws IOException {
		Binary out2;

		if (op.equals("Fisher")) {
			out2 = this.thresholdFisher();
		} else {
			if (op.equals("FuzzyYager")) {
				out2 = this.thresholdFuzzyYager();
			} else {
				if (op.equals("EntropiaJohannsen")) {
					out2 = this.thresholdEntropiaJohannsen();
				} else {
					ByteProcessor out = (ByteProcessor) bp.duplicate();
					AutoThresholder at = new AutoThresholder();
					int u, v, h, w, i;
					int[] hist = new int[256];

					h = out.getHeight();
					w = out.getWidth();
					for (u = 0; u < h; u++) {
						for (v = 0; v < w; v++) {
							i = 0xFF & out.getPixel(u, v);
							hist[i]++;
						}
					}
					System.out.println(op + "  hola, este deberia ser el op.");

					int threshold = at.getThreshold(op, hist);

					out.threshold(threshold);
					out2 = new Binary(new BinaryProcessor(out));
					System.out.println(op + "  segunda prueba");

					out2.copyAtribute("GlobalThresholding_" + op + "(" + threshold + ")", this);

				}
			}
		}
		return out2;
	}

	/**
	 * Metodo utilizado internamento por otros procedimientos. Devuelve el ancho de la imagen.
	 * 
	 * @return int
	 * 
	 * @throws MyException
	 */
	@Override
	public int getWidth() throws MyException {

		return bp.getWidth();

	}

	/**
	 * Metodo utilizado internamento por otros procedimientos Devuelve el alto de la imagen.
	 * 
	 * @return int
	 * 
	 * @throws MyException
	 */
	@Override
	public int getHeight() throws MyException {

		return bp.getHeight();
	}

	// ----------------------------Filtros-------------------------------
	/**
	 * Aplica un filtro promedio a la imagen
	 * 
	 * @param dim
	 *            La dimension de la mascara a ser utilizada para filtrar la imagen
	 * @return imagen en escala de grises
	 * 
	 * @throws IOExeption
	 */

	public Gray filterMean(int dim) throws IOException {
		if (dim % 2 == 0)
			throw new MyException("validator.impar");
		double filter[][] = new double[dim][dim];
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		int M = bp.getWidth();
		int N = bp.getHeight();
		int u, v, i, j, p, q;
		double c, s, sum;
		s = 0;

		for (i = 0; i < dim; i++) {

			for (j = 0; j < dim; j++)
				filter[i][j] = 1.0;
		}

		// arbitrary filter matrix of size (2K+1)x(2L+1)
		for (u = 0; u < filter.length; u++) {
			for (v = 0; v < filter[0].length; v++) {
				s += filter[u][v];
			}
		}

		int K = filter[0].length / 2;
		int L = filter.length / 2;

		ImageProcessor copy = bp.duplicate();

		for (v = L; v <= N - L - 1; v++) {
			for (u = K; u <= M - K - 1; u++) {
				// compute filter result for position (u,v)
				sum = 0;
				for (j = -L; j <= L; j++) {
					for (i = -K; i <= K; i++) {
						p = copy.getPixel(u + i, v + j);
						c = filter[j + L][i + K];
						sum = sum + c * p;
					}
				}
				q = (int) (sum / s);
				// clamp result
				if (q < 0)
					q = 0;
				if (q > 255)
					q = 255;
				bp.putPixel(u, v, q);
			}
		}

		Gray result = new Gray(bp);
		result.copyAtribute("filterMean", this);

		return result;

	}

	/**
	 * Aplica un filtro mediana a la imagen
	 * 
	 * @param size
	 *            La dimension de la mascara a ser utilizada para filtrar la imagen
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */

	public Gray filterMediana(int size) throws MyException, IOException {
		if (size % 2 == 0)
			throw new MyException("validator.filterMedian");// error parametro
															// incorrecto
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		int w = bp.getWidth();
		int h = bp.getHeight();
		int q, v, u, i, j, k;
		int fil, col;
		int[][] copy = bp.getIntArray();
		int[] vector = new int[size * size];
		int center = (int) size / 2;

		for (v = 0; v <= h - 1; v++) {
			for (u = 0; u <= w - 1; u++) {
				k = 0;
				for (j = -center; j <= center; j++) {
					for (i = -center; i <= center; i++) {
						fil = u + i;
						col = v + j;
						if (fil >= 0 && fil < w && col >= 0 && col < h) {
							vector[k++] = copy[fil][col];
						}
					}
				}
				q = (int) mediana(vector);
				bp.putPixel(u, v, q);
			}
		}

		Gray result = new Gray(bp);
		result.copyAtribute("filterMediana", this);

		return result;
	}

	/**
	 * Devuelve la mediana de un vector de enteros
	 * 
	 * @param a
	 *            El vector de enteros
	 * @return int
	 */

	private static int mediana(int[] a) {
		int len = a.length;
		int pos;
		int mediana;
		Arrays.sort(a);
		pos = Math.round(len / 2);
		if (len % 2 == 0) {
			pos--;
			mediana = (a[pos] + a[pos + 1]) / 2;
		} else
			mediana = a[pos];
		return mediana;
	}

	/*********************************
	 * Nuevos filtros
	 * 
	 * @throws IOException
	 **************************************/

	// nolineal

	/**
	 * Aplica un filtro de media geometrica a la imagen
	 * 
	 * @param dim
	 *            La dimension de la mascara a ser utilizada para filtrar la imagen
	 * @return imagen en escala de grises
	 * 
	 * @throws IOExeption
	 */

	public Gray filterGeometricMean(int dim) throws IOException {
		if (dim % 2 == 0)
			throw new MyException("validator.impar");// error parametro
														// incorrecto
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		int w = bp.getWidth();
		int h = bp.getHeight();
		int v, u, i, j;
		int fil = 0, col = 0, mn;
		int[][] copy = bp.getIntArray();
		int center = (int) dim / 2;
		double result;
		int newPixel;
		for (v = 0; v <= h - 1; v++) {
			for (u = 0; u <= w - 1; u++) {
				result = 1;
				mn = 0;
				for (j = -center; j <= center; j++) {
					for (i = -center; i <= center; i++) {
						fil = u + i;
						col = v + j;
						if (fil >= 0 && fil < w && col >= 0 && col < h) {
							mn++;
							result = result * copy[fil][col];
						}
					}
				}

				newPixel = (int) Math.pow((double) result, (double) (1.0 / mn));
				bp.putPixel(u, v, newPixel);
			}
		}
		Gray out = new Gray(bp);
		out.copyAtribute("filterGeometricMean", this);
		return out;
	}

	// nolineal
	/**
	 * Aplica un filtro de media armonica a la imagen
	 * 
	 * @param dim
	 *            La dimension de la mascara a ser utilizada para filtrar la imagen
	 * @return imagen en escala de grises
	 * 
	 * @throws IOExeption
	 */

	public Gray filterHarmonicMean(int dim) throws IOException {
		if (dim % 2 == 0)
			throw new MyException("validator.impar");// error parametro
														// incorrecto
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		int w = bp.getWidth();
		int h = bp.getHeight();
		int v, u, i, j;
		int fil = 0, col = 0, mn;
		int[][] copy = bp.getIntArray();
		int center = (int) dim / 2;
		double result;
		int newPixel;
		for (v = 0; v <= h - 1; v++) {
			for (u = 0; u <= w - 1; u++) {
				result = 0;
				mn = 0;
				for (j = -center; j <= center; j++) {
					for (i = -center; i <= center; i++) {
						fil = u + i;
						col = v + j;
						if (fil >= 0 && fil < w && col >= 0 && col < h) {
							mn++;
							result = result + (double) 1 / copy[fil][col];
						}
					}
				}
				newPixel = (int) (((double) mn) / result);
				bp.putPixel(u, v, newPixel);
			}
		}
		Gray out = new Gray(bp);
		out.copyAtribute("filterHarmonicMean", this);
		return out;
	}

	/**
	 * Aplica un filtro de contra-armonica a la imagen
	 * 
	 * @param dim
	 *            La dimension de la mascara a ser utilizada para filtrar la imagen
	 * @param harmonic
	 *            Numero de armonica
	 * @return imagen en escala de grises
	 * 
	 * @throws IOExeption
	 */

	// nolineal
	public Gray filterContraharmonicMean(int dim, int harmonic) throws IOException {
		if (dim % 2 == 0)
			throw new MyException("validator.impar");// error parametro
														// incorrecto
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		int w = bp.getWidth();
		int h = bp.getHeight();
		int v, u, i, j;
		int fil = 0, col = 0;
		int[][] copy = bp.getIntArray();
		int center = (int) dim / 2;
		double result1, result2;
		int newPixel;
		for (v = 0; v <= h - 1; v++) {
			for (u = 0; u <= w - 1; u++) {
				result1 = 0;
				result2 = 0;
				for (j = -center; j <= center; j++) {
					for (i = -center; i <= center; i++) {
						fil = u + i;
						col = v + j;
						if (fil >= 0 && fil < w && col >= 0 && col < h) {
							result1 = result1 + Math.pow(copy[fil][col], harmonic + 1);
							result2 = result2 + Math.pow(copy[fil][col], harmonic);
						}
					}
				}
				newPixel = (int) (result1 / result2);
				bp.putPixel(u, v, newPixel);
			}
		}
		Gray out = new Gray(bp);
		out.copyAtribute("filterContraharmonicMean", this);
		return out;
	}

	/**
	 * Aplica un filtro de Kuwahara a la imagen
	 * 
	 * @param dim
	 *            La dimension de la mascara a ser utilizada para filtrar la imagen
	 * @return imagen en escala de grises
	 * 
	 * @throws IOExeption
	 */

	// nolineal
	public Gray filterKuwahara(int size) throws IOException {
		if (size >= 3 && size % 2 == 0)
			throw new MyException("validator.impar");// error parametro
														// incorrecto
		Kuwahara_Filter k = new Kuwahara_Filter();
		k.setSize(size);

		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		k.run(bp);

		Gray out = new Gray(bp);
		out.copyAtribute("filterKuwahara", this);

		return out;
	}

	// nolineal
	/**
	 * Aplica un filtro de Nagao Matsuyama a la imagen
	 * 
	 * @return imagen en escala de grises
	 * 
	 * @throws IOExeption
	 */
	public Gray filterNagaoMatsuyama() throws IOException {
		int[][] h1 = { { 0, 0, 0, 0, 0 }, { 0, 1, 1, 1, 0 }, { 0, 1, 1, 1, 0 }, { 0, 1, 1, 1, 0 }, { 0, 0, 0, 0, 0 } };

		int[][] h2 = { { 0, 0, 0, 0, 0 }, { 0, 0, 0, 1, 1 }, { 0, 0, 1, 1, 1 }, { 0, 0, 0, 1, 1 }, { 0, 0, 0, 0, 0 } };

		int[][] h3 = { { 0, 0, 0, 0, 0 }, { 1, 1, 0, 0, 0 }, { 1, 1, 1, 0, 0 }, { 1, 1, 0, 0, 0 }, { 0, 0, 0, 0, 0 } };

		int[][] h4 = { { 0, 1, 1, 1, 0 }, { 0, 1, 1, 1, 0 }, { 0, 0, 1, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 } };

		int[][] h5 = { { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 1, 0, 0 }, { 0, 1, 1, 1, 0 }, { 0, 1, 1, 1, 0 } };

		int[][] h6 = { { 0, 0, 0, 1, 1 }, { 0, 0, 1, 1, 1 }, { 0, 0, 1, 1, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 } };

		int[][] h7 = { { 1, 1, 0, 0, 0 }, { 1, 1, 1, 0, 0 }, { 0, 1, 1, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 } };

		int[][] h8 = { { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 1, 1, 0 }, { 0, 0, 1, 1, 1 }, { 0, 0, 0, 1, 1 } };

		int[][] h9 = { { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 1, 1, 0, 0 }, { 1, 1, 1, 0, 0 }, { 1, 1, 0, 0, 0 } };
		int[][][] h = { h1, h2, h3, h4, h5, h6, h7, h8, h9 };
		int i, j, k, f, c, fil, col, count, sum, pos;
		double var, min;
		double[] means = { 0, 0, 0, 0, 0, 0, 0, 0, 0 };// 9 elements
		double[] variance = { 0, 0, 0, 0, 0, 0, 0, 0, 0 };// 9 elements
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		int[][] copy = bp.getIntArray();
		int width = bp.getWidth();
		int height = bp.getHeight();

		for (i = 0; i < width; i++) {
			for (j = 0; j < height; j++) {

				for (k = 0; k < 9; k++) {
					count = 0;
					sum = 0;
					// mean
					for (f = -2; f <= 2; f++) {
						for (c = -2; c <= 2; c++) {
							fil = i + f;
							col = j + c;
							if (fil >= 0 && fil < width && col >= 0 && col < height && h[k][f + 2][c + 2] == 1) {
								// count++;
								sum = sum + copy[fil][col];
							}
						}
					}
					if (k == 0)
						count = 9;
					else
						count = 7;
					means[k] = sum / count;
					count = 0;
					var = 0;
					// variance
					for (f = -2; f <= 2; f++) {
						for (c = -2; c <= 2; c++) {
							fil = i + f;
							col = j + c;
							if (fil >= 0 && fil < width && col >= 0 && col < height && h[k][f + 2][c + 2] == 1) {
								// count++;
								var = var + (copy[fil][col] - means[k]) * (copy[fil][col] - means[k]);
							}
						}
					}
					if (k == 0)
						count = 9;
					else
						count = 7;
					variance[k] = var / count;
				}
				min = variance[0];
				pos = 0;
				// mean with lowest variance
				for (int x = 0; x < 9; x++) {
					if (variance[x] < min) {
						pos = x;
						min = variance[x];
					}
				}
				bp.set(i, j, (int) means[pos]);
			}
		}
		Gray out = new Gray(bp);
		out.copyAtribute("filterNagaoMatsuyama", this);
		return out;
	}

	// nolineal
	/**
	 * Aplica un filtro Alfa Media Recortado a la imagen
	 * 
	 * @param dim
	 *            La dimension de la mascara a ser utilizada para filtrar la imagen
	 * @param alpha
	 *            Alfa
	 * @return imagen en escala de grises
	 * 
	 * @throws IOExeption
	 */
	public Gray filterAlphaTrimmedMean(int dim, int alpha) throws IOException {
		// se controla que la cantidad de elementos a eliminar no supere dim*dim
		if (dim % 2 == 0 || (alpha >= dim * dim))
			throw new MyException("validator.filterAlpha");// error parametro
															// incorrecto
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		int w = bp.getWidth();
		int h = bp.getHeight();
		int v, u, i, j;
		int fil = 0, col = 0, sw, k;
		int[][] copy = bp.getIntArray();
		int[] vector = new int[dim * dim];
		int center = (int) dim / 2;
		int newPixel, sum, count;
		for (v = 0; v <= h - 1; v++) {
			for (u = 0; u <= w - 1; u++) {
				sw = k = 0;
				for (j = -center; j <= center; j++) {
					for (i = -center; i <= center; i++) {
						fil = u + i;
						col = v + j;
						if (fil >= 0 && fil < w && col >= 0 && col < h) {
							vector[k] = copy[fil][col];
						} else {
							if (sw == 0) {
								vector[k] = 0;
								sw = 1;
							} else {
								vector[k] = 255;
								sw = 0;
							}
						}
						k++;
					}
				}
				Arrays.sort(vector);
				sum = count = 0;
				for (int x = 0; x < dim * dim; x++) {
					if (x >= alpha / 2 && x <= (dim * dim - alpha / 2)) {
						sum = sum + vector[x];
						count++;
					}
				}
				newPixel = sum / count;
				bp.putPixel(u, v, newPixel);
			}
		}
		Gray out = new Gray(bp);
		out.copyAtribute("filterAlphaTrimmedMean", this);
		return out;
	}

	// nolineal
	/**
	 * Aplica un filtro de Media YP a la imagen
	 * 
	 * @param dim
	 *            La dimension de la mascara a ser utilizada para filtrar la imagen
	 * @param P
	 *            El valor de P
	 * @return imagen en escala de grises
	 * 
	 * @throws IOExeption
	 */

	public Gray filterYPMean(int dim, int P) throws IOException {
		if (dim % 2 == 0)
			throw new MyException("validator.impar");// error parametro
														// incorrecto
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		int w = bp.getWidth();
		int h = bp.getHeight();
		int v, u, i, j;
		int fil = 0, col = 0, mn;
		int[][] copy = bp.getIntArray();
		int center = (int) dim / 2;
		double result;
		int newPixel;
		for (v = 0; v <= h - 1; v++) {
			for (u = 0; u <= w - 1; u++) {
				result = 0;
				mn = 0;
				for (j = -center; j <= center; j++) {
					for (i = -center; i <= center; i++) {
						fil = u + i;
						col = v + j;
						if (fil >= 0 && fil < w && col >= 0 && col < h) {
							mn++;
							result = result + Math.pow(copy[fil][col], P);
						}
					}
				}

				newPixel = (int) Math.pow(result / (double) mn, 1.0 / (double) P);
				bp.putPixel(u, v, newPixel);
			}
		}
		Gray out = new Gray(bp);
		out.copyAtribute("filterYPMean", this);
		return out;
	}

	// ----------------------------------ruidos------------------------------------

	/**
	 * Aplica un ruido mediante un generador ramdomico a la imagen
	 * 
	 * @param bp
	 *            Imagen
	 * @param rnd
	 *            Generador Ramdomico
	 * @return ByteProcessor
	 * 
	 */
	private ByteProcessor applyNoise(ByteProcessor bp, RandomGenerator rnd) {
		int v, ran;
		int h = bp.getHeight();
		int w = bp.getWidth();
		int x, y;
		int value;
		for (y = 0; y < h; y++) {
			for (x = 0; x < w; x++) {
				ran = (int) Math.round(rnd.next());
				v = (value = bp.get(x, y) & 0xff) + ran;
				if (v < 0)
					v = 0;
				if (v > 255)
					v = 255;
				System.out.println(v - value);
				bp.set(x, y, (byte) v);
			}
		}
		return bp;
	}

	/**
	 * Aplica un ruido gaussiano a la imagen
	 * 
	 * @param mean
	 *            Media
	 * @param sigma
	 *            Desviacion Estandar
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */
	public Gray noiseGaussian(double mean, double sigma) throws IOException {
		if (sigma <= 0)
			throw new MyException("noiseUniform");
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		GaussianGenerator rnd = new GaussianGenerator(mean, sigma);
		bp = applyNoise(bp, rnd);

		Gray result = new Gray(bp);
		result.copyAtribute("noiseGaussian", this);

		return result;
	}

	/**
	 * Aplica un ruido uniforme a la imagen
	 * 
	 * @param min
	 *            Valor minimo.
	 * @param max
	 *            Valor maximo.
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */
	public Gray noiseUniform(double min, double max) throws IOException, MyException {
		if (min >= max)
			throw new MyException("validator.noiseUniform");
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		UniformGenerator rnd = new UniformGenerator(min, max);
		applyNoise(bp, rnd);

		Gray result = new Gray(bp);
		result.copyAtribute("noiseUniform", this);

		return result;

	}

	/**
	 * Aplica un ruido binomial a la imagen
	 * 
	 * @param trials
	 *            Cantidad de intentos.
	 * @param probability
	 *            Probabilidad.
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */
	public Gray noiseBinomial(int trials, double probability) throws IOException {
		if (trials < 0 || probability > 1 || probability < 0)
			throw new MyException("200");
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		BinomialGenerator rnd = new BinomialGenerator(trials, probability);
		applyNoise(bp, rnd);
		Gray result = new Gray(bp);
		result.copyAtribute("noiseBinomial", this);

		return result;
	}

	/**
	 * Aplica un ruido gamma a la imagen
	 * 
	 * @param order
	 *            Orden
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */

	public Gray noiseGamma(int order) throws IOException {
		if (order <= 0)
			throw new MyException("200");
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		GammaGenerator rnd = new GammaGenerator(order);
		applyNoise(bp, rnd);
		Gray result = new Gray(bp);
		result.copyAtribute("noiseGamma", this);

		return result;
	}

	/**
	 * Aplica un ruido de poisson a la imagen
	 * 
	 * @param mean
	 *            Media.
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */

	public Gray noisePoisson(int mean) throws IOException {
		if (mean < 0)
			throw new MyException("200");
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		PoissonGenerator rnd = new PoissonGenerator(mean);
		applyNoise(bp, rnd);
		Gray result = new Gray(bp);
		result.copyAtribute("noisePoisson", this);

		return result;
	}

	/**
	 * Aplica un ruido exponencial a la imagen
	 * 
	 * @param lambda
	 *            Lambda
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */
	public Gray noiseExponential(int lambda) throws IOException {
		if (lambda <= 0)
			throw new MyException("200");
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		ExponentialGenerator rnd = new ExponentialGenerator(lambda);
		applyNoise(bp, rnd);
		Gray result = new Gray(bp);
		result.copyAtribute("noiseExponential", this);

		return result;
	}

	/**
	 * Aplica un ruido de sal y pimienta a la imagen
	 * 
	 * @param p
	 *            Probabilidad
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */

	public Gray noiseSaltAndPepper(double p) throws IOException {
		if (p < 0 || p > 1)
			throw new MyException("200");
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		double num, p_2;
		int h = bp.getHeight();
		int w = bp.getWidth();
		int x, y;
		int val;
		for (y = 0; y < h; y++) {
			for (x = 0; x < w; x++) {
				num = Math.random();
				if (num < p) {
					p_2 = p / 2;
					if (num < p_2)
						val = 0;
					else
						val = 255;
					bp.set(x, y, (byte) val);
				}
			}
		}

		Gray result = new Gray(bp);
		result.copyAtribute("noiseSaltAndPepper", this);

		return result;
	}

	// ----------------------------------morfologia--------------------------------------

	/**
	 * Realiza una operacion morfologica a la imagen de acuerdo al parametro que reciba.
	 * 
	 * @param a
	 *            Operacion a realizar, puede tomar los valores de "dilate", "erode", "erode2", "open", "close", "externalBound", "internalBound", "externalInternalBound".
	 * 
	 * @param element
	 *            Elemento estructurante.
	 * @param xc
	 *            Centro del elemento estructurante en el eje x.
	 * @param yc
	 *            Centro del elemento estructurante en el eje y.
	 * @param iterations
	 *            Cantidad de iteraciones.
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */

	public Gray morphology(String a, int[][] element, int xc, int yc, int iterations) throws IOException {
		Gray out = this.duplicate();

		out.BinaryMorphology(element, xc, yc);

		if (a.equals("dilate")) {
			for (int i = 0; i < iterations; i++)
				out.dilate();
		} else if (a.equals("erode")) {
			for (int i = 0; i < iterations; i++)
				out.erode();
		} else if (a.equals("erode2"))
			out.erode2();
		else if (a.equals("open"))
			out.open(iterations);
		else if (a.equals("close"))
			out.close(iterations);
		else if (a.equals("topHat"))
			out.topHat(iterations);
		else if (a.equals("bottomHay"))
			out.bottomHat(iterations);
		else if (a.equals("externalBound"))
			out.externalBoundary();
		else if (a.equals("internalBound"))
			out.internalBoundary();
		else if (a.equals("externalInternalBound"))
			out.internalExternalBoundary();

		else
			throw new MyException("100");

		out.copyAtribute(a, this);
		return out;
	}

	/**
	 * Duplica la imagen.
	 * 
	 * @return imagen en escala de grises
	 * 
	 */
	public Gray duplicate() {
		Gray out = new Gray((ByteProcessor) this.bp.duplicate());

		return out;
	}

	/**
	 * Instancia la imagen de acuerdo a los valores recibidos para que poder realizar las operaciones morfologicas.
	 * 
	 * @param structuringElement
	 *            Elemento estructurante.
	 * @param xCenter
	 *            Centro del elemento estructurante en el eje x.
	 * @param yCenter
	 *            Centro del elemento estructurante en el eje y.
	 */
	public void BinaryMorphology(int[][] structuringElement, int xCenter, int yCenter) {
		this.se = (int[][]) structuringElement.clone();
		this.xCenter = xCenter;
		this.yCenter = yCenter;
	}

	/**
	 * Bordes Internos (?)
	 * 
	 * @return void
	 */

	private void internalBoundary() {
		ByteProcessor foreground = (ByteProcessor) bp.duplicate();
		this.erode();
		bp.copyBits(foreground, 0, 0, Blitter.DIFFERENCE);
	}

	/**
	 * Bordes Externos (?)
	 * 
	 * @param null
	 * 
	 * @return void
	 */
	private void externalBoundary() {
		// ByteProcessor foreground2 = (ByteProcessor)ip.duplicate();
		ByteProcessor foreground = (ByteProcessor) bp.duplicate();
		this.dilate();
		bp.copyBits(foreground, 0, 0, Blitter.DIFFERENCE);
	}

	/**
	 * Bordes Internos y Externos (?)
	 * 
	 * @param null
	 * 
	 * @return void
	 */
	private void internalExternalBoundary() {

		Gray i1 = new Gray((ByteProcessor) bp.duplicate(), se, xCenter, yCenter);
		Gray i2 = new Gray((ByteProcessor) bp.duplicate(), se, xCenter, yCenter);
		i1.dilate();
		i2.erode();
		i1.bp.copyBits(i2.bp, 0, 0, Blitter.DIFFERENCE);
		this.bp.copyBits(i1.bp, 0, 0, Blitter.COPY);
	}

	/**
	 * Dilata la imagen
	 * 
	 * @param null
	 * 
	 * @return void
	 */

	private void dilate() {
		Gray x = new Gray((ByteProcessor) this.bp.duplicate(), se, xCenter, yCenter);
		int w = bp.getWidth();
		int h = bp.getHeight();

		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				this.bp.putPixel(j, i, x.max(j, i));
			}
		}
		// ip.copyBits(tmp,0,0,Blitter.COPY);

	}

	/**
	 * Erosiona la imagen
	 * 
	 * @param null
	 * 
	 * @return void
	 */

	private void erode() {
		Gray x = new Gray((ByteProcessor) this.bp.duplicate(), se, xCenter, yCenter);
		int w = bp.getWidth();
		int h = bp.getHeight();

		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				this.bp.putPixel(j, i, x.min(j, i));
			}
		}
		// ip.copyBits(tmp,0,0,Blitter.COPY);

	}

	/**
	 * Erosiona la imagen
	 * 
	 * @param null
	 * 
	 * @return void
	 */

	private void erode2() {
		this.bp.invert();
		se = this.reflect(se);
		this.dilate();
		this.bp.invert();
	}

	/**
	 * Realiza una apertura a la imagen
	 * 
	 * @param iterations
	 *            Cantidad de iteraciones a realizar.
	 * @return void
	 */

	private void open(int iterations) {
		for (int i = 0; i < iterations; i++)
			this.erode();
		for (int i = 0; i < iterations; i++)
			this.dilate();
	}

	/**
	 * Realiza la transformada top hat
	 * 
	 * @param iteration
	 *            número de iteraciones
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public void topHat(int iterations) throws IOException {
		Gray open = new Gray((ByteProcessor) this.bp.duplicate(), se, xCenter, yCenter);

		open.open(iterations);
		bp.copyBits(open.bp, 0, 0, Blitter.SUBTRACT);
	}

	/**
	 * Realiza la transformada bottom hat
	 * 
	 * @param iteration
	 *            número de iteraciones
	 * 
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public void bottomHat(int iterations) throws IOException {
		Gray close = new Gray((ByteProcessor) this.bp.duplicate(), se, xCenter, yCenter);

		close.close(iterations);
		close.bp.copyBits(bp, 0, 0, Blitter.SUBTRACT);
	}

	/**
	 * Realiza una cerradura a la imagen
	 * 
	 * @param iterations
	 *            Cantidad de iteraciones a realizar.
	 * @return void
	 */

	private void close(int iterations) {
		for (int i = 0; i < iterations; i++)
			this.dilate();
		for (int i = 0; i < iterations; i++)
			this.erode();

	}

	/**
	 * Realiza una operacion logica o aritmetica entre dos imagenes
	 * 
	 * @param im2
	 *            La segunda imagen utilizada para la operacion.
	 * @param op
	 *            Indica la operacion a efectuar. Puede ser "AND", "OR", "XOR", "MAX", "MIN", "DIFFERENCE", "MULTIPLY", "DIVIDE".
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */
	public Gray operationBetween(Gray im2, String op) throws IOException {
		Gray out = this.duplicate();

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
			throw new MyException("200");

		out.copyAtribute(im2.getName() + "_" + op, this);

		return out;
	}

	// ***************Modifying image intensity******************

	/**
	 * Aplica contraste a la imagen de acuerdo a un valor recibido.
	 * 
	 * @param p
	 *            Cantidad de contraste a aplicar.
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */
	public Gray constrast(double p) throws IOException {
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		bp.multiply(p + 1);
		Gray result = new Gray(bp);
		result.copyAtribute("constrast", this);

		return result;
	}

	/**
	 * Aplica brillo a la imagen de acuerdo a un valor recibido.
	 * 
	 * @param p
	 *            Cantidad de brillo a aplicar.
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */
	public Gray brightness(int p) throws IOException {
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		bp.add(p);
		Gray result = new Gray(bp);
		result.copyAtribute("brightness", this);

		return result;
	}

	/**
	 * Aplica contraste automaticamente a la imagen.
	 * 
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */
	public Gray autoContrat() throws IOException {
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
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
		Gray result = new Gray(bp);
		result.copyAtribute("autoContrat", this);

		return result;
	}

	// *******************************segmentacion**************************************

	/**
	 * Realiza la segmentacion de acuerdo al parametro recibido.
	 * 
	 * @param seleccion
	 *            Indica que tipo de segmentacion realizar. Puede tomar los siguientes valores: 1 Prewitt X, 2 Prewitt Y, 3 Prewitt Module, 4 Sobel X, 5 Sobel Y, 6 Sobel Module, 7 Laplaciano
	 * 
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */
	public Gray segmentationSelection(int seleccion) throws IOException {
		ByteProcessor orig = (ByteProcessor) this.bp.duplicate();
		switch (seleccion) {
		case (1): {
			int[][] PwX = { { -1, 0, 1 }, { -1, 0, 1 }, { -1, 0, 1 } };
			return filterConvolver(PwX, "PrewittX");

		}
		case (2): {
			int[][] PwY = { { 1, 1, 1 }, { 0, 0, 0 }, { -1, -1, -1 } };
			return filterConvolver(PwY, "PrewittY");
		}
		case (3): {
			int[][] PwY = { { 1, 1, 1 }, { 0, 0, 0 }, { -1, -1, -1 } };
			return modGradiente(PwY, "PrewittMod");
		}
		case (4): {
			int[][] SbX = { { -1, 0, 1 }, { -2, 0, 2 }, { -1, 0, 1 } };
			return filterConvolver(SbX, "SobenX");
		}
		case (5): {
			int[][] SbY = { { -1, -2, -1 }, { 0, 0, 0 }, { 1, 2, 1 } };
			return filterConvolver(SbY, "SobenY");
		}
		case (6): {
			int[][] SbY = { { -1, -2, -1 }, { 0, 0, 0 }, { 1, 2, 1 } };
			return modGradiente(SbY, "SobenMod");
		}
		case (7): {
			int[][] laplace = { { 1, 1, 1 }, { 1, -8, 1 }, { 1, 1, 1 } };
			return filterConvolver(laplace, "Laplacian");
		}
		default: {
			return new Gray(orig);
		}

		}

	}

	/**
	 * Aplica la convolucion sobre una imagen dada la matriz de convolucion
	 * 
	 * @param filter
	 *            Matriz de convolucion.
	 * @param typeConvolver
	 *            El tipo de convolucion a realizar
	 * @return Gris
	 * 
	 * @throws IOException
	 */
	public Gray filterConvolver(int[][] filter, String typeConvolver) throws IOException {
		ByteProcessor orig = (ByteProcessor) this.bp.duplicate();
		int w = orig.getWidth();
		int h = orig.getHeight();
		double sum, c;
		int p, q, v, u, i, j;
		int fil, col;
		int[][] copy = orig.getIntArray();

		for (v = 0; v <= h - 1; v++) {
			for (u = 0; u <= w - 1; u++) {
				// compute filter result for position (u,v)
				sum = 0;
				for (j = -1; j <= 1; j++) {
					for (i = -1; i <= 1; i++) {
						fil = u + i;
						col = v + j;
						if (fil >= 0 && fil < w && col >= 0 && col < h) {
							p = copy[fil][col];
							// get the corresponding filter coefficient
							c = filter[j + 1][i + 1];
							sum = sum + c * p;
						}
					}
				}
				q = (int) sum;
				orig.putPixel(u, v, q);
			}
		}
		Gray result = new Gray(orig);
		result.copyAtribute(typeConvolver, this);

		return result;
	}

	/**
	 * Modifica la imagen de acuerdo a su gradiente
	 * 
	 * @param filter
	 *            Matriz de calculo.
	 * @param typeGradiente
	 *            Tipo de Gradiente.
	 * @return Gris
	 * 
	 * @throws IOException
	 */

	public Gray modGradiente(int[][] filter, String typeGradiente) throws IOException {
		ByteProcessor orig = (ByteProcessor) this.bp.duplicate();
		int w = orig.getWidth();
		int h = orig.getHeight();
		double sum, c, c2, sum2;
		int p, q, v, u, i, j;
		int fil, col;
		// 3x3 filter matrix
		int[][] filterT = new int[3][3];
		for (i = 0; i < 3; i++) {
			for (j = 0; j < 3; j++) {
				filterT[i][j] = filter[j][i];
			}
		}

		int[][] copy = orig.getIntArray();

		for (v = 0; v <= h - 1; v++) {
			for (u = 0; u <= w - 1; u++) {
				// compute filter result for position (u,v)
				sum = sum2 = 0;
				for (j = -1; j <= 1; j++) {
					for (i = -1; i <= 1; i++) {
						fil = u + i;
						col = v + j;
						if (fil >= 0 && fil < w && col >= 0 && col < h) {
							p = copy[u + i][v + j];
							// get the corresponding filter coefficient
							c = filter[j + 1][i + 1];
							c2 = filterT[j + 1][i + 1];
							sum = sum + c * p;
							sum2 = sum2 + c2 * p;
						}
					}
				}
				q = (int) Math.sqrt(sum * sum + sum2 * sum2);
				orig.putPixel(u, v, q);
			}
		}
		Gray result = new Gray(orig);
		result.copyAtribute(typeGradiente, this);

		return result;
	}

	/**
	 * Convierte una matriz de cadenas a una matriz de enteros
	 * 
	 * @param matrix
	 *            Matriz de cadenas
	 * @return int [][]
	 */
	// Metodos parseadores de string a matriz entera
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
	 * Metodo que invoca al metodo que realiza las operaciones morfologicas.
	 * 
	 * @param a
	 *            Matriz de cadenas que representa la imagen.
	 * @param element
	 *            Elemento estructurante.
	 * @param xc
	 *            Centro del elemento estructurante en el eje x.
	 * @param yc
	 *            Centro del elemento estructurante en el eje y.
	 * @return Gris
	 * 
	 * @throws IOException
	 */
	public Gray morphoInvoke(String a, String element, int xc, int yc, int iterations) throws IOException {

		int[][] matriz = parser(element);
		System.out.println(matriz[0][0] + " luego de conversion");
		if (xc < 0 || xc > matriz.length || yc < 0 || yc > matriz[0].length)
			throw new MyException("morphology.outOfRange");
		return morphology(a, matriz, xc, yc, iterations);

	}

	// ------------------thresholding------------------------

	// ////////////////////////////////////////////////////////////

	/**
	 * Realiza la umbralizacion aplicando el metodo de Fuzzy Yager
	 * 
	 * @return imagen binaria
	 * 
	 * @throws IOException
	 */
	public Binary thresholdFuzzyYager() throws IOException {
		int g, t;
		double suma, minDp;
		double num, den, ux;
		double u0[] = new double[256];
		double u1[] = new double[256];
		double Dp[] = new double[256];
		double umbral;
		ByteProcessor out;

		// Obteniendo el Histograma de la imagen leida.
		double m_Histo[] = new double[256];
		for (int i = 0; i < bp.getHistogram().length; i++) {

			m_Histo[i] = (double) bp.getHistogram()[i];

		}

		// Inicializando vectores.
		for (t = 0; t < 256; t++) {
			u0[t] = 0;
			u1[t] = 0;
			Dp[t] = 0;
		}

		// Calculo de C.Diferencia entre el max y min nivel de gris.
		double c = (bp.getMax() - bp.getMin());

		// calculo de u0 que corresponde al valor en nivel de gris
		// para el fondo o background.
		num = 0;
		den = 0;
		for (g = 0; g < 256; g++) {
			num += m_Histo[g] * g;
			den += m_Histo[g];
			if (den == 0) {
				u0[g] = 0;
			} else {
				u0[g] = num / den;
			}
		}

		// calculo de u1 que corresponde al valor en nivel de gris
		// para el frente o foreground.
		num = 0;
		den = 0;
		for (g = 256 - 1; g >= 0; g--) {
			num += m_Histo[g] * g;
			den += m_Histo[g];
			if (den == 0) {
				u1[g] = 0;
			} else {
				u1[g] = num / den;
			}
		}

		// Se calcula la probabilidad de que un pixel pertenezca a u0
		// (background) 0 u1(foreground)
		for (t = 0; t < 256; t++) {
			suma = 0;
			for (g = 0; g < 256; g++) {
				if ((g <= t)) {
					ux = 1. / (1 + Math.abs(g - u0[t]) / c);
				} else // if ( (g > t) )
				{
					ux = 1. / (1 + Math.abs(g - u1[t]) / c);
				}

				suma += Math.pow(2 * ux - 1, 2.0);

			}

			Dp[t] = Math.sqrt(suma);
		}

		// Valor fuzzy
		minDp = Dp[0];
		umbral = 0;
		for (t = 1; t < 256; t++) {
			if ((minDp < Dp[t])) {
				minDp = Dp[t];
				umbral = t;
			}
		}
		out = (ByteProcessor) bp.duplicate();
		out.threshold((int) umbral);
		Binary result = new Binary(new BinaryProcessor(out));
		result.copyAtribute("thresholdFuzzyYager", this);
		return result;
	}

	/**
	 * Realiza la umbralizacion aplicando el metodo de Entropia Johannsen
	 * 
	 * @return imagen binaria
	 * 
	 * @throws IOException
	 */
	public Binary thresholdEntropiaJohannsen() throws IOException {
		int t, g;
		double sum1, sum2, minimo;
		double sb[] = new double[256];
		double sw[] = new double[256];
		double tl[] = new double[256];
		double p[] = new double[256];
		int Limiar;

		int[] histogram = bp.getHistogram();
		for (int i = 0; i < histogram.length; i++) {
			p[i] = (double) histogram[i] / bp.getPixelCount();
			sb[i] = 0;
			sw[i] = 0;
			tl[i] = 0;

		}

		// Calculo
		for (t = 0; t < 256; t++) {
			sum1 = 0;
			sum2 = 0;
			for (g = 0; g <= t; g++) {
				sum1 += p[g];
			}
			for (g = 0; g <= t - 1; g++) {
				sum2 += p[g];
			}
			if ((p[t] != 0) && (sum1 != 0) && (sum2 != 0)) {
				sb[t] = (java.lang.Math.log10((double) sum1)) - (1.0 / sum1) * ((p[t] * java.lang.Math.log10((double) p[t])) + (sum2 * java.lang.Math.log10((double) sum2)));
			}
		}
		for (t = 0; t < 256; t++) {
			sum1 = 0;
			sum2 = 0;
			for (g = t; g < 256; g++) {
				sum1 += p[g];
			}
			for (g = t + 1; g < 256; g++) {
				sum2 += p[g];
			}
			if ((p[t] != 0) && (sum1 != 0) && (sum2 != 0)) {
				sw[t] = (java.lang.Math.log10((double) sum1)) - (1.0 / sum1) * ((p[t] * java.lang.Math.log10((double) p[t])) + (sum2 * java.lang.Math.log10((double) sum2)));
			}
		}

		// Calculo do Limiar de Johannsen
		for (g = 0; g < 256; g++) {
			if ((sb[g] != 0) && (sw[g] != 0)) {
				tl[g] = sb[g] + sw[g];

			}
		}

		t = 0;
		while (tl[t] == 0) {
			// System.out.println(tl[t]);
			t++;
		}
		;
		minimo = tl[t];

		Limiar = t;
		for (g = t + 1; g < 256; g++) {
			if ((tl[g] != 0) && (tl[g] < minimo)) {
				minimo = tl[g];
				// System.out.println(g);
				Limiar = g;
			}
		}
		// File prueba = pruebas.aplicarLimiar("Prueba", Limiar,ime);
		System.out.println("umbral " + Limiar);
		ByteProcessor out = (ByteProcessor) bp.duplicate();
		out.threshold((int) Limiar);
		Binary result = new Binary(new BinaryProcessor(out));
		result.copyAtribute("thresholdEntropiaJohannsen" + "(" + Limiar + ")", this);
		return result;
	}

	/**
	 * Realiza la umbralizacion aplicando el metodo de Fuzzy Mean C
	 * 
	 * @param tal
	 *            Exponente ponderado.
	 * @return imagen binaria
	 * 
	 * @throws IOException
	 */
	public Binary thresholdFuzzyMeanC(double tal) throws IOException {
		if (tal <= 1)
			throw new MyException("validator.tal");// error parametro incorrecto
		int j, g;
		double numb, denb;
		double[] mb = new double[256];
		double numo, deno;
		double[] mo = new double[256];
		double aux, expoente;
		double vb, vbaux, pdb;
		double vo, voaux, pdo;
		int limiar_valor;

		int[] m_hist = bp.getHistogram();

		for (g = 0; g < 256; g++) {
			mb[g] = 0;
			mo[g] = 0;

		}

		// calculo de mb
		numb = 0;
		denb = 0;

		for (g = 0; g < 256; g++) {
			numb += g * m_hist[g];
			denb += m_hist[g];

			if (denb == 0)
				mb[g] = 0;
			else
				mb[g] = numb / denb;
		}

		// calculo de mo
		numo = 0;
		deno = 0;

		for (g = 256 - 2; g >= 0; g--) {

			numo += (g + 1) * m_hist[g + 1];
			deno += m_hist[g + 1];

			if (deno == 0)
				mo[g] = 0;
			else
				mo[g] = numo / deno;
		}

		// manipulacion para mb(xj) pertenencia [0,1] y mo(xj) pertenencia [0,1]
		for (j = 0; j < 256; j++) {

			aux = mb[j] + mo[j];
			mb[j] = mb[j] / aux;
			mo[j] = mo[j] / aux;
		}

		expoente = (2 / (tal - 1));
		vb = 0;
		vo = 0;

		while (true) {

			numb = 0;
			denb = 0;
			numo = 0;
			deno = 0;

			for (j = 0; j < 256; j++) {

				numb += m_hist[j] * j * Math.pow(mb[j], tal);
				denb += m_hist[j] * Math.pow(mb[j], tal);

				numo += m_hist[j] * j * Math.pow(mo[j], tal);
				deno += m_hist[j] * Math.pow(mo[j], tal);
			}

			vbaux = numb / denb;
			voaux = numo / deno;

			limiar_valor = (int) ((vbaux + voaux) / 2);

			if (Math.abs(vbaux - vb) < .0001 && Math.abs(voaux - vo) < .0001)
				break;

			vb = vbaux;
			vo = voaux;

			for (j = 0; j < 256; j++) {

				pdo = Math.pow(Math.abs(j - vo), expoente);
				pdb = Math.pow(Math.abs(j - vb), expoente);
				mo[j] = pdb / (pdo + pdb);
				mb[j] = pdo / (pdo + pdb);

			}

		}
		System.out.println("umbral " + limiar_valor);
		ByteProcessor out = (ByteProcessor) bp.duplicate();
		out.threshold((int) limiar_valor);
		Binary result = new Binary(new BinaryProcessor(out));
		result.copyAtribute("thresholdFuzzyMeanC" + "(" + limiar_valor + ")", this);
		return result;

	}

	/**
	 * Realiza la umbralizacion aplicando el metodo de Fisher.
	 * 
	 * @return imagen binaria
	 * 
	 * @throws IOException
	 */
	public Binary thresholdFisher() throws IOException {

		int w = bp.getWidth();
		int h = bp.getHeight();
		int i, j;

		int divid, divis, k;
		double fTermo1, fTermo2;
		double[] Jp = new double[256];

		int Limiar;

		int[] m_Histo = bp.getHistogram();

		// Se calcula el umbral
		for (i = 0; i < 256; i++) {
			divid = 0;
			divis = 0;
			for (k = 0; k <= i; k++) {
				divid += k * m_Histo[k];
				divis += m_Histo[k];
			}
			if (divis == 0) {
				fTermo1 = 0;
			} else {
				fTermo1 = Math.pow(divid, 2) / divis;
			}

			divid = 0;
			divis = 0;
			for (k = i + 1; k < 256; k++) {
				divid += k * m_Histo[k];
				divis += m_Histo[k];
			}
			if (divis == 0) {
				fTermo2 = 0;
			} else {
				fTermo2 = Math.pow(divid, 2) / divis;
			}
			Jp[i] = fTermo1 + fTermo2;

		}

		Limiar = 0;
		for (i = 1; i < 256; i++) {
			if ((Jp[i] >= Jp[Limiar])) {
				Limiar = i;
			}
		}

		System.out.println("Umbral:" + Limiar);
		ByteProcessor out = (ByteProcessor) bp.duplicate();
		out.threshold((int) Limiar);
		Binary result = new Binary(new BinaryProcessor(out));
		result.copyAtribute("thresholdFisher" + "(" + Limiar + ")", this);
		return result;
	}

	/**
	 * Realiza la umbralizacion aplicando el metodo de Matriz de Co-Ocurrencia
	 * 
	 * @param option
	 *            Opcion: 1 Bussines, 2 Probabilidad Concidional.
	 * @param distancia
	 *            Distancia
	 * @return imagen binaria
	 * 
	 * @throws IOException
	 */
	public Binary thresholdMatrizCoOcurrencia(int option, int distancia) throws IOException {
		final int w = 1;// Coeficiente de peso (Default = 1 para los criterios
						// Business e Prob.Condicional)
		double b1, b2, b3, b4;
		long[][] c = new long[256][256];
		int m, n; // variables usadas para recorrer la matriz de pixeles
		int i, j;// variables usadas para recorrer la matriz de co-ocurrencia

		double auxBusiness;
		double auxProbCond;

		long dwWidth;
		int limiar, pixel1, pixel2;
		int limiarBusiness = 0; // Medida Business
		int limiarProbCond = 0; // Medida da Prob Condicional

		int m_Width = bp.getWidth();
		int m_Height = bp.getHeight();

		/*
		 * if ( !(VerifyConsistentIn() && VerifyConsistentOut()) ) return FALSE;
		 */

		// Inicialização das matrizes Coocorrência
		for (i = 0; i < 256; i++) {
			for (j = 0; j < 256; j++) {
				c[i][j] = 0;
			}
		}

		// Direcciones:
		// 0 - Horizontal a la derecha
		// 1 - Vertical arriba
		// 2 - Horizontal a la izquierda
		// 3 - Vertical abajo

		for (m = 0; m < m_Height; ++m) {
			for (n = 0; n < m_Width; ++n) {
				pixel1 = bp.getPixel(n, m); // Pixel1 =
											// bpBits[(n)+((m)*dwWidth)];

				// Para el caso de direccion=0
				if (n < m_Width - distancia) {
					pixel2 = bp.getPixel(n + distancia, m); // Pixel2 =
															// bpBits[(n+Distancia)+((m
					c[pixel1][pixel2]++; // la cantidad de veces que se repite
											// que el nivel de gris pixel1
					// esté al lado del nivel de gris pixel2
				}

				// Para el caso de direccion=2
				if (n >= distancia) {
					pixel2 = bp.getPixel(n - distancia, m);
					c[pixel1][pixel2]++;
				}

				// Para el caso de direccion=3
				if (m >= distancia) {
					pixel2 = bp.getPixel(n, m - distancia);
					c[pixel1][pixel2]++;
				}
			}
		}

		auxBusiness = 0;
		auxProbCond = 0;

		for (int t = 0; t < 256; ++t) {
			// Cálculo de b1
			b1 = 0;
			for (i = 0; i < t; ++i) {
				for (j = 0; j < t; ++j) {
					b1 += w * c[i][j];
				}
			}

			// Cálculo de b2
			b2 = 0;
			for (i = t; i < 256; ++i) {
				for (j = t; j < 256; ++j) {
					b2 += w * c[i][j];
				}
			}

			// Cálculo de b3
			b3 = 0;
			for (i = 0; i < t; ++i) {
				for (j = t; j < 256; ++j) {
					b3 += w * c[i][j];
				}
			}

			// Cálculo de b4
			b4 = 0;
			for (i = t; i < 256; ++i) {
				for (j = 0; j < t; ++j) {
					b4 += w * c[i][j];
				}
			}

			// Calculo para la Medida Business
			if (((b3 + b4) != 0)) {
				if ((auxBusiness > (b3 + b4)) || (auxBusiness == 0))
				// if ( (AuxBusiness < (B3 + B4)) )
				{
					auxBusiness = b3 + b4;
					limiarBusiness = t;
				}
			}

			// Calculo para la Medida de Probabilidad Condicional
			if (((b1 + b3) != 0) && ((b2 + b4) != 0)) {
				if ((auxProbCond > ((b3 / (b1 + b3)) + (b4 / (b2 + b4)))) || (auxProbCond == 0))
				// if ( (AuxProbCond < (((double)B3 / (B1 + B3)) + ((double)B4 /
				// (B2 + B4)))) )
				{
					auxProbCond = ((b3 / (b1 + b3)) + (b4 / (b2 + b4)));
					limiarProbCond = t;
				}
			}
		}

		if (option == 0)
			limiar = limiarBusiness;
		else
			limiar = limiarProbCond;

		System.out.println("Umbral:" + limiar);
		ByteProcessor out = (ByteProcessor) bp.duplicate();
		out.threshold((int) limiar);
		Binary result = new Binary(new BinaryProcessor(out));
		result.copyAtribute("thresholdMatrizCoOcurrencia" + "(" + limiar + ")", this);
		return result;

	}

	/**********************
	 * ECUALIZACIONES
	 * 
	 * @throws IOException
	 **************************************************/

	/**
	 * Realiza la ecualizacion Lee LIP
	 * 
	 * @param alpha
	 * 
	 * @param beta
	 * 
	 * @param N
	 * 
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */

	public Gray equalizationLeeLIP(double alpha, double beta, int N) throws IOException {
		if (N % 2 == 0)
			throw new MyException("validator.impar");// error parametro
														// incorrecto
		Gray out = this.duplicate();
		int h, w, i, j, center, u, v, p, x, y, count;
		double pixelNormalized, M = 256.0, sum, mean, newPixel;
		h = this.bp.getHeight();
		w = this.bp.getWidth();
		center = N / 2;
		for (u = 0; u < w; u++) {
			for (v = 0; v < h; v++) {
				count = 0;
				sum = 0;
				for (j = -center; j <= center; j++) {
					for (i = -center; i <= center; i++) {
						x = u + i;
						y = v + j;
						if (x >= 0 && x < w && y >= 0 && y < h) {
							pixelNormalized = 1 - (double) this.bp.getPixel(x, y) / M;
							sum = sum + Math.log(pixelNormalized);
							count++;
						}
					}
				}
				pixelNormalized = 1 - (double) this.bp.getPixel(u, v) / M;
				pixelNormalized = Math.log(pixelNormalized);
				mean = sum / count;
				newPixel = Math.exp(alpha * mean + beta * (pixelNormalized - mean));
				newPixel = M * (1 - newPixel);
				out.bp.putPixel(u, v, (int) newPixel);
			}
		}
		out.copyAtribute("EqualizationLeeLIP", this);

		return out;

	}

	/**
	 * Realiza la ecualizacion Lee
	 * 
	 * @param alpha
	 * 
	 * @param beta
	 * 
	 * @param N
	 * 
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */
	public Gray equalizationLee(double alpha, double beta, int N) throws IOException {
		Gray out = this.duplicate();
		int h, w, i, j, center, u, v, p, x, y, count, pixel;
		double sum, mean, newPixel;
		h = this.bp.getHeight();
		w = this.bp.getWidth();
		center = 1;// se asume una ventana de dimension 3x3
		for (u = 0; u < w; u++) {
			for (v = 0; v < h; v++) {
				count = 0;
				sum = 0;
				for (j = -center; j <= center; j++) {
					for (i = -center; i <= center; i++) {
						x = u + i;
						y = v + j;
						if (x >= 0 && x < w && y >= 0 && y < h) {
							pixel = this.bp.getPixel(x, y);
							sum = sum + pixel;
							count++;
						}
					}
				}
				mean = sum / count;
				pixel = this.bp.getPixel(u, v);
				newPixel = alpha * mean + N + beta * (pixel - mean);
				out.bp.putPixel(u, v, (int) newPixel);
			}
		}
		out.copyAtribute("EqualizationLee", this);

		return out;
	}

	/**
	 * Ecualiza la imagen con el metodo BBHE
	 * 
	 * @return imagen ecualizada
	 * 
	 * @throws IOException
	 */
	public Gray Ecualizacion_BBHE() throws  IOException {

		Ecualizacion_BBHE B = new Ecualizacion_BBHE();
		ImageProcessor ip = B.wepi_run(this.bp);
		Gray out = new Gray((ByteProcessor) ip);
		out.copyAtribute("BBHE_", this);
		return out;
	}
	
	/**
	 * Ecualiza la imagen con el metodo Global
	 * 
	 * @return imagen ecualizada
	 * 
	 * @throws IOException
	 */
	public Gray Ecualizacion_Global() throws  IOException {

		Ecualizacion_Global B = new Ecualizacion_Global();
		ImageProcessor ip = B.wepi_run(this.bp);
		Gray out = new Gray((ByteProcessor) ip);
		out.copyAtribute("Global_", this);
		return out;
	}
	
	/**
	 * Ecualiza la imagen con el metodo BBHE
	 * 
	 * @return imagen ecualizada
	 * 
	 * @throws IOException
	 */
	public Gray Ecualizacion_Infrarrojo2PL() throws  IOException {

		Ecualizacion_Infrarrojo2PL B = new Ecualizacion_Infrarrojo2PL();
		ImageProcessor ip = B.wepi_run(this.bp);
		Gray out = new Gray((ByteProcessor) ip);
		out.copyAtribute("Infrarrojo2PL_", this);
		return out;
	}
	
	/**
	 * Ecualiza la imagen con el metodo BHEPL
	 * 
	 * @return imagen ecualizada
	 * 
	 * @throws IOException
	 */
	public Gray Ecualizacion_BHEPL() throws  IOException {

		Ecualizacion_BHEPL B = new Ecualizacion_BHEPL();
		ImageProcessor ip = B.wepi_run(this.bp);
		Gray out = new Gray((ByteProcessor) ip);
		out.copyAtribute("BHEPL_", this);
		return out;
	}
	
	/**
	 * Ecualiza la imagen con el metodo DSIHE
	 * 
	 * @return imagen ecualizada
	 * 
	 * @throws IOException
	 */
	public Gray Ecualizacion_DSIHE() throws  IOException {

		Ecualizacion_DSIHE B = new Ecualizacion_DSIHE();
		ImageProcessor ip = B.wepi_run(this.bp);
		Gray out = new Gray((ByteProcessor) ip);
		out.copyAtribute("DSIHE_", this);
		return out;
	}
	
	/**
	 * Ecualiza la imagen con el metodo DSIHE
	 * 
	 * @return imagen ecualizada
	 * 
	 * @throws IOException
	 */
	public Gray Ecualizacion_MMBEBHE() throws  IOException {

		Ecualizacion_MMBEBHE B = new Ecualizacion_MMBEBHE();
		ImageProcessor ip = B.wepi_run(this.bp);
		Gray out = new Gray((ByteProcessor) ip);
		out.copyAtribute("MMBEBHE_", this);
		return out;
	}
	/**
	 * Aplica un filtro de Nagao Matsuyama Recursivo a la imagen
	 * 
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */
	public Gray filterNagaoMatsuyamaRecursive() throws IOException {
		int[][] h1 = { { 1, 0, 0, 0, 0 }, { 0, 1, 0, 1, 0 }, { 0, 0, 1, 0, 0 }, { 0, 1, 0, 1, 0 }, { 0, 0, 0, 0, 1 } };

		int[][] h2 = { { 0, 0, 0, 0, 1 }, { 0, 1, 0, 1, 0 }, { 0, 0, 1, 0, 0 }, { 0, 1, 0, 1, 0 }, { 1, 0, 0, 0, 0 } };

		int[][] h3 = { { 0, 0, 0, 0, 0 }, { 0, 0, 1, 0, 0 }, { 1, 1, 1, 1, 1 }, { 0, 0, 1, 0, 0 }, { 0, 0, 0, 0, 0 } };

		int[][] h4 = { { 0, 0, 1, 0, 0 }, { 0, 0, 1, 0, 0 }, { 0, 1, 1, 1, 0 }, { 0, 0, 1, 0, 0 }, { 0, 0, 1, 0, 0 } };

		int[][] h5 = { { 0, 0, 0, 0, 0 }, { 0, 1, 1, 1, 0 }, { 0, 0, 1, 0, 0 }, { 0, 1, 1, 1, 0 }, { 0, 0, 0, 0, 0 } };

		int[][] h6 = { { 0, 0, 0, 0, 0 }, { 0, 1, 0, 1, 0 }, { 0, 1, 1, 1, 0 }, { 0, 1, 0, 1, 0 }, { 0, 0, 0, 0, 0 } };

		int[][] h7 = { { 0, 0, 0, 0, 0 }, { 0, 1, 1, 0, 0 }, { 0, 1, 1, 1, 0 }, { 0, 0, 1, 1, 0 }, { 0, 0, 0, 0, 0 } };

		int[][] h8 = { { 0, 0, 0, 0, 0 }, { 0, 0, 1, 1, 0 }, { 0, 1, 1, 1, 0 }, { 0, 1, 1, 0, 0 }, { 0, 0, 0, 0, 0 } };

		int[][] h9 = { { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 1, 0, 0 }, { 0, 1, 1, 1, 0 }, { 0, 1, 1, 1, 0 } };

		int[][] h10 = { { 0, 0, 0, 0, 0 }, { 1, 1, 0, 0, 0 }, { 1, 1, 1, 0, 0 }, { 1, 1, 0, 0, 0 }, { 0, 0, 0, 0, 0 } };

		int[][] h11 = { { 0, 1, 1, 1, 0 }, { 0, 1, 1, 1, 0 }, { 0, 0, 1, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 } };

		int[][] h12 = { { 0, 0, 0, 0, 0 }, { 0, 0, 0, 1, 1 }, { 0, 0, 1, 1, 1 }, { 0, 0, 0, 1, 1 }, { 0, 0, 0, 0, 0 } };
		int[][][] h = { h1, h2, h3, h4, h5, h6, h7, h8, h9, h10, h11, h12 };
		int i, j, k, f, c, fil, col, count, sum, pos;
		double var, min;
		double[] means = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };// 12 elements
		double[] variance = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };// 12
																	// elements
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		int[][] copy = bp.getIntArray();
		int width = bp.getWidth();
		int height = bp.getHeight();
		count = 7;
		boolean idem = false;
		int iterations = 0;
		while (!idem) {
			ImageProcessor aux = bp.duplicate();
			for (i = 0; i < width; i++) {
				for (j = 0; j < height; j++) {

					for (k = 0; k < h.length; k++) {
						sum = 0;
						// mean
						for (f = -2; f <= 2; f++) {
							for (c = -2; c <= 2; c++) {
								fil = i + f;
								col = j + c;
								if (fil >= 0 && fil < width && col >= 0 && col < height && h[k][f + 2][c + 2] == 1) {
									// count++;
									sum = sum + copy[fil][col];
								}
							}
						}
						means[k] = sum / count;
						var = 0;
						// variance
						for (f = -2; f <= 2; f++) {
							for (c = -2; c <= 2; c++) {
								fil = i + f;
								col = j + c;
								if (fil >= 0 && fil < width && col >= 0 && col < height && h[k][f + 2][c + 2] == 1) {
									// count++;
									var = var + (copy[fil][col] - means[k]) * (copy[fil][col] - means[k]);
								}
							}
						}
						variance[k] = var / count;
					}
					min = variance[0];
					pos = 0;
					// mean with lowest variance
					for (int x = 0; x < 9; x++) {
						if (variance[x] < min) {
							pos = x;
							min = variance[x];
						}
					}
					bp.set(i, j, (int) means[pos]);
				}
			}
			if (equals(aux, bp))
				idem = true;
			iterations++;
		}
		System.out.print("iteraciones:" + iterations);
		Gray out = new Gray(bp);
		out.copyAtribute("filterNagaoMatsuyamaRecursive", this);
		return out;
	}

	/**
	 * Metodo equals. Devuelve true si las imagenes son iguales, false en caso contrario.
	 * 
	 * @param ip1
	 *            Primera imagen.
	 * 
	 * @param ip2
	 *            Segunda imagen.
	 * @return boolean
	 */
	// Posiblemente tenga una pequeña falla, puede devolver true y no ser iguales. Verificar.
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
	 * Metodo que invoca al metodo que realiza el filtro de Mediana Ponderada.
	 * 
	 * @param mat
	 *            Matriz de strings a la que se le aplica el filtro.
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */
	public Gray medianWeightInvoke(String mat) throws IOException {
		int[][] matriz = parser(mat);
		int orden = matriz.length;
		if (orden % 2 == 0 || orden < 3)
			throw new MyException("filter.noLineal.orden_Impar");
		return this.filterMedianWeight(matriz);
	}

	/**
	 * Metodo que invoca al metodo que realiza el filtro de Media Ponderada.
	 * 
	 * @param mat
	 *            Matriz de strings a la que se le aplica el filtro.
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */
	public Gray meanWeightInvoke(String mat) throws Throwable {
		int[][] matriz = parser(mat);
		int orden = matriz.length;
		if (orden % 2 == 0 || orden < 3)
			throw new MyException("filter.lineal.orden_Impar");
		return this.filterMeanWeight(matriz);

	}

	/**
	 * Realiza la umbralizacion de acuerdo a un parametro recibido.
	 * 
	 * @param umbral
	 *            Valor que se utiliza como umbral para la binarizacion.
	 * @return imagen binaria
	 * 
	 * @throws IOException
	 */
	public Binary thresholdManual(int umbral) throws IOException {
		ByteProcessor out = (ByteProcessor) this.bp.duplicate();
		out.threshold(umbral);
		Binary out2 = new Binary(new BinaryProcessor(out));
		out2.copyAtribute("thresholdManual", this);
		return out2;
	}

	/**
	 * Aplica el filtro de Mediana Ponderada a una imagen.
	 * 
	 * @param filter
	 *            Matriz ultilizada como filtro.
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */
	public Gray filterMedianWeight(int[][] filter) throws IOException {
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		int w = bp.getWidth();
		int h = bp.getHeight();
		int q, v, u, i, j, k, pos;
		int vectorLen = 0;
		int[][] copy = bp.getIntArray();
		int[] vector;

		for (u = 0; u < filter.length; u++) {
			for (v = 0; v < filter[0].length; v++) {
				if ((pos = filter[u][v]) < 0)
					throw new MyException("200");
				vectorLen += pos;
			}
		}
		vector = new int[vectorLen];

		int K = filter[0].length / 2;
		int L = filter.length / 2;

		for (v = L; v <= h - L - 1; v++) {
			for (u = K; u <= w - K - 1; u++) {
				pos = 0;
				for (j = -L; j <= L; j++) {
					for (i = -K; i <= K; i++) {
						for (k = 0; k < filter[j + L][i + K]; k++)
							vector[pos++] = copy[u + i][v + j];
					}
				}
				q = (int) mediana(vector);
				bp.putPixel(u, v, q);
			}
		}
		Gray out = new Gray(bp);
		out.copyAtribute("FilterMedianWeight", this);
		return out;
	}

	/**
	 * Aplica el filtro de Media Ponderada a una imagen.
	 * 
	 * @param filter
	 *            Matriz ultilizada como filtro.
	 * @return imagen en escala de grises
	 * 
	 * @throws IOException
	 */

	public Gray filterMeanWeight(int[][] filter) throws IOException {
		ByteProcessor bp = (ByteProcessor) this.bp.duplicate();
		int M = bp.getWidth();
		int N = bp.getHeight();
		int u, v, i, j, p, q;
		double c, s, sum;
		s = 0;
		// arbitrary filter matrix of size (2K+1)x(2L+1)
		for (u = 0; u < filter.length; u++) {
			for (v = 0; v < filter[0].length; v++) {
				s += filter[u][v];
			}
		}

		int K = filter[0].length / 2;
		int L = filter.length / 2;

		ImageProcessor copy = bp.duplicate();

		for (v = L; v <= N - L - 1; v++) {
			for (u = K; u <= M - K - 1; u++) {
				// compute filter result for position (u,v)
				sum = 0;
				for (j = -L; j <= L; j++) {
					for (i = -K; i <= K; i++) {
						p = copy.getPixel(u + i, v + j);
						c = filter[j + L][i + K];
						sum = sum + c * p;
					}
				}
				q = (int) (sum / s);
				// clamp result
				if (q < 0)
					q = 0;
				if (q > 255)
					q = 255;
				bp.putPixel(u, v, q);
			}
		}
		Gray out = new Gray(bp);
		out.copyAtribute("filterMeanWeight", this);

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
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Gray filterLipschitz(double slope, boolean down, boolean hat) throws IOException {
		Lipschitz l = new Lipschitz();
		ImagePlus imp = l.wepi_run(this.bp, slope, down, hat);

		Gray result = new Gray((ByteProcessor) imp.getProcessor());
		result.copyAtribute("filterLipschitz", this);
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
	 * Corrige los defectos en la imagen causados por iluminación desbalanceada.
	 * 
	 * @param iterations
	 *            número de iteraciones
	 * @param radius
	 *            radio para el rankeo inferior
	 * @param autocontrast
	 *            realizar autocontraste
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Gray backgroundCorrection(int iterations, int radius, boolean autocontrast) throws IOException {
		Background_Correction b = new Background_Correction();
		b.wepi_setup(iterations, radius, autocontrast);
		ImageProcessor ip = b.wepi_run(this.bp);
		Gray out = new Gray((ByteProcessor) ip);
		out.copyAtribute("BGCorrection_", this);
		return out;
	}

	/**
	 * Desentrelaza la imagen
	 * 
	 * @param choice
	 *            metodo de desentrelazado
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Gray deinterlace(int choice) throws IOException {
		Deinterlace d = new Deinterlace();
		d.wepi_setup(choice);
		ImageProcessor ip = d.wepi_run(this.bp);
		Gray out = new Gray((ByteProcessor) ip);
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
	 * Encuentra el umbral automáticamente a partir de la entropía del histograma
	 * 
	 * @return imagen binaria
	 * @throws IOException
	 *             Señala que ha ocurrido una excepción de I/O.
	 */
	public Binary entropyThreshold() throws IOException {
		Entropy_Threshold e = new Entropy_Threshold();
		BinaryProcessor bp = new BinaryProcessor((ByteProcessor) e.wepi_run(this.bp));
		Binary out = new Binary(bp);
		out.copyAtribute("EntropyThreshold_", this);
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
	 * @return imagen en escala de grises
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public Gray blur() throws IOException {
		Gray out = this.duplicate();
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
	public Gray sharpen() throws IOException {
		Gray out = this.duplicate();
		int[] SHARPEN = { 0, -1, 0, -1, 5, -1, 0, -1, 0 };
		out.bp.convolve3x3(SHARPEN);

		out.copyAtribute("Sharpen_", this);
		return out;
	}
}
