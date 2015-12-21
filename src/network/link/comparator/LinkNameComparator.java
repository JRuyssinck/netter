package network.link.comparator;

import java.util.Comparator;

import network.link.Link;


/**
 * LinkNameComparator is a Comparator for Links to sort based on node names.
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class LinkNameComparator implements Comparator<Link>{

	
	/**
	 * Overrides compare. Compares links by name.
	 */
	@Override
	public int compare(Link a, Link b) {

		if (a.getTf().getName().equals(b.getTf().getName())){
			return a.getTarget().getName().compareTo(b.getTarget().getName());
		}
		else {
			return a.getTf().getName().compareTo(b.getTf().getName());
		}

		
		
	}
}
