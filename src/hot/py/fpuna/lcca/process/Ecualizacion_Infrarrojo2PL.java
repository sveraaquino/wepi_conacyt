package py.fpuna.lcca.process;

import ij.process.ImageProcessor;
import java.util.ArrayList;
import java.util.List;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;

public class Ecualizacion_Infrarrojo2PL implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		return DOES_8G + DOES_STACKS + SUPPORTS_MASKING + NO_CHANGES;
	}

	public ImageProcessor wepi_run(ImageProcessor imageProcessor) {
		ImageProcessor ip = imageProcessor.duplicate();
		int altura = ip.getHeight();
        int ancho = ip.getWidth();
        
        //Histograma total de la imagen
        int [] histogramaTotal = ip.getHistogram();
        
        //Numero total de pixeles en la imagen
        double totalPixeles = altura * ancho;
        
        /*Obtenemos la cantidad de posiciones del histograma que son distintas de cero*/
        int l = 0;
        for(int i = 0; i < histogramaTotal.length; i++){
            if(histogramaTotal[i] != 0){
                l++;
            }
        }
        
        /* Definimos la lista 'polar'como la lista de maximos locales encontrados en el histograma 
         * donde 'p' es el tamanho de dicha lista*/
        List<Integer> polar =  new ArrayList<Integer>();
        
        /*Definimos el vector N que representa los elementos del histograma que 
         * poseen valores distintos de cero y cuya longitud es L*/
        int[] n = new int[l];
        int indice = -1;
        for(int i = 0; i < histogramaTotal.length; i++){
            if(histogramaTotal[i] != 0){
                indice ++;
                n[indice] = histogramaTotal[i];
            }
        }
        
        /*Definimos un vector W que representa la ventana usada para buscar los maximos locales, 
         * donde un maximo local se da cuando el elemento central de la ventana es mayor que sus vecinos:
            W_{(n+1)/2} >= max{W1,...,Wn}. Para este caso asumimos n = 5 como en el paper.
         */
        int [] w = new int[5];
        int posicionCentralVentana = (w.length + 1)/2;
        boolean esMaximoLocal;
        
        //Se procede a buscar los maximos locales
        for(int i = 0; i < (n.length - w.length) + 1 ;i++){
            for(int j = 0; j < w.length; j++){
                //Cargamos los valores en la ventana
                w[j] = n[i+j] ;
            }
            //Verificamos si el valor central de la ventana es el maximo en el vecindario
            esMaximoLocal = true;
            for(int j = 0; j < w.length; j++){
                if(j!= posicionCentralVentana && w[j] > w[posicionCentralVentana]){
                    esMaximoLocal = false;
                    break;
                }
            }
            if(esMaximoLocal){
                polar.add(w[posicionCentralVentana]);
            }
        }
        
        /*Hallamos el limite de plateau mas alto en base a los polares hallados siguindo la siguiente formula:
            p(k_{detail}) = Polar_{avg} = (Polar[1]+...+Polar[P])/P
        */
        double limitePlateauUp = 0.0;
        for(Integer valor:polar){
            limitePlateauUp += valor;
        }
        limitePlateauUp = limitePlateauUp/polar.size();
        
        /*El limite de plateau mas bajo puede ser hallado como:
            T_{DOWN} = min{N_{total}, T_{UP} * L}/M
          donde :
            * M es el numero total de grises original(Asumimos que es 255 para una imagen de escala de grises de 8bit).
            * N_{total}: es el numero total de pixeles dentro de la imagen.
            * L: es el total de posiciones dentro del histograma cuyo valor es distinto de cero.
        */
        double limitePlateauDown = (Math.min(totalPixeles, (limitePlateauUp * l)))/255;
        
        //Una vez hallado los limites de plateau se procede a hallar el histograma acotado
        int [] histogramaAcotado = acotarHistogramaMetodoInfrarrojo(histogramaTotal, limitePlateauUp, limitePlateauDown);
       
        //Se porcede a hallar el histograma acumulado respectivo para el histograma acotado
        int[] histogramaAcumulado = new int[histogramaAcotado.length];
        int valorAcumulado = 0;
        for(int i = 0; i < histogramaAcotado.length; i++){
            valorAcumulado += histogramaAcotado[i];
            histogramaAcumulado[i] = valorAcumulado;
        }
        
        /*Se reconstruyen los niveles de grises en base al histograma acumulado 
         *recientemente hallado usando la siguiente formula:
            D_{m}(k) = redondeo_hacia_abajo((M*F(k)/F(M)))
        */
        int [] dM = new int[histogramaAcumulado.length];
        for(int i = 0; i < histogramaAcumulado.length; i++){
            /*Ya que todos son enteros, siempre toma la parte entera 
             *emulando un redondeo hacia abajo*/
            dM[i] = (255 * histogramaAcumulado[i])/histogramaAcumulado[255];
        }
        
        //Ecualizamos el histograma con los grises reconstruidos a partir del acumulado del histograma acotado
        Double [] funcionEcualizacion = obtenerFuncionDeEcualizacion(dM, 0.0, 255.0);
        
        /*Se recorre la imagen y se reemplaza el valor de intensidad dada la siguiente relacion:
            Y = Y(i,j) = f_l(X_l) union f_u(X_u) donde:
            * f_l(X_l) = f_l(X(i,j)| para todo X(i,j) perteneciente a X_l.
            * f_u(X_u) = f_u(X(i,j)| para todo X(i,j) perteneciente a X_u.
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
	
	private static int[] acotarHistogramaMetodoInfrarrojo(int [] histograma, double pl1, double pl2){
        /*Histograma que poseera los valores acotados del histograma que se pasa por parametro y 
         *cuyo limites son los plateus limits pl1 y pl2 pasados como parametros.*/
        int[] histogramaAcotado = new int[histograma.length];
        
        //Cargamos el histograma acotado con los valores habiles del histograma original
        for(int i = 0; i < histograma.length; i++){
            if(histograma[i] >= pl2){
                //Si el valor es mayor o igual a limite superior
                histogramaAcotado[i] = Double.valueOf(pl2).intValue();
            }else if(histograma[i] >= pl1 && histograma[i] < pl2){
                //Si el valor es mayor o igual al limite inferior y menor al limite superior
                histogramaAcotado[i] = histograma[i];
            }else if(histograma[i] > 0 && histograma[i] < pl1){
                //Si el valor es mayor a cero y menor al limite inferior
                histogramaAcotado[i] = Double.valueOf(pl1).intValue();
            }else if(histograma[i] == 0){
                //Si el valor es igual a cero
                histogramaAcotado[i] = 0;
            }
        }
        
        return histogramaAcotado;
    }
}