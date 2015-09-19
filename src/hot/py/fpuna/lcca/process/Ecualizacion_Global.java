package py.fpuna.lcca.process;

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Ecualizacion_Global implements PlugInFilter {
	
	public int setup(String arg, ImagePlus imp) {
		return DOES_8G + DOES_STACKS + SUPPORTS_MASKING + NO_CHANGES;
	}
	
	public ImageProcessor wepi_run(ImageProcessor imageProcessor) {
		ImageProcessor ip = imageProcessor.duplicate();
		int altura = ip.getHeight();
        int ancho = ip.getWidth();
        
        //Obtenemos la funcion f(x) o funcion de ecualizacion
        Double [] funcionEcualizacion = obtenerFuncionDeEcualizacion(ip.getHistogram(), 0.0, 255.0);
        
        /*Se recorre la imagen y se reemplaza el valor de intensidad dada la siguiente relacion:
            Y = Y(i,j) = f(X(i,j)| para todo X(i,j) perteneciente a X)
        */
        for(int j = 0; j < altura; j++){
            for(int i = 0; i < ancho; i++){
                //Se obtiene la intesidad del pixel actual que se usara como indice para la funcion de ecualizacion
                int intensidadPixelActual = ip.getPixel(i, j);
                ip.putPixelValue(i, j, funcionEcualizacion[intensidadPixelActual]);
            }
        }
        return ip;
	}
	
	public void run (ImageProcessor ip) {

	}
	
	private static Double[] obtenerFuncionDeEcualizacion(int[] histograma, double x0, double xl){
        
        /*Se inicializan los arrays:
            *  funcionEcualizacion.
            *  funcionProbabilidad.
            *  histogramaAcumulado.
         Con la longitud del histograma recibido como parametro*/
        
        //Histograma que sera devuelto por la funcion al finalizar la ecualizacion
        Double funcionEcualizacion [] = new Double[histograma.length] ;
        
        //Array que contiene la propabilidad de ocurrencia de cada intensidad de la imagen
        Double funcionProbabilidad [] = new Double[histograma.length] ;
        
        //Array que representa el histograma acumulado normalizado del histograma que se recibe como parametro
        Double histogramaAcumulado [] = new Double[histograma.length] ;
        
        //N representa el numero de pixeles que contiene la imagen
        double n = 0;
        
        //Obtenemos el numero total de pixeles de la imagen a partir del histograma
        for(int i = 0; i < histograma.length; i++){
            n += histograma[i];
        }
        
        //Obtenemos la probabilidad de aparicion de cada intensidad 
        for(int i = 0; i < histograma.length; i++){
            funcionProbabilidad[i] = histograma[i]/n;
        }
        
        /*Obtenemos el histograma acumulado normalizado mediante la sumarizacion de las probabilidades
         *obtenidas anteriormente*/
        Double acumuladorAux = 0.0;
        for(int i = 0; i < funcionProbabilidad.length; i++){
            acumuladorAux += funcionProbabilidad[i];
            histogramaAcumulado[i] = acumuladorAux;
        }
        
        /*Se procede a hallar la funcion de ecualizacion, mediante la formula:
            f(x)=X_0 + (X_L-1 - X_0) *[c(x) - 0.5*p(x)]
          Donde:
            * X_0 es el menor nivel de intensidad.
            * X_L-1 es el mayor nivel de intensidad, en el codigo sera presendato como 'xl'.
            * c(x) es el histograma acumulado normalizado de la imagen.
            * p(x) es la funcion de probabilidades perteneciente a la imagen.
        */
        for(int i = 0; i < histogramaAcumulado.length; i++){
            //Se redondea la operacion usando el redondeo tradicional
            //Double valorRedondeado = Math.rint(0.0 + ((255.0 - 0.0) * (histogramaAcumulado[i] - (0.5 * funcionProbabilidad[i]))));
            //Se almacena el valor redondeado en la funcion de ecualizacion
            funcionEcualizacion[i] = x0 + ((xl - x0) * (histogramaAcumulado[i] - (0.5 * funcionProbabilidad[i])));
        }
        return funcionEcualizacion;
    }
}
