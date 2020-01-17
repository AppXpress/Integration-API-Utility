package infor.api.integration;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import infor.api.resources.IntegrationQueryResult;

public class InforAPI {
	static final String CONTEXT_TYPE_JSON = "application/json";
	static final String CONTEXT_TYPE_XML = "application/xml";
	
	static String[] expectedArgs = {"user", "accessKey", "secret", "url", "datakey", "method"};
	
	private static InforAPIDefinition apiDef;
	public InforAPI(InforAPIDefinition inforApiDef) {
		if( InforAPI.apiDef != null ) {
			System.err.println("Must define apiDef from config file");
		}
	}
	
	/*
	 * 	Bind apiDefinition, which is sourced from config.properties, to this static class which
	 * 	executes the Infor API Requests
	 */
	public static void setApiDefinition(InforAPIDefinition ad) {
		InforAPI.apiDef = ad;
	}
	
	/*
	 * 	Execute Outbox poll request on Infor Platform
	 * 
	 * 	@Return	Return query result Java object representation of poll server response
	 */
	public static IntegrationQueryResult executeOutboxListReq() {
		String uri = InforAPI.apiDef.getHost() + "/rest/3.1/integration/outbox/list";
		
		String outboxListStringResponse = InforAPI.executeRequest(uri, "GET", CONTEXT_TYPE_XML, null);
		IntegrationQueryResult queryResult = bindOutboxListXmlToObject(outboxListStringResponse);
		//printXmlObject(queryResult);
		return queryResult;
	}
	
	/*
	 * 	Fetch a message using its messageId from Integration API outbox
	 * 	@Param	docId	messageUID
	 * 	@Return	string xml response from the server
	 */
	public static String fetchIntegrationDocument(Integer docId) {
		String uri = InforAPI.apiDef.getHost() + "/rest/3.1/integration/outbox/fetch/" + docId;
		
		String fetchDocXmlString = InforAPI.executeRequest(uri, "GET", CONTEXT_TYPE_XML, null);
		return fetchDocXmlString;
	}
	
	/*
	 * 	Delete a message using its messageId from Integration API outbox
	 * 	@Param	docId	messageUID
	 * 	@Return Based on response xml string, return either 202 for accepted or 0 for failure
	 */
	public static int deleteIntegrationDocument(Integer docId) {
		String uri = InforAPI.apiDef.getHost() + "/rest/3.1/integration/outbox/delete/" + docId;
		
		String fetchDocXmlString = InforAPI.executeRequest(uri, "POST", CONTEXT_TYPE_XML, null);
		if(fetchDocXmlString.length() > 0 ) {
			return 202;
		}
		return 0;
	}
	
	/*
	 * 	Upload a document to the Integration API
	 * 
	 * 	@Param	rawXML		raw xml string
	 * 	@Param	docType		DocType as defined in Adapter Profile associated with Platform Integration
	 * 	@Return	Return raw xml representation string of server response 
	 */
	public static String uploadIntegrationDocument(String rawXML, String docType) {
		String uri = InforAPI.apiDef.getHost() + "/rest/3.1/integration/inbound/upload?docType='" + 
				docType + "'";		
		String fetchDocXmlString = InforAPI.executeRequest(uri, "POST", CONTEXT_TYPE_XML, rawXML);
		return fetchDocXmlString;
	}
	
	/*
	 * 	Fetch message status of document that was uploaded to Integration Inbox
	 * 	@Param	messageId	messageUID returned from Integration API uploading
	 * 	@Return	Return string server response 	
	 */
	public static String fetchMessageStatus(Integer messageId) {
		String uri = InforAPI.apiDef.getHost() + "/rest/3.1/integration/inbound/status/" + messageId;
		
		String fetchDocXmlString = InforAPI.executeRequest(uri, "GET", CONTEXT_TYPE_JSON, null);
		return fetchDocXmlString;
	}
	
	/*
	 * 	Execute Infor API request using HMAC authorization 
	 * 
	 * 	@Param	uri				Infor API request uri
	 * 	@Param	method			GET or POST API method
	 * 	@Param 	contextType		Context type of request's response - either JSON or XML
	 * 	@Param	payload			Payload of request, if applicable
	 * 
	 * 	@Return	Return string representation of Infor platform server response
	 */
	private static String executeRequest(String uri, String method, String contextType,
			String payload) {
		String secretKey = InforAPI.apiDef.getSecret();
		String user = InforAPI.apiDef.getUser();
		String accessKey = InforAPI.apiDef.getAccessKey();
		String xDapiDate = computeXDapiDate();
		String signature = createSignature(uri, secretKey, method, xDapiDate, payload);
		
		String hmacAuthorization = createHmacAuthorization(user,accessKey,signature);
		try {
			URL url = new URL(uri);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(method);
			conn.setRequestProperty("Authorization", hmacAuthorization);
				//System.out.println("Hmac Auth " + hmacAuthorization);
			conn.setRequestProperty("datakey", InforAPI.apiDef.getDatakey() );
			conn.setRequestProperty("x-dapi-date", xDapiDate);
			conn.setRequestProperty("Content-type", contextType);
			if(method.equals("POST") && payload != null) {
				conn.setDoOutput(true);
				byte[] postData = payload.getBytes("UTF-8");
				try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
	                wr.write(postData);
	            }
			}
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())), 10);
			System.out.println("Response Code => " + conn.getResponseCode() + " for " + uri);
			StringBuilder response = new StringBuilder(); 
            String responseSingle = null; 
            while ((responseSingle = br.readLine()) != null) {
                response.append(responseSingle);
            }

			conn.disconnect();
			return response.toString();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(ConnectException e) {
			System.err.println("Cannot find host of " + uri);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
	/*
	 * 	Create Integration API Authorization header value
	 * 
	 * 	@Param	userId		username
	 * 	@Param	accessKey	HMAC user defined access key
	 * 	@Param	signingBase	Signing base hash as calculated
	 * 
	 * 	@Return	Return value of Authorization header to send HMAC API request
	 */
	private static String createHmacAuthorization(String userId, String accessKey, String signingBase) {
		return "HMAC_1 " + accessKey + ":" + signingBase + ":" + userId;
	}
	
	/*	
	 * 	Create Signing Signature to Authenticate API Request using HMAC Authorization
	 * 	See https://developer.infornexus.com/api/api-overview/hmac-authentication for more info
	 * 
	 *  @Param	url			uri of API Request
	 *  @Param	secretKey	secretKey of hmac user making HMAC API request
	 *  @Param	method		Either GET or POST
	 *  @Param	xDapiDate	x-dapi-date request head set based on current time
	 *  @Param	data		payload if requet is a POST
	 *  
	 *  @Return	Return signature to create HMAC Authorization Hash String
	 */
	private static String createSignature(String url, String secretKey,String method, String xDapiDate, String data) {
		HashMap<String,String> requestHeaders = new HashMap<String,String>();
		requestHeaders.put("method", method);
		String path = url.replace("https://", "");
		path = path.substring(path.indexOf("/"));
		requestHeaders.put("pathInfo", path);
		requestHeaders.put("date", xDapiDate);
		String signingBase = canonalizeHeaders(requestHeaders);
		if(method.toUpperCase().equals("POST") && data != null) {
			signingBase += data.toLowerCase();
		}
			//System.out.println("Signing Base " + signingBase );
		String secretKeyArg = secretKey;
	
		try {
			Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			SecretKeySpec secret_key = new SecretKeySpec(secretKeyArg.getBytes("UTF-8"), "HmacSHA256");
		    sha256_HMAC.init(secret_key);
		    
		    //Base64 library used from org.apache.commons.codec.binary.Base64
		    String hash = Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(signingBase.getBytes("UTF-8")));
		    	//System.out.println("Signature is " + hash);
		    return hash;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//IF POST, add raw string
		return null;
	}
	
	/*
	 * 	CanonalizeHeaders using requestHeaders for HMAC Authorization API request - see developer.infornexus.com
	 * 	for more information
	 * 
	 * 	@Param	requestHeaders	set of requestHeaders to be sorted and built into signing base	
	 * 	@Return	return value to build signingBase for HMAC Authorization request
	 */
	private static String canonalizeHeaders(HashMap<String,String> requestHeaders) {
		ArrayList<String> sortedKeys = new ArrayList<String>(requestHeaders.keySet());      
		Collections.sort(sortedKeys);
		String headerStr = "";
		for(String header : sortedKeys) {
			headerStr += requestHeaders.get(header).toLowerCase();
		}
		return headerStr;
	}
	
	/*
	 * 	Computer x-dapi-date header for HMAC API Request
	 * 
	 * 	@Return value for HMAC API Request x-dapi-date header
	 */
	private static String computeXDapiDate() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		return df.format(new Date());
	}
	/*
	public static HashMap<String,String> parseArgs(String args[]) {
		HashMap<String,String> argMap = new HashMap<String,String>();
		String key;
		for(int i = 0; i < args.length - 1 ; i++) {
			if(i % 2 == 0 ) {
				key = args[i].replaceAll("-", "");
				argMap.put(key, args[i+1]);
			}
		}
		validateProgramArgs(argMap);
		return argMap;
	}
	
	private static void validateProgramArgs(HashMap<String,String> programArgs) {
		for(int i =0; i < expectedArgs.length; i++) {
			if(! programArgs.containsKey(expectedArgs[i])) {
				System.out.println("Missing arg " + expectedArgs[i]);
				System.exit(-1);
			}
		}
	}*/
	
	/*
	 * 	Bind servers XML response to IntegrationQueryResult object using JAXB UnMarshaller
	 * 
	 * 	@Param	xmlResponse		xml document representation of Outbox/poll API response
	 * 
	 * 	@Return	Object representation of Outbox/poll API response
	 */
	private static IntegrationQueryResult bindOutboxListXmlToObject(String xmlResponse) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(IntegrationQueryResult.class);              
		 
		    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		 
		    IntegrationQueryResult queryResult = (IntegrationQueryResult) jaxbUnmarshaller.unmarshal(new StringReader(xmlResponse));
		    
		    System.out.println(queryResult);
		    return queryResult;
		}
		catch (JAXBException e) {
		    e.printStackTrace();
		}
		return null;
	}
	
	/*
	 *	Print XML JAXB Object
	 *
	 * 	@Param	xmlObject	JAXB object representation servers XML response 		
	 */
	private static void printXmlObject(Object xmlObject) {
        try {
        	//Create JAXB Context
            JAXBContext jaxbContext = JAXBContext.newInstance(xmlObject.getClass() );//Employee.class);
            //Create Marshaller
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            //Required formatting??
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			//Print XML String to Console
	        StringWriter sw = new StringWriter();
	        //Write XML to StringWriter
	        jaxbMarshaller.marshal(xmlObject, sw);
	        
	        System.out.println(xmlObject); 
            //Verify XML Content
            String xmlContent = sw.toString();
            System.out.println( xmlContent );
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
