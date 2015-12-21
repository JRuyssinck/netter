package network.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import network.Network;



/**
 * Node is the class representing a vertex or node in the graph/network.
 * 
 * Creating a node should happen statically using the method 'createNode', as the same node can be part of different networks for performance reasons.
 * The class NodeCollection is responsible for keeping track of all the nodes.
 * Nodes should have an unique name.
 * 
 * Each node keeps track of it's incoming and outgoing links. It also keeps track of it's bidirectional links and unidirectional links
 * separately for performance reasons.
 * 
 * 
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class Node {
	
	/**
	 * The node's unique name
	 * 
	 */
	private String name;
	
	/**
	 * List of nodes at the start of an incoming edge ending in this node for a certain network context
	 * 
	 */
	private volatile HashMap<Network, ArrayList<Node>> incomingLinks;
	
	/**
	 * List of nodes at the end of an outgoing edge starting in this node for a certain network context
	 * 
	 */
	private volatile HashMap<Network, ArrayList<Node>> outGoingLinks;
	
	/**
	 * List of nodes which have both an incoming as an outgoing edge to this node for a certain network context
	 * 
	 */
	private volatile HashMap<Network, ArrayList<Node>> biDirectionalLinks;
	
	/**
	 * List of nodes which have at least an outgoing or an incoming edge that connects them to this node for a certain network context
	 * 
	 */
	private volatile HashMap<Network, HashSet<Node>> uniDirectionalLinks;

	
	/**
	 * 
	 * No visible constructor, use 'createNode' instead
	 * 
	 */
	private Node() {}
	
	
	/**
	 * 
	 * No visible constructor. This private constructor creates a new node with a certain name and initializes the link-maps.
	 * 
	 * @param name	name of the node
	 * 
	 */
	private Node(String name) {
	
		this.name = name;
		this.incomingLinks = new HashMap<Network, ArrayList<Node>>();
		this.outGoingLinks = new HashMap<Network, ArrayList<Node>>();
		this.biDirectionalLinks = new HashMap<Network, ArrayList<Node>>();
		this.uniDirectionalLinks = new HashMap<Network, HashSet<Node>>();
	}

	
	/**
	 * 
	 * Method responsible for creating a node. It will first poll the NodeCollection to check if the node exists, else it will create a new node and register with the collection.
	 * 
	 * @param name	name of the node to be created
	 * @return a node with that specific unique name
	 */
	public synchronized static Node createNode(String name) {
		Node tmp = null;
		tmp = NodeCollection.getNode(name);
		if (tmp == null) {
			tmp = new Node(name);
			NodeCollection.addNode(tmp);
			return tmp;
		} else {
			return tmp;
		}
	}

	/**
	 * 
	 * Adds an incoming link to this node coming from another node a.
	 * 
	 * @param net	network context of the link
	 * @param node  node to be connected by the link
	 * 
	 */
	public synchronized void addIncomingLink(Network net, Node node) {

		ArrayList<Node> nodes = incomingLinks.get(net);
		nodes.add(node);
		if (outGoingLinks.get(net).contains(node)) {
			this.biDirectionalLinks.get(net).add(node);
		} else {
			this.uniDirectionalLinks.get(net).add(node);
		}
	}

	/**
	 * 
	 * Adds an outgoing link to this node coming from another node a.
	 * 
	 * @param net	network context of the link
	 * @param node  node to be connected by the link
	 * 
	 */
	public synchronized void addOutGoingLink(Network net, Node node) {

		ArrayList<Node> nodes = outGoingLinks.get(net);

		nodes.add(node);

		if (incomingLinks.get(net).contains(node)) {
			this.biDirectionalLinks.get(net).add(node);
		} else {
			this.uniDirectionalLinks.get(net).add(node);
		}

	}

	
	/**
	 * 
	 * Removes an incoming link to this node coming from another node a.
	 * 
	 * @param net	network context of the link
	 * @param node  node to removed 
	 * 
	 */
	public synchronized void removeIncomingLink(Network net, Node node) {

		this.incomingLinks.get(net).remove(node);

		if (outGoingLinks.get(net).contains(node)) {
			this.biDirectionalLinks.get(net).remove(node);
		} else {
			this.uniDirectionalLinks.get(net).remove(node);
		}

	}
	
	/**
	 * 
	 * Removes an outgoing link to this node coming from another node a.
	 * 
	 * @param net	network context of the link
	 * @param node  node to removed 
	 * 
	 */
	public synchronized void removeOutGoingLink(Network net, Node node) {

		this.outGoingLinks.get(net).remove(node);

		if (incomingLinks.get(net).contains(node)) {
			this.biDirectionalLinks.get(net).remove(node);
		} else {
			this.uniDirectionalLinks.get(net).remove(node);
		}

	}

	
	/**
	 * 
	 * Returns list of nodes connected by a birectional link in a given network context
	 * 
	 * @param net	network context of the link
	 * @return list of nodes connected by bidirectional link
	 * 
	 */
	public synchronized ArrayList<Node> getBiDirectionalLinks(Network network) {
		return biDirectionalLinks.get(network);
	}
	
	
	/**
	 * 
	 * Returns list of nodes connected by an incoming link in a given network context
	 * 
	 * @param net	network context of the link
	 * @return list of nodes connected by incoming link
	 * 
	 */
	public synchronized ArrayList<Node> getIncomingLinks(Network net) {
		return incomingLinks.get(net);
	}

	
	/**
	 * 
	 * Returns list of nodes connected by an outgoing link in a given network context
	 * 
	 * @param net	network context of the link
	 * @return list of nodes connected by outgoing link
	 * 
	 */
	public synchronized ArrayList<Node> getOutGoingLinks(Network net) {
		return outGoingLinks.get(net);
	}

	
	/**
	 * 
	 * Returns list of nodes connected by any type of link in a given network context
	 * 
	 * @param net	network context of the link
	 * @return list of nodes connected by link
	 * 
	 */
	public synchronized HashSet<Node> getUnidirectedLinks(Network net) {
		return uniDirectionalLinks.get(net);
	}

	/**
	 * 
	 * Getter for name of node
	 * 
	 * @return node's name
	 * 
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * 
	 * Setter for name of node
	 * 
	 * @param node's name
	 * 
	 */
	public void setName(String name) {
		this.name = name;
	}

	
	/**
	 * 
	 * Overrides equals method, two nodes should be identical if their name is the same
	 * 
	 * @param any object to compare to
	 * 
	 */
	@Override
	public boolean equals(Object a) {
		if (a instanceof Node) {
			return (this.name.equals(((Node) a).getName()));
		} else {
			return false;
		}
	}
	/**
	 * 
	 * Overrides to String method, simple prints the name of node
	 * 
	 * @return toString representation of the Node
	 * 
	 */
	public String toString() {
		return this.name;
	}
	
	
	/**
	 * 
	 * Overrides hashcode to be represented by the name of the node since this is unique
	 * 
	 * @return the hashcode representation of this object
	 * 
	 */
	public int hashCode() {
		return this.getName().hashCode();
	}

	/**
	 * 
	 * Resets this node completely for all networks, removing all links
	 * 
	 * 
	 */
	public synchronized void clear() {

		this.incomingLinks = new HashMap<Network, ArrayList<Node>>();
		this.outGoingLinks = new HashMap<Network, ArrayList<Node>>();
		this.biDirectionalLinks = new HashMap<Network, ArrayList<Node>>();
		this.uniDirectionalLinks = new HashMap<Network, HashSet<Node>>();

	}

	/**
	 * 
	 * Removes all links of this node in a given network, use with caution as the network representation can become inconsistent.
	 * In general, this method should only be invoked if a given network should be removed.
	 * 
	 * @param the network context to be removed
	 * 
	 */
	public synchronized void clearNetwork(Network network) {

		incomingLinks.remove(network);
		outGoingLinks.remove(network);
		biDirectionalLinks.remove(network);
		uniDirectionalLinks.remove(network);

	}
	
	/**
	 * 
	 * Similar to 'clearNetwork' this method resets a given network context. In contrast, a ready to use empty network context is set-up.
	 * ArrayList is re-used if existed to save new object creation time.
	 * 
	 * @param the network context to be reset
	 * 
	 */
	public synchronized void resetNodeConnectivity(Network network) {

		ArrayList<Node> tmp = incomingLinks.get(network);

		if (tmp != null) {
			tmp.clear();
		} else {
			incomingLinks.put(network, new ArrayList<Node>());
		}

		tmp = outGoingLinks.get(network);

		if (tmp != null) {
			tmp.clear();
		} else {
			outGoingLinks.put(network, new ArrayList<Node>());
		}

		tmp = biDirectionalLinks.get(network);
		if (tmp != null) {
			tmp.clear();
		} else {
			biDirectionalLinks.put(network, new ArrayList<Node>());
		}

		HashSet<Node> tmp3 = uniDirectionalLinks.get(network);
		if (tmp3 != null) {
			tmp3.clear();
		} else {
			uniDirectionalLinks.put(network, new HashSet<Node>());
		}

	}

}
