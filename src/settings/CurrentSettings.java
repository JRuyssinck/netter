package settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import loggers.ManualLogger;
import loggers.defined.AcceptanceLogger;
import loggers.defined.PenaltiesLogger;


/**
 * 
 * CurrentSettings is a collection class of all the global settings used in Netter. It is also responsible for reading parameters from a parameter Netter file.
 * 
 * @author Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version 1.0
 * @since 0.0
 */

public class CurrentSettings {

	/**
	 * Directory in which predictions can be found
	 */
	private volatile String testDir;
	
	/**
	 * Predictions that should be re-ranked (e.g. genie.txt, clr.txt). Only used if allPreds=FALSE
	 */
	private volatile String[] preds;
	
	/**
	 * Indicates if all text files should be re-ranked or only those specified by preds
	 */
	private volatile boolean allPreds;
	
	/**
	 * Amount of cores that can be used/ (Amount of threads spawned by the system - 1)
	 */
	private volatile int workersAvailable;
	
	/**
	 * Amount of individual re-rankings
	 */
	private volatile int totalIterations;
	
	/**
	 * How many links at the top of the ranking should be processed
	 */
	private volatile int cutoff;
	
	/**
	 * Subnetwork size
	 */
	private volatile int chuncks;
	
	/**
	 * Increase in importance of smaller subnetworks
	 * 
	 */
	private volatile double chunckIncrease;
	
	/**
	 * Maximum amount of links modified in a single network change
	 */
	private volatile int modifyEachTurn;
	
	/**
	 * 
	 * Maximum amount of positions in the ranking a link can move during a network change
	 */
	private volatile int moveEachTurn;
	
	/**
	 * Starting temperature of the annealing scheme
	 */
	private volatile double startTemperature;
	
	/**
	 * Starting temperature of the annealing scheme
	 */
	private volatile long maxIterations;
	
	/**
	 * Turns automatic temperature setting scheme on or off
	 */
	private volatile boolean temperatureAutoDetermination;
	
	/**
	 * Determines when the temperature scheme should be checked and adjusted if needed
	 */
	private volatile double determinationZone;
	
	/**
	 * Percentage of accepted wrong solutions that we aim for during determination zone
	 */
	private volatile double temperatureTargetGoal;
	
	/**
	 * Allowed deviation from the goal
	 */
	private volatile double temperatureAllowedDeviation;
	
	/**
	 * Multiplier to decrease temperature
	 */
	private volatile double annealingMultiplier;
	
	/**
	 * End temperature of annealing scheme
	 */
	private volatile double endTemperature;
	
	/**
	 * Output file directory in string
	 */
	private volatile String outputDirString;
	
	/**
	 * Output file directory as a File
	 */
	private volatile File outputDir;
	
	/**
	 * List of loggers registered in system
	 */
	private volatile ArrayList<ManualLogger> loggers = new ArrayList<ManualLogger>();
	
	/**
	 * List of penalties registered in system
	 */
	private volatile ArrayList<LoggerOrPenaltyDefinition> penalties = new ArrayList<LoggerOrPenaltyDefinition>();
	
	/**
	 * Suffix to add to outputfilename
	 */
	private volatile String outputSuffix ;

	
	
	
	/**
	 * 
	 * Constructs a new settings objects from a paramater file.
	 * @param settingsFile
	 */
	public CurrentSettings (File settingsFile){
		
		this.setParamsFromFile(settingsFile);
		this.calculateAdditionalSettings ();
		
	}
	
	
	/**
	 * Recalculates the annealing parameters which are determined from the others. Should be called after annealing paramaters have 
	 */
	public void recalculateEndTemperatureAndAnnealingSettings(){
		
	    System.out.println("PARAMETER SETTING:  Start temperature is set at  "+this.startTemperature);
		// these parameters are determined from the others
	    System.out.println("PARAMETER SETTING:  Setting the initial endTemperature to  "+this.startTemperature /100);
		this.endTemperature = this.startTemperature /100;
	    System.out.println("PARAMETER SETTING:  Setting the initial annealingMultiplier to  "+Math.pow(  (endTemperature/this.startTemperature),  1/((double)this.maxIterations) ));
		this.annealingMultiplier = Math.pow(  (endTemperature/this.startTemperature),  1/((double)this.maxIterations) );
		
	}
	

	
	// getters and setters

	
	public double getAnnealingMultiplier() {
		return annealingMultiplier;
	}

	public void setAnnealingMultiplier(double annealingMultiplier) {
		this.annealingMultiplier = annealingMultiplier;
	}

	public double getEndTemperature() {
		return endTemperature;
	}

	public void setEndTemperature(double endTemperature) {
		this.endTemperature = endTemperature;
	}
	
	public String getTestDir() {
		return testDir;
	}

	public void setTestDir(String testDir) {
		this.testDir = testDir;
	}

	public String[] getPreds() {
		return preds;
	}

	public void setPreds(String[] preds) {
		this.preds = preds;
	}

	public boolean isAllPreds() {
		return allPreds;
	}

	public void setAllPreds(boolean allPreds) {
		this.allPreds = allPreds;
	}

	public int getWorkersAvailable() {
		return workersAvailable;
	}

	public void setWorkersAvailable(int workersAvailable) {
		this.workersAvailable = workersAvailable;
	}

	public int getTotalIterations() {
		return totalIterations;
	}

	public void setTotalIterations(int totalIterations) {
		this.totalIterations = totalIterations;
	}

	public int getCutoff() {
		return cutoff;
	}

	public void setCutoff(int cutoff) {
		this.cutoff = cutoff;
	}

	public int getChuncks() {
		return chuncks;
	}

	public void setChuncks(int chuncks) {
		this.chuncks = chuncks;
	}

	public double getChunckIncrease() {
		return chunckIncrease;
	}

	public void setChunckIncrease(double chunckIncrease) {
		this.chunckIncrease = chunckIncrease;
	}

	public int getModifyEachTurn() {
		return modifyEachTurn;
	}

	public void setModifyEachTurn(int modifyEachTurn) {
		this.modifyEachTurn = modifyEachTurn;
	}

	public int getMoveEachTurn() {
		return moveEachTurn;
	}

	public void setMoveEachTurn(int moveEachTurn) {
		this.moveEachTurn = moveEachTurn;
	}

	public double getStartTemperature() {
		return startTemperature;
	}

	public void setStartTemperature(double startTemperature) {
		this.startTemperature = startTemperature;
	}

	public long getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(long maxIterations) {
		this.maxIterations = maxIterations;
	}

	public boolean isTemperatureAutoDetermination() {
		return temperatureAutoDetermination;
	}

	public void setTemperatureAutoDetermination(
			boolean temperatureAutoDetermination) {
		this.temperatureAutoDetermination = temperatureAutoDetermination;
	}

	public double getDeterminationZone() {
		return determinationZone;
	}

	public void setDeterminationZone(double determinationZone) {
		this.determinationZone = determinationZone;
	}

	public double getTemperatureTargetGoal() {
		return temperatureTargetGoal;
	}

	public void setTemperatureTargetGoal(double temperatureTargetGoal) {
		this.temperatureTargetGoal = temperatureTargetGoal;
	}

	public double getTemperatureAllowedDeviation() {
		return temperatureAllowedDeviation;
	}

	public void setTemperatureAllowedDeviation(
			double temperatureAllowedDeviation) {
		this.temperatureAllowedDeviation = temperatureAllowedDeviation;
	}

	public String getOutputDirString() {
		return outputDirString;
	}

	public void setOutputDirString(String outputDirString) {
		this.outputDirString = outputDirString;
	}

	public File getOutputDir() {
		return outputDir;
	}

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	public ArrayList<ManualLogger> getLoggers() {
		return loggers;
	}

	public void setLoggers(ArrayList<ManualLogger> loggers) {
		this.loggers = loggers;
	}

	public ArrayList<LoggerOrPenaltyDefinition> getPenalties() {
		return penalties;
	}

	public void setPenalties(ArrayList<LoggerOrPenaltyDefinition> penalties) {
		this.penalties = penalties;
	}


	public void setOutputSuffix(String outputSuffix) {
		this.outputSuffix = outputSuffix ;
		
	}

	public String getOutputSuffix(){
		return this.outputSuffix;
	}
	
	
	//private methods
	
	private void createOutputDirectory() {
		// create the outputdir
		File outputDir = new File(this.getOutputDirString());
		if (outputDir.exists()){
			if (!outputDir.isDirectory()){
				System.err.println("Error while loading parameter file: there is a file with the same name as the specified output directory.");
				System.exit(-1);
			}else{
					System.err.println("Warning while loading parameter file: The specified output directory already existed. Deleting content. ");
					deleteFolder(outputDir);
			}
		}
		outputDir.mkdirs();
		this.setOutputDir(outputDir);
	}
	
	
	private void addPenaltiesLogger (int iterations, String suffix){

		PenaltiesLogger penLogger = new PenaltiesLogger(iterations, "Penalties_logfile", this.outputDir);
		loggers.add(penLogger);
	
	}
	
	private void addAcceptanceLogger (int iterations, String suffix, int granularity){

		
		int tempIterationCheck = (int) (this.determinationZone * this.maxIterations); // when should we check for the temperature 

		AcceptanceLogger aLogger= new AcceptanceLogger(iterations, "Acceptance_logfile", this.outputDir, granularity, this.temperatureAutoDetermination,  this.temperatureTargetGoal, this.temperatureAllowedDeviation,tempIterationCheck);
			loggers.add(aLogger);
		
	}

	private void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}
	
	private void setParamsFromFile(File param){
		
		BufferedReader reader =null;
		try {
			reader = new BufferedReader(new FileReader(param));
			String line = reader.readLine();
			boolean reachedLoggers = false;
			boolean reachedPenalties = false;
			while (line!=null){
				
				// trim whitespace
				line = line.trim();
				
				// kill the comments
				String[] commentSplit = line.split("#");
				commentSplit[0] = commentSplit[0].trim();
				if (commentSplit[0].equals("")){
					// ignore whitespace
					line = reader.readLine();
					continue;
				}
				String[] split = commentSplit[0].split("=");
				String parameter = split[0];
				
				
				switch (parameter){
				
				case ("testDir"):
					this.testDir = split[1];
				    System.out.println("PARAMETER SETTING: Setting the directory with tests to: "+split[1]);
					break;
				case("allPreds"):
				    System.out.println("PARAMETER SETTING: Setting allPreds parameter to: "+split[1]);
					this.allPreds = Boolean.parseBoolean(split[1]);
					break;
				case("workersAvailable"):
				    System.out.println("PARAMETER SETTING: Setting workersAvailable parameter to: "+split[1]);
					this.workersAvailable = Integer.parseInt(split[1]);
					break;
				case("totalIterations"):
				    System.out.println("PARAMETER SETTING: Setting totalIterations parameter to: "+split[1]);
					this.totalIterations =  Integer.parseInt(split[1]);
					break;
				case("cutoff"):
				    System.out.println("PARAMETER SETTING: Setting cutoff parameter to: "+split[1]);
					this.cutoff = Integer.parseInt(split[1]);
					break;
				case("chuncks"):
				    System.out.println("PARAMETER SETTING: Setting chuncks parameter to: "+split[1]);
					this.chuncks = Integer.parseInt(split[1]);
					break;
				case("chunckIncrease"):
				    System.out.println("PARAMETER SETTING: Setting cutIncrease parameter to: "+split[1]);
					this.chunckIncrease = Double.parseDouble(split[1]);
					break;
				case("modifyEachTurn"):
				    System.out.println("PARAMETER SETTING: Setting modifyEach parameter to: "+split[1]);
					this.modifyEachTurn = Integer.parseInt(split[1]);
					break;
				case("moveEachTurn"):
				    System.out.println("PARAMETER SETTING: Setting moveEachTurn parameter to: "+split[1]);
					this.moveEachTurn = Integer.parseInt(split[1]);
					break;
				case("startTemperature"):
				    System.out.println("PARAMETER SETTING: Setting startTemperature parameter to: "+split[1]);
					this.startTemperature = (Double.parseDouble(split[1]));
					break;
				case("maxIterations"):
				    System.out.println("PARAMETER SETTING: Setting maxIterations parameter to: "+split[1]);
					this.maxIterations = (Integer.parseInt(split[1]));
					break;
				case("temperatureAutoDetermination"):
				    System.out.println("PARAMETER SETTING: Setting temperatureAutoDetermination parameter to: "+split[1]);
					this.temperatureAutoDetermination = (Boolean.parseBoolean(split[1]));
					break;
				case("determinationZone"):
				    System.out.println("PARAMETER SETTING: Setting determinationZone parameter to: "+split[1]);
					this.determinationZone = (Double.parseDouble(split[1]));
					break;
				case("temperatureTargetGoal"):
				    System.out.println("PARAMETER SETTING: Setting temperatureTargetGoal parameter to: "+split[1]);
					this.temperatureTargetGoal = (Double.parseDouble(split[1]));
					break;
				case("temperatureAllowedDeviation"):
				    System.out.println("PARAMETER SETTING: Setting temperatureAllowedDeviation parameter to: "+split[1]);
					this.temperatureAllowedDeviation =(Double.parseDouble(split[1]));
					break;
				case("preds"):
					String[] preds = new String[split.length-1];
			    	System.out.print("PARAMETER SETTING: Setting the allowed predictions to: ");
					for (int i = 0 ; i < split.length -1; i++){
						preds[i] = split[i+1];
						if (i != split.length -2){
							System.out.print(split[i+1]+", ");
						}else{
							System.out.println(split[i+1]+".");
						}
					}
					this.preds = (preds);
					break;
				case("outputDirString"):
				    System.out.println("PARAMETER SETTING: Setting outputDirString parameter to: "+split[1]);
					this.outputDirString =(split[1]);
					this.createOutputDirectory(); // check if directory can be created
					break;
				case("LOGGERS"):
					System.out.println("Set all parameters. Adding loggers to Netter...");		
					reachedLoggers=true;
					break;
				case("PENALTIES"):
					System.out.println("Added all loggers. Adding penalties to Netter...");
					reachedLoggers=false;
					reachedPenalties=true;
					break;

				default:
					if (reachedLoggers){
						/*
					    System.out.println("PARAMETER SETTING: Creating a logger: "+ split[0]+" ");
						String name = split[0];
						String[] content = new String[split.length-1];
						for (int i = 0 ; i < content.length ; i++){
							content[i] = split[i+1];
						}
						this.loggers.add(new LoggerOrPenaltyDefinition(name, content));
						break;
						*/
						
						if (split[0].equals("PenaltiesLogger")){
						    System.out.println("PARAMETER SETTING: Creating a Penaltieslogger with iteration interval: "+split[1] +" and  suffix "+split[2]);
							int iter = Integer.parseInt(split[1]);
							String suffix = split[2];
							addPenaltiesLogger(iter, suffix);
							
							
							
						}else if (split[0].equals("AcceptanceLogger")){
							
						    System.out.println("PARAMETER SETTING: Creating a Acceptancelogger with iteration interval: "+split[1] +" and  suffix "+split[2]);
								int iter = Integer.parseInt(split[1]);
								String suffix = split[2];
								addAcceptanceLogger(1, suffix,iter);
							
						}
						else{
							
							System.err.println(split[0]);
						}
						
			
						
						
						break;
					}else if(reachedPenalties){
						 System.out.println("PARAMETER SETTING: Creating a penalty "+ split[0]+" with coef: "+split[1] );
							String name = split[0];
							String[] content = new String[split.length-1];
							for (int i = 0 ; i < content.length ; i++){
								content[i] = split[i+1];
							}
							this.penalties.add(new LoggerOrPenaltyDefinition(name, content));
							break;
					}else{
						System.err.println("Properties file contained unknown parameter: "+parameter+" ABORTING");
						System.exit(-1);
						break;
					}
				}
				line = reader.readLine();
			}
			
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			if (reader!=null){
				try {
					System.out.println("Added all penalties. Reached end of script.");
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	private void calculateAdditionalSettings(){

		recalculateEndTemperatureAndAnnealingSettings();
	}

}
