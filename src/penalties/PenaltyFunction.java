package penalties;

import network.Network;

/**
 * 
 * PenaltyFunction is an interface for all penalties, but ranking based as subnetwork based.
 * 
 * @author Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version 1.0
 * @since 0.0
 */
public interface PenaltyFunction {

	/**
	 * Calculates the metric from the current subnetwork/ranking
	 */
	public void calculateMetricFromScratch();
	
	/**
	 * Returns the current penalty metric (unmapped to the cost)
	 * 
	 * @return	 the penalty metric
	 */
	public double getMetric();
	
	/**
	 * Returns the current penalty score (metric mapped on cost function)
	 * @return	the penalty score
	 */
	public long   getPenaltyScore();
	
	/**
	 * The penalty name, used for logging functionality
	 * @return	the penalty name
	 */
	public String getPenaltyName();
	
	/**
	 * Calculates the new penalty score in an incremental way after (small) changes have been to the network
	 */
	public void updateIncrementalPenalty();
	
	/**
	 * Reverts the penalty score to the previous value, discarding any changes made by the latest incremental update
	 */
	public void revertIncrementalPenalty();
	
	/**
	 * Indicates if this penalty works on the ranking or on (sub)network level
	 * @return true if the penalty works on the ranking
	 */
	public boolean isGlobalPenalty ();
	
	/**
	 * Sets the network/ranking associated with this penalty
	 * 
	 * @param predictionNetwork	network or ranking associated with this penalty
	 */
	public void setNetwork(Network predictionNetwork);
	
	/**
	 * Gets the relative weight for this penalty
	 * @return the relative weight
	 */
	public double getCoef();
	
	/**
	 * Sets the relative  weight for this penalty
	 * @param coef the relative weight
	 */
	public void setCoef(double coef);
	
	
}
