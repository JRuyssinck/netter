package extractors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.management.RuntimeErrorException;

import network.Network;
import network.link.Link;
import network.node.Node;

/**
 * TranscriptionFactorExtractor is a class responsible for calculating and keeping track of the 'Regulatory gene function' metric associated with a network.
 * 
 * The regulatory gene network penalty metric is defined as percentage of nodes with atleast one outgoing edge in the network.
 * of links in the network.
 * 
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class TranscriptionFactorExtractor {

	/**
	 * The network associated with this class.
	 */
	private Network network ;
	/**
	 * The total amount of nodes in the network.
	 */
	private int nodeCount ;
	/**
	 * The total amount of nodes that have atleast one outgoing edge.
	 */
	private int tfCount;
	/**
	 * The total amount of nodes in the network in its previous state.
	 */
	private int previousNodeCount ;
	/**
	 * The total amount of nodes that have atleast one outgoing edge.
	 */
	private int previousTfCount;
	/**
	 * Indicates if this class currently has the metric calculated
	 */
	private boolean inited ;
	/**
	 * Is used to keep track of edges during incremental update.
	 */
	private HashMap<Node,Integer> nodeCountListTF ;
	/**
	 * Is used to keep track of edges during incremental update.
	 */
	private HashMap<Node,Integer> nodeCountListTarget ;
	/**
	 * Set of all nodes which are affected by the incremental update.
	 */
	private HashSet<Node> changedNodes;

	/**
	 * Constructs a Transcriptionfactor with no network set.
	 * 
	 */
	public TranscriptionFactorExtractor(){
		this.init();
	}
	
	/**
	 * Constructs a Transcriptionfactor with an associated network and already calculates the metric from scratch.
	 * 
	 * @param network the associated network
	 */
	public TranscriptionFactorExtractor(Network network){	
		this.network = network;
		this.init();
		this.update();		
	}
	
	/**
	 * Returns the amount of nodes with at least one outgoing edge in the network. 
	 * 
	 * @return the amount of nodes with > 0 outgoing edge
	 */
	public int getAmountOfTF(){
		if (!inited){
			throw new RuntimeErrorException(null);
		}
		return this.tfCount;
	}
	
	/**
	 * Returns the amount of nodes in the network. 
	 * 
	 * @return the amount of nodes in the network
	 */
	public int getNodeSize() {
		if (!inited){
			throw new RuntimeErrorException(null);
		}
		return  this.nodeCount;
	}

	/**
	 * Calculates the regulatory gene function metric from scratch for the currently associated network
	 * 
	 */
	public void update (){
		
		this.tfCount = 0;
		for (Node a : this.network.getNodes()){
			if (a.getOutGoingLinks(this.network).size() > 0){
				tfCount++;
			}
		}
		this.nodeCount = this.network.getNodeCount();
		this.previousNodeCount =-1;
		this.previousTfCount = -1;
		this.inited = true;
	}
	
	/**
	 * Returns the network associated with this extractor.
	 * 
	 * @return the associated network
	 * 
	 */
	public Network getNetwork() {
		return network;
	}

	/**
	 * Sets the network associated with this extractor.
	 * 
	 * @param network the network which should be associated with this extractor
	 * 
	 */
	public void setNetwork(Network network) {
		this.network = network;
	}


	/**
	 * Calculates and sets the regulatory gene function metric in an incremental way given lists of links added and deleted.
	 * 
	 * @param nonCommittalDeleted a list of links deleted from the current network
	 * @param nonCommitalAdded a list of links added to the network
	 */
	public void incrementalUpdate(ArrayList<Link> nonCommittalDeleted, ArrayList<Link> nonCommitalAdded) {
		
		// first do the initial count if this has not been done before
		if (!inited){
			this.update();
		}
	
		// clear the hashmaps for the new incremental update
		nodeCountListTF.clear();
		nodeCountListTarget.clear();
		this.changedNodes.clear();
		this.previousNodeCount = this.nodeCount;
		this.previousTfCount = this.tfCount;
		
		// now loop over all deleted edges
		for (int i = 0 ; i < nonCommittalDeleted.size() ; i++){
			
			// get the tf in the deleted edge
			Node trans  = nonCommittalDeleted.get(i).getTf();
			Node target = nonCommittalDeleted.get(i).getTarget();
			
			// first handle the target
			// check if the target is already present in our hashmap
			Integer tar = null;
			tar = nodeCountListTarget.get(target);

			// if not
			if (tar==null){
				// get the amount of incoming edges in the previous network state
				int in = target.getIncomingLinks(network).size();
				// and put this count minus one in the hashmap
				nodeCountListTarget.put(target, in-1);

			}
			// else if its already in the map, lower the count by one
			else{
				nodeCountListTarget.put(target, tar-1);
			}
			
			// now check the tf
			// check if the tf is already present in our hashmap
			Integer s = null;
			s =nodeCountListTF.get(trans);
			
			// if not
			if (s== null){
				// get the amount of outgoing edges in the previous network state
				int out = trans.getOutGoingLinks(network).size();
				// and put this count minus one in the hashmap
				nodeCountListTF.put(trans, out-1);
			// else if its already in the map, lower the count by one
			}else{
				nodeCountListTF.put(trans, s-1);
			}
				
		}
		// now loop over all added edges
		for (int i = 0 ; i < nonCommitalAdded.size() ; i++){
				
				// get the tf from the added link
				Node trans  = nonCommitalAdded.get(i).getTf();
				Node target = nonCommitalAdded.get(i).getTarget();
				
				// first handle the target
				// check if the tf is already present in our hashmap
				Integer tar = null;
				tar = nodeCountListTarget.get(target);

				// if not
				if (tar==null){
					// get the amount of incoming edges in the previous network state
					ArrayList<Node> ok = target.getIncomingLinks(network);
				
					int in = 0;
					if (ok !=null){
						in =ok.size();
					}

					// and put this count minus one in the hashmap
					nodeCountListTarget.put(target, in+1);

				}
				// else if its already in the map, increase the count by one
				else{
					nodeCountListTarget.put(target, tar+1);
				}
				
				
				// now check the tf
				// check if the tf is already present in our hashmap
				Integer s = null;
				s =nodeCountListTF.get(trans);
				
				// if not
				if (s== null){
					// get the amount of outgoing edges in the previous network state
					int out = 0;
					// get the amount of incoming edges in the previous network state
					ArrayList<Node> ok = trans.getOutGoingLinks(network);
					if (ok !=null){
						out =	ok.size();
					}
		
					// and put this count minus one in the hashmap
					nodeCountListTF.put(trans, out+1);
				// else if its already in the map, lower the count by one
				}else{
					nodeCountListTF.put(trans, s+1);
				}	
		}
		this.changedNodes.addAll(nodeCountListTF.keySet());
		this.changedNodes.addAll(this.nodeCountListTarget.keySet());
		for (Node a : changedNodes){
			
			Integer newIncoming = null;
			Integer newOutgoing = null;
			
			Integer oldIncoming = null;
			Integer oldOutgoing = null;
			
			newOutgoing = this.nodeCountListTF.get(a);
			newIncoming = this.nodeCountListTarget.get(a);
			
			// if it didnt change, get the old value
			if (newOutgoing == null){
				if (a.getOutGoingLinks(network)== null){
					newOutgoing = 0;
				}else{
					newOutgoing = a.getOutGoingLinks(network).size();
				}
			}
			
			if (newIncoming == null){
				if (a.getIncomingLinks(network)== null){
					newIncoming = 0;
				}else{
					newIncoming = a.getIncomingLinks(network).size();
				}
			}
			
			if (a.getIncomingLinks(network)== null){
				oldIncoming = 0;
			}else{
				oldIncoming = a.getIncomingLinks(network).size();
			}
			
			if (a.getOutGoingLinks(network)== null){
				oldOutgoing = 0;
			}else{
				oldOutgoing = a.getOutGoingLinks(network).size();
			}
			
			
			if (newIncoming == 0 && newOutgoing == 0 && (oldIncoming != 0 || oldOutgoing !=0)){
				this.nodeCount--;
			}
			
			if ((newIncoming != 0 || newOutgoing != 0) && (oldIncoming == 0 && oldOutgoing ==0)){
				this.nodeCount++;
			}
			
			if (newOutgoing == 0 &&  oldOutgoing !=0){
				this.tfCount--;
			}
			
			if (newOutgoing != 0 &&  oldOutgoing ==0){
				this.tfCount++;
			}
		}
	}
	
	
	/**
	 * Reverts back to the current state of the network associated with this extractor.
	 * 
	 * Effectively resets the metric to the current state the associated network is in. Discards any changes made by using an incremental updates.
	 * 
	 */
	public void revert (){
		this.nodeCount = this.previousNodeCount;
		this.tfCount = this.previousTfCount;
	}
	
	
	// Does all variable initiation common for all constructors
	private void init(){
		
		this.inited = false;
		this.nodeCountListTF = new HashMap<Node,Integer>();
		this.nodeCountListTarget = new HashMap<Node,Integer>();
		this.changedNodes = new HashSet<Node>();
		this.previousNodeCount =-1;
		this.previousTfCount = -1;
		
	}

}
