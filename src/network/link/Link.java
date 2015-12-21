package network.link;

import network.node.Node;


/**
 * Link is the class representing a directed link between a transcription factor or regulatory gene and a target node. 
 * 
 * A LinkProperties is attached which can hold additional information about the link.
 * 
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class Link
{

	/**
	 * The Node from which this links originates
	 */
	private Node tf;

	/**
	 * The Node which this link targets
	 */
	private Node target;

	/**
	 * Additional properties of this link
	 */
	private LinkProperties linkProperties;


	/**
	 * Constructs a new link with associated LinkProperties
	 * 
	 * @param tf	Source node
	 * @param target Target node
	 * @param linkProperties	Properties of the link
	 */
	public Link(Node tf, Node target, LinkProperties linkProperties)
	{
		this.tf = tf;
		this.target = target;
		this.linkProperties = linkProperties;
	}

	
	/**
	 * Overwrites equals method. We define a link to be equal if the target and source node are identical, regardless of the LinkProperties.
	 */
	public boolean equals (Object b){
		if ((b instanceof Link)){
			Link link = (Link) b;
			if(this.target.equals(link.getTarget()) && this.tf.equals(link.getTf()) ){
				return true;
			}
		}
		return false;
	}

	
	/**
	 * Overwrites hash value of this class, to be compatible with the new equals method.
	 */
	public int hashCode (){
		return (this.tf.hashCode() /2 + this.target.hashCode()/2);
	}

	/**
	 * The String reprensentation of this class.
	 */
	public String toString()
	{

		return (this.tf.toString() + "\t" + this.target.toString());
	}

	/**
	 * Getter for source node/transcription factor
	 * @return the source node
	 */
	public Node getTf()
	{
		return tf;
	}

	/**
	 * Setter for source node/transcrption factor.
	 * @param tf	the source node to be set
	 */
	public void setTf(Node tf)
	{
		this.tf = tf;
	}

	/**
	 * Getter for target node
	 * @return the target node
	 */
	public Node getTarget()
	{
		return target;
	}

	/**
	 * Setter for target node
	 * @param target	the target node
	 */
	public void setTarget(Node target)
	{
		this.target = target;
	}

	/**
	 * Getter for the properties
	 * @return the link properties
	 */
	public LinkProperties getLinkProperties()
	{
		return linkProperties;
	}

	/**
	 * Setter for the link properties
	 * 
	 * @param linkProperties link properties
	 */
	public void setLinkProperties(LinkProperties linkProperties)
	{
		this.linkProperties = linkProperties;
	}

	/**
	 * Helper method which returns true of the given link is the opposite link of this.
	 * 
	 * @param link the link to be checked
	 * @return true if the link is the opposite link, false otherwhise
	 */
	public boolean isOppositeLink(Link link){

		if(this.target.equals(link.getTf()) && this.tf.equals(link.getTarget()) ){	
			return true;
		}
		else{
			return false;
		}
	}


}
