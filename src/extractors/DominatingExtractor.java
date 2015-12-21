package extractors;
import java.util.ArrayList;
import java.util.HashMap;

import javax.management.RuntimeErrorException;

import network.Network;
import network.link.Link;
import network.node.Node;

/**
 * DominatingExtractor is a class responsible for calculating and keeping track of the 'Anti-dominating' metric associated with a network.
 * 
 * The anti-dominating penalty metric is defined as the ratio between the maximum amount of links originating from a same gene in the network and the total amount
 * of links in the network.
 * 
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */

public class DominatingExtractor {

	/**
	 * Map containing the outgoing link count for each node in the network.
	 */
	private HashMap<Node, Integer> tf;
	/**
	 * The network associated with this class.
	 */
	private Network network;
	/**
	 * The maximum amount of outgoing link from the same gene in the network
	 */
	private int dominatorCount;
	/**
	 * Indicates if this class currently has the metric calculated
	 */
	private boolean inited;

	/**
	 * Constructs a DominatingExtractor without an associated network.
	 */
	public DominatingExtractor() {
		this.init();
	}

	/**
	 * Constructs a DominatingExtractor with an associated network and already calculates the metric from scratch.
	 * 
	 * @param network the associated network
	 */
	public DominatingExtractor(Network network) {
		this.network = network;
		this.init();
		this.update();
	}
	
	
	/**
	 * Returns the current anti-dominating metric associated with the network state.
	 * 
	 * @return the anti-dominating metric
	 * 
	 */
	public double getDominatorPercentage() {
		
		// if the metric is not calculated, throw an exception
		if (!inited) {
			throw new RuntimeErrorException(null);
		}
		return (double) this.dominatorCount / (double) this.network.getLinkCount();
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
	 * Calculates the anti-dominating metric from scratch for the currently associated network
	 * 
	 */
	public void update() {
		this.tf.clear();
		int max = 1;
		for (Link a : network.getLinks()) {
			Node b = a.getTf();
			Integer c = null;
			c = this.tf.get(b);
			if (c == null) {
				this.tf.put(b, 1);
			} else {
				if (c + 1 > max) {
					max = c + 1;
				}
				this.tf.put(b, c + 1);
			}
		}
		this.dominatorCount = max;
		this.inited = true;
	}
	/**
	 * Calculates and sets the anti-dominating metric in an incremental way given lists of links added and deleted.
	 * 
	 * @param nonCommittalDeleted a list of links deleted from the current network
	 * @param nonCommitalAdded a list of links added to the network
	 */
	public void incrementalUpdate(ArrayList<Link> nonCommittalDeleted, ArrayList<Link> nonCommitalAdded) {

		for (Link a : nonCommitalAdded) {
			Node b = a.getTf();
			Integer c = null;
			c = this.tf.get(b);
			if (c == null) {
				this.tf.put(b, 1);
			} else {
				this.tf.put(b, c + 1);
			}
		}

		for (Link a : nonCommittalDeleted) {
			Node b = a.getTf();
			Integer c = null;
			c = this.tf.get(b);
			if (c == null || c <= 0) {
				throw new RuntimeErrorException(null);
			}
			this.tf.put(b, c - 1);
		}

		int max = 0;
		for (Node a : this.tf.keySet()) {
			int tmp = this.tf.get(a);
			if (tmp > max) {
				max = tmp;
			}
		}
		this.dominatorCount = max;

	}

	/**
	 * Reverts back to the current state of the network associated with this extractor.
	 * 
	 * Effectively resets the metric to the current state the associated network is in. Discards any changes made by using an incremental updates.
	 * 
	 */
	public void revert() {
		this.update();
	}
	/**
	 * Performs all actions common for each constructor
	 */
	private void init() {
		this.tf = new HashMap<Node, Integer>();
	}


}
