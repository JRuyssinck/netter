package network.link.comparator;

import java.util.Comparator;

import network.link.Link;



/**
 * LinkRankComparator is a Comparator for Links to sort based on the ranking.
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class LinkRankComparator implements Comparator<Link> {

	
	/**
	 * Overrides compare. Compares links by ranking.
	 */
	@Override
	public int compare(Link a, Link b) {

		if ( a.getLinkProperties().getPredictionRank()  < b.getLinkProperties().getPredictionRank()){
			return -1;
		}
		else if ( a.getLinkProperties().getPredictionRank()  > b.getLinkProperties().getPredictionRank()){
			return +1;
		}
		else if ( a.getTf().getName().compareTo(b.getTf().getName()) ==0){
				
			return (a.getTarget().getName().compareTo(b.getTarget().getName()));
			
		}
		else{
			return (a.getTf().getName().compareTo(b.getTf().getName()));
		}
		
		
	}

	
	
}
