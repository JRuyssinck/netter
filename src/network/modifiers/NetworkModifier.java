package network.modifiers;

/**
 * NetworkModifier is an interface listing all actions a mutator should have.
 * 
 * It should be able to return the Network is working on and implement the three basic actions: modify, revert and commit.
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
import network.Network;

public interface NetworkModifier {

	
	/**
	 * Mutation operation
	 */
	public void modify ();
	
	/**
	 * Revert back to previous state
	 */
	public void revert ();
	
	/**
	 * Make the last mutation permanent
	 */
	public void commit();	
	
	
	/**
	 * Return the current network/ranking the modifier is working on
	 * 
	 * @return	the network/ranking that is being used
	 */
	public Network getNetwork();

}
