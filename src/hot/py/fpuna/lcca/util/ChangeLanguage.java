package py.fpuna.lcca.util;

import java.util.Locale;
import javax.faces.context.FacesContext;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * @author santiago
 *
 *Clase que se encarga de cambiar los propierties para el cambio de 
 *idioma del sistema
 *
 */
@Name("changeLanguage")
@Scope(ScopeType.CONVERSATION)
public class ChangeLanguage {

	private String language;
	
	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}
	
	public void change(){
		if(language.equals("1"))
			this.changeToSpanish();
		else if(language.equals("2"))
			this.changeToEnglish();
		else if(language.equals("3"))
			this.changeToPortugues();
	}

	public void changeToSpanish() {
		FacesContext context = FacesContext.getCurrentInstance();
		context.getViewRoot().setLocale(new Locale("es","ES"));		
		
	}

	public void changeToEnglish() {

		FacesContext context = FacesContext.getCurrentInstance();
		context.getViewRoot().setLocale(new Locale("en","US"));
		

	}

	public void changeToPortugues() {
		FacesContext context = FacesContext.getCurrentInstance();
		context.getViewRoot().setLocale(new Locale("pt","BR"));
	}

}
