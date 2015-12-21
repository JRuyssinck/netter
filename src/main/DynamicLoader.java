package main;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

import penalties.PenaltyFunction;


/**
 * DynamicLoader is responsible for dynamically loading class files that implement (custom) penalty functions.
 * 
 * TODO: Class should be cleaned up
 * 
 * Class files should be located in the 'dynamic' folder. Currently, dynamically loading loggers is not supported.
 * 
 * @author      Joeri Ruyssinck (joeri.ruysssinck@intec.ugent.be)
 * @version     1.0
 * @since      	0.0
 */
public class DynamicLoader {

	/**
	 * Package name of the penalties
	 */
	private static final String PACKAGE_PENALTY_NAME =  "penalties.defined.";
	/**
	 * Folder name/path of the penalty functions to be loaded
	 */
	private static final String PACKAGE_FOLDER = "dynamic";

	/**
	 * Set used to keep track of which classes were already loaded
	 */
	public HashSet<String> loadedClasses = new HashSet<String>();

	/**
	 * Static method responsible for loading and instantiating a new instance of a penalty (or logger).
	 * 
	 * @param className the class to be loaded
	 * @param isPenalty a penalty or a logger
	 * @param toBeParsed strings of arguments for the constructor
	 */
	public  Object loadPenaltyOrLogger (String className, boolean isPenalty, String[] toBeParsed){

		if (isPenalty){

			
			/*
			if (className.equals("TFSparsityPenalty")){
				return new TFSparsityPenalty(toBeParsed);
			}else if(className.equals("GraphletG4Penalty")){
				return new GraphletG4Penalty(toBeParsed);
			}else if(className.equals("PredictionConfidencePenalty")){
				return new PredictionConfidencePenalty(toBeParsed);				
			}
			else {
				return null;
			}
			
			*/
			
			File file = new File(PACKAGE_FOLDER);
			URL url = null;
			try {
				url = file.toURI().toURL();
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			PenaltyFunction function = null;
			if(!loadedClasses.contains(className)){


				URL[] urls = new URL[]{url};
			//	URLClassLoader myClassLoader = new URLClassLoader(urls);
				String packageName = PACKAGE_PENALTY_NAME;
				packageName = packageName + className;

				// load the class
				Class myClass;

				// ugly referencing
				Object[] objects = toBeParsed;
				Object give = objects;
			

				try {
					
			//		myClass = t.getContextClassLoader().loadClass();
				//	myClass = myClassLoader.loadClass(packageName);
					myClass = Class.forName(packageName);
					Constructor<PenaltyFunction> constr =  myClass.getDeclaredConstructor(Object.class);
					function = 	constr.newInstance(give);
					constr = null;
					myClass = null;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) 	{
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
				finally{

				


				}


			}else{


				String packageName = PACKAGE_PENALTY_NAME;
				packageName = packageName + className;

				Class myClass = null;

				// ugly referencing
				Object[] objects = toBeParsed;
				Object give = objects;

				try {
					myClass = Class.forName(packageName);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Constructor<PenaltyFunction> constr = null;
				try {
					constr = myClass.getDeclaredConstructor(Object.class);
					
				} catch (NoSuchMethodException | SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					function = 	constr.newInstance(give);
					constr = null;
					myClass = null;
				} catch (InstantiationException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}



			if(loadedClasses.contains(className)){
			}else{
				loadedClasses.add(className);
				System.out.println("Dynamically loaded penalty: "+ className);
			}	
			
			
			return function;
		}
		else{
			throw new UnsupportedOperationException("Dynamic loading of loggers is not supported in this version of Netter.");
			
			
			
		}
	}
}
