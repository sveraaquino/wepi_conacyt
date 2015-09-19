package py.fpuna.lcca.util;

import java.io.IOException;
import java.io.Serializable;

import py.fpuna.lcca.model.Binary;
import py.fpuna.lcca.model.Color;
import py.fpuna.lcca.model.Gray;
import ij.*;
import ij.process.*;

public class Image implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String Name;
	private String mime;
	private String tipo;
	private int length;

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public String getName() {
		return Name;
	}

	public void copyAtribute(String method, Image img) throws IOException {
		this.setName(method + "_" + img.getName());
		this.setMime(img.getMime());
		this.setTipo(img.getTipo());
		this.setLength(img.getData().length);
	}

	public void setName(String name) {
		Name = name;
		int extDot = name.lastIndexOf('.');
		if (extDot > 0) {
			String extension = name.substring(extDot + 1);
			if ("bmp".equals(extension)) {
				mime = "image/bmp";
				tipo = "bmp";
			} else if ("jpg".equals(extension)) {
				mime = "image/jpeg";
				tipo = "jpg";
			} else if ("gif".equals(extension)) {
				mime = "image/gif";
				tipo = "gif";
			} else if ("png".equals(extension)) {
				mime = "image/png";
				tipo = "png";
			} else if ("BMP".equals(extension)) {
				mime = "image/BMP";
				tipo = "BMP";
			} else {
				mime = "image/unknown";
			}
		}
	}

	public String getMime() {
		return mime;
	}

	public void setMime(String mime) {
		this.mime = mime;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	/**
	 * abre la imagen
	 * 
	 * recibe el path completo de la imagen como string y crea una instancia de Gray, binario o color y retorna
	 * 
	 * @param string
	 * @return Imagen
	 */
	public static Image abrir(String path) throws MyException {
		ImagePlus ip = new ImagePlus(path);
		ImageProcessor pro = ip.getProcessor();

		if (isBinary(pro)) {
			Binary out = new Binary(new BinaryProcessor((ByteProcessor) pro));
			return out;
		} else if (pro.isGrayscale()) {
			ByteProcessor ret = (ByteProcessor) pro.convertToByte(false);
			Gray out = new Gray(ret);
			return out;
		} else {
			Color out = new Color(pro);
			return out;
		}
	};

	private static boolean isBinary(ImageProcessor ip) {
		int w = ip.getWidth();
		int h = ip.getHeight();
		int i, j, pixel;
		for (j = 0; j < h; j++)
			for (i = 0; i < w; i++) {
				pixel = ip.get(i, j);
				if (pixel != 255 && pixel != 0)
					return false;
			}
		return true;
	}

	/**
	 * 
	 * Todos los metodos que se encuentran en Imagen lanzan una excepcion(excepto abrir que es metodo de clase) que sera heredado por todas las demas clases que extiendan de Imagen a no se que los metodos se sobrescriban
	 * 
	 */
	public Image invertirImagen() throws MyException, IOException {
		throw new MyException();
		// return null;
	}

	public Image dilate(int[][] se, int xCenter, int yCenter) throws MyException {
		throw new MyException();
		// return null;
	}

	public int[] histogram() throws MyException {
		throw new MyException();
		// return null;
	}

	public byte[] getData() throws MyException, IOException {
		throw new MyException();
	}

	public Image erode(int[][] se, int xCenter, int yCenter) throws MyException {
		throw new MyException();
	}

	public Image erode2(int[][] se, int xCenter, int yCenter) throws MyException {
		throw new MyException();
	}

	public int[][] reflect(int[][] se) throws MyException {
		throw new MyException();
	}

	public int getWidth() throws MyException {
		throw new MyException();
	}

	public int getHeight() throws MyException {
		throw new MyException();
	}

}
