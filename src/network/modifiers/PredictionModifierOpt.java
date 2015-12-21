package network.modifiers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import javax.management.RuntimeErrorException;

import network.Network;
import network.NetworkFileBackedWrapper;
import network.link.Link;
import network.link.LinkProperties;
import fitnessfunctions.PartitionFitnessFunction;



/**
 * PredictionModifierOpt implement the NetworkModifier interface. It will modify the ranking by moving randomly chosen links up and down the ranking at each action.
 * Both the amount of links to moved and how much they can move in the ranking are randomly determined within a certain range.
 * It will alert the fitnessfunction if applicable that changes were made.
 * 
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class PredictionModifierOpt implements NetworkModifier {

	/**
	 * The network ranking that is being modified
	 */
	private Network network;
	
	/**
	 * The maximum amount of links that can be modified each turn
	 */
	private int modifyEachTurn ;
	
	/**
	 * 
	 * The maximum amount of positions a link can move up or down the ranking in one mutation
	 */
	private int moveEachTurn ;
	
	/**
	 * Generated lookup arrays for performance reasons, generated from the given NetworkPartition
	 */
	private int[] setLookupArray;
	
	/**
	 * Generated lookup arrays for performance reasons, generated from the given NetworkPartition
	 */
	private int[] setUpperLookupArray;
	
	/**
	 * Generated lookup arrays for performance reasons, generated from the given NetworkPartition
	 */
	private int[] setLowerLookupArray;
	
	/**
	 * List of links in the last unconfirmed change made
	 */
	private HashSet<Link> modified ;
	
	/**
	 * A Fitnessfunction to be alerted if changes are made
	 */
	private PartitionFitnessFunction fitnessIncrementFunction;
	
	/**
	 * Indicates if the network was modified and is pending confirmation
	 */
	private boolean modded = false;
	
	/**
	 * Indicates that the latest changes were made permanent
	 */
	private boolean commited = false;
	
	/**
	 * Indicates that the latest changes were reverted
	 */
	private boolean reverted = false;

	/**
	 * Random generator to be re-used
	 */
	private static final Random random = new Random();
	

	/**
	 * Constructs a new PredictionModifierOpt
	 * 
	 * @param wrapper	ranking object to be re-ranked, with a 
	 * @param modifyEachTurn	maximum amount of links that can be mofidied
	 * @param moveEachTurn	maximum amount of positions a link can move up or down
	 */
	public PredictionModifierOpt( NetworkFileBackedWrapper wrapper,int modifyEachTurn, int moveEachTurn){
		
		this.network = wrapper.getNetwork();
		this.modifyEachTurn = modifyEachTurn;
		this.moveEachTurn = moveEachTurn;
		this.modified = new HashSet<Link>();
		this.reverted = false;
		
		ArrayList<Integer> partitions = wrapper.getPartition().getPartitions();

		this.setLookupArray = new int[partitions.get(partitions.size()-1)];
		this.setLowerLookupArray = new int[partitions.get(partitions.size()-1)];
		this.setUpperLookupArray = new int[partitions.get(partitions.size()-1)];

		int j = 0;
		for (int i = 0 ; i < partitions.get(partitions.size()-1) ; i++){
			if(i==partitions.get(j)){
				j++;
			}
			setLookupArray[i] = j;
			if (j==0){
				setLowerLookupArray[i] =  0;

			}else{
				setLowerLookupArray[i] = partitions.get(j-1);
			}
			setUpperLookupArray[i] = partitions.get(j);

		}
		
		network.sortNetworkPredictionRank();
		
		while(network.getLinkCount()> partitions.get(partitions.size()-1)){
			network.removeLinkFromNetwork(network.getLinkCount()-1);
		}
		
		int i = 0;
		for (Link a: network.getLinks()){
			
			if (i == partitions.get(partitions.size()-1)){ break;}
			LinkProperties b = a.getLinkProperties();
			b.setPredictionRank(i+1);
			b.setCurrentSet(this.setLookupArray[i]);
			b.setLowerBoundSet(this.setLowerLookupArray[i]);
			b.setUpperBoundSet(this.setUpperLookupArray[i]);
			i++;
		}
	}
	

	
	/**
	 * Implements the modify action of the interface. This method will alter the ranking by moving a certain amount of links up or down the rank as specified in the class description and manuscript.
	 * It will alert the fitnessfunction if applicable and set the flag of this class to modded state.
	 */
	@Override
	public void modify() {
		
		if (this.modded){
			this.commit();
		}
		
		this.commited = false;
		this.reverted = false;
		

		int moveThisTurn = random.nextInt(this.modifyEachTurn);
		
		
		for (int i = 0 ; i < moveThisTurn; i++){
			
			// move up or down
			if (random.nextInt(1)==1){
				// <- move up
				int move = random.nextInt(moveEachTurn)+1; // calculate amount of positions to move up
				int pos = random.nextInt(this.network.getLinks().size()); // decide which link will move
				
				// check for boundary conditions
				if ( (pos+move) >=  this.network.getLinks().size()){ // if the move would result in a final position which exceeds the number of links, adjust move to end of list
					move =  this.network.getLinks().size() - pos -1;
					
					if (move == 0){ // dont perform useless moves
						continue;
					}
				}

				// get the link which will move up
				Link changeLink = this.network.getLinks().get(pos);
				

				// get the propeties of the links which will move up
				LinkProperties linkPropertiesChangeLink = changeLink.getLinkProperties();
				
				// check if the link already moved during the modifying process, if not, we should store the current prediction rank
				// if not, the old current prediction rank is already stored
				if ( linkPropertiesChangeLink.getPreviousPredictionRank() ==-1){
					 linkPropertiesChangeLink.setPreviousPredictionRank( linkPropertiesChangeLink.getPredictionRank());
				}
				
				
				// now we are ready to move the link to the new spot, which is [ Note: indexing 0 java -> spot in array is one less
				int newSpot =  linkPropertiesChangeLink.getPredictionRank()+move;
				linkPropertiesChangeLink.setPredictionRank(newSpot);
				
				// we should also check if the link accidently moved back to its original prediction, in that case, we should just reset the link properties
				if (newSpot == linkPropertiesChangeLink.getPreviousPredictionRank()){
						linkPropertiesChangeLink.setPreviousPredictionRank(-1) ;
				}
				
				
				// now check whether or not the movement causes the link to jump to another partition
				// we should adjust newSpot for this, as we are now reasoning in indexes
				newSpot--;
				// check if it moves to another set
				if (newSpot >=  linkPropertiesChangeLink.getUpperBoundSet()){
					// if the set changes from the originel se, we should keep track of the original set
					if ( linkPropertiesChangeLink.getPreviousSet() == -1){
						 linkPropertiesChangeLink.setPreviousSet( linkPropertiesChangeLink.getCurrentSet());
						 linkPropertiesChangeLink.setPreviousLowerBound( linkPropertiesChangeLink.getLowerBoundSet());
						 linkPropertiesChangeLink.setPreviousUpperBound( linkPropertiesChangeLink.getUpperBoundSet());
					}
					// check which new set we moved to
					int newSet = this.setLookupArray[newSpot];
					
					// if the set changes back to the original set
					if (newSet ==  linkPropertiesChangeLink.getPreviousSet()){
						 linkPropertiesChangeLink.setPreviousSet(-1);
						 linkPropertiesChangeLink.setPreviousLowerBound(-1);
						 linkPropertiesChangeLink.setPreviousUpperBound(-1);
						this.modified.remove(changeLink);
						
					}else{
						this.modified.add(changeLink);
					}
					// always set the new settings for the set
					 linkPropertiesChangeLink.setUpperBoundSet(this.setUpperLookupArray[newSpot]);
					 linkPropertiesChangeLink.setLowerBoundSet(this.setLowerLookupArray[newSpot]);
					 linkPropertiesChangeLink.setCurrentSet(newSet);
					
				}
				
				
				// now, because we moved this link, all links in between the old spot and new spot will have to move as well
				for (int j = 1 ; j < move+1 ; j++){
					// get the link and the link propertoes
					Link ch = this.network.getLinks().get(pos+j);
					LinkProperties tmp2 = this.network.getLinks().get(pos+j).getLinkProperties();
					

					
					// if this link didnt move yet, store the current rank
					if (tmp2.getPreviousPredictionRank()==-1){
						tmp2.setPreviousPredictionRank(tmp2.getPredictionRank());
					}
					
					int newPosition = tmp2.getPredictionRank()-1;
					tmp2.setPredictionRank(newPosition);
					
					// reset if it moves back to original position
					if (newPosition == tmp2.getPreviousPredictionRank()){
						tmp2.setPreviousPredictionRank(-1) ;
					}
					
					// now check if the set changed, for this move to index numbering
					newPosition--;
					
					// if the set changes
					if (newPosition < tmp2.getLowerBoundSet()){
						
						// if the set changes from the original set
						if (tmp2.getPreviousSet() ==-1){
							tmp2.setPreviousSet(tmp2.getCurrentSet());
							tmp2.setPreviousLowerBound(tmp2.getLowerBoundSet());
							tmp2.setPreviousUpperBound(tmp2.getUpperBoundSet());
						}
						
						int newSet = this.setLookupArray[newPosition];
						
						// if the set changes back to the original set
						if (newSet == tmp2.getPreviousSet()){
							
							tmp2.setPreviousSet(-1);
							tmp2.setPreviousLowerBound(-1);
							tmp2.setPreviousUpperBound(-1);
							this.modified.remove(ch);
						}else{
							this.modified.add(ch);
						}
						// always set the new settings for the set
						tmp2.setUpperBoundSet(this.setUpperLookupArray[newPosition]);
						tmp2.setLowerBoundSet(this.setLowerLookupArray[newPosition]);
						tmp2.setCurrentSet(newSet);
						
					}
					// move this link physically
					this.network.getLinks().set(newPosition,ch );

				}
				// change the original link physically
				this.network.getLinks().set(newSpot,changeLink );

				
				
				
			}
			else{
				
				// <- move down
				int move = random.nextInt(moveEachTurn)+1; // calculate amount of positions to move up
				int pos = random.nextInt(this.network.getLinks().size()); // decide which link will move
				
				// check for boundary conditions
				if ( (pos-move) <  0){ // if the move would result in a final position which is less than zero, put the link at the top
					move =  pos ;
					if (move == 0){ // dont perform useless moves
						continue;
					}
				}

				// get the link which will move up
				Link changeLink = this.network.getLinks().get(pos);
				// get the propeties of the links which will move up
				LinkProperties linkPropertiesChangeLink = changeLink.getLinkProperties();
				
				
				// check if the link already moved during the modying process, if not, we should store the current prediction rank
				// if not, the old current prediction rank is already stored
				if ( linkPropertiesChangeLink.getPreviousPredictionRank() ==-1){
					 linkPropertiesChangeLink.setPreviousPredictionRank( linkPropertiesChangeLink.getPredictionRank());
				}
				
				
				// now we are ready to move the link to the new spot, which is [ Note: indexing 0 java -> spot in array is one less
				int newSpot =  linkPropertiesChangeLink.getPredictionRank()-move;
				linkPropertiesChangeLink.setPredictionRank(newSpot);
				
				// we should also check if the link accidently moved back to its original prediction, in that case, we should just reset the link properties
				if (newSpot == linkPropertiesChangeLink.getPreviousPredictionRank()){
						linkPropertiesChangeLink.setPreviousPredictionRank(-1) ;
				}
				
				
				// now check whether or not the movement causes the link to jump to another partition
				// we should adjust newSpot for this, as we are now reasoning in indexes
				newSpot--;
				// check if it moves to another set
				if (newSpot <  linkPropertiesChangeLink.getLowerBoundSet()){
					// if the set changes from the originel se, we should keep track of the original set
					if ( linkPropertiesChangeLink.getPreviousSet() == -1){
						 linkPropertiesChangeLink.setPreviousSet( linkPropertiesChangeLink.getCurrentSet());
						 linkPropertiesChangeLink.setPreviousLowerBound( linkPropertiesChangeLink.getLowerBoundSet());
						 linkPropertiesChangeLink.setPreviousUpperBound( linkPropertiesChangeLink.getUpperBoundSet());
					}
					// check which new set we moved to
					int newSet = this.setLookupArray[newSpot];
					
					// if the set changes back to the original set
					if (newSet ==  linkPropertiesChangeLink.getPreviousSet()){
						 linkPropertiesChangeLink.setPreviousSet(-1);
						 linkPropertiesChangeLink.setPreviousLowerBound(-1);
						 linkPropertiesChangeLink.setPreviousUpperBound(-1);
						 this.modified.remove(changeLink);
						
					}else{
						this.modified.add(changeLink);
					}
					// always set the new settings for the set
					 linkPropertiesChangeLink.setUpperBoundSet(this.setUpperLookupArray[newSpot]);
					 linkPropertiesChangeLink.setLowerBoundSet(this.setLowerLookupArray[newSpot]);
					 linkPropertiesChangeLink.setCurrentSet(newSet);
					
				}
				
				
				// now, because we moved this link, all links in between the old spot and new spot will have to move as well
				for (int j = 1 ; j < move+1 ; j++){
					// get the link and the link propertoes
					Link ch = this.network.getLinks().get(pos-j);
					LinkProperties tmp2 = this.network.getLinks().get(pos-j).getLinkProperties();
					
					// if this link didnt move yet, store the current rank
					if (tmp2.getPreviousPredictionRank()==-1){
						tmp2.setPreviousPredictionRank(tmp2.getPredictionRank());
					}
					
					int newPosition = tmp2.getPredictionRank()+1;
					tmp2.setPredictionRank(newPosition);
					
					// reset if it moves back to original position
					if (newPosition == tmp2.getPreviousPredictionRank()){
						tmp2.setPreviousPredictionRank(-1) ;
					}
					
					// now check if the set changed, for this move to index numbering
					newPosition--;
					
					// if the set changes
					if (newPosition >= tmp2.getUpperBoundSet()){
						
						// if the set changes from the original set
						if (tmp2.getPreviousSet() ==-1){
							tmp2.setPreviousSet(tmp2.getCurrentSet());
							tmp2.setPreviousLowerBound(tmp2.getLowerBoundSet());
							tmp2.setPreviousUpperBound(tmp2.getUpperBoundSet());
						}
						
						int newSet = this.setLookupArray[newPosition];
						
						// if the set changes back to the original set
						if (newSet == tmp2.getPreviousSet()){
							
							tmp2.setPreviousSet(-1);
							tmp2.setPreviousLowerBound(-1);
							tmp2.setPreviousUpperBound(-1);
							this.modified.remove(ch);
						}else{
							this.modified.add(ch);
						}
						// always set the new settings for the set
						tmp2.setUpperBoundSet(this.setUpperLookupArray[newPosition]);
						tmp2.setLowerBoundSet(this.setLowerLookupArray[newPosition]);
						tmp2.setCurrentSet(newSet);
						
					}
					// move this link physically
					this.network.getLinks().set(newPosition,ch );

				}
				// change the original link physically
				this.network.getLinks().set(newSpot,changeLink );

			}
		}
		
		if (this.fitnessIncrementFunction != null){
			this.fitnessIncrementFunction.modify();
		}
		this.modded = true;
	
	}
	
	
	/**
	 * Implements the revert action of the interface. This method undo all changes. Can only go back one step in time.
	 * Clears the modified list and reverts the fitnessfunction.
	 */
	public void revert(){
		
		if (this.reverted){
			throw new RuntimeErrorException(null,"Cannot revert more than once in a row.");
		}
		if (this.fitnessIncrementFunction != null){
			this.fitnessIncrementFunction.revert();
		}
		this.reverted = true;
		
		int i = 0;
		while (i < network.getLinks().size()){
			
			Link a = network.getLinks().get(i);
			LinkProperties tmp = a.getLinkProperties();
			
			if(tmp.getPreviousPredictionRank()==-1){
				i++;
				continue;
			}else{

				// get the original position
				int previousSpot = tmp.getPreviousPredictionRank()-1;
				// revert the link and put at the correct place
				tmp.revert();
				
				// get the link currently at this position
				Link replace = network.getLinks().get(previousSpot);
				
				// already at correct place
				if (a.equals(replace)){i++; continue;}
		
				network.getLinks().set(previousSpot, a);
				network.getLinks().set(i,replace);
				
				// continue, without incrementing i
				continue;
			}
			

		}
		
		this.modified.clear();
		this.reverted = true;
		this.modded = false;
		
		
	}
	
	
	/**
	 *	Implements the commit method of this interface. Makes all changes permanent and alerts the  fitnessfunction. Can only be called once in a row.
	 */
	public void commit(){
		if(this.commited ){
			throw new RuntimeErrorException(null,"Already committed.");
		}
		for (Link a : this.network.getLinks()){
			a.getLinkProperties().commit();
		}
		if (this.fitnessIncrementFunction != null){
			this.fitnessIncrementFunction.commit();
		}
		this.modified.clear();
		this.commited = true;
	}



	
	// getters and setters
	
	public Network getNetwork() {
		return network;
	}
	public void setNetwork(Network network) {
		this.network = network;
	}
	public boolean isReverted() {
		return reverted;
	}
	public void setReverted(boolean reverted) {
		this.reverted = reverted;
	}
	public PartitionFitnessFunction getFitnessFunction() {
		return fitnessIncrementFunction;
	}
	public void setFitnessIncrementFunction(PartitionFitnessFunction fitnessIncrementFunction) {
		this.fitnessIncrementFunction = fitnessIncrementFunction;
	}
	public boolean isModded() {
		return modded;
	}
	public void setModded(boolean modded) {
		this.modded = modded;
	}
	public boolean isCommited() {
		return commited;
	}
	public void setCommited(boolean commited) {
		this.commited = commited;
	}
	public HashSet<Link> getModified() {
		return modified;
	}
	public void setModified(HashSet<Link> modified) {
		this.modified = modified;
	}
	public int getModifyEachTurn() {
		return modifyEachTurn;
	}
	public void setModifyEachTurn(int modifyEachTurn) {
		this.modifyEachTurn = modifyEachTurn;
	}
	public int getMoveEachTurn() {
		return moveEachTurn;
	}
	public void setMoveEachTurn(int moveEachTurn) {
		this.moveEachTurn = moveEachTurn;
	}


}
