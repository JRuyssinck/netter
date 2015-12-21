package loggers.defined;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.management.RuntimeErrorException;

import loggers.ManualLogger;
import schedulers.NetworkSA;
import wrappers.EnsembleSA;

/**
 * 
 * Acceptance is a logger which keeps track of the success of the changes to the network in a single re-ranking. I.e. it logs the annealing process.
 * It is also responsible to advice the re-ranking at set times if the annealing parameters are set well.
 * 
 * At desired intervals, it will also print out characteristics of the annealing process to file.
 * 
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */

public class AcceptanceLogger extends ManualLogger {

	/**
	 * Counts how many times a change resulted in a better ranking
	 */
	private int better ;
	/**
	 * Counts how many times a change resulted in a worse ranking that was accepted
	 */
	private int accepted;
	/**
	 * Counts how many times a change resulted in a worse ranking that was reverted
	 */
	private int reverted ;
	/**
	 * Counts how many times a change has occurred 
	 */
	private int logged;
	
	/**
	 * Keeps track of the chance at every log action, as calculated by the annealing process to accept a re-ranking
	 */
	private ArrayList<Double> chance = new ArrayList<Double>();
	
	/**
	 * Keeps track of the percentage of changes to the network resulted in a better ranking at every log action
	 */
	private ArrayList<Double> betterList = new ArrayList<Double>();
	
	/**
	 * Keeps track of the percentage of changes to the network resulted in worse but accepted ranking at every log action
	 */
	private ArrayList<Double> acceptedList =new ArrayList<Double>();
	
	/**
	 * Keeps track of the percentage of changes to the network resulted in network being reverted at every log action
	 */
	private ArrayList<Double> revertedList = new ArrayList<Double>();
	
	/**
	 * This loggers needs to perform its log action every iteration, but writing to file would be too costly. Therefore this variable determines the frequency of logging to file.
	 */
	private int granularity ;

	// annealing parameters
	/**
	 * 
	 * Determines if this logger checks the temperature scheme
	 */
	private boolean tempMaxSetting ;
	
	/**
	 * Determines when this logger checks the temperature scheme in iterations
	 */
	private int tempIterationCheck;
	
	/**
	 * Determines the goal of % of accepted rankings
	 */
	private double tempTargetGoal;
	
	/**
	 * Determines the deviation allowed of the goal
	 */
	private double tempTargetGoalAllowedDeviation ;
	
	/**
	 * True if temperature was too high
	 */
	private boolean tempToHigh = false;
	
	/**
	 * True if temperature was too high
	 */
	private boolean tempToLow = false;
	
	/**
	 * True if temperature was okay
	 */
	private boolean tempGood = false;
	
	
	/**
	 * Constructs a new Acceptancelogger with given parameters.
	 * 
	 * @param iterations	frequency of logging, passed to super constructor, for this penalty, should be 1
	 * @param logFile	file to log too
	 * @param outputDir	output directory
	 * @param granularity	frequency of writing to file
	 * @param tempMaxSetting	if temperature scheme should be checked by logger
	 * @param tempTargetGoal	percentage goal for temperature scheme
	 * @param tempTargetAllowedDeviation allowed deviation from the goal
	 * @param tempIterationCheck	after how many iterations should temperature scheme be checked
	 */
	public AcceptanceLogger (int iterations, String logFile,File outputDir,int granularity, boolean tempMaxSetting, double tempTargetGoal, double tempTargetAllowedDeviation, int tempIterationCheck){
	
		
		super(iterations,logFile,outputDir);
		// warn if iterations !=1
		
		if(iterations!=1){
			System.out.println("Warning: Creating an AcceptanceLogger with iterations parameter not set to 1.");
		}
		this.granularity = granularity;
		this.tempMaxSetting = tempMaxSetting;
		this.tempTargetGoal = tempTargetGoal;
		this.tempTargetGoalAllowedDeviation = tempTargetAllowedDeviation;
		this.tempIterationCheck = tempIterationCheck;
	}
	
	/**
	 * 
	 * Copy constructor, creates and returns a new AcceptanceLogger in the form of a ManuaLogger, with the same settings as the argument but a different logfile.
	 * 
	 * @param suffix new output filename
	 *
	 */
	@Override
	public ManualLogger makeCopy(String suffix) {
		
		String nLogFile = suffix+".txt";
		AcceptanceLogger nLogger = new AcceptanceLogger(this.loggingInterval,nLogFile, this.outputDir,this.granularity, this.tempMaxSetting,this.tempTargetGoal,this.tempTargetGoalAllowedDeviation, this.tempIterationCheck);
		return nLogger;
	}

	
	/**
	 * Overrides and defines the logAction for an AcceptanceLogger. Keeps track of the changes, chances of the temperature scheme.
	 * Can request changes to the scheme.
	 * 
	 * @param annealingProcess the current optimization procedure requesting the log action
	 * 
	 */
	@Override
	protected void logAction(NetworkSA annealingProcess) {

		this.logged++;
		
		if (annealingProcess.getLastAction().equals(NetworkSA.LastAction.BETTER)){		
			this.better++;
			this.chance.add(-1.0);	
		}
		else if(annealingProcess.getLastAction().equals(NetworkSA.LastAction.ALLOWED)) {
			this.accepted++;
			this.chance.add(annealingProcess.getLastChance());
		}
		else{
			this.reverted++;
			this.chance.add(annealingProcess.getLastChance());
		}
		this.acceptedList.add((double)this.accepted/ (double)(this.logged));
		this.revertedList.add((double)this.reverted/ (double)(this.logged));
		this.betterList.add((double)this.better/ (double)(this.logged));

		// check if we should call an abort
		if (this.tempMaxSetting && this.logged == this.tempIterationCheck){
			System.out.println("Checking temperature");
			double avg = 0.0;
			int valid = 0;
			for (int i = 0 ; i < chance.size(); i++){
				if (chance.get(i) != -1.0){
					avg += chance.get(i);
					valid++;
				}
			}
			// if not enough valid, report and do nothing
			if (valid < 50){
				System.err.println("Warning: Automatic temperature setting possibly failed. Not enough samples to determine chance");
			}
			avg /= valid;
			System.out.println("Chance was: "+ avg);
			// check if its too much
			if (avg > this.tempTargetGoal+ this.tempTargetGoalAllowedDeviation){	
				System.out.println("Requesting abort, temperature too high");
				this.tempToHigh =true;
				this.setPositive(false);
				this.setFired(true);
			}
			else if ( avg < this.tempTargetGoal- this.tempTargetGoalAllowedDeviation){
				System.out.println("Requesting abort, temperature too low");
				this.tempToLow = true;
				this.setPositive(false);
				this.setFired(true);	
			}
			else{
				System.out.println("Accepting temperature.");
				this.setPositive(true);
				this.setFired(true);
	
			}
		}
	}
	
	/**
	 * Overrides and defines the fileAction for an AcceptanceLogger
	 * 
	 * @param writer the PrintWriter requesting input to log to file
	 * 
	 */
	@Override
	protected void fileAction(PrintWriter writer) throws IOException {

		double averageB= 0.0;
		double averageA = 0.0;
		double averageR = 0.0;
		double averageP = 0.0;
		int count = 0;
		
		for (int i = 0 ; i < this.logged ; i++){
			averageB += this.betterList.get(i);
			averageA += this.acceptedList.get(i);
			averageR += this.revertedList.get(i);
			double tmp = this.chance.get(i);
			if (tmp != -1.0){
				averageP += tmp;
				count ++;
			}
			if (i % granularity == 0 && i!=0){
				averageB /=granularity;
				averageA /=granularity;
				averageR /=granularity;
				if (count !=0){
					averageP /=count;
				}
				
				
				writer.println(i+ "\t"+ averageA+ "\t"+ averageB+"\t"+ averageR+"\t"+averageP+"\t");
				averageB= 0.0;
				averageA = 0.0;
				averageR = 0.0;
				averageP = 0.0;
				count = 0;
			}
		}
	}
	
	
	/**
	 * Overrides and defines the advice function for an Acceptance logger.
	 * 
	 * Re-ranking process agrees it needs to be changed and requests advice. This class will lower or increase the temperature settings.
	 * 
	 * @param sa	the re-ranking progress requesting advice
	 * 
	 */
	@Override
	public void advice(EnsembleSA sa) {
		System.out.println("Giving advice.");
		if (this.tempToHigh){			
			sa.alterAndRecalculateTemperatureSettings(0.5);
		}	
		else if (this.tempToLow){
			sa.alterAndRecalculateTemperatureSettings(1.25);
		}else{
			throw new RuntimeErrorException(null,"Logger requested abort but is in illegal state.");
		}
		sa.setSettingParameters(false);
	}
	
	
	
	/*
	 * Getters and setters
	 * 
	 */
	
	public boolean isTempGood() {
		return tempGood;
	}


	public void setTempGood(boolean tempGood) {
		this.tempGood = tempGood;
	}

	
	public int getBetter() {
		return better;
	}


	public void setBetter(int better) {
		this.better = better;
	}


	public int getAccepted() {
		return accepted;
	}


	public void setAccepted(int accepted) {
		this.accepted = accepted;
	}


	public int getReverted() {
		return reverted;
	}


	public void setReverted(int reverted) {
		this.reverted = reverted;
	}


	public int getLogged() {
		return logged;
	}


	public void setLogged(int logged) {
		this.logged = logged;
	}


	public ArrayList<Double> getChance() {
		return chance;
	}


	public void setChance(ArrayList<Double> chance) {
		this.chance = chance;
	}


	public ArrayList<Double> getBetterList() {
		return betterList;
	}


	public void setBetterList(ArrayList<Double> betterList) {
		this.betterList = betterList;
	}


	public ArrayList<Double> getAcceptedList() {
		return acceptedList;
	}


	public void setAcceptedList(ArrayList<Double> acceptedList) {
		this.acceptedList = acceptedList;
	}


	public ArrayList<Double> getRevertedList() {
		return revertedList;
	}


	public void setRevertedList(ArrayList<Double> revertedList) {
		this.revertedList = revertedList;
	}


	public int getGranularity() {
		return granularity;
	}


	public void setGranularity(int granularity) {
		this.granularity = granularity;
	}


	public boolean isTempMaxSetting() {
		return tempMaxSetting;
	}


	public void setTempMaxSetting(boolean tempMaxSetting) {
		this.tempMaxSetting = tempMaxSetting;
	}


	public int getTempIterationCheck() {
		return tempIterationCheck;
	}


	public void setTempIterationCheck(int tempIterationCheck) {
		this.tempIterationCheck = tempIterationCheck;
	}


	public double getTempTargetGoal() {
		return tempTargetGoal;
	}


	public void setTempTargetGoal(double tempTargetGoal) {
		this.tempTargetGoal = tempTargetGoal;
	}


	public double getTempTargetGoalAllowedDeviation() {
		return tempTargetGoalAllowedDeviation;
	}


	public void setTempTargetGoalAllowedDeviation(double tempTargetGoalAllowedDeviation) {
		this.tempTargetGoalAllowedDeviation = tempTargetGoalAllowedDeviation;
	}


	public boolean isTempToHigh() {
		return tempToHigh;
	}


	public void setTempToHigh(boolean tempToHigh) {
		this.tempToHigh = tempToHigh;
	}


	public boolean isTempToLow() {
		return tempToLow;
	}


	public void setTempToLow(boolean tempToLow) {
		this.tempToLow = tempToLow;
	}

	
	
	
}