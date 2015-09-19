package py.fpuna.lcca.controller;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import py.fpuna.lcca.util.ManagerMessage;


@Name("messagesBean")
@Scope(ScopeType.EVENT)
public class MessagesBean extends ManagerMessage{

	public boolean isError() {  
        return !getCurrentContext().getMessageList().isEmpty();  
    }  
}
