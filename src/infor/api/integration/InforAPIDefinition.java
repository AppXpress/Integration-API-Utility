package infor.api.integration;
import java.util.Properties;
/*
 * 	Define authorization and connection points to the Infor API
 * 	Read from config.properties
 * 	Any User using this tool needs to define
 * 		Datakey		User's organizational software development key
 * 		AccessKey	HMAC Users defined accessKey
 * 		Secret		secret hash used to identify HMAC user attempting authorization over API
 * 		User		Username
 * 		Host		environment hostname ...ex : https://preprod.infornexus.com
 * 
 * 	If any of these properties are not defined, your API requests will not authenticate properly
 */
public class InforAPIDefinition {
	private String host;
	private String datakey;
	private String accessKey;
	private String user;
	private String secret;
	//Default Concurrent Sessions is 5
	private Integer maxConcurrentSessions = 5;
	
	public InforAPIDefinition(Properties propFile) {
		this.datakey = propFile.getProperty("datakey");
		this.accessKey = propFile.getProperty("accessKey");
		this.user = propFile.getProperty("user");
		this.secret = propFile.getProperty("secret");
		this.host = propFile.getProperty("host");
		if(this.datakey == null) {
			System.out.println("Datakey needs to be defined in config file");
		}
		if(this.accessKey == null) {
			System.out.println("AccessKey needs to be defined in config file");
		}
		if(this.user == null) {
			System.out.println("User needs to be defined in config file");
		}
		if(this.secret == null) {
			System.out.println("Secret needs to be defined in config file");
		}
		if(this.host == null) {
			System.out.println("Host needs to be defined in config file");
		}
		String s = propFile.getProperty("maxConcurrentSessions");
		if(s == null) {
			System.out.println("maxConcurrentSessions property needs to be defined");
		} else {
			try {
				this.maxConcurrentSessions = Integer.parseInt(s);
			}catch(Exception e) {
				System.out.println("maxConcurrentSessions property must be a valid integer");
			}
		}
	}
	
	public String getDatakey() {
		return this.datakey;
	}
	public String getAccessKey() {
		return this.accessKey;
	}
	public String getUser() {
		return this.user;
	}
	public String getSecret() {
		return this.secret;
	}
	public String getHost() {
		return this.host;
	}
	public Integer getMaxConcurrentSessions() {
		return this.maxConcurrentSessions;
	}
}