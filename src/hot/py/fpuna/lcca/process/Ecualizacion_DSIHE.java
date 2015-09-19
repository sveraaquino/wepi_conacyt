package py.fpuna.lcca.process;

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;


public class Ecualizacion_DSIHE implements PlugInFilter {
	
	
	public int setup(String arg, ImagePlus imp) {
		return DOES_8G + DOES_STACKS + SUPPORTS_MASKING + NO_CHANGES;
	}
	
	public ImageProcessor wepi_run(ImageProcessor imageProcessor) {
		ImageProcessor ip = imageProcessor.duplicate();
		
		int altura = ip.getHeight();
        int ancho = ip.getWidth();
        
        //Histograma total de la imagen
        int [] histogramaTotal = ip.getHistogram();
        
        //Array que contiene la propabilidad de ocurrencia de cada intensidad de la imagen
        Double funcionProbabilidad [] = new Double[histogramaTotal.length];
        
        //Numero total de pixeles en la imagen
        double n = altura * ancho;
        
        //Obtenemos la probabilidad de aparicion de cada intensidad 
        for(int i = 0; i < histogramaTotal.length; i++){
            funcionProbabilidad[i] = histogramaTotal[i]/n;
        }
        
        //Xe representa el nivel de gris cuya probabilidad acumulada es de 0.5
        double xe = 0.0;
        
        //Booleano que indica si se encontro el nivel de gris con propabilidad acumulada de 0.5
        boolean seEncontroIntensidad = false;
        
        /*Obtenemos el histograma acumulado normalizado mediante la sumarizacion de las probabilidades
         *obtenidas anteriormente*/
        Double acumuladorAux = 0.0;
        for(int i = 0; i < funcionProbabilidad.length; i++){
            acumuladorAux += funcionProbabilidad[i];
            
            /*Buscamos la intensidad que posea probabilidad de 0.5, o en todo caso su inmediato sucesor
             *a medida que construimos el histograma acumulado*/
            if(!seEncontroIntensidad && acumuladorAux >= 0.5){
                xe = i;
                seEncontroIntensidad = true;
                break;
            }
        }
        
        /*Xt viene dado por la formula:
            * Xt = 0.5 * (Xe + Xg)
         *donde:
            * Xe: representa el nivel de gris cuya probabilidad acumulada es de 0.5
            * Xg: es el nivel medio de gris (127.5).
        */
        
        Double xt = 0.5 * (xe + 127.5);
        
        /*En base al Xt hallado procedemos a dividir el histograma total en:
            *lowerHistogram: que posee las intensidades de 0 a Xt.
            *upperHistogram: que posee las intensideades de (Xt + 1) a 255.
        */
        int [] lowerHistogram = new int [xt.intValue() + 1];
        
        //Se halla la longitud del upperHistogram y se inicializa el mismo
        int longitudUpperHistogram = 255 - xt.intValue();
        int [] upperHistogram = new int [longitudUpperHistogram];
        
        //Cargamos cada histograma con sus respectivos valores a partir del histograma total
        int contadorAux = -1;
        for(int i = 0; i < histogramaTotal.length; i++){
            if(i <= xt){
                //Si las intensidades van de 0 - xt, se guardan en lowerHistogram
                lowerHistogram[i] = histogramaTotal[i];
            }else{
                //Si van de (xt+1) a 255 se guarda en upperHistogram
                contadorAux ++;
                upperHistogram[contadorAux] = histogramaTotal[i];
            }
        }
        
        //Se obtienen las funciones de ecualizacion tanto para el lowerHistogram como para el upperHistogram
        Double [] funcionEcualizacionLower = obtenerFuncionDeEcualizacion(lowerHistogram, 0.0, xt.intValue());
        
        Double [] funcionEcualizacionUpper = obtenerFuncionDeEcualizacion(upperHistogram, xt.intValue() + 1, 255.0);
        
        /*Se recorre la imagen y se reemplaza el valor de intensidad dada la siguiente relacion:
            Y = Y(i,j) = f_l(X_l) union f_u(X_u) donde:
            * f_l(X_l) = f_l(X(i,j)| para todo X(i,j) perteneciente a X_l.
            * f_u(X_u) = f_u(X(i,j)| para todo X(i,j) perteneciente a X_u.
        */
        int indiceInicialUpper = xt.intValue() + 1;
        for(int j = 0; j < altura; j++){
            for(int i = 0; i < ancho; i++){
                int intensidadPixelActual = ip.getPixel(i, j);
                if(intensidadPixelActual < indiceInicialUpper){
                    //Si la intensidad actual se encuentra en la parte baja del histograma
                    ip.putPixelValue(i, j, funcionEcualizacionLower[intensidadPixelActual]);
                }else{
                    //Si se ecuentra en la parte superior del histograma
                    ip.putPixelValue(i, j, funcionEcualizacionUpper[intensidadPixelActual - indiceInicialUpper]);
                }
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