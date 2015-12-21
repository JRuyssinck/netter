package penalties.defined;

import javax.management.RuntimeErrorException;
import network.Network;
import network.link.Link;
import penalties.PenaltyFunction;


/**
 * PredictionConfidencePenalty penalty implements the cost function diverging from the original ranking. i.e. regularizing effect
 * 
 * It is a ranking based penalty and does not support incremental updates.
 * 
 * Please see the manuscript for the exact definition of this penalty.
 * 

 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class PredictionConfidencePenalty  implements PenaltyFunction{



	/**
	 * Indicates if the penalty has been calculated
	 */
	private boolean inited ;
	
	/**
	 * The network on which the metric is calculated
	 */
	private Network network ;
	
	/**
	 * Current cost of this penalty
	 */
	private long predictionDiffSum ;
	
	/**
	 * Name of penalty
	 */
	private static final String NAME = "Prediction-Confidence-penalty";
	
	/**
	 * Relative weight of penalty
	 */
	private double coef;
	
	/**
	 * Constructs a new PredictionConfidencePenalty, the arguments are specified as an String[] passed as an object for classloading.
	 * 
	 * @param argumentList	an Object in the form of a String array
	 */
	public PredictionConfidencePenalty (Object argumentList){
		
		String[] objectToBeParsed = (String[]) argumentList;
		
		this.predictionDiffSum = -1 ;
		this.inited = false;
		// try to parse the argumentList
		try {
			// first argument should be the coef 
			Double coef = Double.parseDouble(objectToBeParsed[0]);
			this.coef = coef; 
		}catch(Exception e){
			System.err.println("Error while initating an"+ NAME+ "y. Exiting.");
		}

	}
	
	@Override
	public String getPenaltyName(){
		return NAME;
	}
	
	@Override
	public void calculateMetricFromScratch(){
		if (!inited){
			if (this.network ==null){
				throw new RuntimeErrorException(null);
			}
		}
		this.updatePenalty(network);
		inited = true;
	}

	@Override
	public double getMetric(){
		throw new UnsupportedOperationException("PredictionConfidencePenalty is not based on a network metric. Please use 'getPenaltyScore'.");
	}
	
	@Override
	public long getPenaltyScore() {
		return this.predictionDiffSum;
	}

	@Override
	public void updateIncrementalPenalty() {
		throw new UnsupportedOperationException("Global penalties do not support incremental updates.");
	}

	@Override
	public void revertIncrementalPenalty() {
		throw new UnsupportedOperationException("Global penalties do not support incremental updates.");
	}

	@Override
	public boolean isGlobalPenalty (){
		return  true;
	}

	@Override
	public void setNetwork(Network predictionNetwork) {
		this.network = predictionNetwork;
	}

	@Override
	public double getCoef() {
		return this.coef;
	}

	@Override
	public void setCoef(double coef) {
		this.coef = coef;
	}

	
	// Private methods
	
	private void updatePenalty(Network update) {
		
		this.network = update;
		int tmp = 0;
		for (Link a : network.getLinks()){
				long tmp2 = (long) Math.pow(a.getLinkProperties().getPredictionRank() - a.getLinkProperties().getOriginalPredictionRank(),2);
				tmp += tmp2;
		}
		this.predictionDiffSum = tmp;		
	}
}
