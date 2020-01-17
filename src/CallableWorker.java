import java.util.concurrent.Callable;

public class CallableWorker implements Callable{
	
	protected Object [] params;
	Object [] myObjects = new Object[0];
	protected ExecuteFunctionalInterface boundCallFn = (myObjects) -> { 
		System.err.println("Must bind function to apply callable");
		return "--null";
	};
	
	public CallableWorker() {}
	
	public String call() throws InterruptedException {
		//System.err.println("Must define CallableWorkers call method");
		//return "-null-";
		return boundCallFn.exec(this.params);
	}
	
	public void setFunctionToBind(ExecuteFunctionalInterface fn) {
		this.boundCallFn = fn;
	}
	
	public void defineInput(Object...objects) {
		this.params = objects;
	}
}
