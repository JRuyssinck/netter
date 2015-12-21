package network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.management.RuntimeErrorException;

import network.link.Link;
import network.node.Node;
import network.node.NodeCollection;

/**
 * NetworkFileBackedWrapper represents the target of the re-ranking of Netter. It consists of the top of a ranking, originating from a certain file.
 * This top of the ranking is represented as a Network and will be modified. Optionally, the network is cut in pieces and represented by a NetworkPartition with associated weights.
 * 
 * 

 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class NetworkFileBackedWrapper {

	/**
	 * Part of the network which will be re-ranked
	 */
	private Network network;

	/**
	 * 
	 * String representation of the filename
	 */
	private String fileName;

	/**
	 * Amount of links at the top of ranking which will be re-ranked
	 */
	private int cutoff ;

	/**
	 * Size of a single network partition
	 */
	private int chuncks;

	/**
	 * Network partitions associated with this ranking
	 */
	private NetworkPartition partition;

	/**
	 * Weights associated with the network partitions
	 */
	private double coefs;

	/**
	 * String representation of the current outputfilename should this object be written to file
	 */
	private String outputFileName ;

	/**
	 * 
	 * Constructs a new NetworkFileBackedWrapper with the given parameters
	 * 
	 * @param fileName	ranking file to read from
	 * @param cutoff	cutoff value to determine how many links at the the top of the ranking should be re-ranked
	 * @param chuncks	size of the partitions, ignored if createPartition is false
	 * @param coefs	weights of the partitions, ignored if createPartition is false
	 * @param createPartition	should  the network be partitioned
	 */
	public NetworkFileBackedWrapper (String fileName,int cutoff, int chuncks, double coefs, boolean createPartition){

		this.fileName = fileName;
		this.cutoff = cutoff;
		this.chuncks = chuncks;
		this.coefs = coefs;
		this.network = new Network(fileName, cutoff);		

		if (cutoff>network.getLinkCount()){
			this.cutoff = network.getLinkCount();
			cutoff = network.getLinkCount();
		}

		if (createPartition){

			ArrayList<Integer> partitions = new ArrayList<Integer> ();
			ArrayList<Double> coef = new ArrayList<Double> ();
			double coefStart =  coefs * (cutoff/chuncks);
			for (int i = chuncks ; i <= cutoff ; i+=chuncks){			
				partitions.add(i);
				coef.add(coefStart);
				coefStart -= coefs;
			}

			this.partition = new NetworkPartition(network, cutoff, chuncks, coefs);
		}else{
			this.partition = null;
		}

	}

	
	/**
	 * Copy constructor of a NetworkFileBackedWrapper. It will copy the network, link to the same fileName, networkPartition as well as all other paramaters.
	 * 
	 * @param copy	The ranking file representation to be copied
	 */
	public NetworkFileBackedWrapper (NetworkFileBackedWrapper copy){

		this.network = new Network (copy.getNetwork());
		this.fileName = copy.getFileName();
		this.cutoff = copy.getCutoff();
		this.chuncks = copy.getChuncks();
		this.coefs = copy.getCoefs();
		// cannot use a copy constructor for the partition, as the network should not be the same 
		this.partition = new NetworkPartition(this.network, this.cutoff,this.chuncks, this.coefs);


	}


	/**
	 * This method should be called when the ranking file is no longer needed. It deregisters the network context from the NodeCollection to free memory.
	 * 
	 */
	public void networkClear (){
		// completely remove all references to the temporary network
		for (Node node: NodeCollection.returnAllRegisteredNodes()){
			node.clearNetwork(this.network);
		}
		this.partition.networkClear();
	}


	/**
	 * 
	 * Writes the current ranking to the outputFileName.
	 * 
	 */
	public void networkToTSVfinal() {


		File file = new File( this.outputFileName);

		int count = 0;
		//_logger.info("writing network to file...");
		FileWriter writer = null;
		try
		{
			writer = new FileWriter(file);



			//	network.sortNetworkPredictionRank();
			for (Link a : network.getLinks())
			{
				if (count==cutoff){
					break;
				}
				writer.write(a.toString() + "\t" + a.getLinkProperties().getPredictionRank() + "\t" + a.getLinkProperties().getOriginalPredictionRank() + "\n");
				count++;

			}
			writer.flush();



			count = 0 ;
			// do the rest of the files
			BufferedReader reader = null;
			FileReader fReader = null;

			try {
				fReader = new FileReader(new File(this.fileName));
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
			throw new RuntimeErrorException(null,"");
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


	// Getters and setters

	public int getCutoff() {
		return cutoff;
	}

	public void setCutoff(int cutoff) {
		this.cutoff = cutoff;
	}

	public int getChuncks() {
		return chuncks;
	}

	public void setChuncks(int chuncks) {
		this.chuncks = chuncks;
	}

	public NetworkPartition getPartition() {
		return partition;
	}

	public void setPartition(NetworkPartition partition) {
		this.partition = partition;
	}

	public String getOutputFileName() {
		return outputFileName;
	}

	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}

	public String getFileName() {
		return fileName;
	}


	public void setFileName(String fileName) {
		this.fileName = fileName;
	}


	public Network getNetwork() {
		return network;
	}

	public void setNetwork(Network network) {
		this.network = network;
	}


	public double getCoefs() {
		return coefs;
	}

	public void setCoefs(double coefs) {
		this.coefs = coefs;
	}


}
