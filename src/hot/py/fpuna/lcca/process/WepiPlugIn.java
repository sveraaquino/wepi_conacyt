package py.fpuna.lcca.process;

import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/**
 * Interfaz PlugIn para el WEPI.
 */
public interface WepiPlugIn extends PlugInFilter {

	/**
	 * Metodo que ejecuta el plugin. No debe desplegar dialogos. Debe modificar una copia del original.
	 * 
	 * @param ip
	 *            ImageProcessor de la imagen
	 * @return ImageProcessor de la copia modificada
	 */
	public ImageProcessor wepi_run(ImageProcessor ip);

	/**
	 * Configura los parámetros necesarios para la ejecución. No debe desplegar diálogos.
	 * 
	 * @param params
	 *            the params
	 * @return true, si se puede continuar la ejecución
	 */
	public boolean wepi_setup(Object... params);
}
