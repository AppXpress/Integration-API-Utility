package infor.api.resources;

/*
 * 	Java object representation of JSON resposne of /inbound/status/{fetch-id} REST call
 */
public class IntegrationStatusResponse {
	public static final String COMPLETE_STATUS = "Completed";
	public static final String FAILED_STATUS = "Failed";
	public IntegrationStatusResponse() {}
	private Integer messageId;
	private String state = "-unknown-";
	private String stateActionType;
	
	public String getState() {
		return this.state;
	}
	public String getStateActionType() {
		return this.stateActionType;
	}
	
	public boolean isMessageCompleted() {
		return this.state == COMPLETE_STATUS;
	}
	@Override
	public String toString() {
		return "Message state is " + this.state + ", actionType is " + this.stateActionType;
	}
}
