import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import infor.api.integration.InforAPI;
import infor.api.integration.InforAPIDefinition;
import infor.api.integration.IntegrationAPIConnect;
import infor.api.resources.IntegrationStatusResponse;
import infor.api.resources.IntegrationUploadResponse;

public class IntegrationApiUploader {
	static final int threadNum = 5;
	static final int messageUploaderFetchStatusInterval = 4000;
	static final String uploaderPropertyFileName = "uploader-config.properties";
	static final String docToUploadPropKey = "docToUpload";
	static final String docUploadNumber = "docUploadNumber";
	static final String docType = "docType";
	static final String[] requiredProperties = { docType };
	//Optional Property - if set, attempt to upload a folder of files to platform
	static final String uploadDocumentFolderKey = "uploadFolderPath"; 
	
	public static void main(String args[]) {
		System.out.println("Program Start...");
		Properties uploaderProps = Common.loadPropertyFile(uploaderPropertyFileName);
		validateProperties(uploaderProps);
		Properties apiProps = Common.loadApiDefPropertyFile();
		InforAPIDefinition inforApiDef = new InforAPIDefinition(apiProps);
		InforAPI.setApiDefinition(inforApiDef);

		//	Two Modes
		//	Either upload files within a folder or take one file l and upload that X times
		String uploadAFolderPath = uploaderProps.getProperty(uploadDocumentFolderKey);
		List<Document> docXMLsToUpload;
		if(uploadAFolderPath != null) {
			docXMLsToUpload = loadDocumentsToUploadFromFolder(uploadAFolderPath);
		} else {
			validateGenericDocUploadProperties(uploaderProps);
			docXMLsToUpload = buildDocsToUploadFromGenericOrderXml(uploaderProps);
		}
		Integer maxConcurrentSessions = inforApiDef.getMaxConcurrentSessions();
		
		uploadXMLToIntegrationAPI(uploaderProps, docXMLsToUpload, maxConcurrentSessions);
	}
	
	/*
	 * 	Upload a list of document to Infor Integration API
	 * 	Delegate actually thread pool management and uploading to Common Class
	 * 	@Param	uploaderProps		Properties defined by uploader property file
	 * 	@Param	xmlDocsToUpload		List of Documents to upload to platform
	 */
	public static void uploadXMLToIntegrationAPI(Properties uploaderProps, List<Document> xmlDocsToUpload, Integer sessions) {
		String docTypeForUpload = uploaderProps.getProperty(docType);
		ExecuteFunctionalInterface executeFunction = (Object [] objs) -> {
			Document xmlDoc = (Document) objs[0];
			String xmlStr = getStringFromDoc(xmlDoc);
			ArrayList<IntegrationUploadResponse> uploadResponses = IntegrationAPIConnect.uploadDocument(xmlStr, docTypeForUpload);
			IntegrationUploadResponse res1 = uploadResponses.get(0);
			//Poll for message status until either FAILED OR COMPLETED
			pollForMessageStatus(res1);
			System.out.println(res1);
			return res1.toString();
		};
		try {
			Common.executeCallableRequestsConcurrently(executeFunction, xmlDocsToUpload, sessions);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/*
	 * 	
	 */
	public static List<Document> loadDocumentsToUploadFromFolder(String folderPath) {
		List<Document> myDocuments = new ArrayList<Document>();
		File folderToUpload = new File(folderPath);
		if(! (folderToUpload.exists() && folderToUpload.isDirectory())) {
			System.err.println("Input folder " + folderPath + " cannot be found");
			System.exit(-1);
		}
		Document d;
		for(File f : folderToUpload.listFiles()) {
			d = convertXMLFileToXMLDocument(f.getAbsolutePath());
			myDocuments.add(d);
		}
		
		return myDocuments;
	}
	
	public static void pollForMessageStatus(IntegrationUploadResponse servResponse) {
		try {
			boolean stopPolling = false;
			while(! stopPolling) {
				IntegrationStatusResponse status = IntegrationAPIConnect.fetchDocumentStatus( servResponse.getMessageId() );
				stopPolling = status.getState().equalsIgnoreCase(IntegrationStatusResponse.COMPLETE_STATUS) ||
							  status.getState().equalsIgnoreCase(IntegrationStatusResponse.FAILED_STATUS);
				Thread.sleep(messageUploaderFetchStatusInterval);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * 	Build variation of generic Order XML to Infor Platform based on property file
	 * 	Each generic Order will have its poNumber incremented by 1 so each is considered unique
	 * 	@Param props	property defined by Uploader property file
	 * 	@Return	Return List of XML Documents
	 */
	public static List<Document> buildDocsToUploadFromGenericOrderXml(Properties props) {
		Document genericOrderXmlDoc = convertXMLFileToXMLDocument( props.getProperty(docToUploadPropKey) );
		Node poNode = findPoNumberNode(genericOrderXmlDoc);
		System.out.println( poNode.getTextContent() );
		
		int uploadDocNum = Integer.parseInt(props.getProperty(docUploadNumber));
		List<Document> docXMLsToUpload = new ArrayList<Document>();
		for(int i = 0; i < uploadDocNum; i++) {
			Document buildXml = copyAndIncrementOrderXML(genericOrderXmlDoc, i);
			docXMLsToUpload.add(buildXml);
		}
		return docXMLsToUpload;
	}
	
	/*
	 * 	Transpile Document into string representation
	 * 	@Param	doc		Xml Document
	 * 	@Return	Return string representation of document
	 */
	public static String getStringFromDoc(org.w3c.dom.Document doc)    {
	    DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
	    LSSerializer lsSerializer = domImplementation.createLSSerializer();
	    return lsSerializer.writeToString(doc);
	}
	
	/*
	 * 	Clone generic Order Document and increment the order poNumber to make that field unique for upload
	 * 	@Param	genericOrderXmlDoc	Infor XML Order Document
	 * 	@Param	add					increment poNumber by this value
	 * 	@Return	Return cloned and in theory unique Order XML Document
	 */
	private static Document copyAndIncrementOrderXML(Document genericOrderXmlDoc, int add) {		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document copiedDocument;
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			copiedDocument = (Document) genericOrderXmlDoc.cloneNode(true);
	        Node poNode = findPoNumberNode(copiedDocument);
	        String textContent = poNode.getTextContent() + "-XXX" + add;
	        System.out.println("Po Number would be - " + textContent);
	        poNode.setTextContent(textContent);
	        return copiedDocument;
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        return null;
	}
	
	/*
	 * 	Find poNumber from Document Order Representation
	 * 	@Param	inforOrder	Document of InforNexus Order
	 * 	@Return	Return poNumber Node
	 */
	private static Node findPoNumberNode(Document inforOrder) {
		NodeList myNodes = inforOrder.getElementsByTagName("poNumber");
		return myNodes.item(0);
	}
	
	/*
	 * 	Build an XML Document from an XML File via its FilePath
	 * 	@Param	filePath	xml file path
	 * 	@Return	Return Document object representation of loaded xml object
	 */
	private static Document convertXMLFileToXMLDocument(String filePath) 
    {
        //Parser that produces DOM object trees from XML content
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         
        //API to obtain DOM Document instance
        DocumentBuilder builder = null;
        try {
            //Create DocumentBuilder with default configuration
            builder = factory.newDocumentBuilder();
             
            //Parse the content to Document object
            Document doc = builder.parse(new File(filePath));
            return doc;
        }  catch (FileNotFoundException e1) {
			System.out.println("Cannot find file " + filePath + " to upload");
			System.exit(-1);
		} 
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
	
	/*
	 * 	For uploading a generic document X times, you need to define docToUpload and docNumber
	 * 	properties. If either is missing or malformed, exit the program	
	 */
	private static void validateGenericDocUploadProperties(Properties props) {
		if( props.getProperty(docToUploadPropKey) == null) {
			System.out.println("Must include property " + docToUploadPropKey + " to indicate document to generic upload");
			System.exit(-1);
		}
		if( props.getProperty(docUploadNumber) == null ) {
			System.out.println("Must include property " + docUploadNumber + " to indicate number of documents to upload");
			System.exit(-1);
		} else {
			try {
				Integer i = Integer.parseInt(props.getProperty(docUploadNumber));
			}catch(Exception e) {
				System.out.println("Property " + docUploadNumber + " must be a valid integer");
				System.exit(-1);
			}
		}
	}
	
	/*
	 * 	Validate Properties based on requiredProperties array - exit program if missing a required
	 * 	tool property
	 */
	private static void validateProperties(Properties props) {
		for(int i = 0; i < requiredProperties.length; i++) {
			String propName = requiredProperties[i];
			if( props.get(propName) == null ) {
				System.err.println("Uploader Property file " + uploaderPropertyFileName + " is missing required property " + propName);
				System.exit(-1);
			}
		}
	}
}
