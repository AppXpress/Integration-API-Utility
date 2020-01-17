package infor.api.integration;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import infor.api.resources.IntegrationQueryResult;
import infor.api.resources.IntegrationQueryResultNode;
import infor.api.resources.IntegrationStatusResponse;
import infor.api.resources.IntegrationUploadResponse;

/*
 * 	Connect the tool access points - IntegrationAPIUploader and Downloader - to the InforAPI class
 * 	to execute the server requests. Functions here will process the results, in some cases creating
 * 	GSON object, Java objects, or  XML Document Objects to represent the server's responses
 */
public class IntegrationAPIConnect {
	private static final Gson gson = new Gson();
	
	/*
	 * 	Execute a poll of the Integration API outbox
	 * 	
	 * 	@Return	Return a list of IntegrationQueryResultNodes from the servers response to pollOutbox API call
	 */
	public static List<IntegrationQueryResultNode> pollOutbox() {
		IntegrationQueryResult outboxResult =  InforAPI.executeOutboxListReq();
		return outboxResult.getResults();
	}
	
	/*
	 * 	Fetch a document via its messageId from the server's outbox via Integration API
	 * 	Print the fetched XML representation of said document into a file
	 */
	public static String fetchDocument(IntegrationQueryResultNode node) {
		String rawXmlDoc = InforAPI.fetchIntegrationDocument(node.getActionId());
		return rawXmlDoc;
	}
	
	/*
	 * 	Using a node's messageId, delete document from the outbox
	 */
	public static void deleteDocument(IntegrationQueryResultNode node) {
		int responseCode = InforAPI.deleteIntegrationDocument(node.getActionId());
		if(responseCode == 202) {
			System.out.println("Deleted message " + node.getActionId() + " from outbox");
		}
	}
	
	/*
	 * 	Upload a raw xml string to the Infor platform using the Inbox Integration API
	 * 
	 * 	@Param	rawXml	string representation of raw xml structure
	 * 	@Param	docType	docType as defined by property file; must correspond with docType
	 * 					as defined in organizational profile adapter
	 */
	public static ArrayList<IntegrationUploadResponse> uploadDocument(String rawXml, String docType) {
		String rawResponseJson = InforAPI.uploadIntegrationDocument(rawXml, docType);
		JsonArray responseArray = new JsonParser().parse(rawResponseJson).getAsJsonArray();
		ArrayList<IntegrationUploadResponse> messageList = new ArrayList<IntegrationUploadResponse>();
		for(JsonElement jEl : responseArray) {
			messageList.add( gson.fromJson(jEl, IntegrationUploadResponse.class));
		}
		for(IntegrationUploadResponse toPrint : messageList) {
			System.out.println( toPrint );
		}
		return messageList;
	}
	
	/*
	 * 	Fetch document status from Inbox Integration API
	 * 	
	 * 	@Param	messageId	Id of message to fetch status of
	 * 	@Return	Return server response as gson representational object
	 */
	public static IntegrationStatusResponse fetchDocumentStatus(Integer messageId) {
		String rawJsonResponse = InforAPI.fetchMessageStatus(messageId);
		IntegrationStatusResponse response = gson.fromJson(rawJsonResponse, IntegrationStatusResponse.class);
		System.out.println("STATE OF " + messageId + " IS " + response.getState());
		return response;
	}
}
