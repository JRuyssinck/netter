package schedulers;

import java.util.ArrayList;
import java.util.Random;

import loggers.ManualLogger;
import network.Network;
import network.NetworkFileBackedWrapper;
import network.modifiers.NetworkModifier;
import network.node.Node;
import network.node.NodeCollection;
import wrappers.EnsembleSA;
import fitnessfunctions.PartitionFitnessFunction;


/**
 * 
 * NetworkSA represents a single re-ranking in Netter. It implements the Runnable interface to be spawned as a new Thread.
 * 
 * NetworkSA does the optimization using Simulated Annealing/
 * 
 * @author Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version 1.0
 * @since 0.0
 */
public class NetworkSA implements Runnable {
	
	/**
	 * The network.
	 */
	private  Network network;
	/**
	 * The network/ranking from a file.
	 */
	private  NetworkFileBackedWrapper wrapper ;

	/**
	 *  Starting temperature 
	 */
	private  double T0;
	
	/**
	 * Cool down factor
	 */
	private  double coolingMultiplier;
	
	/**
	 * End temperature
	 */
	private  double freezingTemperature;
	
	/**
	 * The mutator of the network/ranking
	 */
	private  NetworkModifier modifier;
	
	/**
	 * The fitness funtion of the procedure
	 */
	private  PartitionFitnessFunction fitness;
	
	/**
	 * The maximum amount of iterations (changes to the network)
	 */
	private  long maxIterations ;
	
	/**
	 * The overseeing ensemble that started this optimization thread
	 */
	private  EnsembleSA ensemble ;
	
	/**
	 * Indicates if the total optimization ensemble is requesting for restart and this thread is/should reboot
	 */
	private volatile boolean restarting = false;
	
	/**
	 * The current temperature
	 */
	private double T;			
	
	/**
	 * The current amount of iterations
	 */
    private long iterations;
    
    /**
     * Possible outcomes of the last mutation
     *
     */
    public enum LastAction  {BETTER,ALLOWED,REVERTED};
      
    /**
     * The outcome of the last mutation
     */
    private LastAction lastAction = LastAction.BETTER;

    /**
     * The chance generated from the latest change to the ranking in the simulated annealing scheme
     */
    private double chanceAllowed ;
    
    /**
     * Random generator to be re-sued
     */
    private static final Random random = new Random();    
    
    /**
     * Booleant to turn of annealing scheme
     */
    private boolean allowWorse = true;					
	
    /**
     * Registered loggers subscribed to the this re-ranking
     */
	private ArrayList<ManualLogger> loggers ;



	/**
	 * Constructs a new optimization procedure with following temperature settings, network, modifier, logger and fitnessfunction
	 * 
	 * @param T0	start temperature
	 * @param coolingMultiplier	cooldown factor of temperature scheme
	 * @param freezingTemperature	end temperature
	 * @param modifier	modifies the network 
	 * @param fitness	current fitness of the ranking
	 * @param allowWorseSolutions	boolean if worse solutions can be allowed
	 * @param maxIterations	maximum amount of iterations  (chances to the network)
	 * @param loggers	registered loggers following this optimization
	 * @param wrapper	network/ranking
	 * @param ensemble	responsible ensemble which spawned this optimization
	 */
	public NetworkSA( double T0, double coolingMultiplier, double freezingTemperature, NetworkModifier modifier, PartitionFitnessFunction fitness, boolean allowWorseSolutions,long maxIterations,ArrayList<ManualLogger> loggers,NetworkFileBackedWrapper wrapper,   EnsembleSA ensemble ){
		
		this.ensemble = ensemble;	
		this.T0 = T0;
		this.coolingMultiplier = coolingMultiplier;
		this.freezingTemperature = freezingTemperature;
		this.modifier = modifier;
		this.fitness = fitness;				
		this.maxIterations = maxIterations;
		this.wrapper = wrapper;
		this.loggers = loggers;
		this.network = this.wrapper.getNetwork();
		
		// delete previous logfiles
		for (ManualLogger logger: loggers){
			logger.deleteFile();
		}
	
	}
	
	/**
	 * Runs the optimization procedure. Will constantly try to get to the next state of the network. Signals the ensemble asynchronously if it crashed, finished or restarted.
	 */
	public void run(){
		
		try {
			this.nextState();
		}
		catch (NullPointerException nu){
			nu.printStackTrace();
			System.err.println(Thread.currentThread().getName()+ " is causing this exception.");

			this.ensemble.workerSignalingCrash(this);
			return;
		}
		catch (Exception e){
			e.printStackTrace();
			System.err.println(Thread.currentThread().getName()+ " is causing this exception.");
			this.ensemble.workerSignalingCrash(this);
			return;
		}
		
		if (restarting){
			this.ensemble.workerSignalingRestart(this);
		}else{
			this.ensemble.workerSignalingFinish(this);
		}

		System.out.println(Thread.currentThread().getName()+" is finishing.");		
	}
	
	
	/**
	 * Indicates the ensemble procedure accepted a chance in the temperature parameters and is now asking for feedback
	 */
	public void approved (){
		for (ManualLogger logger: this.loggers){	
			if (logger.isFired() && !logger.isPositive()){
				System.err.println("Requesting advice to logger.");
				logger.advice(this.ensemble);
			}
		}
	}
	

	/**
	 * Completely clears all the resources registered by the procedure.
	 */

	public synchronized void clear() {
		
		// completely remove all references to the temporary network
		for (Node node: NodeCollection.returnAllRegisteredNodes()){
			node.clearNetwork(this.network);
		}
		this.fitness.clearNetworks();
		this.wrapper.networkClear();
		this.network =null;
		this.modifier =null;
		this.fitness=null;
		this.wrapper =null;
		this.ensemble=null ;	
	}

	
	
	
	//getters and setters
	
	public boolean isRestarting() {
		return this.restarting;
	}


	public void setRestarting(boolean restarting) {
		this.restarting = restarting;
	}

	private double f(Network s) {
		return fitness.getFitness();				
	}
	
	public long getIterationsSoFar(){
		return iterations;
	}

	private Network newInitialSolution() {
		return this.network;
	}
	
	public Network getNetwork() {
		return network;
	}

	public void setNetwork(Network network) {
		this.network = network;
	}

	public double getT0() {
		return T0;
	}

	public void setT0(double t0) {
		T0 = t0;
	}

	public double getCoolingMultiplier() {
		return coolingMultiplier;
	}

	public void setCoolingMultiplier(double coolingMultiplier) {
		this.coolingMultiplier = coolingMultiplier;
	}

	public double getFreezingTemperature() {
		return freezingTemperature;
	}

	public void setFreezingTemperature(double freezingTemperature) {
		this.freezingTemperature = freezingTemperature;
	}

	public NetworkModifier getModifier() {
		return modifier;
	}

	public void setModifier(NetworkModifier modifier) {
		this.modifier = modifier;
	}

	public PartitionFitnessFunction getFitness() {
		return fitness;
	}

	public void setFitness(PartitionFitnessFunction fitness) {
		this.fitness = fitness;
	}

	public long getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(long maxIterations) {
		this.maxIterations = maxIterations;
	}


	public NetworkFileBackedWrapper getWrapper() {
		return wrapper;
	}

	public void setWrapper(NetworkFileBackedWrapper wrapper) {
		this.wrapper = wrapper;
	}


	public EnsembleSA getEnsemble() {
		return ensemble;
	}

	public void setEnsemble(EnsembleSA ensemble) {
		this.ensemble = ensemble;
	}

	public double getLastChance(){
		return this.chanceAllowed;
	}

	public LastAction getLastAction() {
		return this.lastAction;
	}
	
	
	
	//private functions
	
	private void endFirstTurn() {
		// 
		this.ensemble.setEligible();
		
	}
	
	private void nextState(){
		System.out.println("This thread started: "+ Thread.currentThread().getName());
		 	T=T0();
	        Network s=newInitialSolution();
	        iterations=0;
	        double lastf = f(s);
	        while (!terminateCond()){
	            if (null==newpickAtRandom(s)){
	            	return ;
	            }
	            
	            double newf = f(s);
	            double deltaf=newf-lastf;
	            if (deltaf < 0 ){
	            	lastf = newf;
	            	lastAction = LastAction.BETTER;
	           
	            }
	            else{
	            	chanceAllowed = chanceFunction(T,deltaf);
	            	if (chanceAllowed >1.0 || chanceAllowed < 0.0){
	            	}
	            	else{
	            	}
	            	if ( (random.nextDouble() < chanceAllowed) && allowWorse) {
		            	lastf = newf;
		            	lastAction = LastAction.ALLOWED;
	            	}else{
		            	reverseActions();
		            	lastAction = LastAction.REVERTED;
	            	}
	            }
	            iterations++;
	            updateT();
	            if(iterations==5){
	            	endFirstTurn();
	            }
	            
	        }
	        finalActions();
	        return ;
	}




	private double T0() {
		return T0;
	}


	private boolean terminateCond() {
		
		
		if (restarting){
			return true;
		}
		
		else{
			if (this.maxIterations == 0){
				return T < this.freezingTemperature;		
			}else {
				return maxIterations == this.iterations;
			}
			
		}


	}



	private Network newpickAtRandom(Network s) {

		
		for (ManualLogger logger: this.loggers){
			logger.log(this);
			if (logger.isFired()){
				System.err.println("NetworkSA reporting temperature.");
				if (logger.isPositive()){
					logger.setFired(false);
					this.ensemble.workerReportingPositive() ;
					continue;
				}
				System.err.println("NetworkSA reporting temperature.");

				boolean approved = this.ensemble.workerRequestingRestart(this);
				if (approved){
					System.err.println("Restarting approved by ensemble.");
					this.restarting = true;
					return null;
				}else{
					System.err.println("Restarting was NOT approved by ensemble.");

					logger.setFired(false);
				}
					
			}
		}
		
		modifier.modify();
		return s;		
	}
	
	private double chanceFunction(double T, double deltaf) {
		double lastp = Math.exp(-deltaf/T);
		return lastp;
	}

	private void updateT() {
		T *= this.coolingMultiplier;
	}
	
	private void reverseActions() {
		modifier.revert();
	}

	private void finalActions() {
		
		// write to files
		for (ManualLogger logger: this.loggers){
			logger.toFile();
		}
		System.out.println("SA: iterated - "+iterations+" times");
	}
}
