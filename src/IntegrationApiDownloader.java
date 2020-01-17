import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import infor.api.integration.InforAPI;
import infor.api.integration.InforAPIDefinition;
import infor.api.integration.IntegrationAPIConnect;
import infor.api.resources.IntegrationQueryResultNode;

public class IntegrationApiDownloader {
	public static final String downloaderConfigFileName = "downloader-config.properties";
	public static final String doDeletePropKey = "deleteOnDownload";
	public static final String outboxPollPropKey = "outboxPollIntervalInSeconds";
	public static final String outputFolderKey = "outboxOutputFolder";
	public static final boolean doDeletePropDefaultVal = false;
	public static final int outboxPollIntervalDefault = 600;
	public static final String outputFolderDefault = ".";
	
	public static void main(String args[]) throws InterruptedException, ExecutionException {
		System.out.println("Program Start...");
		long start = System.nanoTime();
		Properties apiProps = Common.loadApiDefPropertyFile();
		InforAPIDefinition inforApiDef = new InforAPIDefinition(apiProps);
		InforAPI.setApiDefinition(inforApiDef);
		Properties downloaderProperties = definePropertiesFromPropFile();
				
		boolean deleteAfterFetch = Boolean.parseBoolean( downloaderProperties.getProperty(doDeletePropKey));
		Integer maxConcSessions = inforApiDef.getMaxConcurrentSessions();
		//If delete on Fetch, Poll
		if(deleteAfterFetch) {
			int milliSecondDelay = Integer.parseInt(downloaderProperties.getProperty(outboxPollPropKey)) * 1000;
			while(true) {
				runIntegrationApiDownloader(deleteAfterFetch, downloaderProperties.getProperty(outputFolderKey), maxConcSessions);
				Thread.sleep(milliSecondDelay);
			}
		} else {
			runIntegrationApiDownloader(deleteAfterFetch, downloaderProperties.getProperty(outputFolderKey), maxConcSessions);
		}
		
		long finish = System.nanoTime();
		double msTimeElapsed = (( finish - start ) / 1e6);
		System.out.println("Time elapsed " + msTimeElapsed);
	}
	
	public static void runIntegrationApiDownloader(boolean deleteAfterFetch, String outputFolderName, Integer threadNums) {
		List<?> results = IntegrationAPIConnect.pollOutbox();
		ExecuteFunctionalInterface fetchDocFn = (Object[] params) -> {
			IntegrationQueryResultNode boundNode = (IntegrationQueryResultNode) params[0];
			System.out.println("Fetch document -> " + boundNode.getActionId());
			String xmlResponse = IntegrationAPIConnect.fetchDocument(boundNode);
			printDocToFS(xmlResponse, boundNode, outputFolderName);
			if( deleteAfterFetch ) {
				IntegrationAPIConnect.deleteDocument( boundNode );
			}
			return "Returned " + boundNode.getMessageUid();
		};
		
		//Execute requests concurrently
		try {
			Common.executeCallableRequestsConcurrently(fetchDocFn, results, threadNums);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * 	Initialize properties from Download Property File
	 * 	For some properties, set to default value if not defined via property file
	 * 	@Return	Return properties to use in Downloader
	 */
	public static Properties definePropertiesFromPropFile() {
		Properties props = Common.loadPropertyFile(downloaderConfigFileName);
		if(props.getProperty(doDeletePropKey) == null) {
			props.setProperty(doDeletePropKey, Boolean.toString(doDeletePropDefaultVal));
		}
		String outboxPollVal = props.getProperty(outboxPollPropKey);
		if(outboxPollVal == null) {
			props.setProperty(outboxPollPropKey, Integer.toString(outboxPollIntervalDefault));
		} else {
			//Validate input property
			try {
				Integer isValid = Integer.parseInt(outboxPollVal);
			} catch(Exception e) {
				System.err.println("Property " + outboxPollPropKey + " has invalid value of " + outboxPollVal + "; must be an integer ");
				System.exit(-1);
			}
		}
		String outputFolderVal = props.getProperty(outputFolderKey);
		if(outputFolderVal == null) {
			props.setProperty(outputFolderKey, outputFolderDefault);
		} else {
			//Validate input property
				File f = new File(outputFolderVal);
				if ( ! (f.exists() && f.isDirectory())) {
					System.err.println("Output folder does not exist " + outputFolderVal);
					System.exit(-1);
				}
		}
		
		return props;
	}
	
	/*
	 * 	Print string to a file 
	 * 	Build filename from Integration Query Result Node
	 */
	public static void printDocToFS(String rawXmlStr, IntegrationQueryResultNode node, String folderPath) {
		try {
	    	String fileName = node.getDocType() + "-" + node.getActionId() + ".xml";
	    	System.out.println("Write document " + node.getActionId() + " to file -> " + fileName );
	    	String fullFilePath = folderPath + File.separator + fileName;
	    	BufferedWriter writer = new BufferedWriter(new FileWriter(fullFilePath));
	    	String prettyPrintXml = Common.prettyFormat(rawXmlStr);
			writer.write(prettyPrintXml);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}