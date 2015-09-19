package py.fpuna.lcca.process;

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;


public class Ecualizacion_BHEPL implements PlugInFilter {
	
	
	public int setup(String arg, ImagePlus imp) {
		return DOES_8G + DOES_STACKS + SUPPORTS_MASKING + NO_CHANGES;
	}
	
	public ImageProcessor wepi_run(ImageProcessor imageProcessor) {
		ImageProcessor ip = imageProcessor.duplicate();
		
		Boolean usarMediana = true;
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
        
        //Obtenemos la intensidad media de los pixeles en la imagen
        Double xm = 0.0;
        for(int i = 0; i < funcionProbabilidad.length; i++){
            xm += funcionProbabilidad[i] * i;
        }
        
        /*Xt viene dado por la formula:
            * Xt = 0.5 * (Xm + Xg)
         *donde:
            * Xm: es la intensidad media de los pixeles en la imagen.
            * Xg: es el nivel medio de gris (127.5).
        */
        
        Double xt = 0.5 * (xm + 127.5);
        
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
        
        /*Una vez que hemos dividido el histograma total en dos, procedemos a acotar los mismos, 
         * para ello debemos calcular el plateauLimit para cada sub-histograma*/
        double lowerPlateauLimit = 0.0;
        double upperPlateauLimit = 0.0;
        
        if(!usarMediana){
            /*Procedemos a hallar el lowerPlateauLimit*/
            int acumuladorAux = 0;
            for(int i = 0; i < lowerHistogram.length; i++){
                acumuladorAux += lowerHistogram[i];
            }

            lowerPlateauLimit = acumuladorAux/(xt + 1);

            /*Procedemos a hallar el upperPlateauLimit*/
            acumuladorAux = 0;
            for(int i = 0; i < upperHistogram.length; i++){
                acumuladorAux += upperHistogram[i];
            }

            upperPlateauLimit = acumuladorAux/(255 - xt);
        }else{
            /*Procedemos a hallar el lowerPlateauLimit*/
            lowerPlateauLimit = obtenerMediana(lowerHistogram);
            
            /*Procedemos a hallar el upperPlateauLimit*/
            upperPlateauLimit = obtenerMediana(upperHistogram);
        }
        
        //Una vez hallado los limites de plateau se procede a hallar los histogramas acotados
        int [] lowerClippedHistogram = acotarHistograma(lowerHistogram, lowerPlateauLimit);
        
        int [] upperClippedHistogram = acotarHistograma(upperHistogram, upperPlateauLimit);
        
        //Usando los histogramas acotados procedemos a hallar sus respectivas funciones de ecualizacion
        Double [] funcionEcualizacionLower = obtenerFuncionDeEcualizacion(lowerClippedHistogram, 0.0, xt.intValue());
        
        Double [] funcionEcualizacionUpper = obtenerFuncionDeEcualizacion(upperClippedHistogram, xt.intValue() + 1, 255.0);
        
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
	
	private static double obtenerMediana(int[] histograma){
        
        //Valor de la mediana
        double mediana = 0.0;
        
        //Vector auxiliar que nos ayudara a hallar la mediana
        int[] vectorAux = new int[histograma.length];
        
        //Copiamos en vectorAux el histograma que se nos pasa como parametro
        System.arraycopy(histograma, 0, vectorAux, 0, vectorAux.length);
        
        //Ordenamos el vector auxiliar
        Quicksort(vectorAux, 0, vectorAux.length - 1);
        
        if(vectorAux.length % 2 == 0){
            //En caso de que el tamanho del array sea par
            int medio = vectorAux.length /2;
            mediana = (vectorAux[medio] + vectorAux[medio-1])/2.0;
        }else{
            //En caso de que el tamanho del array sea impar
            int medio = vectorAux.length /2;
            mediana = vectorAux[medio + 1];
        }
        
        return mediana;
    }
	
	private static int[] acotarHistograma (int [] histograma, double plateauLimit){
        
        /*Histograma que poseera los valores acotados del histograma que se pasa por parametro y 
         *cuyo limite es el plateauLimit que tambien se pasa por parametro*/
        int[] histogramaAcotado = new int[histograma.length];
        
        //Cargamos el histograma acotado con los valores habiles del histograma original
        for(int i = 0; i < histograma.length; i++){
            if(histograma[i] <= plateauLimit){
                /*Si el valor es menor o igual al limite impuesto se carga dicho valor en 
                 *el histograma acotado*/
                histogramaAcotado[i] = histograma[i];
            }else{
                //Si supera el limite, se setea el limite de plateau en el histograma acotado
                histogramaAcotado[i] = (Double.valueOf(plateauLimit)).intValue();
            }
        }
        
        return histogramaAcotado;
    }
	
	private static void Quicksort(int[] vector, int first, int last){
        
        int i=first, j=last;
        int pivote=vector[(first + last) / 2];
        int auxiliar;
        do{
            while(vector[i] < pivote) i++;      
            while(vector[j] > pivote) j--;
            if (i <= j){
                auxiliar=vector[j];
                vector[j]=vector[i];
                vector[i]=auxiliar;
                i++;
                j--;
            }
        }while (i <= j);
 
        if(first < j){
            Quicksort(vector, first, j);
        }

        if(last > i){
            Quicksort(vector, i, last);
        }
    }
}