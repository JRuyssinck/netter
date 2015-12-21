package loggers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import schedulers.NetworkSA;
import wrappers.EnsembleSA;


/**
 * 
 * ManualLogger is an abstract class implementing all common methods required for the logging system. All loggers should extend this class.
 * 
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */

public abstract class ManualLogger {

	/**
	 * Frequency of logging expressed in changes to the network
	 */
	protected int loggingInterval;
	
	/**
	 * Changes to the network since the last logging action
	 */
	protected int iterationsSinceLastLog;
	
	/**
	 * Output directory of the logger
	 */
	protected File outputDir ;
	
	/**
	 * Output filename
	 */
	protected String logFile;
	
	/**
	 * Indicates if this logger is ready to give feedback on current optimization process
	 */
	private boolean fired =false;
	
	/**
	 * Indicates if this logger is positive on the current optimization process or believes it should be restarted (e.g. if the annealing parameters are not set right)
	 */
	private boolean positive = false;
	
	
	/**
	 * Abstract method which the associated optimization procedure calls when requesting a log action
	 */
	protected abstract void logAction (NetworkSA getter);
	
	/**
	 * Abstract method that can be called to ask the logger to output to given outputstream
	 */
	protected abstract void fileAction (PrintWriter writer) throws IOException;
	
	/**
	 * Abstract method that allows the logger to intervene in the optimization process and perform actions when its advice is requested
	 */
	public abstract void advice(EnsembleSA sa);
	
	/**
	 * Copy constructor for this logger, with a new output filename
	 */
	public abstract ManualLogger makeCopy (String suffix);
	
	
	/**
	 * Constructs a new Manuallogger (abstract class, cannot be called directly)
	 *
	 * @param loggingInterval	perform logging action every 'loggingInterval' changes to the network
	 * @param logFile	name of output file
	 * @param outputDir name/path of the output directory
	 * 
	 */
	public ManualLogger (int loggingInterval, String logFile,File outputDir){
		this.loggingInterval = loggingInterval;
		this.logFile = logFile;
		this.outputDir = outputDir;
	}

	/**
	 * Getter which returns if the logger is ready to send feedback
	 * 
	 * @return boolean if logger is ready to send feedback
	 */
	public boolean isFired(){
		return this.fired;
	}
	/**
	 * Getter which returns if the logger is positive on the current optimization process
	 * 
	 * @return boolean if logger is positive on the current optimization
	 */
	public boolean isPositive() {
		return positive;
	}

	/**
	 * Setter which sets logger's opinion on optimization process
	 * 
	 * @param positive logger's opinion on optimization process
	 */
	public void setPositive(boolean positive) {
		this.positive = positive;
	}
	
	/**
	 * Setter to indicate if logger is ready to send feedback
	 * 
	 * @param fired logger is ready to send feedback
	 */
	public void setFired(boolean fired) {
		this.fired = fired;
	}

	/**
	 * Getter which returns the log filename
	 * 
	 * @return log filename
	 */
	public String getLogFile() {
		return logFile;
	}

	/**
	 * Setter to set the log filename
	 * 
	 * @param String log filename
	 */
	public void setLogFile(String logFile) {
		this.logFile = logFile;
	}

	/**
	 * Getter which return frequency of logging 
	 * 
	 * @return logging frequency
	 */
	public int getIterations() {
		return loggingInterval;
	}

	/**
	 * Getter which return frequency of logging 
	 * 
	 * @return logging frequency
	 */
	public int getIterationsSoFar() {
		return iterationsSinceLastLog;
	}

	public void log (NetworkSA getter){
		
		if (iterationsSinceLastLog >= this.loggingInterval || iterationsSinceLastLog ==0){
			this.logAction(getter);
			this.iterationsSinceLastLog =0;
		}	
		this.iterationsSinceLastLog++;
	}

	
	public void deleteFile(){
		File file = new File(outputDir.getAbsoluteFile()+"/"+this.logFile);
		try {
			if (file.exists()){
				file.delete();
			}
		}
		catch (Exception e){
		}
	}
	
	public void toFile() {
		try {
			FileWriter writer = new FileWriter(outputDir+this.logFile,true);
			BufferedWriter bwriter = new BufferedWriter(writer);
			PrintWriter pwriter = new PrintWriter(bwriter);
			this.fileAction(pwriter);
			pwriter.flush();
			pwriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
