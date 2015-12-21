package network.node;

import java.util.Collection;
import java.util.HashMap;

import network.Network;

/**
 * NodeCollection is responsible for keeping a collection of all existing nodes
 * in the program and for providing the correct reference to a name, identified
 * by its name.
 * 
 * 
 * @author Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version 1.0
 * @since 0.0
 */
public class NodeCollection {

	/**
	 * Static collection of all nodes represented by an unique name
	 */
	private static HashMap<String, Node> nodes = new HashMap<String, Node>();

	/**
	 * Returns the node identified by a certain name. If non-existing return
	 * null
	 * 
	 * @param name	the name of requested node
	 * @return the node or null if no node exists with that name
	 */
	public synchronized static Node getNode(String name) {
		return nodes.get(name);
	}

	/**
	 * Adds a new node to the collection. Removes any node that existed before
	 * with the same name.
	 * 
	 * @param name	the node to be added to the collection
	 */
	public synchronized static void addNode(Node node) {
		nodes.put(node.getName(), node);
		return;
	}
	
	/**
	 * Returns a collection of all nodes currently registered in the system
	 * 
	 * @return collection of all nodes
	 */
	public synchronized static Collection<Node> returnAllRegisteredNodes() {
		
		return nodes.values();
	
	}

	/**
	 * Clears all the mapping in all nodes. Then unsubscribes all nodes from the system.
	 * 
	 */
	public synchronized static void reset() {
		
		for (Node node : nodes.values()) {
			node.clear();
		}
		nodes.clear();
	}

}
