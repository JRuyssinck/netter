package settings;




/**
 * 
 * LoggerOrPenaltyDefinition defines a penalty or logger as a class and associated content/settings
 * 
 * @author Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version 1.0
 * @since 0.0
 */
public class LoggerOrPenaltyDefinition {

	/**
	 * The class name of the penalty of logger
	 */
	private String className;
	
	/**
	 * The content/settings specified as a string array
	 */
	private String[] content;
	
	
	/**
	 * Constructs a LoggerOrPenaltyDefinition.
	 * 
	 * @param className	The class name of the penalty or logger
	 * @param content	the content/settings represented as a string array
	 */
	public LoggerOrPenaltyDefinition (String className,String[] content) {
		this.className = className;
		this.content = content;
	}

	
	// getters and setters
	
	
	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String[] getContent() {
		return content;
	}

	public void setContent(String[] content) {
		this.content = content;
	}


	
	
}
