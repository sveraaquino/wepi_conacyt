package py.fpuna.lcca.process;

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;


public class Ecualizacion_MMBEBHE implements PlugInFilter {
	
	
	public int setup(String arg, ImagePlus imp) {
		return DOES_8G + DOES_STACKS + SUPPORTS_MASKING + NO_CHANGES;
	}
	
	public ImageProcessor wepi_run(ImageProcessor imageProcessor) {
		ImageProcessor ip = imageProcessor.duplicate();
		int altura = ip.getHeight();
        int ancho = ip.getWidth();
        
        double n = altura * ancho;
        
        //Variable que alamacenara el minMBE encontrado 
        double minMBE = Double.MAX_VALUE;
        
        //Intensidades de la imagen original
        double intensidadMediaOriginal = 0.0;
        
        //Acumulador de MBE
        double acMBE = 0.0;
        
        //Punto de division del histograma global
        int xt = 0;
        
        //Histograma total de la imagen
        int [] histogramaTotal = ip.getHistogram();
        
        //Funcion de probabilidad de la imagen original
        Double [] funcionProbabilidad = new Double[histogramaTotal.length];
        
        //Obtenemos la funcion de probabilidad de la imagen original
        for(int i = 0; i < histogramaTotal.length; i++){
            funcionProbabilidad[i] = histogramaTotal[i]/n;
        }
        
        //Calculamos la intensidad media de la imagen sin ecualizar
        for(int i = 0; i < funcionProbabilidad.length; i++){
            intensidadMediaOriginal += funcionProbabilidad[i] * i;
        }
        
        /*Procedemos a hallar el menor MBE (Mean Brigthness Error) usando la siguiente ecuacion:
            MBE_0 = 0.5 * (L * (1 - P(X_0))) - E(X) para i = 0
            MBE_i = MBE_i-1 + 0.5 * (1 - (L * P(X_i))) para i = 1 hasta 255,
            donde L = 255 ya que se trata de una imagen de escala de grises de 8-bit
            
        */
        for(int i = 0; i < histogramaTotal.length; i++){
            if(i == 0){
                acMBE = (0.5 * (255 * (1 - funcionProbabilidad[i]))) - intensidadMediaOriginal;
            }else{
                acMBE = acMBE + (0.5 * (1 - (255 * funcionProbabilidad[i]))); 
            }
            
            //Si acMBE es menor al menor MBE encontrado hasta ahora entonces guardamos el valor y el punto de corte
            if(Math.abs(acMBE) < minMBE){
                minMBE = Math.abs(acMBE);
                xt = i;
            }
        }
        
        /*En base al Xt hallado procedemos a dividir el histograma total en:
            *lowerHistogram: que posee las intensidades de 0 a Xt.
            *upperHistogram: que posee las intensideades de (Xt + 1) a 255.
        */
        int [] lowerHistogram = new int [xt + 1];
        
        //Se halla la longitud del upperHistogram y se inicializa el mismo
        int longitudUpperHistogram = 255 - xt;
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
        Double [] funcionEcualizacionLower = obtenerFuncionDeEcualizacion(lowerHistogram, 0.0, xt);
        
        Double [] funcionEcualizacionUpper = obtenerFuncionDeEcualizacion(upperHistogram, xt + 1, 255.0);
        
        /*Se recorre la imagen y se reemplaza el valor de intensidad dada la siguiente relacion:
            Y = Y(i,j) = f_l(X_l) union f_u(X_u) donde:
            * f_l(X_l) = f_l(X(i,j)| para todo X(i,j) perteneciente a X_l.
            * f_u(X_u) = f_u(X(i,j)| para todo X(i,j) perteneciente a X_u.
        */
        int indiceInicialUpper = xt + 1;
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