package py.fpuna.lcca.util;

/**
 * 
 * @author Martin Poletti todos los metodos lanzan una unica excepcion por el
 *         momento, esta excepcion indica que se intento ejecutar un metodo en
 *         una imagen cuya instancia real(sea binario, gris o color) no tiene
 *         soportado ese petodo y hereda de la calse Imagen el metodo que
 *         simplemente lanza la excepcion que deberia ser manejada luego esta
 *         excepcion extiende de RuntimException para que sea obligatorio poner
 *         dentro de un try catch todos lo s metodos que muchas veces se utilzan
 *         dentro de otros metodos sabiendo ya que la imagen es del tipo
 *         correcto pero fuera de los metodos si debe de utilizarce el try catch
 *         para poder asegurar que se capte la excepcion
 */
public class MyException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */

	public MyException(String msg) {
		super(msg);
	}

	public MyException(String msg, Throwable ex) {
		super(msg,ex);
	}
	public MyException() {
		
	}
}
