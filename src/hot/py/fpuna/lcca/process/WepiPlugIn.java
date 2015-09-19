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
	 * Configura los par�metros necesarios para la ejecuci�n. No debe desplegar di�logos.
	 * 
	 * @param params
	 *            the params
	 * @return true, si se puede continuar la ejecuci�n
	 */
	public boolean wepi_setup(Object... params);
}
