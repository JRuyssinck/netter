package extractors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import network.Network;
import network.link.Link;
import network.node.Node;

/**
 * GraphletCounterExtractor is a class responsible for calculating and keeping track of all graplet counts associated with a network.
 * 
 * Graphlets have been introduced as small connected non-isomorphic induced subgraphs of a larger network. 
 * They differ from the concept of network motifs by the fact that an induced subgraph needs to contains all the edges between its
 * nodes which are present in the parent network. This class calculates the 4-node graphlet count of an associated network and can do this both from scratch 
 * as in an incremental update. In time this class should be replaced by the IncGraph model which will do this job in a more efficient way.
 * This class will not be further annotated until it has been replaced by IncGraph.
 * 
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */

public class GraphletCounterExtractor {

	private Network network; 
	private ArrayList<ArrayList<Integer>> nodeCounts ;
	private HashMap<Node,Integer> nodeToNodeCount;
	private ArrayList<ArrayList<Integer>> nodeCountsPrevious ;
	private double[] freqSingle ;
	private int[] graphletCounts;
	private HashMap<Node,HashSet<Node>> tabuList;
	private HashMap<Node,HashSet<Node>> addedList;
	private HashSet<Node>  cList ;
	private HashSet<Node>  cListOpposite ;
	private HashMap<Node,ArrayList<Node>>  cListAdd ;
	private HashMap<Node,ArrayList<Node>>  cListOppositeAdd ;
	private HashMap<Node,ArrayList<Node>> sevenLookup;
	private HashMap<Node,ArrayList<Node>> eightLookup;
	private HashMap<Node,ArrayList<Node>> eightLookupAdd;
	private HashMap<Node,ArrayList<Node>> sevenLookupAdd;
	private boolean inited ;	
	private ArrayList<Link> tmpList ;

	
	public GraphletCounterExtractor(Network network) {

		this.network = network;
		this.nodeCounts = new ArrayList<ArrayList<Integer>>();
		this.nodeToNodeCount = new HashMap<Node,Integer>();
		this.freqSingle = new double[9];
		this.graphletCounts = new int[9];
		this.tabuList = new HashMap<Node, HashSet<Node>>();
		this.addedList = new HashMap<Node,HashSet<Node>>();
		this.cList = new HashSet<Node>();
		this.cListOpposite = new HashSet<Node>();
		this.cListAdd = new HashMap<Node,ArrayList<Node>>();
		this.cListOppositeAdd = new HashMap<Node,ArrayList<Node>>();
		this.sevenLookup = new HashMap<Node,ArrayList<Node>>();
		this.eightLookup = new HashMap<Node,ArrayList<Node>> ();
		this.sevenLookupAdd = new HashMap<Node,ArrayList<Node>>();
		this.eightLookupAdd = new HashMap<Node,ArrayList<Node>> ();
		this.nodeCountsPrevious = new ArrayList<ArrayList<Integer>> ();
		this.inited = false;
		this.tmpList = new ArrayList<Link> ();

	}
	
	
	
	public GraphletCounterExtractor (){

		this.nodeCounts = new ArrayList<ArrayList<Integer>>();
		this.nodeToNodeCount = new HashMap<Node,Integer>();
		this.freqSingle = new double[9];
		this.graphletCounts = new int[9];
		this.tabuList = new HashMap<Node, HashSet<Node>>();
		this.addedList = new HashMap<Node,HashSet<Node>>();
		this.cList = new HashSet<Node>();
		this.cListOpposite = new HashSet<Node>();
		this.cListAdd = new HashMap<Node,ArrayList<Node>>();
		this.cListOppositeAdd = new HashMap<Node,ArrayList<Node>>();
		this.sevenLookup = new HashMap<Node,ArrayList<Node>>();
		this.eightLookup = new HashMap<Node,ArrayList<Node>> ();
		this.sevenLookupAdd = new HashMap<Node,ArrayList<Node>>();
		this.eightLookupAdd = new HashMap<Node,ArrayList<Node>> ();
		this.inited = false;
		this.nodeCountsPrevious = new ArrayList<ArrayList<Integer>> ();
		this.tmpList = new ArrayList<Link> ();

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
	 * Calculates the graphlet-counts from scratch for the currently associated network
	 * 
	 */
	public void update(){
		
		

		LinkedHashSet<Node> nodes = network.getNodes();
		int sizeNodes = nodes.size();
		int sizeArray = this.nodeCounts.size();
		if (sizeNodes > sizeArray){
			int diff = sizeNodes-sizeArray;
			
			for (int i =0 ; i < diff ; i++){
				
				ArrayList<Integer> tmp = new ArrayList<Integer>(9);
				
				for(int j = 0 ; j < 9;j++){
					tmp.add(0);
				}
				
		
				
				this.nodeCounts.add(tmp);
			}
		
		}
		else if (sizeArray> sizeNodes){
			
			int diff = sizeArray-sizeNodes;
			for (int i =0 ; i < diff ; i++){
				
				this.nodeCounts.remove(this.nodeCounts.size()-1);
			}
		}
		
		
		
		int i = 0;
		this.nodeToNodeCount.clear();
		for (Node a: network.getNodes()){
			this.nodeToNodeCount.put(a, i);
			
			this.count(a,network,i);
			i++;
		}
		
		
		this.graphletCount();
		this.graphletFrequency();
		
		this.inited = true;

	}

	/**
	 * Returns the graphlet counts associated with the network
	 * 
	 * @return the graphlet counts
	 * 
	 */
	public int[] getGraphletCounts(){
		
		return this.graphletCounts;
	}

	/**
	 * Returns the relative graphlet frequencies associated with the network
	 * 
	 * @return the relative graphlet counts
	 * 
	 */
	public double[] getFrequency() {
		return this.freqSingle;
	}
	/**
	 * Reverts back to the current state of the network associated with this extractor.
	 * 
	 * Effectively resets the metric to the current state the associated network is in. Discards any changes made by using an incremental updates.
	 * 
	 */
	public void revert (){
		
		// make a backup copy
		for (int i = 0 ; i < this.nodeCountsPrevious.size() ; i++){
			
			ArrayList<Integer> tmp ;
			if (i >= this.nodeCounts.size()){
				tmp = new ArrayList<Integer>();
				this.nodeCounts.add(tmp);
			}else{
				tmp = this.nodeCounts.get(i);
			}
			tmp.clear();

			for (int j = 0 ; j < this.nodeCountsPrevious.get(i).size(); j++){
				
				tmp.add(this.nodeCountsPrevious.get(i).get(j));
			}
			
		}
		this.graphletCount();
		this.graphletFrequency();
	}

	
	/**
	 * Calculates and sets graphlet counts and frequencies in an incremental way given lists of links added and deleted
	 * 
	 * @param nonCommittalDeleted a list of links deleted from the current network
	 * @param nonCommittalAdded a list of links added to the network
	 */
	public void incrementalUpdate (ArrayList<Link> nonCommittalDeleted, ArrayList<Link> nonCommittalAdded){
		
		if(!inited){
			this.update();
		}
		
		tmpList.clear();
		for (int x = 0 ; x <  nonCommittalAdded.size(); x++){
			for (int y = 0 ; y <  nonCommittalDeleted.size(); y++){
				Link xx = nonCommittalAdded.get(x);
				Link yy = nonCommittalDeleted.get(y);
				if (xx.isOppositeLink(yy)){
					tmpList.add(nonCommittalAdded.remove(x));
					tmpList.add(nonCommittalDeleted.remove(y));
					x--;
					break;
				}
			}
		}
		this.tabuList.clear();
		this.addedList.clear();
		// make a backup copy
		for (int i = 0 ; i < this.nodeCounts.size() ; i++){
			
			ArrayList<Integer> tmp ;
			if (i >= this.nodeCountsPrevious.size()){
				tmp = new ArrayList<Integer>();
				this.nodeCountsPrevious.add(tmp);
			}else{
				tmp = this.nodeCountsPrevious.get(i);
			}
			tmp.clear();
			for (int j = 0 ; j < this.nodeCounts.get(i).size(); j++){
				tmp.add(this.nodeCounts.get(i).get(j));		}
		}
		// do the deletes
		this.incrementalDelete(nonCommittalDeleted, this.network);
		// Now do the adds
		this.incrementalAdd(nonCommittalAdded,this.network);
		for (int i = 0 ; i < this.tmpList.size(); i+=2){
			nonCommittalAdded.add(tmpList.get(i));
			nonCommittalDeleted.add(tmpList.get(i+1));
		}
		
		tmpList.clear();
		this.graphletCount();
		this.graphletFrequency();

		
	}
	
	
	private void graphletFrequency() {


		double sum1 = 0;
		double sum2 = 0;
		
		this.freqSingle[0] = 100; // 2-node frequency is always 100%
		
		for (int i =1; i < 3;i++){
			sum1+= this.graphletCounts[i];
		}
		for (int i =3;i<this.graphletCounts.length;i++){
			sum2+= this.graphletCounts[i];
		}
		
		
		if (sum1==0){
			for (int i =1;i<3;i++){
				this.freqSingle[i] = 0;
			}
		}else{
			for (int i =1;i<3;i++){
				this.freqSingle[i] = ((double)this.graphletCounts[i]) / sum1;
			}
		}
		
		if (sum2==0){
			for (int i =1;i<3;i++){
				this.freqSingle[i] = 0;
			}
		}else{
			
			for (int i =3;i<this.graphletCounts.length;i++){
				this.freqSingle[i] = ((double)this.graphletCounts[i]) / sum2;
			}
			
		}
	}
	
	

	private void graphletCount (){
		
		for (int i = 0 ; i < this.freqSingle.length; i++ ){
			this.graphletCounts[i] = 0;
		}
		
		for (int i = 0 ; i < this.nodeCounts.size(); i++){
			
			
			ArrayList<Integer> th = this.nodeCounts.get(i);
			
			
			this.graphletCounts[0] += th.get(0);
			this.graphletCounts[1] += th.get(1);
			this.graphletCounts[2] += th.get(2);
			this.graphletCounts[3] += th.get(3);
			this.graphletCounts[4] += th.get(4);
			this.graphletCounts[5] += th.get(5);
			this.graphletCounts[6] += th.get(6);
			this.graphletCounts[7] += th.get(7);
			this.graphletCounts[8] += th.get(8);


			
		}
		
		
		
		this.graphletCounts[0] = this.graphletCounts[0] / 2;
		this.graphletCounts[1] = this.graphletCounts[1] / 2;
		this.graphletCounts[2] = this.graphletCounts[2] / 3;
		this.graphletCounts[3] = this.graphletCounts[3] / 2;
		this.graphletCounts[4] = this.graphletCounts[4] / 3;
		this.graphletCounts[5] = this.graphletCounts[5] / 4;
		this.graphletCounts[6] = this.graphletCounts[6];
		this.graphletCounts[7] = this.graphletCounts[7] / 2;
		this.graphletCounts[8] = this.graphletCounts[8] / 4;
		
		


		
		
	}
	
	
private void count(Node a,Network networkArg, int indexArray)  {

		
		ArrayList<Integer> ret = this.nodeCounts.get(indexArray);
		
		
		for (int i = 0 ; i < 9 ; i++){
			ret.set(i, 0);
		}
		
		Set<Node> neighboursA = a.getUnidirectedLinks(networkArg);

		for (Node b : neighboursA) {

			ret.set(0,ret.get(0)+1);

			Set<Node> neighboursB = b.getUnidirectedLinks(networkArg);
			for (Node c : neighboursB) {

				if (c.equals(a)) {
					continue;
				}

				Set<Node> neighboursC = c.getUnidirectedLinks(networkArg);

				
				
				if (neighboursA.contains(c)) {
					ret.set(2,ret.get(2)+1);

					for (Node d : neighboursC) {

						if (d.equals(a)) {
							continue;
						}
						if (d.equals(b)) {
							continue;
						}
						if (neighboursA.contains(d)){
							if (neighboursB.contains(d)) {
								ret.set(8,ret.get(8)+1);
				//				System.out.println(a+"="+b+"="+"="+c+"="+d);
							// fully connected
							} else {
								ret.set(7,ret.get(7)+1);
							}
							
						}



					}

				} else { // chain
					ret.set(1,ret.get(1)+1);

					// extend towards graphlet g4

					for (Node cc : neighboursB) {

						if (cc.equals(c)) {
							continue;
						}
						if (cc.equals(a)) {
							continue;
						}

						if (neighboursA.contains(cc)) {
							continue;
						}
						if (neighboursC.contains(cc)) {
							continue;
						}

						ret.set(4,ret.get(4)+1);
					}

					// extend towards graphlet 3
					for (Node d : neighboursC) {
						// d cannot be a, because we already established that
						// neighbours a does not contain c
						if (d.equals(b)) {
							continue;
						}
						// G6 [9]
						if (neighboursB.contains(d)) {
							// G7
							if (neighboursA.contains(d)) {
								// G6
							} else {
								ret.set(6,ret.get(6)+1);
							}
						} else {
							// Check if D is in neighbours A
							if (neighboursA.contains(d)) {
								ret.set(5,ret.get(5)+1);
							} else {
								ret.set(3,ret.get(3)+1);
							}

						}
					}

				}
			}

			// check for 4-node graphlets

		}// end a

		
		adjustForOverCountingPerNode(ret);


	}

	private void adjustForOverCountingPerNode(ArrayList<Integer> counts) {


		counts.set(2, counts.get(2)/2) ;
		counts.set(4, counts.get(4)/2) ;
		counts.set(5, counts.get(5)/2) ;
		counts.set(6, counts.get(6)/2) ;
		counts.set(7, counts.get(7)/2) ;
		counts.set(8, counts.get(8)/6) ;


	}
	
	private void incrementalDelete(ArrayList<Link> deleted, Network net){
		
		
		// first make adjustments for the deleted links
		
		for (int i  = 0 ; i < deleted.size(); i++){
			
			Link link = deleted.get(i);

			// get some variables
			Node tf = link.getTf();
			Node target = link.getTarget();
			ArrayList<Integer> tfCounts = nodeCounts.get(nodeToNodeCount.get(tf));
			ArrayList<Integer> targetCounts = nodeCounts.get(nodeToNodeCount.get(target));
			
			
			// clear the list for the g8 graplet
			
			this.eightLookup.clear();
			this.sevenLookup.clear();
			this.cList.clear();
			this.cListOpposite.clear();

			
			// first check if it was a bidirectional link, if so, nothing changed really
			boolean cont = true;
			if (tf.getBiDirectionalLinks(network).contains(target)){
				cont = false;
				// check if both links are deleted, in this case, something changes after all
				for (int j = i+1 ; j < deleted.size();j++){ // this way, the second time, no changes will be made
					Link linkOpposite = deleted.get(j);
					if (linkOpposite.isOppositeLink(link)){
						cont = true; 
						break;
					}
				}
			}
			if (!cont){continue;}
			
			
			// if not, the G0 graphlet is of course ones less for both
			tfCounts.set(0,tfCounts.get(0)-1);
			targetCounts.set(0,targetCounts.get(0)-1);

			
			// G1 graphlet and G2 graphlet
			// this is COMPLICATED
			// since the links are undirected, we should consider both nodes and the effects on them
			// Removing a link from a graphlet 
			// we only count the one starting at node 1 (black node) 
			// but we also kill another 'orbit' by removing this link, more specific the graphlet ending at TF
			// lets check which nodes are affected : all the neighbours of B
			Set<Node> neighboursB = target.getUnidirectedLinks(net);
			HashSet<Node> tabuB = this.tabuList.get(target);
			HashSet<Node> tabuA = this.tabuList.get(tf);

			int affectedGraphletG1 = 0;
			int affectedGraphletG2 = 0;
			
			for (Node c : neighboursB) {
				// if c is the tf
				if (c.equals(tf)){ continue;}
				// if c is no longer connected because of previous adjustments
				if (tabuB != null){
					if (tabuB.contains(c)){continue;}
				}
				
				// add the current c to the cList, causing it to no longer being able a D
				this.cList.add(c);
				
				Set<Node> neighboursC = c.getUnidirectedLinks(net);

				if (neighboursC.contains(tf) && (tabuA == null ||  !tabuA.contains(c))){
				
					affectedGraphletG2 += this.triangleCaseTF(net, tf, target, c, tfCounts, targetCounts, neighboursB, neighboursC, tabuA, tabuB);
						
				}
				else{

					affectedGraphletG1 = this.chainCaseTF(affectedGraphletG1,net, tf, target, c, tfCounts, targetCounts, neighboursB, neighboursC, tabuA, tabuB);

				}
				
				
			}
			// lower by the amount of affected nodes
			tfCounts.set(1, tfCounts.get(1)-affectedGraphletG1);

			tfCounts.set(1, tfCounts.get(1)+affectedGraphletG2);
			targetCounts.set(1,targetCounts.get(1)+ affectedGraphletG2);
			
			
			
			
		
			// now do the same but with target as starting node
			Set<Node> neighboursA = tf.getUnidirectedLinks(net);
			affectedGraphletG1 = 0;
			affectedGraphletG2= 0;
			for (Node c : neighboursA) {
				if (c.equals(target)) {continue;}
				if (tabuA != null) {if (tabuA.contains(c)) {continue;}
				
				}
				if (!(c.getUnidirectedLinks(net).contains(target) && (tabuB == null || !tabuB.contains(c)))) {

					// add the current c to the cList, causing it to no longer being able a D
					this.cListOpposite.add(c);
					
					affectedGraphletG1++;
					ArrayList<Integer> cG1 = nodeCounts.get(this.nodeToNodeCount.get(c));
					// lower this count
					cG1.set(1, cG1.get(1) - 1);

					/**
					 * 
					 * 4-NODE GRAPHLETS : C NOT CONNECTED TO TARGET
					 * 
					 */
					Set<Node> neighboursC = c.getUnidirectedLinks(net);

					for (Node d : neighboursC) {

						// continue if not 4 distinct nodes
						if (d.equals(target) || d.equals(tf)) {
							continue;
						}

						// continue if d is no longer a neighbour of c
						HashSet<Node> tabuC = this.tabuList.get(c);
						if (tabuC != null && tabuC.contains(d)) {
							continue;
						}

						if (cListOpposite.contains(d)){continue;}
						// else valid nodes

						// we only need to check the cases in which d is not
						// connected to target

						Set<Node> neighboursD = d.getUnidirectedLinks(net);
						if (neighboursD.contains(target) && (tabuB == null || !tabuB.contains(d))) {

							// nothing to do here

						} else {

							if (neighboursD.contains(tf)&& (tabuA == null || !tabuA.contains(d))) {

								// remove
								targetCounts.set(6, targetCounts.get(6) - 1);
								
							//	System.err.println(tf+" "+target+" "+c+" "+d+ " "+ "TARGET-C- d connected tf");


							}
							// long chain
							else {

								// remove
								ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
								dd.set(3, dd.get(3) - 1);
								targetCounts.set(3, targetCounts.get(3) - 1);
								
							//	System.err.println(tf+" "+target+" "+c+" "+d+ " "+ "TARGET-C- d not connected tf");


							}

						}

						/**
						 * 
						 * END 4-NODE GRAPHLET
						 * 
						 */

					} // also check for these
					
					for (Node d : neighboursA) {

						// continue if not 4 distinct nodes
						if (d.equals(target) || d.equals(c)) {
							continue;
						}

						// continue if d is no longer a neighbour of tf
						if (tabuA != null && tabuA.contains(d)) {
							continue;
						}
						
						
						// d, has already been a D
						if (this.cListOpposite.contains(d)){continue;}
						

						// else valid nodes

						// we only need to check the cases in which d is not
						// connected to target

						Set<Node> neighboursD = d.getUnidirectedLinks(net);
						if (neighboursD.contains(target) && (tabuB == null || !tabuB.contains(d))) {

							

						} else {
							HashSet<Node> tabuC = this.tabuList.get(c);

							if (neighboursD.contains(c)&& (tabuC == null || !tabuC.contains(d))) {


							}
							// g4
							else {

								// remove
								ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
								ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));

								dd.set(4, dd.get(4) - 1);
								cc.set(4, cc.get(4) - 1);
								targetCounts.set(4, targetCounts.get(4) - 1);
								


					//			System.err.println(tf+" "+target+" "+c+" "+d+ " "+ "TARGET-A- d not con c");

							}

						}

						/**
						 * 
						 * END 4-NODE GRAPHLET
						 * 
						 */

					} 
				
					
				

					

					// there is no else, nothing to do

				}
				else{
					
					
//					// 
//					Set<Node> neighboursC =c.getUnidirectedLinks(net);
//					
//					for(Node d: neighboursC){
//						
//						if (d.equals(tf)){continue;}
//						if (d.equals(target)){continue;}
//
//						
//						
//						
//					}
//					
					
					
//				}
					
				}

			}
			// lower by the amount of affected nodes
			targetCounts.set(1, targetCounts.get(1)-affectedGraphletG1);



			
			// we should keep track of the fact that from now on, this link no longer exists, as such, we should add it to the tabu list of the nodes
			HashSet<Node> tab = null;
			tab = this.tabuList.get(target);
			if (tab==null){
				HashSet<Node> tmp = new HashSet<Node>();
				tmp.add(tf);
				this.tabuList.put(target, tmp);
			}else{
				tab.add(tf);
			}
			
			tab = null;
			tab =this.tabuList.get(tf);
			if (tab==null){
				HashSet<Node> tmp = new HashSet<Node>();
				tmp.add(target);
				this.tabuList.put(tf, tmp);
			}else{
				tab.add(target);
			}
			

			
			
			
			
			
		}
		
		
		
	}
	
	private int chainCaseTF (int affectedGraphletG1,Network net, Node tf, Node target, Node c, ArrayList<Integer >tfCounts,ArrayList<Integer> targetCounts, Set<Node> neighboursB,Set<Node> neighboursC, HashSet<Node> tabuA, HashSet<Node> tabuB){
		
		
		
		// this means there is no connection between tf and c after all
		affectedGraphletG1++;
		ArrayList<Integer> cG1 = nodeCounts.get(this.nodeToNodeCount.get(c));
		// lower this count
		cG1.set(1, cG1.get(1)-1);
		 // also check for these
		
		
		/**
		 * 
		 *  4-NODE GRAPHLETS : C NOT CONNECTED TO TF
		 * 
		 */
		// NOW CHECK FOR 4 node graphlets , C not connected to TF
		
		
		// Check all cases with a neighbour of C
		this.chainCaseNeighboursCTF(affectedGraphletG1, net, tf, target, c, tfCounts, targetCounts, neighboursB, neighboursC, tabuA, tabuB);
		
		// Check all cases with a neighbour of B
		this.chainCaseNeighboursBTF(affectedGraphletG1, net, tf, target, c, tfCounts, targetCounts, neighboursB, neighboursC, tabuA, tabuB);
		
		// Check all cases with a neighbour of C
		Set<Node> neighboursA = tf.getUnidirectedLinks(net);
		this.chainCaseNeighboursATF(affectedGraphletG1, net, tf, target, c, tfCounts, targetCounts, neighboursA, neighboursB, neighboursC, tabuA, tabuB);
		
		
	
		
		/**
		 * 
		 * END 4 NODE GRAPHLET
		 */
		
		return affectedGraphletG1;
		
	}
	
	private void chainCaseNeighboursCTF (int affectedGraphletG1,Network net, Node tf, Node target, Node c, ArrayList<Integer >tfCounts,ArrayList<Integer> targetCounts, Set<Node> neighboursB,Set<Node> neighboursC, HashSet<Node> tabuA, HashSet<Node> tabuB){
		
		HashSet<Node> tabuC = this.tabuList.get(c);
		for (Node d : neighboursC){
			
			// could be target, can also be tf because we are in the tabu case
			if (d.equals(target)){continue;}
			if (d.equals(tf)){continue;}
			
			// in this case, node D, isn't really a  neighbour at all
			if (tabuC != null && tabuC.contains(d)){
				continue;
			}
			
			if (cList.contains(d)){continue;}
			
			// from here on , we have a valid D
			// now check which case applies
			
			Set<Node> neighboursD = d.getUnidirectedLinks(net);
			
			// this is the case where there is a link between D and TF
			if (neighboursD.contains(tf) && (tabuA ==null || !tabuA.contains(d)) ){
				
				// check if link between D and target, if so this
				if (neighboursD.contains(target) && (tabuB ==null || !tabuB.contains(d))){
					
					/**
					 * 
					 * This is the case where there is a  G7 graphlet starting in 
					 *     target 
					 *     D is destroyed
					 * 
					 * and becomes a G6 graphlet starting in TF 
					 */
					ArrayList<Node> y = null;
					y = sevenLookup.get(c);
					if (y!=null && y.contains(d)){continue;}
						
						
					y = null;
					y = sevenLookup.get(d);
					if (y==null){
						
						ArrayList<Node> tmp = new ArrayList<Node>();
						tmp.add(c);
						sevenLookup.put(d, tmp);
						
					}else{
						
						y.add(c);
						
					}
					
					
					ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
					// lower this count
					dd.set(7, dd.get(7)-1);
					targetCounts.set(7, targetCounts.get(7)-1);
					// add this count
					tfCounts.set(6, tfCounts.get(6)+1);
					
				//	System.err.println(tf+" "+target+" "+c+" "+d+ " "+ "chainCaseNeighboursCTF- d connected target - d connected tf");
					
				}
				// d not connected to target
				else{
					
					
					/**
					 * 
					 * This is the case where there  where there is a square G5, square is destroyed in
					 *     tf, target, c and D
					 *    
					 * 
					 *  and becomes a long chain graphlet G3, in both tf and target
					 */
					ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
					ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));

					// lower this count
					dd.set(5, dd.get(5)-1);
					cc.set(5, cc.get(5)-1);
					targetCounts.set(5, targetCounts.get(5)-1);
					tfCounts.set(5, tfCounts.get(5)-1);

					// add this count
					tfCounts.set(3, tfCounts.get(3)+1);
					targetCounts.set(3, targetCounts.get(3)+1);
					
			//		System.err.println(tf+" "+target+" "+c+" "+d+ " "+ "chainCaseNeighboursCTF- d not  connected target - d connected tf");


					
				}
				
			}
			// this is the case where there is NO link between D and TF
			else{
				
				// connected to target

				if (neighboursD.contains(target) && (tabuB ==null || !tabuB.contains(d))){
					
					
					/**
					 * 
					 * This is the case where there is a G6 graphlet starting in tf
					 *     
					 *    tf is destroyed
					 * 
					 *   and there are no new graphlets created because the graph becomes disjunct
					 */
					tfCounts.set(6, tfCounts.get(6)-1);


					
					
				}
				// d not connected to target

				else{
					
					
					/**
					 * This is the case where there is a long chain (graphlet g3)
					 *     
					 *    tf and d are destroyed
					 * 
					 *   and there are no new graphlets created because the graph becomes disjunct
					 */
					
					tfCounts.set(3, tfCounts.get(3)-1);
					ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
					dd.set(3, dd.get(3)-1);

					
					
					
				}
				
			}
			
			
		}
		
		
	}
	
	private void chainCaseNeighboursBTF(int affectedGraphletG1,Network net, Node tf, Node target, Node c, ArrayList<Integer >tfCounts,ArrayList<Integer> targetCounts, Set<Node> neighboursB,Set<Node> neighboursC, HashSet<Node> tabuA, HashSet<Node> tabuB){
		
		for (Node d:  neighboursB){
			//  check if the node can exist
			if (d.equals(target)){continue;}
			if (d.equals(tf)){continue;}
			if (d.equals(c)){continue;}
			// check if the current D has already been a C, in which case, everything has already been calculated
			if (this.cList.contains(d)){continue;}
			
			

			// check if d is still a neighbour
			if (tabuB !=null && tabuB.contains(d)){continue;}
			
			// also check if d is not a neighbour of C
			HashSet<Node> tabuC = this.tabuList.get(c);
			if (neighboursC.contains(d)  && (tabuC ==null || !tabuC.contains(d))){continue;}
			
			// in this case, we have a valid d
			
		
			
			// !!!!!!!!!!!!!! WE NEED TO PAY SPECIAL ATTENTION TO THE FACT THAT C AND D ARE SYMETRIC
			// !!!!!!!!!!!!!! this causes our counts to double
			Set<Node> neighboursD = d.getUnidirectedLinks(net);
			if (neighboursD.contains(tf) && (tabuA == null || !tabuA.contains(d))){
				
				

				// i'm not sure if we already counted this case
				
				// check the case where TF is not connected to C and D is not connected to C 
				// !!!!!!!!!!!!!! WE NEED TO PAY SPECIAL ATTENTION TO THE FACT THAT C AND D ARE SYMETRIC
				// !!!!!!!!!!!!!! this causes our counts to double
				
				/**
				 * 
				 * 
				 * This is the case where there is a  G6 graphlet in 
				 * 		C
				 *   
				 *   
				 *   this is destroyed and  a chain appears as replacement in
				 *   
				 *   C and TF
				 * 
				 */
				ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));


				// lower this count
				cc.set(6, cc.get(6)-1);
				// add this count
				tfCounts.set(3, tfCounts.get(3)+1);
				cc.set(3, cc.get(3)+1);



				
			}
			else{
				
				

				// check the case where TF is not connected to C and D is not connected to C 
				// !!!!!!!!!!!!!! WE NEED TO PAY SPECIAL ATTENTION TO THE FACT THAT C AND D ARE SYMETRIC
				// !!!!!!!!!!!!!! this causes our counts to double
				
				/**
				 * 
				 * 
				 * This is the case where there is a G4 graphlet in
				 *   TF D and C
				 *   
				 *   this is destroyed and no graphlets appear as a replacement as the graph is disjunct
				 * 
				 */
				
				ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
				ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));

				// lower this count
				dd.set(4, dd.get(4)-1);
				cc.set(4, cc.get(4)-1);
				tfCounts.set(4, tfCounts.get(4)-1);
			//	System.err.println(tf+" "+target+" "+c+" "+d+ " "+ "chainCaseNeighboursBTF- d not connected tf");

				// add this count

			
			}
		}
		
	}
	
	private void chainCaseNeighboursATF(int affectedGraphletG1,Network net, Node tf, Node target, Node c, ArrayList<Integer >tfCounts,ArrayList<Integer> targetCounts, Set<Node> neighboursA, Set<Node> neighboursB,Set<Node> neighboursC, HashSet<Node> tabuA, HashSet<Node> tabuB){

		for (Node d:  neighboursA){
			//  check if the node can exist
			if (d.equals(target)){continue;}
			if (d.equals(tf)){continue;}
			if (d.equals(c)){continue;}


			
			
			// check if d is still a neighbour
			if (tabuA !=null && tabuA.contains(d)){continue;}
			
			


			// valid d
			
			
			// d cannot be connected to anything else but A
			HashSet<Node> tabuC = this.tabuList.get(c);

			if (neighboursC.contains(d) && (tabuC == null || !tabuC.contains(d))){continue;}
			if (neighboursB.contains(d) && (tabuB == null || !tabuB.contains(d))){continue;}
			

			// if we are still here, we have a kite motif G 6, starting in d
			
			
			/**
			 * 
			 * This is the case there is a G3 graphlet starting at 
			 *     D
			 *     C
			 * 	   
			 *is destroyed
			 * 	
			 */
			ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
			ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));

			// remove

			dd.set(3, dd.get(3)-1);
			cc.set(3, cc.get(3)-1);
			
		//	System.err.println(tf+" "+target+" "+c+" "+d+ " "+ "chainCaseNeighboursATF");


		}
		
	}
	
	private int triangleCaseTF (Network net, Node tf, Node target, Node c, ArrayList<Integer >tfCounts,ArrayList<Integer> targetCounts, Set<Node> neighboursB, Set<Node> neighboursC, HashSet<Node> tabuA, HashSet<Node> tabuB){
		
		
		int affectedGraphletG2= 0;

		// this means, a triangle is cut, and as such, a G1 graphlet appears at A and B

		// also, for all node sin the triangle, the triagle is cut
		tfCounts.set(2, tfCounts.get(2)-1);
		targetCounts.set(2, targetCounts.get(2)-1);
		ArrayList<Integer> cG1 = nodeCounts.get(this.nodeToNodeCount.get(c));
		cG1.set(2, cG1.get(2)-1);

		affectedGraphletG2++;
		
		
		/**
		 * 
		 *  4-NODE GRAPHLETS : C CONNECTED TO TF
		 * 
		 */
		
		HashSet<Node> tabuC = this.tabuList.get(c);
		
		this.triangleCaseNeighboursCTF(tabuC,net, tf, target, c, tfCounts, targetCounts, neighboursB, neighboursC, tabuA, tabuB);
		
		this.triangleCaseNeighboursBTF(tabuC, net, tf, target, c, tfCounts, targetCounts, neighboursB, neighboursC, tabuA, tabuB);

		
		
		this.triangleCaseNeighboursATF(tabuC,net, tf, target, c, tfCounts, targetCounts, neighboursB, neighboursC, tabuA, tabuB);
		
				
		
	
		
		
		/**
		 * 
		 *  4-NODE GRAPHLETS : C CONNECTED TO TF
		 * 
		 */
		
		return affectedGraphletG2;
		
		
	}
	
	private void triangleCaseNeighboursCTF (HashSet<Node> tabuC,Network net, Node tf, Node target, Node c, ArrayList<Integer >tfCounts,ArrayList<Integer> targetCounts, Set<Node> neighboursB,Set<Node> neighboursC, HashSet<Node> tabuA, HashSet<Node> tabuB){
		
		
		for (Node d : neighboursC){
			
			// could be target, can also be tf because we are in the tabu case
			if (d.equals(target)){continue;}
			if (d.equals(tf)){continue;}
			
			// in this case, node D, isn't really a  neighbour at all
			if (tabuC != null && tabuC.contains(d)){
				continue;
			}
			
			// from here on , we have a valid D
			// now check which case applies
			
			Set<Node> neighboursD = d.getUnidirectedLinks(net);
			
			// this is the case where there is a link between D and TF
			if (neighboursD.contains(tf) && (tabuA ==null || !tabuA.contains(d)) ){
				
				// check if link between D and target, if so this
				if (neighboursD.contains(target) && (tabuB ==null || !tabuB.contains(d))){
			
					
					ArrayList<Node> y = null;
					y = eightLookup.get(c);
					if (y!=null && y.contains(d)){continue;}
						
						
					y = null;
					y = eightLookup.get(d);
					if (y==null){
						
						ArrayList<Node> tmp = new ArrayList<Node>();
						tmp.add(c);
						eightLookup.put(d, tmp);
						
					}else{
						
						y.add(c);
						
					}

					
					/**
					 * 
					 * This is the case there is G 8 graphlet in all
					 *     TF, target, C and D
					 *     
					 * 
					 * and we create a new G7 
					 * in c and d
					 * 
					 * 
					 */
					ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));
					ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
					// remove
					cc.set(8, cc.get(8)-1);
					dd.set(8, dd.get(8)-1);
					tfCounts.set(8, tfCounts.get(8)-1);
					targetCounts.set(8, targetCounts.get(8)-1);
					
					// add
					cc.set(7, cc.get(7)+1);
					dd.set(7, dd.get(7)+1);
					
					//System.err.println(tf+" "+target+" "+c+" "+d+ " "+ "triangleCaseNeighboursCTF- d connected target - d connected tf");

					

					
				}else{
					
					
					/**
					 * 
					 * This is the case there is a   G7 graphlet in
					 *     TF
					 *     C
					 * 
					 * and we create a new G6 graphlet
					 * in target
					 * 
					 * 
					 */
					
					// remove
					ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));
					cc.set(7, cc.get(7)-1);
					tfCounts.set(7, tfCounts.get(7)-1);

					// add
					targetCounts.set(6, targetCounts.get(6)+1);
					
					
				//	System.err.println(tf+" "+target+" "+c+" "+d+ " "+ "triangleCaseNeighboursCTF- d not connected target - d connected tf");

					
					
					
				}
				
				
				
			}else{
				
				// check if link between D and target, if so this
				if (neighboursD.contains(target) && (tabuB ==null || !tabuB.contains(d))){
			
					
					ArrayList<Node> y = null;
					y = sevenLookup.get(c);
					if (y!=null && y.contains(d)){continue;}
						
						
					y = null;
					y = sevenLookup.get(d);
					if (y==null){
						
						ArrayList<Node> tmp = new ArrayList<Node>();
						tmp.add(c);
						sevenLookup.put(d, tmp);
						
					}else{
						
						y.add(c);
						
					}
					
					/**
					 * 
					 * This is the case there is a  G7 graphlet in
					 *     TAR
					 *     C
					 * 
					 * and we create a new G6 graphlet in
					 * 	   TF
					 * 
					 * 
					 */
					
					ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));
					// remove
					cc.set(7, cc.get(7)-1);
					targetCounts.set(7, targetCounts.get(7)-1);
					// add
					tfCounts.set(6, tfCounts.get(6)+1);

				//	System.err.println(tf+" "+target+" "+c+" "+d+ " "+ "COMMENT=triangleCaseNeighboursCTF- d connected target - d not connected tf");

					
					
				}else{
					

					/**
					 * 
					 * This is the case there is a G6 graphlet starting at D 
					 *     D is destroyed
					 * 
					 * and spawns a lot of G4 graphlets 
					 * TF
					 * TAR
					 * D
					 */
					
					
					// remove
					ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
					dd.set(6, dd.get(6)-1);
					
					// add
					dd.set(4, dd.get(4)+1);
					tfCounts.set(4, tfCounts.get(4)+1);
					targetCounts.set(4, targetCounts.get(4)+1);

			//		System.err.println(tf+" "+target+" "+c+" "+d+ " "+ "triangleCaseNeighboursCTF- d not connected target - d not connected tf");

					
				}
				
				
			}
		
		}
		
		
	}
	
	private void triangleCaseNeighboursBTF (HashSet<Node> tabuC,Network net, Node tf, Node target, Node c, ArrayList<Integer >tfCounts,ArrayList<Integer> targetCounts, Set<Node> neighboursB,Set<Node> neighboursC, HashSet<Node> tabuA, HashSet<Node> tabuB){

		
		for (Node d:  neighboursB){
			//  check if the node can exist
			if (d.equals(target)){continue;}
			if (d.equals(tf)){continue;}
			if (d.equals(c)){continue;}
			// check if the current D has already been a C, in which case, everything has already been calculated
			if (this.cList.contains(d)){continue;}
			
			// check if d is still a neighbour
			if (tabuB !=null && tabuB.contains(d)){continue;}
			
			// also check if d is not a neighbour of C
			
			if (neighboursC.contains(d)  && (tabuC ==null || !tabuC.contains(d))){continue;}
			
			// in this case, we have a valid d
			
		
			
			// !!!!!!!!!!!!!! WE NEED TO PAY SPECIAL ATTENTION TO THE FACT THAT C AND D ARE SYMETRIC
			// !!!!!!!!!!!!!! this causes our counts to double
			Set<Node> neighboursD = d.getUnidirectedLinks(net);
			if (neighboursD.contains(tf) && (tabuA == null || !tabuA.contains(d))){
				
				
				/**
				 * 
				 * This is the case there is a G7 graphlet starting at 
				 *     target
				 * 	   tf
				 *is destroyed and becomes a square
				 * TF
				 * TAR
				 * C
				 * D
				 */


				
				ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));
				ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
				// remove
				targetCounts.set(7, targetCounts.get(7)-1);
				tfCounts.set(7, tfCounts.get(7)-1);

				// add
				cc.set(5, cc.get(5)+1);
				dd.set(5, dd.get(5)+1);
				targetCounts.set(5, targetCounts.get(5)+1);
				tfCounts.set(5, tfCounts.get(5)+1);

			//	System.err.println(tf+" "+target+" "+c+" "+d+ " "+ "triangleCaseNeighboursBTF- - d connected tf");

				

				
			}
			else{
				
				/**
				 * 
				 * This is the case there is a G6 graphlet starting at 
				 *     D
				 * 	   
				 *is destroyed and long chain at 
				 * 	TF
				 *	 D
				 */

				ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
				// remove
				dd.set(6, dd.get(6)-1);

				


				// add
				tfCounts.set(3, tfCounts.get(3)+1);
				dd.set(3, dd.get(3)+1);
		//		System.err.println(tf+" "+target+" "+c+" "+d+ " "+ "triangleCaseNeighboursBTF- d connected tf");

			
			}
		}
		
	}

	private void triangleCaseNeighboursATF (HashSet<Node> tabuC,Network net, Node tf, Node target, Node c, ArrayList<Integer >tfCounts,ArrayList<Integer> targetCounts,Set<Node> neighboursB,Set<Node> neighboursC, HashSet<Node> tabuA, HashSet<Node> tabuB){
		
		Set<Node> neighboursA = tf.getUnidirectedLinks(net);
		for (Node d:  neighboursA){
			//  check if the node can exist
			if (d.equals(target)){continue;}
			if (d.equals(tf)){continue;}
			if (d.equals(c)){continue;}

			
			
			// check if d is still a neighbour
			if (tabuA !=null && tabuA.contains(d)){continue;}


			// valid d
			
			
			// d cannot be connected to anything else but A

			if (neighboursC.contains(d) && (tabuC == null || !tabuC.contains(d))){continue;}
			if (neighboursB.contains(d) && (tabuB == null || !tabuB.contains(d))){continue;}
			

			// if we are still here, we have a kite motif G 6, starting in d
			
			
			/**
			 * 
			 * This is the case there is a G6 graphlet starting at 
			 *     D
			 * 	   
			 *is destroyed and long chain at 
			 * 	target
			 *	 D
			 */
			ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
			// remove
			dd.set(6, dd.get(6)-1);
			
			// add
			targetCounts.set(3, targetCounts.get(3)+1);
			dd.set(3, dd.get(3)+1);
			
			//System.err.println(tf+" "+target+" "+c+" "+d+ " "+ "triangleCaseNeighboursATF-");

			
		}
			
			// in this case, we have a valid d
		
	
		
	
		
	}
	
	private void incrementalAdd(ArrayList<Link> nonCommittalAdded,Network net){
		

		
		// second, make adjustments for the added links
		for (int i = 0 ; i < nonCommittalAdded.size(); i++){
			
			
			Link link = nonCommittalAdded.get(i);
			

			// clear the lists
			this.eightLookupAdd.clear();
			this.sevenLookupAdd.clear();
			this.cListAdd.clear();
			this.cListOppositeAdd.clear();
			
			
			// get some variables
			Node tf = link.getTf();
			Node target = link.getTarget();
			
			Integer x = nodeToNodeCount.get(tf);
			Integer y = nodeToNodeCount.get(target);
			
			if (x==null){
			//	System.err.println("Adding node");

				nodeToNodeCount.put(tf,nodeCounts.size());
				ArrayList<Integer> op = new ArrayList<Integer>();
				for (int l = 0 ; l < 9 ; l++){
					op.add(0);
				}
				nodeCounts.add(op);
				x = nodeToNodeCount.get(tf);

			}
			
			if (y==null){
				//System.err.println("Adding node");
				nodeToNodeCount.put(target,nodeCounts.size());
				ArrayList<Integer> op = new ArrayList<Integer>();
				for (int l = 0 ; l < 9 ; l++){
					op.add(0);
				}
				nodeCounts.add(op);
				y = nodeToNodeCount.get(target);
			}
			
			ArrayList<Integer> tfCounts = nodeCounts.get(x);
			ArrayList<Integer> targetCounts = nodeCounts.get(y);
				
			HashSet<Node> tabuB = this.tabuList.get(target);
			HashSet<Node> tabuA = this.tabuList.get(tf);
			
			HashSet<Node> addB = this.addedList.get(target);
			HashSet<Node> addA = this.addedList.get(tf);

			// add
			HashSet<Node>tab = null;
			tab = this.tabuList.get(target);
			if (tab!=null){

			 if (	tab.remove(tf)){
		//			System.err.println("do it");

			 }

			}
			
			// add
			tab = null;
			tab = this.tabuList.get(tf);
			if (tab!=null){
				 if (	tab.remove(target)){
					//	System.err.println("do it");

				 }

			}
			
			
			// first check if we are adding a bidirectional link, if so, nothing is going to change
			boolean cont = true;
			// 2 cases, a incoming link existed in the previous network and has not been deleted
			// or the previous link did not exist in the previous network, but the link has been previously added in this loop
			if ((tf.getIncomingLinks(net) != null && tf.getIncomingLinks(net).contains(target) && (tabuA ==null || !tabuA.contains(target))) || ( (tf.getIncomingLinks(net) == null || (!tf.getIncomingLinks(net).contains(target))) && (addB!=null && addB.contains(tf)))   ){
				cont = false;
			}
			if (!cont){continue;}
			
			
			// either way, the graphlet count of G0 increases by 1
			tfCounts.set(0,tfCounts.get(0)+1);
			targetCounts.set(0,targetCounts.get(0)+1);
			
			
			// check out the neighbours of B
			Set<Node> neighboursA = tf.getUnidirectedLinks(net);
			Set<Node> neighboursB = target.getUnidirectedLinks(net);
			// loop all neighbours
			
			if (neighboursB != null){
				for (Node c: neighboursB){
					
					if ((tabuB !=null && tabuB.contains(c))){
						continue;
					}
					//this.cListAdd.add(c);
					addNeighbourBIntroCase(neighboursA, neighboursB,addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
				}
			}
			
			
			// of course, also include previously added neighbours
			if (addB!=null){
				for (Node c: addB){
				//	this.cListAdd.add(c);
					addNeighbourBIntroCase(neighboursA,neighboursB,addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
				}
			}
			


			// check out the neighbours of A (from the other side)
			
			
			

			// loop all neighbours
			
			if (neighboursA !=null){
				for (Node c: neighboursA  ){

					if ((tabuA !=null && tabuA.contains(c))){
						continue;
					}
				//	this.cListOppositeAdd.add(c);
					this.addNeighbourAIntroCase(neighboursA,neighboursB,addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
					
				}
			}
			
			
			// of course, also include previously added neighbours
			if (addA!=null){

				for (Node c: addA){
					//this.cListOppositeAdd.add(c);

					this.addNeighbourAIntroCase(neighboursA,neighboursB,addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
				}
			
		}
			

			
			// add
			 tab = null;
			tab = this.addedList.get(target);
			if (tab==null){
				HashSet<Node> tmp = new HashSet<Node>();
				tmp.add(tf);
				this.addedList.put(target, tmp);
			}else{
				tab.add(tf);
			}
			
			tab = null;
			tab =this.addedList.get(tf);
			if (tab==null){
				HashSet<Node> tmp = new HashSet<Node>();
				tmp.add(target);
				this.addedList.put(tf, tmp);
			}else{
				tab.add(target);
			}
			
			
			
	
			
		
		}
		
		
	}

	// first all c neighbour of target
	private void addNeighbourBIntroCase(Set<Node> neighboursA, Set<Node> neighboursB,HashSet<Node> addA, HashSet<Node> addB, Network net, Node tf, Node target, Node c, HashSet<Node> tabuA, HashSet<Node> tabuB,ArrayList<Integer> targetCounts, ArrayList<Integer> tfCounts){
		
		// check if node c, is in fact still a neighbour of B
		if (tabuB != null &&tabuB.contains(c)  &&   (addB ==null || !addB.contains(c))){return;}
		
		Set<Node> neighboursC = c.getUnidirectedLinks(net);

		if (neighboursC != null && neighboursC.contains(tf)  ){
			// this means we create a triangle and 2  G1
		
			if (   (tabuA !=null && tabuA.contains(c)) &&   (addA ==null || !addA.contains(c))   ){
				this.addNeighbourBChainCase(neighboursA, neighboursB,addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);	
			}else{
				this.addNeighbourBTriangleCase(neighboursA, neighboursB,addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
			}
		}
		else{
			// it could be that there still is a link because we just added it
			if (addA != null &&addA.contains(c) ){
				this.addNeighbourBTriangleCase(neighboursA, neighboursB,addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
			}
			else{	
				this.addNeighbourBChainCase(neighboursA, neighboursB,addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
			}		
		}
	}
	
	private void addNeighbourBTriangleCase(Set<Node> neighboursA, Set<Node> neighboursB,HashSet<Node> addA, HashSet<Node> addB, Network net, Node tf, Node target, Node c, HashSet<Node> tabuA, HashSet<Node> tabuB,ArrayList<Integer> targetCounts, ArrayList<Integer> tfCounts){

		//add the triangles
		tfCounts.set(2, tfCounts.get(2)+1);
		targetCounts.set(2, targetCounts.get(2)+1);
		ArrayList<Integer> cG1 = nodeCounts.get(this.nodeToNodeCount.get(c));
		cG1.set(2, cG1.get(2)+1);
		
	
		targetCounts.set(1, targetCounts.get(1)-1);
		tfCounts.set(1, tfCounts.get(1)-1);
		
		/**
		 * 
		 *  EXTEND to 4-node graphlet
		 *  
		 */
		
		
		
		// for all neighbours of C
		this.addNeigbourBTriangleCaseNeighboursC(neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
		// for all neighbours of B
		this.addNeigbourBTriangleCaseNeighboursB(neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
		// for all neighbours of A
		this.addNeigbourBTriangleCaseNeighboursA(neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);

		
		
		// this is the case in which a triangle exists between the nodes a, b, c after adding the link
		
		
		
		
	
		
		
	}
	
	private void addNeigbourBTriangleCaseNeighboursC(Set<Node> neighboursA, Set<Node> neighboursB,HashSet<Node> addA, HashSet<Node> addB, Network net, Node tf, Node target, Node c, HashSet<Node> tabuA, HashSet<Node> tabuB,ArrayList<Integer> targetCounts, ArrayList<Integer> tfCounts){
		
		
		// first loop over all neighbours of C
		Set<Node> neighboursC = c.getUnidirectedLinks(net);
		HashSet<Node> addC = this.addedList.get(c);
	
		if (neighboursC != null){
			
			for (Node d : neighboursC){
				ArrayList<Node> tmp = this.cListAdd.get(d);
				// do we need this ???
				if (tmp !=null && tmp.contains(c)){
					continue;}
				this.nodeDAddNeigbourBTriangleCaseNeighboursC(d, neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
			}
		
		}
		
		if (addC != null){
			for (Node d: addC){
				ArrayList<Node> tmp = this.cListAdd.get(d);
				// do we need this ???
				if (tmp !=null && tmp.contains(c)){
					continue;}
				this.nodeDAddNeigbourBTriangleCaseNeighboursC(d, neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
			}
			
		}

	}
	
	private void addNeigbourBTriangleCaseNeighboursB(Set<Node> neighboursA, Set<Node> neighboursB,HashSet<Node> addA, HashSet<Node> addB, Network net, Node tf, Node target, Node c, HashSet<Node> tabuA, HashSet<Node> tabuB,ArrayList<Integer> targetCounts, ArrayList<Integer> tfCounts){
		

		if (neighboursB !=null){
			for (Node d: neighboursB){
				ArrayList<Node> tmp = this.cListAdd.get(d);
				// do we need this ???
				if (tmp !=null && tmp.contains(c)){
					continue;}
				this.nodeDaddNeigbourBTriangleCaseNeighboursB(d, neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);

				
			}
			
		}
		

		if (addB != null){
			
			for (Node d: addB){
				ArrayList<Node> tmp = this.cListAdd.get(d);
				// do we need this ???
				if (tmp !=null && tmp.contains(c)){
					continue;}
				this.nodeDaddNeigbourBTriangleCaseNeighboursB(d, neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);

			}
		}
		
		
		
		
	}

	private void addNeigbourBTriangleCaseNeighboursA(Set<Node> neighboursA, Set<Node> neighboursB,HashSet<Node> addA, HashSet<Node> addB, Network net, Node tf, Node target, Node c, HashSet<Node> tabuA, HashSet<Node> tabuB,ArrayList<Integer> targetCounts, ArrayList<Integer> tfCounts){
		

		if (neighboursA != null){
			
		
		
			for (Node d: neighboursA){
				ArrayList<Node> tmp = this.cListAdd.get(d);
				// do we need this ???
				if (tmp !=null && tmp.contains(c)){
			continue;}
				this.nodeDaddNeigbourBTriangleCaseNeighboursA(d, neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
	
				
			}
			
		}
		
		if (addA != null){
			
			for (Node d: addA){
				ArrayList<Node> tmp = this.cListAdd.get(d);
				// do we need this ???
				if (tmp !=null && tmp.contains(c)){
					continue;}
				this.nodeDaddNeigbourBTriangleCaseNeighboursA(d, neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);

			}
		}
		
		
		
		
	}
	
	private void nodeDAddNeigbourBTriangleCaseNeighboursC(Node d,Set<Node> neighboursA, Set<Node> neighboursB,HashSet<Node> addA, HashSet<Node> addB, Network net, Node tf, Node target, Node c, HashSet<Node> tabuA, HashSet<Node> tabuB,ArrayList<Integer> targetCounts, ArrayList<Integer> tfCounts){
		
		
		// first loop over all neighbours of C
		HashSet<Node> tabuC = this.tabuList.get(c);
		HashSet<Node> addC = this.addedList.get(c);
		
		// check for distinct nodes
		if(d.equals(tf) || d.equals(target) ){return;}
		
		// also check if d is still a neighbour of c
		if (tabuC != null && tabuC.contains(d)){
			if (addC ==null || !addC.contains(d)){

				return;
			}
		}
		

		
		// we have a valid D
		
		
		// check the cases
		if ( (addA!=null && addA.contains(d)) ||  (neighboursA != null && neighboursA.contains(d) && (tabuA == null || !tabuA.contains(d)))){
			
			
			// yes-yes
			if ( (addB!=null && addB.contains(d)) ||  (neighboursB != null && neighboursB.contains(d) && (tabuB == null || !tabuB.contains(d)))){
				
				
				
				/**
				 * UC!
				 * This is the case in which a G7 exists and is destroyed by the add in:
				 *  	target
				 *  	tf
				 *  	
				 *  a new graphlet G8 appears  in tf, c, d, target
				 *
				 *  we need to be careful here, as d and c will appear twice without special care
				 * 
				 */
				
				ArrayList<Node> y = null;
				y = eightLookupAdd.get(c);
				if (y!=null && y.contains(d)){
					//System.err.println("eightlookup");
					return;}
					
					
				y = null;
				y = eightLookupAdd.get(d);
				if (y==null){
					
					ArrayList<Node> tmp = new ArrayList<Node>();
					tmp.add(c);
					eightLookupAdd.put(d, tmp);
					
				}else{
					
					y.add(c);
					
				}
				
				
				ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
				ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));

				
				// remove
				targetCounts.set(7, targetCounts.get(7) - 1);
				tfCounts.set(7, tfCounts.get(7) - 1);

				
				// add
				cc.set(8, cc.get(8) + 1);
				dd.set(8, dd.get(8) + 1);
				targetCounts.set(8, targetCounts.get(8) + 1);
				tfCounts.set(8, tfCounts.get(8) + 1);
				
				
				
				
				ArrayList<Node> tmp = this.cListAdd.get(c);
				
				if (tmp==null){
					ArrayList<Node> tmp2 = new ArrayList<Node>();
					tmp2.add(d);
					this.cListAdd.put(c, tmp2);
				}
				else{
					tmp.add(d);
				}
				

				
			}
			else{
				
				
			
				
				/**
				 * UC!
				 * This is the case in which a G6 exists and is destroyed by the add in:
				 *  	target
				 *  	
				 *  	
				 *  a new graphlet G7 appears  in tf, c
				 *  
				 *  we need to be careful here, as d and c will appear twice without special care
				 * 
				 */
				
				ArrayList<Node> y = null;
				y = this.sevenLookupAdd.get(c);
				if (y!=null && y.contains(d)){return;}
					
					
				y = null;
				y =sevenLookupAdd.get(d);
				if (y==null){
					
					ArrayList<Node> tmp = new ArrayList<Node>();
					tmp.add(c);
					sevenLookupAdd.put(d, tmp);
					
				}else{
					
					y.add(c);
					
				}
				
				
				ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));

				
				// remove
				targetCounts.set(6, targetCounts.get(6) - 1);
				
				// add
				cc.set(7, cc.get(7) + 1);
				tfCounts.set(7, tfCounts.get(7) + 1);
				
				

				ArrayList<Node> tmp = this.cListAdd.get(c);
				
				if (tmp==null){
					ArrayList<Node> tmp2 = new ArrayList<Node>();
					tmp2.add(d);
					this.cListAdd.put(c, tmp2);
				}
				else{
					tmp.add(d);
				}			
				//System.err.println(tf +" " + target + " "+ c+ " "+d+ " ADDneighbour-C-TRIANGLE- tf YES- target NO");

				
			}
			
		}else{
			
			// no-yes
			if ( (addB!=null && addB.contains(d)) ||  (neighboursB != null &&neighboursB.contains(d) && (tabuB == null || !tabuB.contains(d)))){


				ArrayList<Node> y = null;
				y = this.sevenLookupAdd.get(c);
				if (y!=null && y.contains(d)){return;}
					
					
				y = null;
				y =sevenLookupAdd.get(d);
				if (y==null){
					
					ArrayList<Node> tmp = new ArrayList<Node>();
					tmp.add(c);
					sevenLookupAdd.put(d, tmp);
					
				}else{
					
					y.add(c);
					
				}
				
				
				/**
				 * UC!
				 * This is the case in which a G6 exists and is destroyed by the add in:
				 *  	tf
				 *  	
				 *  	
				 *  a new graphlet G7 appears  in target, c
				 *  
				 *  we need to be careful here, as d and c will appear twice without special care
				 * 
				 */
				
				ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));

				
				// remove
				tfCounts.set(6, tfCounts.get(6) - 1);
				
				// add
				cc.set(7, cc.get(7) + 1);
				targetCounts.set(7, targetCounts.get(7) + 1);

				

				ArrayList<Node> tmp = this.cListAdd.get(c);
				
				if (tmp==null){
					ArrayList<Node> tmp2 = new ArrayList<Node>();
					tmp2.add(d);
					this.cListAdd.put(c, tmp2);
				}
				else{
					tmp.add(d);
				}			
				//System.err.println(tf +" " + target + " "+ c+ " "+d+ " ADDneighbour-C-TRIANGLE- tf NO- target YES");

			}
			// no-no
			else{
					
				/**
				 * 
				 * This is the case in which a G4 graphlet exists and is destroyed by the add in:
				 *  	tf
				 *  	target
				 *  	d
				 *  a new graphlet G6 appears  in d
				 * 
				 */
				
				
				ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));

				// remove
				dd.set(4, dd.get(4) - 1);
				tfCounts.set(4, tfCounts.get(4) - 1);
				targetCounts.set(4, targetCounts.get(4) - 1);
				
				
				// add
				dd.set(6, dd.get(6) + 1);

				

				ArrayList<Node> tmp = this.cListAdd.get(c);
				
				if (tmp==null){
					ArrayList<Node> tmp2 = new ArrayList<Node>();
					tmp2.add(d);
					this.cListAdd.put(c, tmp2);
				}
				else{
					tmp.add(d);
				}			
				//System.err.println(tf +" " + target + " "+ c+ " "+d+ " ADDneighbour-C-TRIANGLE- tf NO- target NO");

			}
		}
		
		
	}
	
	private void nodeDaddNeigbourBTriangleCaseNeighboursB(Node d,Set<Node> neighboursA, Set<Node> neighboursB,HashSet<Node> addA, HashSet<Node> addB, Network net, Node tf, Node target, Node c, HashSet<Node> tabuA, HashSet<Node> tabuB,ArrayList<Integer> targetCounts, ArrayList<Integer> tfCounts){
		
		
		// first loop over all neighbours of C
		HashSet<Node> tabuC = this.tabuList.get(c);
		HashSet<Node> addC = this.addedList.get(c);
		
		Set<Node> neighboursC = c.getUnidirectedLinks(net);
		
		// check for distinct nodes
		if(d.equals(tf) || d.equals(c) ){return;}
		
		// also check if d is still a neighbour of c
		if (tabuB != null && tabuB.contains(d)){
			if (addB ==null || !addB.contains(d)){
				return;
			}
		}
		
		// also return when the current D has already been a C
		if ( (addC!=null && addC.contains(d)) ||  ( neighboursC != null &&neighboursC.contains(d) && (tabuC == null || !tabuC.contains(d)))){
			

			return;}


		


		
		// we have a valid D
		
		// check the cases
		if ( (addA!=null && addA.contains(d)) ||  (neighboursA != null &&neighboursA.contains(d) && (tabuA == null || !tabuA.contains(d)))){
					

			/**
			 * 
			 * IP
			 * 
			 * This is the case where there is a  G5 graphlet in all nodes
			 * 
			 * and this is destroyed and a new graphlet G7 appears in
			 * TF 
			 * target
			 * 
			 */
			
			
			ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
			ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));

					
			// remove
			targetCounts.set(5, targetCounts.get(5) - 1);
			tfCounts.set(5, tfCounts.get(5) - 1);
			cc.set(5, cc.get(5) - 1);
			dd.set(5, dd.get(5) - 1);

					
			// add
			targetCounts.set(7, targetCounts.get(7) + 1);
			tfCounts.set(7, tfCounts.get(7) + 1);

			ArrayList<Node> tmp = this.cListAdd.get(c);
			
			if (tmp==null){
				ArrayList<Node> tmp2 = new ArrayList<Node>();
				tmp2.add(d);
				this.cListAdd.put(c, tmp2);
			}
			else{
				tmp.add(d);
			}	//		System.err.println(tf +" " + target + " "+ c+ " "+d+ " ADDneighbour-C-B-TRIANGLE- tf YES");

					
		}else{
			

			
			/**
			 * 
			 * IP
			 * 
			 * This is the case where there is a long chain  graphlet G3 in 
			 * 	d
			 *  tf
			 *  
			 * 
			 * and this is destroyed and a new graphlet G6 appears in
			 * d 
			 * 
			 * 
			 */
					
			ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));

					
			// remove
			tfCounts.set(3,tfCounts.get(3) - 1);
			dd.set(3, dd.get(3) - 1);

					
			// add
			dd.set(6, dd.get(6) + 1);
			//??

			ArrayList<Node> tmp = this.cListAdd.get(c);
			
			if (tmp==null){
				ArrayList<Node> tmp2 = new ArrayList<Node>();
				tmp2.add(d);
				this.cListAdd.put(c, tmp2);
			}
			else{
				tmp.add(d);
			}		
			//System.err.println(tf +" " + target + " "+ c+ " "+d+ " ADDneighbour-C-B-TRIANGLE- tf NO");

		}
		
		
	}
	
	private void nodeDaddNeigbourBTriangleCaseNeighboursA(Node d,Set<Node> neighboursA, Set<Node> neighboursB,HashSet<Node> addA, HashSet<Node> addB, Network net, Node tf, Node target, Node c, HashSet<Node> tabuA, HashSet<Node> tabuB,ArrayList<Integer> targetCounts, ArrayList<Integer> tfCounts){
		
		
		// first loop over all neighbours of C
		HashSet<Node> tabuC = this.tabuList.get(c);
		HashSet<Node> addC = this.addedList.get(c);
		
		Set<Node> neighboursC = c.getUnidirectedLinks(net);
		
		// check for distinct nodes
		if(d.equals(target) || d.equals(c) ){return;}
		
		// also check if d is still a neighbour of c
		if (tabuA != null && tabuA.contains(d)){
			if (addA ==null || !addA.contains(d)){
				return;
			}
		}
		
		// also return when the current D has already been a C
		if ( (addC!=null && addC.contains(d)) ||  (neighboursC != null &&neighboursC.contains(d) && (tabuC == null || !tabuC.contains(d)))){return;}
		// also return when the current D has already been a B
		if ( (addB!=null && addB.contains(d)) ||  (neighboursB != null &&neighboursB.contains(d) && (tabuB == null || !tabuB.contains(d)))){return;}

		
		
		// we have a valid D
		
		// There is only one case, 
		
		
		/**
		 * 
		 * IP
		 * 
		 * This is the case where there is a  G3 graphlet starting at 
		 *   D
		 *   target
		 * 
		 * and this is destroyed and a new graphlet G7 appears in
		 * d 
		 * 
		 * 
		 */
	
					
		ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));

				
		// remove
		targetCounts.set(3,targetCounts.get(3) - 1);
		dd.set(3, dd.get(3) - 1);

				
		// add
		dd.set(6, dd.get(6) + 1);
		

		ArrayList<Node> tmp = this.cListAdd.get(c);
		
		if (tmp==null){
			ArrayList<Node> tmp2 = new ArrayList<Node>();
			tmp2.add(d);
			this.cListAdd.put(c, tmp2);
		}
		else{
			tmp.add(d);
		}		
		
		//System.err.println(tf +" " + target + " "+ c+ " "+d+ " ADDneighbour-C-A-TRIANGLE-");

		
		
		
	}

	private void addNeighbourBChainCase(Set<Node> neighboursA, Set<Node> neighboursB,HashSet<Node> addA, HashSet<Node> addB, Network net, Node tf, Node target, Node c, HashSet<Node> tabuA, HashSet<Node> tabuB,ArrayList<Integer> targetCounts, ArrayList<Integer> tfCounts){

		// we create a G1 graphlet in both target and C
		ArrayList<Integer> cG1 = nodeCounts.get(this.nodeToNodeCount.get(c));
		cG1.set(1, cG1.get(1)+1);
		
		tfCounts.set(1, tfCounts.get(1)+1);
		

		
		/**
		 * 
		 *  EXTEND to 4-node graphlet
		 *  
		 */
		
		
		
		// for all neighbours of C
		this.addNeigbourBChainCaseNeighboursC(neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
		// for all neighbours of B
		this.addNeigbourBChainCaseNeighboursB(neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
		// for all neighbours of A
		this.addNeigbourBChainCaseNeighboursA(neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);

		// this is the case in which a triangle exists between the nodes a, b, c after adding the link
		
		
		
		
		
	}

	private void addNeigbourBChainCaseNeighboursC(Set<Node> neighboursA,
			Set<Node> neighboursB, HashSet<Node> addA, HashSet<Node> addB,
			Network net, Node tf, Node target, Node c, HashSet<Node> tabuA,
			HashSet<Node> tabuB, ArrayList<Integer> targetCounts,
			ArrayList<Integer> tfCounts) {

		
		Set<Node> neighboursC = c.getUnidirectedLinks(net);
		HashSet<Node> addC = this.addedList.get(c);
		
		
		if (neighboursC != null){
			
			for (Node d: neighboursC){
				ArrayList<Node> tmp = this.cListAdd.get(d);
				// do we need this ???
				if (tmp !=null && tmp.contains(c)){
					continue;}
				this.nodeDaddNeigbourBChainCaseNeighboursC(d, neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);

				
			}
			
		}

		
		if (addC != null){
			
			for (Node d: addC){
				ArrayList<Node> tmp = this.cListAdd.get(d);
				// do we need this ???
				if (tmp !=null && tmp.contains(c)){
					continue;}
				this.nodeDaddNeigbourBChainCaseNeighboursC(d, neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);

			}
		}
		
		
	}

	private void addNeigbourBChainCaseNeighboursB(Set<Node> neighboursA,
			Set<Node> neighboursB, HashSet<Node> addA, HashSet<Node> addB,
			Network net, Node tf, Node target, Node c, HashSet<Node> tabuA,
			HashSet<Node> tabuB, ArrayList<Integer> targetCounts,
			ArrayList<Integer> tfCounts) {


		if (neighboursB != null){
			for (Node d: neighboursB){
				ArrayList<Node> tmp = this.cListAdd.get(d);
				// do we need this ???
				if (tmp !=null && tmp.contains(c)){
					continue;}
				this.nodeDaddNeigbourBChainCaseNeighboursB(d, neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);

				
			}
		}
		
		
		if (addB != null){
			
			for (Node d: addB){
				ArrayList<Node> tmp = this.cListAdd.get(d);
				// do we need this ???
				if (tmp !=null && tmp.contains(c)){
					continue;}
				this.nodeDaddNeigbourBChainCaseNeighboursB(d, neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);

			}
		}
		
	}

	private void addNeigbourBChainCaseNeighboursA(Set<Node> neighboursA,
			Set<Node> neighboursB, HashSet<Node> addA, HashSet<Node> addB,
			Network net, Node tf, Node target, Node c, HashSet<Node> tabuA,
			HashSet<Node> tabuB, ArrayList<Integer> targetCounts,
			ArrayList<Integer> tfCounts) {

		if (neighboursA !=null){
			for (Node d: neighboursA){
				
				ArrayList<Node> tmp = this.cListAdd.get(d);
				// do we need this ???
				if (tmp !=null && tmp.contains(c)){
					continue;}
				this.nodeDaddNeigbourBChainCaseNeighboursA(d, neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);

				
			}
			
		}

		
		if (addA != null){
			
			for (Node d: addA){
				ArrayList<Node> tmp = this.cListAdd.get(d);
				// do we need this ???
				if (tmp !=null && tmp.contains(c)){
					continue;}
				this.nodeDaddNeigbourBChainCaseNeighboursA(d, neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);

			}
		}
		
		
		
		
	}

	private void nodeDaddNeigbourBChainCaseNeighboursC(Node d,
			
			
		Set<Node> neighboursA, Set<Node> neighboursB, HashSet<Node> addA,
		HashSet<Node> addB, Network net, Node tf, Node target, Node c,
		HashSet<Node> tabuA, HashSet<Node> tabuB,
		ArrayList<Integer> targetCounts, ArrayList<Integer> tfCounts) {
		
		// first loop over all neighbours of C
		HashSet<Node> tabuC = this.tabuList.get(c);
		HashSet<Node> addC = this.addedList.get(c);
		
		
		// check if this d has already been a c
		
		
		// check for distinct nodes
		if(d.equals(target) || d.equals(tf) ){return;}
		
		// also check if d is still a neighbour of c
		if (tabuC != null && tabuC.contains(d)){
			if (addC ==null || !addC.contains(d)){
				return;
			}
		}
		
		
		
		
		// we have a valid D
		
		// check cases
		
		if ( (addB!=null && addB.contains(d)) ||  ( neighboursB != null &&neighboursB.contains(d) && (tabuB== null || !tabuB.contains(d)))){

			if ( (addA!=null && addA.contains(d)) ||  (neighboursA != null &&neighboursA.contains(d) && (tabuA== null || !tabuA.contains(d)))){

				// already taken care of
				
			}
			else{
				
				/**
				 * 
				 * IP
				 * 
				 * This is the case where there are no graphlets because rgraph is not connected
				 * 		
				 * 	
				 * 
				 * and this is destroyed and a new graphlet g6 is created in tf
				 * 
				 * 
				 * 
				 */
				
				

						
				// add;
				tfCounts.set(6, tfCounts.get(6) + 1);

				ArrayList<Node> tmp = this.cListAdd.get(c);
				
				if (tmp==null){
					ArrayList<Node> tmp2 = new ArrayList<Node>();
					tmp2.add(d);
					this.cListAdd.put(c, tmp2);
				}
				else{
					tmp.add(d);
				}				
		//		System.err.println(tf +" " + target + " "+ c+ " "+d+ " ADDneighbour-C-B-CHAIN- tf NO - target yes");
				
			}
			
		}else{
			
			if ( (addA!=null && addA.contains(d)) ||  (neighboursA != null &&neighboursA.contains(d) && (tabuA== null || !tabuA.contains(d)))){

				/**
				 * 
				 * IP
				 * 
				 * This is the case where there is a  G3 graphlet in 
				 * 		tf
				 * 		d
				 * 
				 * and this is destroyed and a new graphlet g5 in all
				 * 
				 * 
				 * 
				 */
				
				
				ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
				ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));

						
				// remove
				tfCounts.set(3, tfCounts.get(3) - 1);
				dd.set(3, dd.get(3) - 1);
						
				// add
				targetCounts.set(5, targetCounts.get(5) + 1);
				tfCounts.set(5, tfCounts.get(5) + 1);
				cc.set(5, cc.get(5) + 1);
				dd.set(5, dd.get(5) + 1);
				
				
	
				
				ArrayList<Node> tmp = this.cListAdd.get(c);
				
				if (tmp==null){
					ArrayList<Node> tmp2 = new ArrayList<Node>();
					tmp2.add(d);
					this.cListAdd.put(c, tmp2);
				}
				else{
					tmp.add(d);
				}
				
				
			//	System.err.println(tf +" " + target + " "+ c+ " "+d+ " ADDneighbour-C-B-CHAIN- tf YES - target no");
			
			
			}
			else{
				
				
				/**
				 * 
				 * IP
				 * 
				 * This is the case where there are no graphlets because the graph is disconnected
				 * 		
				 * 	
				 * 
				 * and  a graphlet G3 is created in tf and d
				 * 
				 * 
				 * 
				 */
				
				
				ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));

						
				// add
				tfCounts.set(3, tfCounts.get(3) + 1);
				dd.set(3, dd.get(3) + 1);
			
				//??
				ArrayList<Node> tmp = this.cListAdd.get(c);
				
				if (tmp==null){
					ArrayList<Node> tmp2 = new ArrayList<Node>();
					tmp2.add(d);
					this.cListAdd.put(c, tmp2);
				}
				else{
					tmp.add(d);
				}
			//	System.err.println(tf +" " + target + " "+ c+ " "+d+ " ADDneighbour-C-B-CHAIN- tf NO - target no");
		
			}
			
			
		}
		
	
		
		
	}

	private void nodeDaddNeigbourBChainCaseNeighboursB(Node d,
			Set<Node> neighboursA, Set<Node> neighboursB, HashSet<Node> addA,
			HashSet<Node> addB, Network net, Node tf, Node target, Node c,
			HashSet<Node> tabuA, HashSet<Node> tabuB,
			ArrayList<Integer> targetCounts, ArrayList<Integer> tfCounts) {
	

		
		// first loop over all neighbours of C
		HashSet<Node> tabuC = this.tabuList.get(c);
		HashSet<Node> addC = this.addedList.get(c);
		
		Set<Node> neighboursC = c.getUnidirectedLinks(net);
		

		
		// check for distinct nodes
		if( d.equals(c) || d.equals(tf) ){return;}
		
		// also check if d is still a neighbour of b
		if (tabuB != null && tabuB.contains(d)){
			if (addB ==null || !addB.contains(d)){
				return;
			}
		}

		
		
	
		
		// we have a valid D
		
		// check cases
		// d cannot be connected to tf, otherwhise we had a triangle
		// dd cannot be connected to c, otherwhise, we counted it in neighboursC
		
		if ( (addC!=null && addC.contains(d)) ||  (neighboursC != null && neighboursC.contains(d) && (tabuC== null || !tabuC.contains(d)))){return ;}
		if ( (addA!=null && addA.contains(d)) ||  (neighboursA != null &&neighboursA.contains(d) && (tabuA== null || !tabuA.contains(d)))){return ;}


		
		/**
		 * 
		 * This is the case in which the graph is not connected
		 * and a  graphlet g4  appears in all but target
		 */
		
		ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
		ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));

				

		// add
		tfCounts.set(4, tfCounts.get(4) + 1);
		cc.set(4, cc.get(4) + 1);
		dd.set(4, dd.get(4) + 1);
		
	
		//??
		ArrayList<Node> tmp = this.cListAdd.get(c);
		
		if (tmp==null){
			ArrayList<Node> tmp2 = new ArrayList<Node>();
			tmp2.add(d);
			this.cListAdd.put(c, tmp2);
		}
		else{
			tmp.add(d);
		}

		
		
		
	}
	
	private void nodeDaddNeigbourBChainCaseNeighboursA(Node d,
			Set<Node> neighboursA, Set<Node> neighboursB, HashSet<Node> addA,
			HashSet<Node> addB, Network net, Node tf, Node target, Node c,
			HashSet<Node> tabuA, HashSet<Node> tabuB,
			ArrayList<Integer> targetCounts, ArrayList<Integer> tfCounts) {

		
		// first loop over all neighbours of C
		HashSet<Node> tabuC = this.tabuList.get(c);
		HashSet<Node> addC = this.addedList.get(c);
		
		Set<Node> neighboursC = c.getUnidirectedLinks(net);
		

		
		// check for distinct nodes
		if( d.equals(c) || d.equals(target) ){return;}
		
		// also check if d is still a neighbour of a
		if (tabuA != null && tabuA.contains(d)){
			if (addA ==null || !addA.contains(d)){
				return;
			}
		}

		
		// we have a valid D
		
		// check cases
		// d cannot be connected to target, otherwhise we had a triangle
		// dd cannot be connected to c, otherwhise, we counted it in neighboursC
		
		if ( (addC!=null && addC.contains(d)) ||  (neighboursC !=null &&neighboursC.contains(d) && (tabuC== null || !tabuC.contains(d)))){return ;}
		if ( (addB!=null && addB.contains(d)) ||  (neighboursB !=null &&neighboursB.contains(d) && (tabuB== null || !tabuB.contains(d)))){return ;}


		
		/**
		 * 
		 * This is the case in which the graph is not connected
		 * and a  graphlet g3 graphlet  appears
		 * d and 
		 * c
		 */
		
		ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
		ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));

				

		// add
		cc.set(3, cc.get(3) + 1);
		dd.set(3, dd.get(3) + 1);
		
		// ????
		ArrayList<Node> tmp = this.cListAdd.get(c);
		
		if (tmp==null){
			ArrayList<Node> tmp2 = new ArrayList<Node>();
			tmp2.add(d);
			this.cListAdd.put(c, tmp2);
		}
		else{
			tmp.add(d);
		}
	//	System.err.println(tf +" " + target + " "+ c+ " "+d+ " ADDneighbour-B-A-CHAIN- tf NO - target NO");

		
		
		
	}

	private void addNeighbourAIntroCase(Set<Node> neighboursA, Set<Node> neighboursB,HashSet<Node> addA, HashSet<Node> addB, Network net, Node tf, Node target, Node c, HashSet<Node> tabuA, HashSet<Node> tabuB,ArrayList<Integer> targetCounts, ArrayList<Integer> tfCounts){

		// check if node c, is in fact still a neighbour of B
		if (tabuA != null && tabuA.contains(c) &&   (addA ==null || !addA.contains(c))){return;}
		

		if (addB != null && addB.contains(c)  || ( neighboursB !=null && neighboursB.contains(c) && (tabuB ==null || !tabuB.contains(c) )) ){return;}
		
	
		// we create a G1 graphlet in both target and C
		ArrayList<Integer> cG1 = nodeCounts.get(this.nodeToNodeCount.get(c));
		cG1.set(1, cG1.get(1)+1);
		targetCounts.set(1, targetCounts.get(1)+1);
				
		
		/**
		 * 
		 * extend to 4-node graphlets
		 * 
		 * 
		 * 
		 */
		
	
		// for all neighbours of C
		this.addNeigbourACaseNeighboursC(neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
		// for all neighbours of A
		this.addNeigbourAChainCaseNeighboursA(neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
		// for all neighbours of B

		// already did these
		
		
		

	


		
		
		
		
	}
	
	private void addNeigbourACaseNeighboursC(Set<Node> neighboursA,
			Set<Node> neighboursB, HashSet<Node> addA, HashSet<Node> addB,
			Network net, Node tf, Node target, Node c, HashSet<Node> tabuA,
			HashSet<Node> tabuB, ArrayList<Integer> targetCounts,
			ArrayList<Integer> tfCounts) {
	
		
		Set<Node> neighboursC = c.getUnidirectedLinks(net);
		HashSet<Node> addC = this.addedList.get(c);
		
		if ( neighboursC != null ){
			
			
		
			for (Node d: neighboursC){
				ArrayList<Node> tmp = this.cListOppositeAdd.get(d);
				// do we need this ???
				if (tmp !=null && tmp.contains(c)){
					continue;}
				this.nodeDaddNeigbourAChainCaseNeighboursC(d, neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);
	
				
			}
		
		}
		
		if (addC != null){
			
			for (Node d: addC){
				ArrayList<Node> tmp = this.cListOppositeAdd.get(d);
				// do we need this ???
				if (tmp !=null && tmp.contains(c)){
					continue;}
				this.nodeDaddNeigbourAChainCaseNeighboursC(d, neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);

			}
		}
		
	}

	private void nodeDaddNeigbourAChainCaseNeighboursC(Node d,
			Set<Node> neighboursA, Set<Node> neighboursB, HashSet<Node> addA,
			HashSet<Node> addB, Network net, Node tf, Node target, Node c,
			HashSet<Node> tabuA, HashSet<Node> tabuB,
			ArrayList<Integer> targetCounts, ArrayList<Integer> tfCounts) {

		
		// first loop over all neighbours of C
		HashSet<Node> tabuC = this.tabuList.get(c);
		HashSet<Node> addC = this.addedList.get(c);
		
		
		
		
		// check for distinct nodes
		if(d.equals(target) || d.equals(tf) ){return;}
		
		// also check if d is still a neighbour of c
		if (tabuC != null && tabuC.contains(d)){
			if (addC ==null || !addC.contains(d)){
				return;
			}
		}
		if ( (addB!=null && addB.contains(d)) ||  (neighboursB != null && neighboursB.contains(d) && (tabuB== null || !tabuB.contains(d)))){return;}

		
		// we have a valid D
		
		// check cases
		
		if ( (addA!=null && addA.contains(d)) ||  (neighboursA != null &&neighboursA.contains(d) && (tabuA== null || !tabuA.contains(d)))){

			
			
				/**
				 * 
				 * This is the case in which the graph is disconnected and a kite graphlet appears in target
				 * 
				 */
		
		

			targetCounts.set(6, targetCounts.get(6) + 1);

			ArrayList<Node> tmp = this.cListOppositeAdd.get(c);
			
			if (tmp==null){
				ArrayList<Node> tmp2 = new ArrayList<Node>();
				tmp2.add(d);
				this.cListOppositeAdd.put(c, tmp2);
			}
			else{
				tmp.add(d);
			}
		//	System.err.println(tf +" " + target + " "+ c+ " "+d+ " ADDneighbour-C-A-CHAIN- tf YES - target NO");
				
			
			
		}else{
		
			
			
			/**
			 * 
			 * This is the case in which the graph is disconnected and a G3 graphlet appears in
			 * d and target
			 * 
			 */
				
			ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));

					

			// add
			targetCounts.set(3, targetCounts.get(3) + 1);
			dd.set(3, dd.get(3) + 1);

			ArrayList<Node> tmp = this.cListOppositeAdd.get(c);
			
			if (tmp==null){
				ArrayList<Node> tmp2 = new ArrayList<Node>();
				tmp2.add(d);
				this.cListOppositeAdd.put(c, tmp2);
			}
			else{
				tmp.add(d);
			}	
			
			//		System.err.println(tf +" " + target + " "+ c+ " "+d+ " ADDneighbour-C-A-CHAIN- tf NO - target NO");
		
		
		}
		
	}

	private void addNeigbourAChainCaseNeighboursA(Set<Node> neighboursA,
			Set<Node> neighboursB, HashSet<Node> addA, HashSet<Node> addB,
			Network net, Node tf, Node target, Node c, HashSet<Node> tabuA,
			HashSet<Node> tabuB, ArrayList<Integer> targetCounts,
			ArrayList<Integer> tfCounts) {
		

		if (neighboursA != null ){
			for (Node d: neighboursA){
				ArrayList<Node> tmp = this.cListOppositeAdd.get(d);
				if (tmp !=null && tmp.contains(c)){continue;}
				this.nodeDaddNeigbourAChainCaseNeighboursA(d, neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);

				
			}
			
		}
		
		
		
		if (addA != null){
			
			for (Node d: addA){
				ArrayList<Node> tmp = this.cListOppositeAdd.get(d);
				// do we need this ???
				if (tmp !=null && tmp.contains(c)){continue;}
				this.nodeDaddNeigbourAChainCaseNeighboursA(d, neighboursA, neighboursB, addA, addB, net, tf, target, c, tabuA, tabuB, targetCounts, tfCounts);

			}
		}
		
		
	}

	private void nodeDaddNeigbourAChainCaseNeighboursA(Node d,
			Set<Node> neighboursA, Set<Node> neighboursB, HashSet<Node> addA,
			HashSet<Node> addB, Network net, Node tf, Node target, Node c,
			HashSet<Node> tabuA, HashSet<Node> tabuB,
			ArrayList<Integer> targetCounts, ArrayList<Integer> tfCounts) {

		
				// first loop over all neighbours of C
				HashSet<Node> tabuC = this.tabuList.get(c);
				HashSet<Node> addC = this.addedList.get(c);
				
				Set<Node> neighboursC = c.getUnidirectedLinks(net);
		
				
				// check for distinct nodes
				if(d.equals(target) || d.equals(c) ){return;}
				
				// also check if d is still a neighbour of a
				if (tabuA != null && tabuA.contains(d)){
					if (addA ==null || !addA.contains(d)){
						return;
					}
				}
				
				
				// we have a valid D
				
				// check cases
				
				if ( (addC!=null && addC.contains(d)) ||  (neighboursC !=null &&neighboursC.contains(d) && (tabuC== null || !tabuC.contains(d)))){return;}
				if ( (addB!=null && addB.contains(d)) ||  (neighboursB !=null &&neighboursB.contains(d) && (tabuB== null || !tabuB.contains(d)))){return;}


					
					
				/**
				 * 
				 * This is the case in which the graph is disconnected and a g3 graphlet appears in target and d
				 * 
				 */
				
				// ????
				
					ArrayList<Integer> dd = nodeCounts.get(this.nodeToNodeCount.get(d));
					ArrayList<Integer> cc = nodeCounts.get(this.nodeToNodeCount.get(c));

					
					targetCounts.set(4, targetCounts.get(4) + 1);
					dd.set(4,dd.get(4) + 1);
					cc.set(4,cc.get(4) + 1);



					ArrayList<Node> tmp = this.cListOppositeAdd.get(c);
					
					if (tmp==null){
						ArrayList<Node> tmp2 = new ArrayList<Node>();
						tmp2.add(d);
						this.cListOppositeAdd.put(c, tmp2);
					}
					else{
						tmp.add(d);
					}					
					
					//System.err.println(tf +" " + target + " "+ c+ " "+d+ " ADDneighbour-A-A-CHAIN- tf NO - target NO");
						
					
					
			
	}



	
	
	

}
