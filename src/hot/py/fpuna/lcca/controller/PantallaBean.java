package py.fpuna.lcca.controller;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Synchronized;

import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

import com.google.gson.Gson;

import py.fpuna.lcca.model.Gray;
import py.fpuna.lcca.model.Color;
import py.fpuna.lcca.model.Binary;
import py.fpuna.lcca.util.Image;
import py.fpuna.lcca.util.ManagerMessage;
import py.fpuna.lcca.util.MyException;
import py.fpuna.lcca.util.Series;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

/**
 * @author santiago
 * 
 *         Clase que implementa el controlador del sistema en el se encuentra las funciones y variables que menejan la vista del cliente
 * 
 */

@Name("pantallaBean")
@Scope(ScopeType.SESSION)
@Synchronized(timeout = 10000000)
public class PantallaBean extends ManagerMessage {

	private ArrayList<Image> imagenes = new ArrayList<Image>();
	private int uploadsAvailable = 20;
	private Image ImageActual = null;
	private int filaActual;

	private Image paramAuxImg = null;
	private String numParam;
	private int paramAuxInt;
	private String paramAuxStr;
	private double paramAuxDouble;
	private Boolean paramAuxBoolean = false;

	private String chartData;
	private String categories;

	private String pathImageView = "cruz.png";
	private String pathImageSelection = "cross";

	private Object[] parameters = null;
	private Class[] classParam = null;
	private String methodCurrent;
	private int index = 0;

	private int[][] cross = { { 0, 1, 0 }, { 1, 1, 1 }, { 0, 1, 0 } };
	private int[][] square = { { 1, 1, 1 }, { 1, 1, 1 }, { 1, 1, 1 } };
	private int[][] vertical = { { 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 } };
	private int[][] horizontal = { { 0, 0, 0 }, { 1, 1, 1 }, { 0, 0, 0 } };
	private int[][] circle = { { 0, 1, 1, 1, 0 }, { 1, 1, 1, 1, 1 }, { 1, 1, 1, 1, 1 }, { 1, 1, 1, 1, 1 }, { 0, 1, 1, 1, 0 } };

	public String getPathImageSelection() {
		return pathImageSelection;
	}

	public String imageActualName() {
		return this.ImageActual.getName();
	}

	public void setPathImageSelection(String pathImageSelection) {
		this.pathImageSelection = pathImageSelection;
		setPathImageView(pathImageSelection);
		System.out.println("Valor del radio selection:  " + this.pathImageSelection);
	}

	public void trueSet() {
		this.paramAuxBoolean = true;
		System.out.println("true xxxxxx");
	}

	/**
	 * @return retorna un boolean diciendo si ya se cargaron imagenes a la lista
	 */
	public Boolean isEmpty() {

		if (this.imagenes.size() == 0)
			return false;
		else
			return true;
	}

	public Boolean isGray() {
		return Gray.class.isInstance(ImageActual);
	}

	public Boolean isColor() {
		return Color.class.isInstance(ImageActual);
	}

	public Boolean isBinary() {
		return Binary.class.isInstance(ImageActual);
	}

	public Boolean getParamAuxBoolean() {
		return paramAuxBoolean;
	}

	public void setParamAuxBoolean(Boolean paramAuxBoolean) {
		this.paramAuxBoolean = paramAuxBoolean;
		parameters[index] = paramAuxBoolean;
		classParam[index] = Boolean.TYPE;
		index++;
	}

	public String getChartData() {
		return chartData;
	}

	public void setChartData(String chartData) {
		this.chartData = chartData;
	}

	public String getCategories() {
		return categories;
	}

	public void setCategories(String categories) {
		this.categories = categories;
	}

	/**
	 * @return list es una lista de imagenes el cual contiene todos lo que tiene la misma instancia de la imagen actual
	 */
	public List<SelectItem> getImageType() {

		Class gray = Gray.class;
		Class binary = Binary.class;

		List<SelectItem> list = new ArrayList<SelectItem>();

		if (gray.isInstance(ImageActual)) {
			for (Image aux : imagenes) {
				if (gray.isInstance(aux))
					list.add(new SelectItem(imagenes.indexOf(aux), aux.getName()));

			}

		} else if (binary.isInstance(ImageActual)) {
			for (Image aux : imagenes) {
				if (binary.isInstance(aux))
					list.add(new SelectItem(imagenes.indexOf(aux), aux.getName()));
			}

		}

		return list;
	}

	public int getIndex() {
		return index;
	}

	/**
	 * @return Retorna un string con el mime de la imagen ha ser graficada en la vista
	 */
	public String mimePopup() {

		return imagenes.get(filaActual).getMime();
	}

	public void popupPaint(OutputStream stream, Object object) throws IOException {

		stream.write(imagenes.get(filaActual).getData());

	}

	public void setIndex(int index) {

		parameters[0] = imagenes.get(index);
		classParam[0] = parameters[0].getClass();
		this.index = 1;
	}

	public Image getParamAuxImg() {
		return paramAuxImg;
	}

	public void setParamAuxImg(Image paramAuxImg) {
		parameters[0] = paramAuxImg;
		classParam[0] = paramAuxImg.getClass();
		System.out.println(paramAuxImg.getName());
		index++;
	}

	public String getPathImageView() {
		// System.out.println("eleccion :" + this.pathImageView);
		return pathImageView;
	}

	public void setPathImageView(String pathImageView) {

		parameters[2] = 1;
		classParam[2] = Integer.TYPE;

		parameters[3] = 1;
		classParam[3] = Integer.TYPE;
		classParam[1] = this.cross.getClass();

		if (pathImageView.equals("cross")) {
			parameters[1] = this.cross;
			this.pathImageView = "cruz.png";

		} else if (pathImageView.equals("square")) {
			parameters[1] = this.square;
			this.pathImageView = "cuadrado.png";

		} else if (pathImageView.equals("vertical")) {
			parameters[1] = this.vertical;
			this.pathImageView = "vertical.png";
		} else if (pathImageView.equals("horizontal")) {
			parameters[1] = this.horizontal;
			this.pathImageView = "horizontal.png";

		} else if (pathImageView.equals("circle")) {
			parameters[1] = this.circle;
			this.pathImageView = "circular.png";

		} else {

			parameters[0] = pathImageView;
			classParam[0] = String.class;
			System.out.println(pathImageView);

		}

	}

	public String getMethodCurrent() {
		return methodCurrent;
	}

	public void setMethodCurrent(String methodCurrent) {
		this.methodCurrent = methodCurrent;
		System.out.println("metodo :" + methodCurrent);

	}

	public double getParamAuxDouble() {
		return paramAuxDouble;
	}

	public void setParamAuxDouble(double paramAuxDouble) {
		this.paramAuxDouble = paramAuxDouble;
		parameters[index] = paramAuxDouble;
		classParam[index] = Double.TYPE;
		index++;
	}

	public int getParamAuxInt() {
		return paramAuxInt;
	}

	public void setParamAuxInt(int paramAuxInt) {

		if (this.methodCurrent.equals("morphology"))
			this.index = 4;

		this.paramAuxInt = paramAuxInt;
		parameters[index] = paramAuxInt;
		classParam[index] = Integer.TYPE;
		this.paramAuxInt = 1;
		index++;
	}

	public String getParamAuxStr() {
		return paramAuxStr;
	}

	public void setParamAuxStr(String paramAuxStr) {
		this.paramAuxStr = paramAuxStr;
		if (this.methodCurrent.equals("operationBetween")) {
			parameters[1] = paramAuxStr;
			classParam[1] = paramAuxStr.getClass();
		} else {
			parameters[index] = paramAuxStr;
			classParam[index] = paramAuxStr.getClass();
			index++;
		}

	}

	public Object[] getParameters() {
		return parameters;
	}

	public String getNumParam() {
		return numParam;
	}

	public void setNumParam(String numParam) {

		this.paramAuxStr = " ";
		this.pathImageSelection = "cross";
		this.paramAuxDouble = 0.0;
		this.paramAuxInt = 0;
		pathImageSelection = "cross";
		if (numParam.equals("0")) {
			parameters = null;
			classParam = null;

		} else {
			this.numParam = numParam;
			int aux = Integer.parseInt(numParam);
			parameters = new Object[aux];
			classParam = new Class[aux];
			System.out.println("cantidad de parametros " + aux);
		}
		index = 0;
	}

	public int getSize() {

		if (this.imagenes.size() > 0) {
			return getImagenes().size();
		} else {
			return 0;
		}
	}

	public PantallaBean() {
	}

	public void paint(OutputStream stream, Object object) throws IOException {

		stream.write(getImageActual().getData());

	}

	public void listener(UploadEvent event) throws IOException {
		UploadItem item = event.getUploadItem();
		Image file = new Image();

		try {
			file = Image.abrir(item.getFile().getPath());
			file.setName(item.getFileName());
			// file.setMime(item.getContentType());
			file.setLength((int) item.getFile().length());
			imagenes.add(file);

			ImageActual = file;
			uploadsAvailable--;
		} catch (Exception e) {
			System.out.println("ERROR:[listener()]: " + e);
		}
	}

	public String clearUploadData() {
		imagenes.clear();
		setUploadsAvailable(20);
		return null;
	}

	public Image getImageActual() {
		return ImageActual;
	}

	public void setImageActual(Image img) {
		ImageActual = img;
	}

	public int getFilaActual() {
		return filaActual;
	}

	public void setFilaActual(int filaActual) {
		this.filaActual = filaActual;
	}

	public long getTimeStamp() {
		return System.currentTimeMillis();
	}

	public ArrayList<Image> getImagenes() {
		return this.imagenes;

	}

	public void setImagenes(ArrayList<Image> files) {
		this.imagenes = files;
	}

	public int getUploadsAvailable() {
		return uploadsAvailable;
	}

	public void setUploadsAvailable(int uploadsAvailable) {
		this.uploadsAvailable = uploadsAvailable;
	}

	/**
	 * Funcion que se encarga de eliminar elementos de la lista de imagenes a travez de la variable filaActual el cual es el indice del elemento en la lista
	 */
	public void delete() {

		if (this.ImageActual.equals(this.imagenes.get(filaActual))) {
			if (getSize() > 1) {
				this.imagenes.remove(filaActual);
				this.ImageActual = this.imagenes.get(0);
			} else {
				this.imagenes.remove(filaActual);
				this.ImageActual = null;

			}
		} else {
			this.imagenes.remove(filaActual);
		}
	}

	/**
	 * @param methodName
	 *            nombre de la funcion a ser ejecutada
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws ReflectiveOperationException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * 
	 *             metodo que se encarga de ejecutar en forma automatica o generica cualquier funcion con solo recibir el nombre de la misma alojada en la variable method
	 */
	public void cleanAll() {
		numParam = "0";
		parameters = null;
		classParam = null;

	}

	public void execute_method() throws ClassNotFoundException, NoSuchMethodException, SecurityException, Exception, IllegalArgumentException, InvocationTargetException, MyException {

		Class gray = Gray.class;
		Class color = Color.class;
		Class binary = Binary.class;
		Object res;

		try {
			if (gray.isInstance(ImageActual)) {

				Method function = gray.getMethod(methodCurrent, classParam);
				res = function.invoke(ImageActual, parameters);
				imagenes.add((Image) res);
				ImageActual = (Image) res;
				cleanAll();

			} else if (color.isInstance(ImageActual)) {
				Method function = color.getMethod(methodCurrent, classParam);
				res = function.invoke(ImageActual, parameters);
				imagenes.add((Image) res);
				ImageActual = (Image) res;
				cleanAll();

			} else if (binary.isInstance(ImageActual)) {
				Method function = binary.getMethod(methodCurrent, classParam);
				res = function.invoke(ImageActual, parameters);

				imagenes.add((Image) res);
				ImageActual = (Image) res;
				cleanAll();

			}

		} catch (Exception e) {

			if (e.getCause() == null) {
				addError(this.getMessage("type.Image"));
				this.setNumParam(this.numParam);
			} else {
				addError(this.getMessage(e.getCause().getMessage()));
				this.setNumParam(this.numParam);
			}
		}

	}

	/**
	 * Funcion que se encarga de realizar el histograma de la imagen seleccionada, y de cargar los valores que seran necesarios para ser graficas como JSON
	 */
	public void loadHist() {

		Class color = Color.class;
		this.categories = null;
		this.chartData = null;
		List<String> categoryList = this.generateCat();
		List<Series> series = new ArrayList<Series>();

		if (color.isInstance(this.imagenes.get(filaActual))) {
			Color aux = (Color) this.imagenes.get(filaActual);
			int count[][] = aux.getHistogram();

			series.add(new Series("BLUE", count[2]));
			series.add(new Series("GREEN", count[1]));
			series.add(new Series("RED", count[0]));

		} else {

			Image aux = this.imagenes.get(filaActual);
			int count[] = aux.histogram();
			series.add(new Series("GRAY", count));
		}
		this.setChartData(new Gson().toJson(series));
		this.setCategories(new Gson().toJson(categoryList));
		this.paramAuxStr = this.imagenes.get(filaActual).getName();
		this.paramAuxBoolean = false;

	}

	private List<String> generateCat() {
		List<String> categoryList = new ArrayList<String>();
		for (int i = 0; i < 256; i++) {
			categoryList.add(Integer.toString(i));

		}

		return categoryList;

	}

}
