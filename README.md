

<body>
<h2> Practical guidelines: Netter: re-ranking gene network inference predictions using structural network properties.</h2>

<h3> <Note: this page is under construction.> </h3>


<h3> 1. Running Netter </h3>

<ind> Netter uses a single command line argument: the path to a Netter configuration file. A typical execution of Netter is: </ind>
<bor>java -jar netter.jar config_file.txt</bor>


<h3> 2. A Netter configuration file </h3>

<ind> An example of a typical Netter configuration file: </ind>


<pre><code>
# Use '#' to add comments 
# ---------------------------------------------------------
# Global parameter settings 
# ---------------------------------------------------------
testDir=data# folder location of the networks
preds=clr.txt# predictions that should be considered if allPreds is false, seperate with '='
allPreds=false # consider all the predictions in the folder, or only those specified in 'preds'
workersAvailable=2# amount of threads that should be spawned ( best <= amount of cores available on machine )
totalIterations=75# the total amount of iterations the algorithm should run
cutoff=300# the top X links of the ranking that should re-ordered
chuncks=50# the increase in size of subsequent subnetworks
chunckIncrease=0.5# the increase in penalty coefficient for subsequent subnetworks
modifyEachTurn=50# the maximum amount of modifications each turn (sampled uniform [1,modifyEachturn]
moveEachTurn=50# the maximum amount a link can move in the ranking in one modification, sampled uniform [-moveEachTurn, +moveEachTurn]
startTemperature=10.0# the starting temperature of the annealing schedule
maxIterations=500# the amount of turns of modification in one iteration
temperatureAutoDetermination=false# use an automatic scheme to set the temperature to a certain range
determinationZone=10.0# at which % of progress should we check if the temperature was set correctly ( default 0.1 =10%)
temperatureTargetGoal=0.12# the average target temperature of the automatic temperature scheme 
temperatureAllowedDeviation=10.0# the allowed deviation of the target goal without rebooting 
outputDirString=output# folder with output
LOGGERS=
# ---------------------------------------------------------
# ADD LOGGERS HERE, previous parameters should be set
# ---------------------------------------------------------
AcceptanceLogger=100=AcceptanceLogger.txt   # Specify classname, logging interval in iterations and outputfile name as arguments  seperated by = 
PenaltiesLogger=100=PenaltiesLogger.txt     # Specify classname, logging interval in iterations and outputfile name as arguments  seperated by =

PENALTIES=
# ---------------------------------------------------------
# ADD PENALTYFUNCTIONS HERE, previous parameters and loggers should be set
# ---------------------------------------------------------
AntiDominatePenalty=75.000000000000
GraphletG4Penalty=2.000000000000
PredictionConfidencePenalty=0.000010000000
TFSparsityPenalty=25.0=-0.001=600=0.55=0=1.001=490

</code>
</pre>

<ind> The file can be split into four parts:
	 <ol>
 		 <li> Comments: All text following a '#" is ignored on a line. </li>
  		 <li> Global settings: All parameter settings of Netter not related to loggers or penalty functions. Description of each parameter is mentioned in comments. </li>
  		 <li> Logger to include: syntax 'ClassName'='Interval='OutputFileName' , the string 'LOGGERS=' specificies the start of the logger specification. </li>  
             	 <li> Penalties to include: syntax 'ClassName'='Relative weight of penalty' , custom mapping of individual structure cost penalty can be specified by additional 'Key=Value' pairs ,the string 'PENALTIES= specificies the start of the logger specification.  </li>
</ol> 
</ind>

<h3> 3. Setting the parameters </h3>

Netter is shown to be robust for a wide setting of parameter values. <p> In practice, we suggest the following steps.


When using default penalty functions (basic): 
		 <ul>
  			<li> Set input and output folder location and specify files to be re-ranked. </li>
  			<li> Leave all parameters unchanged. </li>
		</ul> 
	When using custom penalty functions (advanced): 
		 <ul>
  			<li> Set input and output folder location and specify files to be re-ranked. </li>
  			<li> Set network cutoff value to desired value.  </li>
  			<li> Chunk size can be set higher if network cutoff is chosen and performance suffers. </li>
  			<li> Annealing parameters will be automatically set. A better starting value can however speed up the progress. </li>
			<li> Include the desired loggers and penalty functions.</li>
			<li> Setting the relative weights of the penalties </li>
			<ul>
				<li> Run Netter with random relative weights for custom penalties and default settings for included default penalties</li>
				<li> Inspect penalty logfile of Netter </li>
				<li> Adjust the weights of all structural penalties that have a non-zero cost at the start such that they contribute evenly to the total cost function (up to an order of magnitude). e.g Graphlet G4 penalty </li>
				<li> Adjust the weights of all structural penalties that have a zero cost at the start, such that their cost remains (close to) zero  e.g. Anti-dominate penalty </li>
				<li> Adjust the weights of the regularization parameter such that the cost at the end of the ranking does not exceeed the cost of any of the structural penalties but is non-zero. </li>
				<li> Re-run Netter and adjust weights until these rules of thumb are achieved. </li>
			</ul>
			</ul>
<h3> 4. Netter GUI </h3>
The Netter GUI is an interface to create Netter configuration files and track progress of running jobs. All computational functionality is implemented in the main Netter jar file.


<h3> 5. Contact </h3>

Please e-mail me if you have any questions. joeri.ruyssinck@intec.ugent.be




