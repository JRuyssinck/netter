package main;
import java.io.File;
import java.util.ArrayList;
import network.NetworkFileBackedWrapper;
import network.NetworkNameGenerator;
import network.node.NodeCollection;
import settings.CurrentSettings;
import wrappers.EnsembleSA;


/**
 * Netter is the main class. It creates a new Netter object and runs it with the command line arguments.
 * 
 * Netter is responsible for creating an ensemble of optimization procedures and keeps track of the current settings to be used.
 * 
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */

public class Netter {

	/**
	 * Ensemble of optimization procedures
	 */
	private EnsembleSA ensemble = null;
	/**
	 * Ensemble of optimization procedures
	 */
	private CurrentSettings currentSettings ;
	
	/**
	 * Default and only constructor.
	 */
	public Netter(){}

	
	/**
	 * Main method. Create a new object and start 'nett' method with command line arguments.
	 */
	public static void main(String[] args)
	{
		Netter net = new Netter();
		net.nett(args);
	}
	
	
	/**
	 * Starts an optimization procedure with the given parameters
	 * @param args	arguments in the form of an String array, currently only the first argument is used (The path to the paramater file)
	 * 
	 */
	public void nett (String[] args){

		
		Netter netter = this;
		
		System.out.println("Netter version 1.0");
		
		// Load the setting into the settings class using the specified settings file, default parameters are no longer supported
		if (args.length>0 && args[0] != null){
			File param = new File(args[0]);
			if (param !=null && param.exists()){
				this.currentSettings = new CurrentSettings(param);
			}else{
				System.err.println("Error: no parameter file was specified. The correct syntax is java -jar netters.jar <parameter file>.  Exiting.");
			}
		}else{
			System.err.println("Error: no parameter file was specified. The correct syntax is java -jar netters.jar <parameter file>.  Exiting.");
		}
		
		// start up the procedure
		try
		{
			// check the location of the predictions
			File file = new File(currentSettings.getTestDir());
	
				// each file should have a prediction folder 
				try {
				File pred = new File( file.getAbsolutePath()+"/predictions/");
				File[] predictions = pred.listFiles();
				// chose which predictions to eval, either all or the selected list
				ArrayList<File> chosenFiles = new ArrayList<File>();
				if (currentSettings.isAllPreds()){
					for (File tmp: predictions){
						chosenFiles.add(tmp);
					}
				}
				else{
					for (File tmp : predictions){
						for (String name: currentSettings.getPreds()){
							if (tmp.getName().equalsIgnoreCase(name)){
								chosenFiles.add(tmp);
							}
						}
					}
				}
				
				// Do the algorithm for all predictions
				for (File chosenFile: chosenFiles){
				
					// reset the networkname generator
					NetworkNameGenerator.reset();
					
					NodeCollection.reset();
					
					// Read the prediction file
					NetworkFileBackedWrapper predictionBack = new NetworkFileBackedWrapper(chosenFile.getAbsolutePath(),currentSettings.getCutoff(), currentSettings.getChuncks(), currentSettings.getChunckIncrease(),true);

					// create the subfolder for output
					String[] suf = chosenFile.getName().split(".txt");
					File subfolderOutput = new File(currentSettings.getOutputDir().getAbsoluteFile()+"/"+file.getName());
					
					if (!subfolderOutput.exists()){
						subfolderOutput.mkdir();
					}
					
					subfolderOutput = new File(currentSettings.getOutputDir().getAbsoluteFile()+"/"+file.getName()+"/"+suf[0]);
					
					if (!subfolderOutput.exists()){
						subfolderOutput.mkdir();
					}
					
					String outputSuffix = "/"+file.getName()+"/"+suf[0];
					
					this.currentSettings.setOutputSuffix(outputSuffix);

					
					System.out.println("Starting Netter: ");
					this.ensemble = new EnsembleSA(netter);
					ensemble.ensembleSA(predictionBack,currentSettings.getTotalIterations());
					
				}
				
				}
				catch(Exception e){
					System.err.println("Invalid folder for testing.");
					e.printStackTrace();
				}
				
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		
	}
	
	
	/**
	 * 
	 * Reports back on the progress of the optimization process in %.
	 * 
	 * @return the progress in % of the optimization process
	 */
	public double getProgress(){
		if (this.ensemble==null){
			return 0.0;
		}
		if (this.ensemble.isFinished()){
			return 100.1;
		}
		return ((double)this.ensemble.getIterations()/ (double)this.currentSettings.getTotalIterations()) *100.0;
	}

	
	/**
	 * 
	 * Getter for the current settings
	 * 
	 * @return the current settings
	 */
	public CurrentSettings getCurrentSettings() {
		return currentSettings;
	}

	/**
	 * 
	 * Setter for the current settings
	 * 
	 * @param the settings
	 */
	public void setCurrentSettings(CurrentSettings currentSettings) {
		this.currentSettings = currentSettings;
	}



	
}
	

