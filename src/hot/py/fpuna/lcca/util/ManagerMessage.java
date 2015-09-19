package py.fpuna.lcca.util;

import java.util.ResourceBundle;

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.context.FacesContext;

/**
 * @author santiago
 *Clase Padre, el cual se encarga de manejar los mensajes en el Contexto
 *de cada usuario, ya sea mensajes de informacion o de error
 */
public class ManagerMessage {

	protected FacesContext getCurrentContext() {
		return FacesContext.getCurrentInstance();
	}

	public void addInfo(String msg) {
		addMessage(msg, FacesMessage.SEVERITY_INFO);
	}

	public void addError(String msg) {
		addMessage(msg, FacesMessage.SEVERITY_ERROR);
	}

	private void addMessage(String msg, Severity severity) {
		FacesMessage message = new FacesMessage(msg);
		message.setSeverity(severity);
		FacesContext ctx = getCurrentContext();
		ctx.addMessage(null, message);
	}

	public String getMessage(String key) {
		return (String) getExpression("tag['" + key + "']");
	}

	
	private Object getExpression(String expression) {
		FacesContext ctx = getCurrentContext();
		ExpressionFactory factory = ctx.getApplication().getExpressionFactory();
		ValueExpression ex = factory.createValueExpression(ctx.getELContext(),"#{" + expression + "}", Object.class);
		return ex.getValue(ctx.getELContext());

	}

}
