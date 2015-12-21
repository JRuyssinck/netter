package loggers.defined;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import loggers.ManualLogger;
import schedulers.NetworkSA;
import wrappers.EnsembleSA;
import fitnessfunctions.PartitionFitnessFunction;
import fitnessfunctions.PenaltyPair;


/**
 * 
 * PenaltiesLogger is a logger which keeps track of all registered penalties during single re-rankings.
 * 
 * At desired intervals, it will poll all registered penalty functions and print the current score to a log file.
 * 
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */

public class PenaltiesLogger extends ManualLogger {

	
	/**
	 * A list of registered Penalties with associated costs
	 */
	private ArrayList<ArrayList<PenaltyPair>> pairs;

	/**
	 * Constructs a Penalitieslogger, calls constructor of logger abstract class
	 * 
	 * @param loggingInterval	perform logging action every 'loggingInterval' changes to the network
	 * @param logFile	name of output file
	 * @param outputDir name/path of the output directory
	 */
	public PenaltiesLogger(int loggingInterval,String logFile,File outputDir) {
		super(loggingInterval,logFile,outputDir);
		this.pairs = new ArrayList<ArrayList<PenaltyPair>>();
	}
	
	/**
	 * Overrides and creates a copy constructor for a PenaltiesLogger
	 * 
	 * @param logFile name of output file
	 * 
	 * @return a new Manualogger/PenaltiesLogger with the same settings as this instantation and a new logFile
	 */
	@Override
	public ManualLogger makeCopy(String logFile) {
		logFile = logFile+".txt";;
		PenaltiesLogger nLogger = new PenaltiesLogger(this.loggingInterval,logFile,outputDir);
		return nLogger;
	}

	/**
	 * Overrides and defines the logAction for a PenaltiesLogger
	 * 
	 * @param networkSa the current optimization procedure requesting the log action
	 * 
	 */
	@Override
	protected void logAction(NetworkSA networkSa) {
		PartitionFitnessFunction function = networkSa.getFitness();
		this.pairs.add(function.getDetailedFitness());
	}

	/**
	 * Overrides and defines the fileAction for a penaltiesLogger
	 * 
	 * @param writer the PrintWriter requesting input to log to file
	 * 
	 */
	@Override
	protected void fileAction(PrintWriter writer) throws IOException {
	
		for (int i = 0 ; i < this.pairs.size();i++){
			double sum = 0;
			for (int j = 0 ; j < pairs.get(i).size(); j++){
				double score = this.pairs.get(i).get(j).getScore();
				sum+= score;
				if (j==0){
					writer.print(loggingInterval *(i+1) + "\t" +this.pairs.get(i).get(j).getName() + "\t+" + score+"\t");
				}
				else{
					writer.print(this.pairs.get(i).get(j).getName() + "\t+" + score+"\t");
				}
			}
			writer.print("TOTAL" + "\t+" + sum+"\t");
			writer.println();
		}		
	}
	
	/**
	 * PenaltiesLogger does not give feedback to Netter to guide the re-ranking process
	 * 
	 * @param sa The Netter procedure requesting feedback
	 * 
	 */
	@Override
	public void advice(EnsembleSA sa) {}
	
}
