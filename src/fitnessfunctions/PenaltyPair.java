package fitnessfunctions;


/**
 * PenaltyPair is a small helper class representing a penaltyname-currentfitness pair.
 * 
 * 
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class PenaltyPair {

	/**
	 * The penalty name.
	 */
	private String name;
	
	/**
	 * The penalty's current fitness
	 */
	private double score;
	
	
	/**
	 * Constructs a PenaltyPair with the given name and score
	 * 
	 * @param name the penalty's name
	 * @param score the fitness of the penalty
	 * 
	 */
	public PenaltyPair(String name , double score){
		this.name= name;
		this.score=score;
	}


	/**
	 * Getter for the penalty name
	 * 
	 * @return the penalty name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setter for the penalty name
	 * 
	 * @param name penalty name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Getter for the fitness score
	 * 
	 * @return the penalty name
	 */
	public double getScore() {
		return score;
	}

	/**
	 * Setter for the fitness
	 * 
	 * @param score the fitness
	 */
	public void setScore(double score) {
		this.score = score;
	}
	
}
