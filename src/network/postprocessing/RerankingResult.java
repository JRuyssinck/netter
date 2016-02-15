package network.postprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.management.RuntimeErrorException;

import network.NetworkFileBackedWrapper;
import network.link.Link;


/**
 * RerankingResult is a class storing the results of individual re-rankings. It merges new results with previous reranking and can output the final result to the standard TSV format.
 * It was created to avoid storing all NetworkFileBackedWrapper results until they could be merged at the end, which resulted in memory issues.
 * 
 * @author Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version 1.0
 * @since 1.0
 */
public class RerankingResult {

	/**
	 * The original file which is being re-ranked
	 */
	private String originalFile;
	
	/**
	 * Top of the ranking which is being re-ranked
	 */
	private int cutoff;
	
	/**
	 * The first column (transcription factor) node names
	 */
	private String[] firstCol;
	
	/**
	 * The second column (target gene) node name
	 */
	private String[] secondCol;
	
	/**
	 * The current sum of the new positions in the ranking created from the re-rankings positions for each link
	 */
	private int[] sums ;
	
	/**
	 * 
	 * The original ranks for each tf/target gene pair
	 */
	private int [] originalRanks;
	
	/**
	 * 
	 * Indicates if this class already has atleast one result stored
	 */
	private boolean inited;
	
	
	/**
	 * Constructs a new ReRankingResult
	 */
	public RerankingResult (){
		this.inited = false;
	}
	
	
	/**
	 * Writes the current reranking result to TSV
	 * 
	 * @param outputFileName	the name of the outputfile to write to
	 */
	public void resultToTSV (String outputFileName){
		
		// do a sort on the new ranking
		ArrayList<Integer> copyArray = new ArrayList<Integer>(this.sums.length);
		for (int i =0; i < sums.length;i++){
			copyArray.add(i,this.sums[i]);
		}
		Collections.sort(copyArray);
		File file = new File(outputFileName);
		int count = 0;
		FileWriter writer = null;
		try
		{
			writer = new FileWriter(file);
			int to = firstCol.length;	
			int last = -1;
			int sinceLast = 0;
			for (int i = 0; i < to; i++){
				int next = copyArray.get(i);
				if(next==last){
					sinceLast++;
				}else{
					sinceLast=0;
				}	
				int j = 0;
				int countDown = sinceLast ;
				for (j = 0; j < to ; j++){
					
					if (sums[j]==next){
						if (countDown==0){
							break;
						}else{
							countDown--;
						}
					}	
				}
				writer.write(firstCol[j] + "\t" + secondCol[j]+ "\t" + sums[j]  + "\n");
				count++;
				last = next;
				
			}
			writer.flush();
			count = 0 ;
			// do the rest of the files
			BufferedReader reader = null;
			FileReader fReader = null;

			try {
				fReader = new FileReader(new File(this.originalFile));
				reader = new BufferedReader(fReader);

				// Read the first line
				String s = null;
				s = reader.readLine();

				// Read until the end
				while (s != null)
				{
					count++;
					if (count> cutoff  ){
						writer.write (s+"\n");
					}
					s = reader.readLine();
				}
			}
			catch (Exception e){
				e.printStackTrace();
			}
			finally{
				reader.close();
				writer.close();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeErrorException(null,e.getMessage());
		}
		finally
		{
			try
			{
				writer.close();
			}
			catch (IOException e)
			{
				throw 	new RuntimeErrorException(null,"Unknown output format specified while writing to network to file.");
			}
		}
		
		
	}


	/**
	 * Add's a result of a individual re-ranking to the ensemble.
	 * 
	 * @param wrapper	the re-ranking resuslt to be added
	 */
	public void addResult(NetworkFileBackedWrapper wrapper) {
	
		// tie break on original ranking
		wrapper.getNetwork().sortOriginalRankInterval(wrapper.getChuncks(), 0,wrapper.getCutoff());

		// sort by name
		wrapper.getNetwork().sortNetworkNameLink();
		
		if (!inited){		
			this.originalFile = wrapper.getFileName();
			this.cutoff = wrapper.getCutoff();
			this.firstCol = new String[this.cutoff];
			this.secondCol = new String[this.cutoff];
			this.sums  = new int [this.cutoff];	
			this.originalRanks = new int [this.cutoff];
		}
		
		ArrayList<Link> sortedLinklist = wrapper.getNetwork().getLinks();
		for (int i = 0 ; i < sortedLinklist.size() ; i++){
			if(!inited){
				this.firstCol[i] = sortedLinklist.get(i).getTf().getName();
				this.secondCol[i] = sortedLinklist.get(i).getTarget().getName();
				this.originalRanks[i] = sortedLinklist.get(i).getLinkProperties().getOriginalPredictionRank();
				this.sums[i] = 0;
			}
			this.sums[i] = this.sums[i] + sortedLinklist.get(i).getLinkProperties().getPredictionRank();
		}

		
		
		this.inited = true;

	}
	
	
	// Getters and setters
	
	public String getOriginalFile() {
		return originalFile;
	}


	public void setOriginalFile(String originalFile) {
		this.originalFile = originalFile;
	}


	public int getCutoff() {
		return cutoff;
	}


	public void setCutoff(int cutoff) {
		this.cutoff = cutoff;
	}

	
}
