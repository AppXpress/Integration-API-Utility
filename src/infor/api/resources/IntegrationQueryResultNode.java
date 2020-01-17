package infor.api.resources;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "result")
@XmlAccessorType(XmlAccessType.FIELD)
public class IntegrationQueryResultNode implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer messageUid;
	private String documentType;
	private String messagePriority;
	private Integer actionUid;
	
	public IntegrationQueryResultNode() {
		super();
	}
	
	public Integer getActionId() {
		if( this.actionUid != null) {
			return this.actionUid;
		}
		System.err.print("No Action ID is not defined");
		return -1;
	}
	
	public String getDocType() {
		return this.documentType;
	}
	
	public Integer getMessageUid() {
		return this.messageUid;
	}
	
	@Override
    public String toString() {
        return "result [actionUid=" + this.actionUid + "]";
    }
	
}