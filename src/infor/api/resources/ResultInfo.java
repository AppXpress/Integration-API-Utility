package infor.api.resources;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "resultInfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResultInfo implements Serializable{
	private Integer count;
	private Integer offset;
	private Integer firstRowNumber;
	private Integer estimatedTotalCount;
	private boolean hasMore;
	
	public ResultInfo() {
		super();
	}
	
	public Integer getCount() {
		return this.count;
	}
	
	@Override
    public String toString() {
        return "resultInfo [count=" + this.count + "]";
    }
}