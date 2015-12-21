package fitnessfunctions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import main.DynamicLoader;
import network.Network;
import network.NetworkFileBackedWrapper;
import network.NetworkPartition;
import network.link.Link;
import network.link.LinkProperties;
import network.modifiers.PredictionModifierOpt;
import penalties.PenaltyFunction;
import settings.LoggerOrPenaltyDefinition;



/**
 * PartitionFitnessfunction is a class responsible for calculating and aggregating the total cost function associated with a certain ranking.
 * 
 * 
 * The ranking is represented both as a NetworkPartition for structural penalties and as a normal network for ranking based penalties (i.a. regularization penalty).
 * It supports incremental penalty function updates by requesting recent changes from the PredictionModifierOpt class.  
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */

public class PartitionFitnessFunction {

	
	/**
	 * Penaltyfunctions associated with the sub-networks (structural penalties)
	 */
	private HashMap<Network,ArrayList<PenaltyFunction>> penaltyFunctionsPerNetwork ;
	
	/**
	 *  penalty functions associated with the ranking (e.g. divergence)
	 */
	private ArrayList<PenaltyFunction> penaltyFunctionGlobalNetwork;
	
	/**
	 * The network partioned into subnetworks
	 */
	private NetworkPartition partition ;
	
	/**
	 * The network as a ranking
	 */
	private Network predictionNetwork;

	/**
	 * The modifier which performs random modifications and is used here to request a list of recent changes
	 */
	private PredictionModifierOpt modifier;
	
	/**
	 * The incremental coefficients assigned to subnetworks of increasing size
	 */
	private HashMap<Network,Double> networkCoef;
	
	/**
	 * The total penalty value of a certain penalty function per network (aggregated over subnetworks)
	 */
	private HashMap<String,Double> perNetwork ;
	
	/**
	 * The individual weighing coefficients associated with each penalty
	 * 
	 */
	private HashMap<String,Double> penaltyCoef;
	
	
	/**
	 * The total penalty value of a certain penalty function per network (aggregated over subnetworks)
	 */
	private HashMap<PenaltyFunction, Double> penaltyScoreEach ;

	
	/**
	 * The current total fitness fo the ranking
	 */
	private long currentFitness ;
	
	
	/**
	 * Indicates if fitnessfunction has been initialized
	 */
	boolean inited = false;

	
	/**
	 * Constructs a PartitionFitnessFunction with an associated network, modifier and associated penalties. Loads and creates penaltyfunction from the definition and assigns them to the correct networks.
	 * 
	 * @param penalties A list of penalty functions to be created and assigned to networks
	 * @param wrapper The network represented as a NetworkFileBackedWrapper
	 * @param modifier The modifier which will mutate the network. Will be polled for incremental updates of the penalties.
	 * 
	 */
	
	public PartitionFitnessFunction(ArrayList<LoggerOrPenaltyDefinition>  penalties, NetworkFileBackedWrapper wrapper,PredictionModifierOpt modifier){

		this.penaltyFunctionsPerNetwork = new HashMap<Network, ArrayList<PenaltyFunction>>();
		this.penaltyFunctionGlobalNetwork = new ArrayList<PenaltyFunction>();
		this.networkCoef = new HashMap<Network,Double>();
		this.penaltyCoef = new HashMap<String,Double>();
		this.penaltyScoreEach = new HashMap<PenaltyFunction,Double>();
		this.partition = wrapper.getPartition();
		this.perNetwork =  new HashMap<String,Double>();
		this.modifier = modifier;
		this.predictionNetwork = this.partition.getPrediction();
		this.networkCoef = partition.getNetworkCoefMap();

		// For each network, init an empty list of penaltyfunctions associated with it
		for (Network net : partition.getNetworks()){
			this.penaltyFunctionsPerNetwork.put(net, new ArrayList<PenaltyFunction>());
		}
		// load the penalties and assign them networks
		dynamicallyLoadAndAssignPenaltiesToNetworks(penalties);


}
	/**
	 * Uses as input a list of penalty definitions which will be loaded and assigned to the assigned (sub) network of this class. Assumes a network/network partition is set.
	 * 
	 * @param penalties A list of penalty functions to be created and assigned to networks
	 * 
	 */
	private void dynamicallyLoadAndAssignPenaltiesToNetworks (ArrayList<LoggerOrPenaltyDefinition>  penalties){
		
		DynamicLoader loader = new DynamicLoader();
		
		for (int i = 0 ; i < penalties.size(); i++){
			
			LoggerOrPenaltyDefinition penalty = penalties.get(i);
			PenaltyFunction loadedPenalty = (PenaltyFunction) loader.loadPenaltyOrLogger(penalty.getClassName(), true, penalty.getContent());

			if (loadedPenalty.isGlobalPenalty()){
				penaltyFunctionGlobalNetwork.add(loadedPenalty);
				this.penaltyCoef.put(loadedPenalty.getPenaltyName(),loadedPenalty.getCoef());
				loadedPenalty.setNetwork(predictionNetwork);
			}else{
				for (int j = 0 ; j < partition.getNetworks().length-1 ;j++){	
					Network net = partition.getNetworks()[j];
					if(j==0){
						loadedPenalty.setNetwork(net);
						this.penaltyFunctionsPerNetwork.get(net).add(loadedPenalty);

					}else{
						loadedPenalty = (PenaltyFunction) loader.loadPenaltyOrLogger(penalty.getClassName(), true, penalty.getContent());
						loadedPenalty.setNetwork(net);		
						this.penaltyFunctionsPerNetwork.get(net).add(loadedPenalty);
					}
				}			
				this.penaltyCoef.put(loadedPenalty.getPenaltyName(),loadedPenalty.getCoef());
			}

		}
	}
	
	/**
	 * Clears all assigned penalties and coefficients.
	 * 
	 * 
	 */
	
	public void clearNetworks (){
		this.partition.networkClear();
	}

	/**
	 * Returns the calculated fitness score of the network ranking. 
	 * 
	 * @return the current fitness of the network ranking
	 */
	public long getFitness() {
		if (!inited){
			this.modify();
			inited = true;
		}
		return this.currentFitness;
	}


	/**
	 * Commits the changes made by the modifier to all subnetworks.
	 * 
	 */
	public void commit() {
	for (Network net : partition.getNetworks()){
			net.commit();
		}
	}
	
	/**
	 * Reverts the changes made by the modifier to all subnetworks and resets the penaltyfunction scores.
	 * 
	 */
	public void revert() {
		for (Network net : partition.getNetworks()){
			for (PenaltyFunction penaltyFunction: this.penaltyFunctionsPerNetwork.get(net)) {
					penaltyFunction.revertIncrementalPenalty();
			}
				 net.revert();	
		}
	}
	

	/**
	 * Gets the list of modifications made to the ranking and translates them into subnetwork changes. Proceeds to calculate the new penaltyscores and aggregates them with the correct coefficients.
	 * 
	 */
	public void modify() {
	
		this.perNetwork.clear();
		HashSet<Link> modified = modifier.getModified();
		
		for (Link a : modified){
			
			LinkProperties b = a.getLinkProperties();
			int previousPartition = b.getPreviousSet();
			int nextSet = b.getCurrentSet();
			
			// in this case we should add the link to all networks below
			if (previousPartition > nextSet){
				for (int j = previousPartition-1 ; j >= nextSet ; j--){
					this.partition.getNetworks()[j].addNonCommitalLinkToNetwork(a);	
				}

			}
			else{ // previousPartition < nextSet , we should remove the link from all partitions above
				for (int j = previousPartition ; j < nextSet ; j++){
					this.partition.getNetworks()[j].removeNonCommitalLinkToNetwork(a);	
				}
				
			}

		}

		this.modifier.getModified().clear();
		long sum = 0;

		
		perNetwork.clear();
		
		// Per penalty
		for (Network subnet : this.penaltyFunctionsPerNetwork.keySet()){
			double networkCoefMultiplier = this.networkCoef.get(subnet);
			for (PenaltyFunction penaltyFunction: this.penaltyFunctionsPerNetwork.get(subnet)) {
				penaltyFunction.updateIncrementalPenalty();
				long score = penaltyFunction.getPenaltyScore();
				Double perNetScore = this.perNetwork.get(penaltyFunction.getPenaltyName());
				if(perNetScore == null){
					this.perNetwork.put(penaltyFunction.getPenaltyName(), networkCoefMultiplier * score);
				}else{
					this.perNetwork.put(penaltyFunction.getPenaltyName(),  networkCoefMultiplier* score + perNetScore);
				}
			}
		}
		
		
		// Calculate the fitness of the global penalties
		for (PenaltyFunction penalty : this.penaltyFunctionGlobalNetwork){	
			penalty.calculateMetricFromScratch();;
			double multiplierCoefPenalty = this.penaltyCoef.get(penalty.getPenaltyName());
			double tmp = multiplierCoefPenalty * penalty.getPenaltyScore();
			sum += tmp ;
			this.penaltyScoreEach.put(penalty, tmp );
		}
		
		
		// aggregate the structural penalties with the correct weighing coefficients
		for (PenaltyFunction pen : this.penaltyFunctionsPerNetwork.get(partition.getNetworks()[0])){
			double perSum = this.perNetwork.get(pen.getPenaltyName());	
			double norm = this.penaltyFunctionsPerNetwork.keySet().size();
			perSum /= norm;
			 perSum =  this.penaltyCoef.get(pen.getPenaltyName()) * perSum;
			this.penaltyScoreEach.put(pen, perSum );
			sum+=perSum;
		}

		// this is the current fitness
		this.currentFitness = sum;		
		
	}

	/**
	 * Returns the list of pairs consisting of the penalty name and its current fitness score.
	 * 
	 * @return an Arraylist<PenaltyPair> containing all current loaded structural penalties and associated fitness scores
	 */
	public ArrayList<PenaltyPair> getDetailedFitness() {
		ArrayList<PenaltyPair> pairs = new ArrayList<PenaltyPair>();
		for (PenaltyFunction function: this.penaltyScoreEach.keySet()){
			pairs.add(new PenaltyPair(function.getPenaltyName(),this.penaltyScoreEach.get(function)));
		}
		return pairs;
	}




}
