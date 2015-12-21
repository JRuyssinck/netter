package network.link;

import javax.management.RuntimeErrorException;


/**
 * LinkProperties is the class representing all additional properties a Link can have besides the defining properties: target and source node.
 * 
 * LinkProperties e.g. holds properties which can be used in temporary incremental updates of the ranking or to determine if a link is a true positive link
 *  * 
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class LinkProperties {

	/**
	 * Indicates if this link is a true positive link according to a gold standard.
	 */
	private boolean trueLink;
	
	/**
	 * The current position in the ranking of this link
	 */
	private int predictionRank;
	
	/**
	 * The position in the ranking of this link when this link was created
	 */
	private int originalPredictionRank ;
	
	/**
	 * The previous position in the ranking of this link
	 */
	private int previousPredictionRank =-1;
	
	/**
	 * The upper bound in the ranking this link can reach before this link changes to a different network partition
	 * 
	 * @see NetworkModifier
	 */
	private int upperBoundSet =-1;
	
	/**
	 * The lower bound in the ranking this link can reach before this link changes to a different network partition
	 * 
	 *  @see NetworkModifier
	 */
	private int lowerBoundSet = -1;
	
	/**
	 * The previous upper bound in the ranking this link can reach before this link changes to a different network partition
	 * 
	 * @see NetworkModified
	 */
	private int previousUpperBound = -1;
	
	/**
	 * The previous lower bound in the ranking this link can reach before this link changes to a different network partition
	 * 
	 *  @see NetworkModifier
	 */
	private int previousLowerBound = -1;
	
	/**
	 * The current partition this set is in
	 * 
	 *  @see NetworkModifier
	 */
	private int currentSet = -1;
	
	
	/**
	 * The previous partition this set was in
	 * 
	 *  @see NetworkModifier
	 */
	private int previousSet = -1;
	
	/**
	 * Constructs a LinkProperties with a certain position in the ranking and indicates if this is a true positive link
	 * @param trueLink	if this a true positive link
	 * @param predictionRank	the position in the ranking
	 */
	public LinkProperties(boolean trueLink,int predictionRank){
		
		this.trueLink = trueLink;
		this.predictionRank = predictionRank ;
		this.originalPredictionRank = predictionRank;
		
	}
	
	/**
	 * This method reverts the last changes made to this link and goes back to the previous state.  One cannot revert twice in a row.
	 */
	public void revert(){
		
		if(this.previousPredictionRank ==-1){
			throw new RuntimeErrorException(null,"Cannot revert a link more than once in a row.");
		}
		
		this.predictionRank = this.previousPredictionRank;
		this.previousPredictionRank = -1;
		
		if (this.previousSet != -1){
			this.upperBoundSet = this.previousUpperBound ;
			this.previousUpperBound =-1;
			this.lowerBoundSet = this.previousLowerBound;
			this.previousLowerBound =-1;
			this.currentSet = this.previousSet;
			this.previousSet = -1;
		}
	}
	
	/**
	 * This method confirms the latest changes to this link and makes them permanent, such that a revert is no longer possible.
	 */
	public void commit(){
		
		this.previousPredictionRank = -1;
		this.previousUpperBound =-1;
		this.previousLowerBound =-1;
		this.previousSet = -1;

		
	}
	
	
	/*
	 * Getters and setters
	 */
	
	public int getPreviousUpperBound() {
		return previousUpperBound;
	}

	public void setPreviousUpperBound(int previousUpperBound) {
		this.previousUpperBound = previousUpperBound;
	}

	public int getPreviousLowerBound() {
		return previousLowerBound;
	}

	public void setPreviousLowerBound(int previousLowerBound) {
		this.previousLowerBound = previousLowerBound;
	}

	
	public int getPreviousSet() {
		return previousSet;
	}

	public void setPreviousSet(int previousSet) {
		this.previousSet = previousSet;
	}

	public int getUpperBoundSet() {
		return upperBoundSet;
	}

	public void setUpperBoundSet(int upperBoundSet) {
		this.upperBoundSet = upperBoundSet;
	}

	public int getCurrentSet() {
		return currentSet;
	}

	public void setCurrentSet(int currentSet) {
		this.currentSet = currentSet;
	}

	public int getLowerBoundSet() {
		return lowerBoundSet;
	}

	public void setLowerBoundSet(int lowerBoundSet) {
		this.lowerBoundSet = lowerBoundSet;
	}


	public int getOriginalPredictionRank() {
		return originalPredictionRank;
	}


	public void setOriginalPredictionRank(int originalPredictionRank) {
		this.originalPredictionRank = originalPredictionRank;
	}


	public int getPreviousPredictionRank() {
		return previousPredictionRank;
	}


	public void setPreviousPredictionRank(int previousPredictionRank) {
		this.previousPredictionRank = previousPredictionRank;
	}

	public boolean isTrueLink() {
		return trueLink;
	}

	public void setTrueLink(boolean trueLink) {
		this.trueLink = trueLink;
	}

	public int getPredictionRank() {
		return predictionRank;
	}

	public void setPredictionRank(int predictionRank) {
		this.predictionRank = predictionRank;
	}
	
	
	
	
}
