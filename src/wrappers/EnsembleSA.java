package wrappers;

import java.util.ArrayList;

import loggers.ManualLogger;
import main.Netter;
import network.NetworkFileBackedWrapper;
import network.modifiers.PredictionModifierOpt;
import network.postprocessing.RerankingResult;
import schedulers.NetworkSA;
import settings.CurrentSettings;
import fitnessfunctions.PartitionFitnessFunction;

public class EnsembleSA {


	/**
	 * Amount of threads currently spawned
	 */
	private volatile int workersInProgress;
	
	/**
	 * Current optimization procedures spawned as seperate threads
	 */
	private volatile ArrayList<NetworkSA> threadPool;
	
	/**
	 * Network/ranking to be optimized
	 */
	private NetworkFileBackedWrapper network ;
	
	/**
	 * Indicates the current process is ready to start new threads
	 */
	private volatile boolean eligbleForStart ;
	
	/**
	 * How many optimizations runs before averaging
	 */
	private volatile int iterations ;
	
	/**
	 * Indicates of a restart as suggested by loggers is allowed
	 */
	private volatile boolean restartPossible;
	
	/**
	 * Indicates if the current process is in the progress of restarting
	 */
	private volatile boolean restarting;
	
	/**
	 * Indicates if the current process is setting parameters
	 */
	private volatile boolean settingParameters ;
	
	/**
	 * Indicates if the current process is stopping execution
	 */
	private volatile boolean stopping;
	
	/**
	 * Indicates if the current process is finished
	 */
	private boolean finished ;
	
	/**
	 * Link to the main program, to extract and change settings
	 */
	private Netter net ;
	
	/**
	 * Result of the re-ranking
	 */
	private volatile RerankingResult result ;
	
	
	/**
	 * Constructs a new EnsembleSa, started by the main class Netter
	 * @param net
	 */
	public EnsembleSA(Netter net){
		
		this.net = net;
		init();

	}
	
	
	/**
	 * Main method. Start a Netter re-ranking procedure with a set amount of iterations
	 * 
	 * @param network	the network/ranking to be re-ranked
	 * @param totalIterations	Total amount of mutations that should be performed before stopping
	 */
	public void ensembleSA (NetworkFileBackedWrapper network, int totalIterations){
		

		this.network = network;
		this.iterations =0;		// the amount of iterations we already did
		long next = System.currentTimeMillis();		// start time measurement
		this.restarting = false;		// at this moment we are not restarting
		boolean entry = true;		// create a variable to enter the loop first time
		this.eligbleForStart = true;
		
		this.result = new RerankingResult();
		
		
		// enter this loop either for the first time or after a restart
		while (entry || restarting){
			
			if (stopping){
				stopProcedure();
				// now all threads stopped, so just stop everything
				return;
			}
			// inform users how we got here, either after an restart procedure or 
			if (restarting){
				System.out.println("Entering the main loop of EnsembleSA after a restart procedure.");

			}else{
				System.out.println("Entering the main loop of EnsembleSA for the first time.");
			}
			
			
			// sleep as long as the parameters arent set
			while (restarting && this.settingParameters && (this.getOrSetWorkersInProgress(-1, true) !=0)){
				System.out.println("Idling until new parameters are set after a restart procedure.");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					System.err.println("Thread interrupted for unknown reason");
				}
			}
			
			// from now on we atleast visited this loop once and we will only return her in case we had to restart the thread
			entry = false;
			this.iterations = 0;
			this.restarting = false;
			this.restartPossible = true;
			// this should be thread safe, as no other thread can be active at this moment
			this.threadPool.clear();
			
			//
			this.result = new RerankingResult();
			
			
			//
			this.eligbleForStart = true;

			// normal execution, wait for all threads to have finished
			while((this.iterations != totalIterations || this.getOrSetWorkersInProgress(-1,true) !=0) && !restarting){
				

				
				// start workers until we reach goal of iterations or until we are restarting
				while (this.iterations + this.getOrSetWorkersInProgress(-1, true) < totalIterations && !restarting  ){
					// just sleep this thread, as long as no free cores are available
					while (this.getOrSetWorkersInProgress(-1, true) == this.net.getCurrentSettings().getWorkersAvailable()  && !restarting || !this.eligbleForStart){
						try {
							Thread.sleep(1000);
						}
						catch (InterruptedException execption){
							System.err.println("Main thread was interrerupted for unknown reason.");
							
						}
						if (stopping){restarting=true;}
					}
					
					// as soon as atleast half of the available threads have finished without requesting a restart, disallow restarting
					if (iterations >= this.net.getCurrentSettings().getWorkersAvailable()){
						this.restartPossible = false;
					}

					if (restarting){
						continue;
					}
					
					// prepare a new thread			
					launchNewOptimizationThread();
					
					
				}
			
			
				try {
					
					// if we are restarting, keep signaling all threads to stop
					if (restarting){
						
						System.err.println("Restarting stage: signaling and waiting for threads to finish.");
						while(this.getOrSetWorkersInProgress(-1, true)!=0){
							
							for (NetworkSA sa : threadPool){
								sa.setRestarting(true);
							}
							
							try {
								Thread.sleep(1000);
							}
							catch (InterruptedException execption){
								System.err.println("Main thread was interrerupted for unknown reason.");
								
							}
							
						}
					}
					else{
						Thread.sleep(1000);
					}
				}
				catch (InterruptedException execption){
					System.err.println("Main thread was interrerupted for unknown reason.");
					
				}
				
			}	
		}
		
	
		// if we get here, we finished and should aggregate the results

		this.result.resultToTSV(this.net.getCurrentSettings().getOutputDirString()+ this.net.getCurrentSettings().getOutputSuffix()+"--"+ "-ENSEMBLE-second.txt");	
		long current = System.currentTimeMillis();
		this.finished = true;
		System.out.println("Finished ensembleSA in "+ (current-next)+" ms");
		
		
	}
	


	/**
	 * Method which a thread can use to signal it finished.
	 * 
	 * @param worker	The thread/optimization procedure that wants to signal
	 */
	public synchronized void workerSignalingFinish(NetworkSA worker){
		
		NetworkFileBackedWrapper wrapper = new NetworkFileBackedWrapper(worker.getWrapper());
		// get the required information
		this.result.addResult(wrapper);
		
		// increase the amount of iterations
		this.iterations++;
		
		//clear the worker
		worker.clear();

		this.threadPool.remove(worker);
		
		// free a processor core
		this.getOrSetWorkersInProgress(-1, false);
	}
	
	
	/**
	 * Method which a thread can use to signal it crashed.
	 * 
	 * @param worker	The thread/optimization procedure that wants to signal
	 */
	public synchronized void workerSignalingCrash(NetworkSA networkSA) {

		
		this.getOrSetWorkersInProgress(-1, false);
		System.err.println("Thread failed for unknown reason. Rebooting thread.");
		
	}
	

	/**
	 * Method which a thread can use to signal it thinks temperature settings should change
	 * 
	 * @param worker	The thread/optimization procedure that wants to signal
	 */
	public synchronized boolean workerRequestingRestart (NetworkSA networkSA){
		
		
		System.err.println("Evaluating restart request:");
		// callback to chance parameters if allowed

		if (restartPossible && iterations < this.net.getCurrentSettings().getWorkersAvailable()/2){
			// make sure only one thread can make the call
			this.restartPossible = false;
			networkSA.setRestarting(true);

			// we are now officialy restarting
			this.restarting = true;
			System.err.println("Restarting");
			// 
			this.settingParameters = true;
			
			networkSA.approved();
			
			return true;
		}
		
		return false;
		
	}
	
	/**
	 * Method which a thread can use to signal it crashed.
	 * 
	 * @param worker	The thread/optimization procedure that wants to signal
	 */
	public synchronized void workerSignalingRestart(NetworkSA networkSA) {
		this.getOrSetWorkersInProgress(-1, false);
	}
	
	/**
	 * Method which a thread can use to signal
	 * 
	 * @param worker	The thread/optimization procedure that wants to signal
	 */

	
	public synchronized void workerReportingPositive() {
		this.restartPossible =false;
	}

	/**
	 * Method that changes the temperature settings
	 * 
	 * @param coef factor to change with
	 */
	public synchronized void alterAndRecalculateTemperatureSettings(double coef) {

		double oldTemp = this.net.getCurrentSettings().getStartTemperature();
		this.net.getCurrentSettings().setStartTemperature(oldTemp*coef);
		this.net.getCurrentSettings().recalculateEndTemperatureAndAnnealingSettings();
		
	}





	
	// getters and setters
	
	
	public synchronized void setEligible (){
		this.eligbleForStart = true;
	}
	
	
	public boolean isFinished() {
		return finished;
	}



	public void setFinished(boolean finished) {
		this.finished = finished;
	}



	
	public boolean isStopping() {
		return stopping;
	}

	public void setStopping(boolean stopping) {
		this.stopping = stopping;
	}
	
	public int getIterations() {
		return iterations;
	}



	public void setIterations(int iterations) {
		this.iterations = iterations;
	}




	public boolean isRestartPossible() {
		return restartPossible;
	}



	public void setRestartPossible(boolean restartPossible) {
		this.restartPossible = restartPossible;
	}



	public boolean isRestarting() {
		return this.restarting;
	}



	public void setRestarting(boolean restarting) {
		this.restarting = restarting;
	}



	public boolean isSettingParameters() {
		return settingParameters;
	}



	public void setSettingParameters(boolean settingParameters) {
		this.settingParameters = settingParameters;
	}
	
	public int getWorkersInProgress() {
		return workersInProgress;
	}



	public void setWorkersInProgress(int workersInProgress) {
		this.workersInProgress = workersInProgress;
	}



	public ArrayList<NetworkSA> getThreadPool() {
		return threadPool;
	}



	public void setThreadPool(ArrayList<NetworkSA> threadPool) {
		this.threadPool = threadPool;
	}



	//private methods
	
	private synchronized void launchNewOptimizationThread(){
		
		
		CurrentSettings cs = this.net.getCurrentSettings();
		
		// first copy the networkwrapper
		NetworkFileBackedWrapper networkCopy = new NetworkFileBackedWrapper(network);
		networkCopy.setOutputFileName(cs.getOutputDirString()+"/"+"/"+ (this.iterations+this.getOrSetWorkersInProgress(-1,true))+ "--"+(this.iterations+"-"+this.getOrSetWorkersInProgress(-1, true)));
		// create a new modifier
		PredictionModifierOpt modifier = new PredictionModifierOpt(networkCopy,cs.getModifyEachTurn(),cs.getMoveEachTurn());
		// create a new fitnessfunction
		

		PartitionFitnessFunction fitnessFunction = new PartitionFitnessFunction(cs.getPenalties(),networkCopy,modifier);
		modifier.setFitnessIncrementFunction(fitnessFunction);
		
		ArrayList<ManualLogger> nLoggers = new ArrayList<ManualLogger>();
		// create new loggers
		for (ManualLogger logger: cs.getLoggers()){			
			ManualLogger copyLogger = logger.makeCopy("/--"+ (this.iterations+this.getOrSetWorkersInProgress(-1,true))+ "--"+(this.iterations+"-"+this.getOrSetWorkersInProgress(-1, true))+"--"+logger.getLogFile());	
			nLoggers.add(copyLogger);
		}
		
		if (!restarting){


		
			NetworkSA sa =new NetworkSA(cs.getStartTemperature(),cs.getAnnealingMultiplier(), cs.getEndTemperature(), modifier, fitnessFunction, true,cs.getMaxIterations(),nLoggers,networkCopy,  this);
			this.threadPool.add(sa);
			
			System.out.println("Starting another thread:"+ 	this.getOrSetWorkersInProgress(-1,true )+" in progress. Threadpool size: "+threadPool.size());
	
	
			this.getOrSetWorkersInProgress(1, false);
			this.eligbleForStart = false;
			new Thread(sa).start();
		
		}
	}
	
	
	
	
	private synchronized int getOrSetWorkersInProgress(int changeWorkers,boolean get){
		
		
		if (get){
			return this.workersInProgress;
		}
		else{
			this.workersInProgress += changeWorkers;
			return this.workersInProgress;
		}
		
	}
	
	private void stopProcedure (){
		
		System.out.println("Stopping command being executed.");
		restarting = true;
		try {
			
			// if we are restarting, keep signaling all threads to stop
			if (restarting){
				System.err.println("Restarting stage: signaling and waiting for threads to finish.");
				while(this.getOrSetWorkersInProgress(-1, true)!=0){
					for (NetworkSA sa : threadPool){
						sa.setRestarting(true);
					}
					try {
						Thread.sleep(1000);
					}
					catch (InterruptedException execption){
						System.err.println("Main thread was interrerupted for unknown reason.");
						
					}
				}
			}
			else{
				Thread.sleep(1000);

			}
		}
		catch (InterruptedException execption){
			System.err.println("Main thread was interrerupted for unknown reason.");
			
		}
	}
	
	private void init(){
		
		this.workersInProgress = 0;
		this.threadPool = new ArrayList<NetworkSA>();
		this.eligbleForStart = true;
		this.restartPossible = true;
		this.restarting = false;
		this.settingParameters =false;
		this.stopping = false;
		this.finished =false;

	}
	
	

	
}
