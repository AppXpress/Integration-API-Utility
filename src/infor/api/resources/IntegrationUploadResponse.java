package infor.api.resources;

public class IntegrationUploadResponse {
	public IntegrationUploadResponse() {}
	private Integer messageId;
	
	public Integer getMessageId() { return this.messageId; }
	
	public String toString() {
		return "Message Id is " + this.messageId;
	}
}
