package penalties.defined;

import java.util.Arrays;
import java.util.TreeMap;

import javax.management.RuntimeErrorException;

import penalties.PenaltyFunction;
import network.Network;
import extractors.GraphletCounterExtractor;
import extractors.TranscriptionFactorExtractor;

/**
 * TFSparsityPenalty  implements the the cost function penalizing a large amount of nodes in the network with atleast one outgoing link.
 * 
 * It is a (sub)network based penalty and supports incremental updates. * 
 * Please see the manuscript for the exact definition of this penalty.
 * 

 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class TFSparsityPenalty implements PenaltyFunction{


	


	
	/**
	 * Mapping of penalty metric to cost
	 */
	private TreeMap<Double,Integer> mappingPercentageToPenalty;
	/**
	 * Extractor used to calculate the metric
	 */
	private TranscriptionFactorExtractor extractor ;
	/**
	 * Name of penalty
	 */
	private static final String NAME = "TF_spartity_penalty";
	/**
	 * The network on which the metric is calculated
	 */
	private Network network ;
	/**
	 * Indicates if the penalty has been calculated (cannot incremental update on an initialized penalty)
	 */
	private boolean inited;
	/**
	 * Relative weight of penalty
	 */
	private double coef ;
	
	
	/**
	 * Constructs a new TFSparsityPenalty, the arguments are specified as an String[] passed as an object for classloading.
	 * 
	 * @param argumentList	an Object in the form of a String array
	 */
	public TFSparsityPenalty(Object argumentList){
		
		String[] objectToBeParsed = (String[]) argumentList;
		
		this.inited =false;
		this.extractor = new TranscriptionFactorExtractor();

		try {
			// first argument should be the coef 
			Double coef = Double.parseDouble(objectToBeParsed[0]);
			this.coef = coef; 

			if (objectToBeParsed.length> 1){
				// second argument should be custom mapping
				String[] subset =  Arrays.copyOfRange(objectToBeParsed, 1, objectToBeParsed.length);
				this.parseAndSetMapping(subset);
			}else{
				// set the default mapping
				this.setDefaultMapping();	
			}
			
		}catch(Exception e){
			System.err.println("Error while initating an"+ NAME+ "y. Exiting.");
		}
	
		
		this.extractor.setNetwork(network);
		
	}
	@Override
	public boolean isGlobalPenalty (){
		
		return false;
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
		
		if (!inited ){
			if (this.network ==null){
				throw new RuntimeErrorException(null);
			}
			this.updatePenalty(network);
			inited = true;
		}
		
		int amountOfNodesWithNoOutgoingLinks ;
		int networkSize ;
		double percentage = 0.0;
		
		amountOfNodesWithNoOutgoingLinks = extractor.getNodeSize() -extractor.getAmountOfTF();
		networkSize = extractor.getNodeSize();		
		percentage = (double) amountOfNodesWithNoOutgoingLinks/ ((double)networkSize);		
		
	
		return percentage;
	}



	@Override
	public long getPenaltyScore() {
		int amountOfNodesWithNoOutgoingLinks ;
		int networkSize ;
		double percentage = 0.0;
		amountOfNodesWithNoOutgoingLinks = extractor.getNodeSize() -extractor.getAmountOfTF();
		networkSize = extractor.getNodeSize();		
		percentage = (double) amountOfNodesWithNoOutgoingLinks/ ((double)networkSize);		
		return percentageToPenalty(percentage);

	}
	
	@Override
	public String getPenaltyName(){
		return NAME;
	}

	@Override
	public void updateIncrementalPenalty() {
		
		this.extractor.setNetwork (network);
		this.extractor.incrementalUpdate(network.getNonCommittalDeleted(), network.getNonCommitalAdded());
	
	}
	@Override
	public void revertIncrementalPenalty() {
		this.extractor.revert();
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
	
	
	// private methods
	
	private void parseAndSetMapping(String[] mapping){
		
		this.mappingPercentageToPenalty = new TreeMap<Double,Integer>();
		
		for (int k = 0 ; k <  mapping.length ; k+=2){
			mappingPercentageToPenalty.put(Double.parseDouble(mapping[k]),Integer.parseInt(mapping[k+1]));
		}
	}
	
	private void setDefaultMapping() {
		
		
		this.mappingPercentageToPenalty = new TreeMap<Double,Integer>();
		this.mappingPercentageToPenalty.put(-0.01,1000);
		this.mappingPercentageToPenalty.put(0.25, 500);
		this.mappingPercentageToPenalty.put(0.5, 200);
		this.mappingPercentageToPenalty.put(0.6, 100 );
		this.mappingPercentageToPenalty.put(0.75, 0);
		this.mappingPercentageToPenalty.put(0.9, 100 );
		this.mappingPercentageToPenalty.put(0.95, 150);
		this.mappingPercentageToPenalty.put(1.01,800); 
		
		
	}

	private void updatePenalty(Network network){
		this.extractor.setNetwork (network);
		this.extractor.update();
	}
	
	private long percentageToPenalty (double percentage){
		
		double highestKey =0.0;
		double lowestKey = 0.0;
		
		try {
		 highestKey=  this.mappingPercentageToPenalty.higherEntry(percentage).getKey();
		 lowestKey = this.mappingPercentageToPenalty.lowerEntry(percentage).getKey();
		}
		
		catch (Exception e){
			System.err.println(percentage);
		}
		
		int highest=  this.mappingPercentageToPenalty.higherEntry(percentage).getValue();
		int lowest = this.mappingPercentageToPenalty.lowerEntry(percentage).getValue();
		double rico = ((double)(highest-lowest))/ (double)((highestKey-lowestKey)) ;
		double x = (  (rico * percentage)   +  ( lowest - ( rico*lowestKey)))     ;
		
		return (long) x;
	}
	
	
}
