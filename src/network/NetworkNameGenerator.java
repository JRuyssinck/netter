package network;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;



/**
 * 
 * NetworkNameGenerator is responsible for giving unique names to networks and as such keeps track of all network names currently registered.
 * It does not keep track of the networks.
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class NetworkNameGenerator {

	/**
	 * The set of all registered network names
	 */
	private static volatile HashSet<String> list= new HashSet<String>();
	
	/**
	 * 
	 * Indicates if the list of network names can be reset.
	 */
	private static volatile boolean resetSafe = true;
	
	/**
	 * 
	 * Indicates if the list of network names is resetting.
	 */
	private static volatile boolean resetting = false;
	
	
	/**
	 * This class should not have any instances.
	 */
	private NetworkNameGenerator(){}
	
	/**
	 * Request a new name for a network
	 * 
	 * @returna new unique network name
	 */
	public synchronized static String getNextName(){
	
		while (resetting){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		resetSafe= false;
		String uuid = UUID.randomUUID().toString();
		while (list.contains(uuid)){
			uuid =UUID.randomUUID().toString();
		}
		
		list.add(uuid);
		resetSafe = true;
		return uuid;
	}
	
	/**
	 * 
	 * Clears all registered names. Warning, if there are still networks in existence, network name are no longer guarenteed to be unique
	 * 
	 */
	public static void reset(){
		while (!resetSafe){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		resetting = true;
		list.clear();
		resetting = false;
	}
	
}
