package infor.api.resources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "QueryResult")
@XmlAccessorType(XmlAccessType.FIELD)
public class IntegrationQueryResult implements Serializable {
	private static final long serialVersionUID = 1L;
	private ResultInfo resultInfo;
	@XmlElement(name = "result")
	private List<IntegrationQueryResultNode> results = new ArrayList<IntegrationQueryResultNode>();
	
	public IntegrationQueryResult() {
		super();
	}
	
	public Integer getListSize() {
		if( this.resultInfo != null) {
			return this.resultInfo.getCount();
		}
		System.err.print("List is not defined");
		return -1;
	}
	
	public List<IntegrationQueryResultNode> getResults() {
		return this.results;
	}
	
	@Override
    public String toString() {
        return "QueryResult [resultInfo=" + this.resultInfo + "]";
    }
	
}