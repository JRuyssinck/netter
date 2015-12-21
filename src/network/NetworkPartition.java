package network;

import java.util.ArrayList;
import java.util.HashMap;

import network.node.Node;
import network.node.NodeCollection;


/**
 * 
 * NetworkPartition represents a ranking split into different network partitions of increasing size. It keeps track of partitions as network object with associated weights.
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class NetworkPartition {

	/**
	 * The network partitions
	 */
	private Network[] networks;
	
	/**
	 * The original complete network which was partioned
	 */
	private Network prediction ;
	
	/**
	 * List of Integers which indicates at which position in the ranking/original network the cuts to partition were made
	 */
	private ArrayList<Integer> partitions;
	
	/**
	 * 
	 * Mapping of each network partition to the associated weight
	 */
	private HashMap<Network,Double> networkCoefMap;
	
	
	/**
	 * Constructs a new NetworkPartition object from a given ranking/original network, but first selecting the top of the ranking (defined by cutoff) and dividing it in X (defined by chuncks) parts of increasing size.
	 * 
	 * @param prediction	The ranking to be partioned
	 * @param cutoff	cutoff indicating the top of the ranking
	 * @param chuncks	how amount of partitions
	 * @param coefs	the coefficients associated with the resulting partitions
	 */
	public NetworkPartition (Network prediction, int cutoff, int chuncks, double coefs){
		
		this.networkCoefMap = new HashMap<Network,Double>();
		this.prediction = prediction;
		
	
		// divide the network into partitions
		this.partitions = new ArrayList<Integer> ();
		ArrayList<Double> coef = new ArrayList<Double> ();
		double coefStart =  coefs * (cutoff/chuncks);
		for (int i = chuncks ; i <= cutoff ; i+=chuncks){			
			partitions.add(i);
			coef.add(coefStart);
			coefStart -= coefs;
		}
		
		// sort to be sure
		prediction.sortNetworkPredictionRank();
		
		// check if partitions are sorted
		int o = -1 ;
		for (int i = 0 ; i < partitions.size(); i++){
			if (partitions.get(i) < o){
				 throw new IllegalArgumentException("Specifiy a sorted partition cut.");
				
			}else{
				o = partitions.get(i);
			}
		}
		
		// check if cutoffs > partition last
		if (cutoff != partitions.get(partitions.size()-1) || cutoff > prediction.getLinkCount()){
			 
			//cutoff = prediction.getLinkCount();
			throw new IllegalArgumentException("Cutoff not set to valid range "+ prediction.getLinkCount() );
		}
		
		this.networks = new Network[partitions.size()];

		// create the networks
		for (int i = 0 ; i <  this.networks.length ; i++){
			 Network network = new Network();

			 networks[i] = network;
			 this.networkCoefMap.put(network, coef.get(i));
		}
		
		int j = 0 ;
		for (int i = 0 ; i < partitions.get(partitions.size()-1) ; i ++){
			
			if (i== partitions.get(j)){
				j++;
			}
			for (int k = j ; k< partitions.size(); k++){
				prediction.getLinks().get(i).getLinkProperties().setCurrentSet(j);
				if (j > 0){
					prediction.getLinks().get(i).getLinkProperties().setLowerBoundSet(partitions.get(j-1));
				}
				else{
					prediction.getLinks().get(i).getLinkProperties().setLowerBoundSet(0);
				}
				prediction.getLinks().get(i).getLinkProperties().setUpperBoundSet(partitions.get(j));

				networks[k].addLinkToNetwork(prediction.getLinks().get(i));
			}
			
		}
		
	}


	/**
	 * This method should be called when the network partition is no longer needed. It deregisters the network context from the NodeCollection to free memory.
	 * 
	 */
	public void networkClear (){

		for (Network net: networks){
			
			// completely remove all references to the temporary network
			for (Node node: NodeCollection.returnAllRegisteredNodes()){
				node.clearNetwork(net);
			}
		}
		// completely remove all references to the temporary network
		for (Node node: NodeCollection.returnAllRegisteredNodes()){
			node.clearNetwork(prediction);
		}		
		
	}


	
	// Getters and setters

	public HashMap<Network, Double> getNetworkCoefMap() {
		return networkCoefMap;
	}

	public void setNetworkCoefMap(HashMap<Network, Double> networkCoefMap) {
		this.networkCoefMap = networkCoefMap;
	}

	public Network[] getNetworks() {
		return networks;
	}

	public void setNetworks(Network[] networks) {
		this.networks = networks;
	}

	public Network getPrediction() {
		return prediction;
	}

	public void setPrediction(Network prediction) {
		this.prediction = prediction;
	}

	public ArrayList<Integer> getPartitions() {
		return partitions;
	}

	public void setPartitions(ArrayList<Integer> partitions) {
		this.partitions = partitions;
	}


}
