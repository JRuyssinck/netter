package penalties.defined;

import java.util.Arrays;
import java.util.TreeMap;

import javax.management.RuntimeErrorException;

import network.Network;
import penalties.PenaltyFunction;
import extractors.GraphletCounterExtractor;


/**
 * GraphletG4Penalty penalty implements the cost function encouraging the appareance of G4 graphlets in the subnetworks
 * 
 * It is a (sub)network based penalty and supports incremental updates.
 * 
 * Please see the manuscript for the exact definition of this penalty.
 * 

 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class GraphletG4Penalty implements PenaltyFunction {

	/**
	 * Name of penalty
	 */
	private static final String NAME = "Graphlet-G4-penalty";
	
	/**
	 * Mapping of penalty metric to cost
	 */
	private TreeMap<Double,Integer> mappingPercentageToPenalty;
	
	/**
	 * GraphletCounterExtractor used to calculate the metric
	 */
	private GraphletCounterExtractor extractor ;
	
	/**
	 * The network on which the metric is calculated
	 */
	private Network network ;
	
	/**
	 * Indicates if the penalty has been calculated (cannot incremental update on an initialized penalty)
	 */
	private boolean inited = false;
	
	/**
	 * Relative weight of penalty
	 */
	private double coef ;
	
	
	/**
	 * Constructs a new GraphletG4Penalty, the arguments are specified as an String[] passed as an object for classloading.
	 * 
	 * @param argumentList	an Object in the form of a String array
	 */
	public GraphletG4Penalty(Object argumentList){
		
		String[] objectToBeParsed = (String[]) argumentList;
		this.extractor = new GraphletCounterExtractor();

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
		double percentage = 0.0;
		percentage = extractor.getFrequency()[4];
		
		if (percentage < 0.0){
			throw new RuntimeErrorException(null);
		}
		return percentage;
	}
	@Override
	public long getPenaltyScore() {
		if (!inited ){
			if (this.network ==null){
				throw new RuntimeErrorException(null);
			}
			this.updatePenalty(network);
			inited = true;
		}
		double percentage = 0.0;
		percentage = extractor.getFrequency()[4];
		return this.percentageToPenalty(percentage);

	}
	@Override
	public String getPenaltyName() {
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
	
	
	// private method
	
	
	
	
	private void updatePenalty(Network network){
		this.extractor.setNetwork (network);
		this.extractor.update();
	}
	
	
	private void parseAndSetMapping(String[] mapping){
		
		this.mappingPercentageToPenalty = new TreeMap<Double,Integer>();
		for (int k = 0 ; k <  mapping.length ; k+=2){
			mappingPercentageToPenalty.put(Double.parseDouble(mapping[k]),Integer.parseInt(mapping[k+1]));
		}
	}
	
	private void setDefaultMapping(){
		
		
		this.mappingPercentageToPenalty = new TreeMap<Double,Integer>();
		this.mappingPercentageToPenalty.put(-0.01,7000); 
		this.mappingPercentageToPenalty.put(0.6, 1000 );
		this.mappingPercentageToPenalty.put(0.75, 500);
		this.mappingPercentageToPenalty.put(0.85, 200 );
		this.mappingPercentageToPenalty.put(0.90, 0);
		this.mappingPercentageToPenalty.put(0.95, 0);
		this.mappingPercentageToPenalty.put(1.01,800); 
		
		
	}
	
	

	private long percentageToPenalty(double percentage) {
		
		double highestKey =0.0;
		double lowestKey = 0.0;
		
	
		highestKey=  this.mappingPercentageToPenalty.higherEntry(percentage).getKey();
		lowestKey = this.mappingPercentageToPenalty.lowerEntry(percentage).getKey();
		

		int highest=  this.mappingPercentageToPenalty.higherEntry(percentage).getValue();
		int lowest = this.mappingPercentageToPenalty.lowerEntry(percentage).getValue();
		
		
		double rico = ((double)(highest-lowest))/ (double)((highestKey-lowestKey)) ;
		double x = (  (rico * percentage)   +  ( lowest - ( rico*lowestKey)))     ;
		
		return (long) x;
	}

	
	
}
