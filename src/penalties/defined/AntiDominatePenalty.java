package penalties.defined;

import java.util.Arrays;
import java.util.TreeMap;

import javax.management.RuntimeErrorException;

import penalties.PenaltyFunction;
import network.Network;
import extractors.DominatingExtractor;


/**
 * Anti-Dominate penalty implements the cost function penalizing rankings which focus too much on 1 part of the network.
 * 
 * It is a (sub)network based penalty and supports incremental updates.
 * 
 * Please see the manuscript for the exact definition of this penalty.
 * 

 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class AntiDominatePenalty implements PenaltyFunction{

	/**
	 * Name for this penalty
	 */
	private static final String NAME = "Anti-Dominating Penalty";
	
	/**
	 * Mapping the penalty metric to the cost
	 */
	private TreeMap<Double,Integer> mappingPercentageToPenalty;
	
	/**
	 * DominatingExtractor class used to calculate this penalty
	 */
	private DominatingExtractor extractor ;
	
	/**
	 * Network used to calculate the metric for
	 */
	private Network network ;
	
	/**
	 * Indicates if the penalty has been calculated (cannot incremental update on an initialized penalty)
	 */
	private boolean inited = false;
	
	/**
	 * Relative weight of this penalty
	 */
	private double coef ;
	

	/**
	 * Constructs a new AntiDominatePenalty, the arguments are specified as an String[] passed as an object for classloading.
	 * 
	 * @param argumentList	an Object in the form of a String array
	 */
	public AntiDominatePenalty(Object argumentList){
		
		
		String[] objectToBeParsed = (String[]) argumentList;

		this.extractor = new DominatingExtractor();
		// try to parse the argumentList
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
	
	// For documentation, see interface
	
	@Override
	public String getPenaltyName() {
		return NAME;
	}
	@Override
	public boolean isGlobalPenalty (){
		return false;
	}
	@Override
	public double getMetric() {	
		if (!inited ){
			if (this.network ==null){
				throw new RuntimeErrorException(null);
			}
			this.updatePenalty(network);
			inited = true;
		}	
		double percentage = 0.0;
		percentage = extractor.getDominatorPercentage();
		return percentage;
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
	public long getPenaltyScore() {
		if (!inited){
			if (this.network ==null){
				throw new RuntimeErrorException(null);
			}
			this.updatePenalty(network);
			inited = true;
		}
		double percentage = 0.0;
		percentage = extractor.getDominatorPercentage();
		return this.percentageToPenalty(percentage);
	}
	@Override
	public void calculateMetricFromScratch() {


		if (!inited){
			if (this.network ==null){
				throw new RuntimeErrorException(null);
			}
		}
		this.updatePenalty(network);
		inited = true;
		
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

	private void updatePenalty(Network network){
		this.extractor.setNetwork(network);
		this.extractor.update();
	}

	private long percentageToPenalty(double percentage) {
		
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

	
	private void parseAndSetMapping(String[] mapping){
		
		

		this.mappingPercentageToPenalty = new TreeMap<Double,Integer>();
		
		for (int k = 0 ; k <  mapping.length ; k+=2){
			mappingPercentageToPenalty.put(Double.parseDouble(mapping[k]),Integer.parseInt(mapping[k+1]));
		}
		
	
		
		
	}
	
	private void setDefaultMapping(){
		
		
		System.err.println("Default mapping anti");

		this.mappingPercentageToPenalty = new TreeMap<Double,Integer>();
		this.mappingPercentageToPenalty.put(-0.01,0); 
		this.mappingPercentageToPenalty.put(0.10, 0 );
		this.mappingPercentageToPenalty.put(1.0, 900);
		this.mappingPercentageToPenalty.put(1.01,2000); 
		
		
	}




}
