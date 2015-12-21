package network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

import javax.management.RuntimeErrorException;

import network.link.Link;
import network.link.LinkProperties;
import network.link.comparator.LinkNameComparator;
import network.link.comparator.LinkRankComparator;
import network.node.Node;
import network.node.NodeCollection;


/**
 * Network is an essential class in Netter. Representing a network both as a collection of Links and as a collection of Nodes.
 * 
 * Network's can have temporary states, with links temporaraly removed or added.
 * 

 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class Network {

	/**
	 * List of links in the network, except temporary added links or removed links
	 */
	private ArrayList<Link> links;
	
	/**
	 * List of nodes in the network, except nodes that are removed or added by temporary changes
	 */
	private LinkedHashSet<Node> nodes;
	
	/**
	 * Temporary added links
	 */
	private ArrayList<Link> tmpAdded ;
	
	/**
	 * Temporary removed links
	 */
	private ArrayList<Link> tmpRemoved;

	/**
	 * Automatically generated name for this network. 
	 */
	private final String name = NetworkNameGenerator.getNextName();
	
	/**
	 * Comperator which can be used to sort network by ranking
	 */
	private static final LinkRankComparator rankSorter = new LinkRankComparator();
	
	/**
	 * Comperator which can be used to sort network by name
	 */
	private static final LinkNameComparator nameSorter = new LinkNameComparator();

	
	
	/**
	 * 
	 * Construct an empty network and inits the members 
	 */
	public Network() {
		this.init();
	}

	/**
	 * 
	 * Copy constructor, will return a copy of the network with new objects
	 * 
	 * @param network	the network to be copied
	 */
	public Network(Network network) {

		this.init();
		File tmp = null;
		try {
			tmp = File.createTempFile("tmp", "tmp");
			if (tmp == null) {
				throw new IOException("Could not create temporary file.");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		network.networkToTSV(tmp);
		
		Network net = null;
		net = new Network(tmp.getAbsolutePath(), network.getLinkCount());
		this.links = net.getLinks();
		this.nodes = new LinkedHashSet<Node>(net.getNodes());
		tmp.delete();
		this.updateNodeConnectivity();
		// completely remove all references to the temporary network
		for (Node node: NodeCollection.returnAllRegisteredNodes()){
			node.clearNetwork(net);
		}
	}


	/**
	 * Creates a network by reading a certain amount of lines from a tsv file.
	 * 
	 * File should be formatted, each link separated by newline
	 * Each link should be formatted as: source[whitespace]target[whitespace] all other text will be ignored
	 * A LinkProperties object will be attached to each link, the ranking position equals the line number
	 * Checks that links need to be unique and cannot be self-regulating
	 * 
	 * @param tsvFile	string representation of file to read network from
	 * @param numberOfLines	links/lines to read (for big files)
	 */
	public Network(String tsvFile, int numberOfLines)

	{
		this.init();

		// Create a new file from the path
		File file = new File(tsvFile);

		// Init new reader
		BufferedReader reader = null;
		FileReader fReader = null;

		HashSet<Node> nod = new HashSet<Node>();

		// Try to create a reader from the file
		try {
			fReader = new FileReader(file);
			reader = new BufferedReader(fReader);

			// Read the first line
			String s = null;
			s = reader.readLine();

			int j = 0;

			// Read until the end
			while (s != null) {

				if (isWhiteSpace(s)) {
					s = reader.readLine();
					continue;
				}

				if (numberOfLines == j) {
					break;
				}

				j++;
				// Create a new link
				int firstTab = s.indexOf("\t");
				int secondTab = 0;
				if (firstTab == -1) {
					firstTab = s.indexOf(" ");
					secondTab = s.substring(firstTab + 1).indexOf(" ")
							+ firstTab + 1;
				}
				else {
					secondTab = s.substring(firstTab + 1).indexOf("\t")
							+ firstTab + 1;
				}

				String firstString = null;
				String secondString = null;

				firstString = s.substring(0, firstTab);
				secondString = s.substring(firstTab + 1, secondTab);
				firstString = firstString.replaceAll("\"", "");
				secondString = secondString.replaceAll("\"", "");


				if (firstString.equals(secondString)) {
					System.out.println("Adding a self-regulating link, this link will be deleted");
					s = reader.readLine();
					continue;
				}

				Node a;
				Node b;

				a = Node.createNode(firstString);
				b = Node.createNode(secondString);

				if (this.links.contains(new Link(a, b, new LinkProperties(false, j)))) {
					System.out.println("Adding a link which already exists during read, this link will be deleted "+ new Link(a, b, new LinkProperties(false,j)));
					s = reader.readLine();
					continue;
				}

				nod.add(a);
				nod.add(b);

				this.links.add(new Link(a, b, new LinkProperties(false, j)));

				s = reader.readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeErrorException(null, "Network IO error.");
		} finally {
			try {
				if (fReader != null) {
					fReader.close();
				}
				if (reader != null) {
					reader.close();
				}
			} catch (Exception e) {
				throw new RuntimeErrorException(null, "Network IO error.");
			}
		}
		this.nodes = new LinkedHashSet<Node>(nod);
		this.updateNodeConnectivity();

	}
	
	/**
	 * Writes this network to a tsv file in the following format.
	 * 
	 * Each link separated by newline in the order of ranking.
	 * Each link formatted as: source[whitespace]target[whitespace]ranking
	 * @param file	file to write the network to
	 * @param numberOfLines	links/lines to read (for big files)
	 */
	public void networkToTSV(File file) {
		
		
		// this.sortNetworkPredictionRank(); ? should we do this
		
		// _logger.info("writing network to file...");
		FileWriter writer = null;
		try {
			writer = new FileWriter(file);

			for (Link a : this.links) {
				writer.write(a.toString() + "\t"
						+ a.getLinkProperties().getPredictionRank() + "\n");
			}
			writer.flush();

		} catch (Exception e) {
			System.err.println(e.getStackTrace());
			throw new RuntimeErrorException(null, "Network output exception.");
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				throw new RuntimeErrorException(null,
						"Network output exception.");
			}
		}
	}


	/**
	 * Sorts the links by rank
	 */
	public void sortNetworkPredictionRank() {
		Collections.sort(this.links, Network.rankSorter);
	}

	/**
	 * Sorts the links by name
	 */
	public void sortNetworkNameLink() {
		Collections.sort(this.links, Network.nameSorter);
	}

	
	/**
	 * 
	 * Adds a link to the network permanently, returns false if links already exists or if it is a self-regulating link
	 * 
	 * @param link	the link to be added
	 * @return true if link was added, false otherwhise
	 */
	public boolean addLinkToNetwork(Link link) {

		Node a = link.getTf();
		Node b = link.getTarget();

		if (this.links.contains(link)) {
			System.out.println("Adding a link which already exists, removing this link.");
			return false;
		}

		if (a.equals(b)) {
			System.out.println("Adding a self-regulatory link, removing this link.");
			return false;
		}

		this.links.add(link);

		if (this.nodes.add(a)) {
			// if the node not in the network, we should reset the node
			a.resetNodeConnectivity(this);
		}

		if (this.nodes.add(b)) {
			// if the node not in the network, we should reset the node
			b.resetNodeConnectivity(this);
		}

		a.addOutGoingLink(this, b);
		b.addIncomingLink(this, a);

		return true;
	}


	/**
	 * Removes a link at a certain index permanently in the network. Use with care if the network is sorted by name.
	 * 
	 * @param index	the index at which the link is located
	 * @return	The link which was removed
	 */
	public Link removeLinkFromNetwork(int index) {

		Link link = this.links.remove(index);
		Node a = link.getTf();
		Node b = link.getTarget();
		b.removeIncomingLink(this, a);
		a.removeOutGoingLink(this, b);

		if (a.getIncomingLinks(this).isEmpty() && a.getOutGoingLinks(this).isEmpty()){
			this.nodes.remove(a);
		}
		
		if (b.getIncomingLinks(this).isEmpty() && b.getOutGoingLinks(this).isEmpty()){
			this.nodes.remove(b);
		}
		return link;
	}
	
	
	
	/**
	 * Removes the link specified from the network permanently.
	 * 
	 * @param e	the link to remove
	 * @return	The link which was removed
	 */
	public Link removeLinkFromNetwork(Link e) {
		return this.removeLinkFromNetwork(this.links.indexOf(e));
	}

	/**
	 * Returns the amount of nodes in the network (nodes have atleast one link) Ignores temporary changes to the network.
	 * @return
	 */
	public int getNodeCount() {
		return this.nodes.size();
	}

	/**
	 * Returns the amount of nodes in the network (nodes have atleast one link) Ignores temporary changes to the network.
	 * @return
	 */
	public int getLinkCount() {
		return this.links.size();
	}

	
	/**
	 * Adds a temporary link to the network
	 * @return always true
	 */
	public boolean addNonCommitalLinkToNetwork(Link a) {

		this.tmpAdded.add(a);
		return true;
	}
	
	/**
	 * Removes a temporary link from the network
	 * @return the link which was removed
	 */
	public Link removeNonCommitalLinkToNetwork(Link a) {

		this.tmpRemoved.add(a);
		return a;

	}
	
	/**
	 * Checks if the network is empty
	 * @return true if network does not contain links, false otherwhise
	 */
	public boolean isEmpty() {

		return this.links.isEmpty();

	}
	
	/**
	 * Overwrites hash code for network, network is uniquely defined by name
	 */
	public int hashCode() {
		return this.name.hashCode();
	}

	/**
	 * Overrides equals. Network's are equal if their names are by definition.
	 */
	public boolean equals(Object network) {

		if (network instanceof Network) {
			return ((Network) network).getName().equals(this.name);
		} else {
			return false;
		}

	}

	/**
	 * Reverts network to original state. All temporary links which were added or removed will be restored.
	 */
	public void revert() {

		this.tmpAdded.clear();
		this.tmpRemoved.clear();

	}
	
	/**
	 * Commits network by making all temporary modifications permanent.
	 */
	public void commit() {

		for (Link a : tmpAdded) {
			this.addLinkToNetwork(a);
		}
		for (Link a : tmpRemoved) {
			this.removeLinkFromNetwork(a);
		}
		this.tmpAdded.clear();
		this.tmpRemoved.clear();
	}
	/**
	 * Alters the current ranking by tie-breaking on the original ranking
	 * 
	 * @param interval	Partition sizes
	 * @param startPos	Start of the ranking to be resorted
	 * @param endPos	End of the ranking to be resorted
	 */
	public  void sortOriginalRankInterval (int interval, int startPos, int endPos){


		int i =0 ;
		while (true){

			int s = startPos+i;
			int e = startPos+i+interval -1;
					
			if (s >=endPos){
				break;
			}
			if (e > endPos){
				e = endPos;
			}
			if (s == e){ 
				break;
			}
			for (int k = s+1 ; k <= e ; k++  ){
				
				Link tbSorted = links.get(k);
				int insert = 1 ;
				while ( (k-insert >=s)  &&   links.get(k-insert).getLinkProperties().getOriginalPredictionRank() > tbSorted.getLinkProperties().getOriginalPredictionRank() ){
					
					links.set(k-insert+1, links.get(k-insert));
					insert ++;
				}
				links.set(k-insert+1, tbSorted);
			}
			
			i+= interval;
		}
		
		for (int j = 0 ; j < this.getLinkCount() ; j++){
			this.getLinks().get(j).getLinkProperties().setPredictionRank(j);
		}
		
	}


	
	
	// getters and setters
	
	
	/**
	 * Getter for network name
	 * @return the name of the network
	 */
	public String getName() {
		return name;
	}
	
	public ArrayList<Link> getLinks() {
		return links;
	}

	public void setLinks(ArrayList<Link> links) {
		this.links = links;
	}

	public LinkedHashSet<Node> getNodes() {
		return nodes;
	}

	public void setNodes(LinkedHashSet<Node> nodes) {
		this.nodes = nodes;
	}
	
	public ArrayList<Link> getNonCommittalDeleted() {
		return this.tmpRemoved;
	}

	public ArrayList<Link> getNonCommitalAdded() {
		return this.tmpAdded;
	}
	
	
	
	// private methods
	
	private void init() {

		this.links = new ArrayList<Link>();
		this.nodes = new LinkedHashSet<Node>();
		this.tmpAdded = new ArrayList<Link>();
		this.tmpRemoved = new ArrayList<Link>();

	}
	
	private void updateNodeConnectivity() {

		for (Node a : this.nodes) {
			a.resetNodeConnectivity(this);
		}

		for (Link link : this.links) {

			Node tf = link.getTf();
			Node target = link.getTarget();

			tf.addOutGoingLink(this, target);
			target.addIncomingLink(this, tf);
		}

	}

	
	// returns true for whitespace
	private boolean isWhiteSpace(String str) {
		int strLen;
		if (str == null || (strLen = str.length()) == 0) {
			return true;
		}
		for (int i = 0; i < strLen; i++) {
			if ((Character.isWhitespace(str.charAt(i)) == false)) {
				return false;
			}
		}
		return true;
	}
}
