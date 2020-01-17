import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/*
 * 	Common static functions used by the integrationApiUploader and the integrationApiDownloader
 */
public class Common {
	static final String apiDefPropertyFileName = "config.properties";
	
	static Properties loadApiDefPropertyFile() {
		return loadPropertyFile(apiDefPropertyFileName);
	}
	/*
	 *	Load property file
	 *
	 *	@Param	configPropertyFileName	name of property file
	 *	@Return	Return properties sourced from relative file	
	 */
	public static Properties loadPropertyFile(String configPropertyFileName) {
		File configFile = new File(configPropertyFileName);
		Properties props = new Properties();
		try {
		    FileReader reader = new FileReader(configFile);
		    props.load(reader);		    
		    
		    reader.close();
		} catch (FileNotFoundException ex) {
		    // file does not exist
			System.out.print("Required property file " + configPropertyFileName + " is missing; see instructions for tool setup");
			System.exit(-1);
		} catch (IOException ex) {
		    // I/O error
			System.out.print(ex);
		}
		return props;
	}
	/*
	 *	Execute a set of functions in multi-threads concurrently
	 *	Loop through the list and bind each list item to the passed in function
	 *	Then execute each function in a number of threads concurrently - keep looping through the set
	 *	until the function workerFunction has been executed N times where N is the size of the iterList
	 *
	 * 	@Param	workerFunction	Annonymous function to be executed in the Executor thread pool
	 * 	@Param	iterList		Iterate through the list, bind each item to workerFunction before they 
	 * 							are executed in threadPool
	 */
	public static void executeCallableRequestsConcurrently(ExecuteFunctionalInterface workerFunction, List<?> iterList, int threadNum) throws InterruptedException, ExecutionException {
		ExecutorService pool = Executors.newFixedThreadPool(threadNum);
		ArrayList<CallableWorker> callables;
		int startIndx = 0;
		int iterX = 0;
		while( (callables = getNextPoolForExec(startIndx, threadNum, iterList, workerFunction) ).size() != 0 ) {
			ArrayList<Future> theseFutures = new ArrayList<Future>();
			for(int k = 0; k < callables.size(); k++) {
				Future f = pool.submit( callables.get(k) );
				theseFutures.add(f);
			}
			for(int k = 0; k < theseFutures.size(); k++) {
				String s = (String) theseFutures.get(k).get();
					//System.out.println(s);
			}
			startIndx += threadNum;
			iterX++;
		}
		
		pool.shutdown();
	}
	
	/*
	 * 	Same as above - but default threadNum to 5
	 */
	public static void executeCallableRequestsConcurrently(ExecuteFunctionalInterface workerFunction, List<?> iterList) throws InterruptedException, ExecutionException {
		executeCallableRequestsConcurrently(workerFunction, iterList, 5);
	}
	
	
	/*
	 * 	Iterate through the list from startIndx to stardInx + threadNum
	 * 	Bind each list item to the workerFunction and add it to a list of Callables
	 * 	
	 * 	@Param	startIndx		start index of iterList
	 * 	@Param	threadNum		Maximum amount of callables added to return callable arraylist
	 * 	@Param	workerFunction	Bind list item to function and add to callable arraylist
	 * 
	 * 	@Return	List of callable object that are to be returned and then executed in a thread pool concurrently
	 */
	private static ArrayList<CallableWorker> getNextPoolForExec(int startIndx, int threadNum, List<?> results, ExecuteFunctionalInterface workerFunction) {
		ArrayList<CallableWorker> callablePool = new ArrayList<CallableWorker>();
		int i = 0;
		while(i < threadNum) {
			if( startIndx < results.size() ) {
				Object thisNode = results.get(startIndx);
				CallableWorker worker1 = new CallableWorker();
				worker1.defineInput(thisNode);
				worker1.setFunctionToBind(workerFunction);
				callablePool.add(worker1);
			}
			i++;
			startIndx++;
		}
		return callablePool;
	}
	
	/*
	 * 	Pretty print XML string using indent
	 * 
	 * 	@Return	return a string which once printed will be human readable
	 */
	public static String prettyFormat(String input, int indent) {
	    try {
	        Source xmlInput = new StreamSource(new StringReader(input));
	        StringWriter stringWriter = new StringWriter();
	        StreamResult xmlOutput = new StreamResult(stringWriter);
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        transformerFactory.setAttribute("indent-number", indent);
	        Transformer transformer = transformerFactory.newTransformer(); 
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.transform(xmlInput, xmlOutput);
	        return xmlOutput.getWriter().toString();
	    } catch (Exception e) {
	        throw new RuntimeException(e); // simple exception handling, please review it
	    }
	}

	/*
	 * 	Default indent is 2 - run pretty function above
	 */
	public static String prettyFormat(String input) {
	    return prettyFormat(input, 2);
	}
	
}
