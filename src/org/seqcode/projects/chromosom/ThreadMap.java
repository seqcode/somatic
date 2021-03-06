package org.seqcode.projects.chromosom;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @author trk5150
 *
 */
public class ThreadMap
{
	public Reader reader;
	public boolean cos, countIntraChromosomal;
	
	public int xNodes, yNodes,  count, dpSize, nodeNum, farthestNeighbor, threadNum, maxDist, chrCount;
	public double sgm, sgmStop, iterations, learningRate, alpha, pearsonQuality, cosineQuality, stability, zero, timerIterate, timerAssign, timerElse;
	
	IterateThread[] iThreads;
	AssignThread[] aThreads;
	double[][] dp, nodes;
	String[] dpNames;
	boolean[][] assignments;
	int[][] neighborly, threadLims;
	int[] dpcount;
	int[] dpChr,dpPerChr;
	Map<String, Integer> chr2id = new HashMap<String, Integer>();
	
	double[] dpMag, nMag, weightsByHood;
	
	public String filePrefix;
	
	public ThreadMap(int xNode, int yNode, double sigma, double sigmaStop, int it, boolean cosine, String outPrefix, String mat, int availibleThreads, boolean countIntraChrom)
	{
		countIntraChromosomal = countIntraChrom;
		sgmStop = sigmaStop;
		iThreads = new IterateThread[availibleThreads];
		aThreads = new AssignThread[availibleThreads];
		threadNum = availibleThreads;
		farthestNeighbor = 0;
		zero = Math.pow(10, -7);
		stability = 0;
		pearsonQuality = 0;
		cosineQuality = 0;
		xNodes = xNode;
		yNodes = yNode;
		sgm = sigma;
		nodeNum = xNode * yNode;
		iterations = it;
		String ff = mat;
		filePrefix = outPrefix;
		reader = new Reader(ff);
		cos = cosine;
		timerElse =0; timerIterate = 0; timerAssign = 0;
	}
	public class IterateThread extends Thread
	{
		public double[][] partOfNeighbors;
		public int thisT;
		public IterateThread(int i)
		{
			super();
			thisT = i;
			partOfNeighbors = new double[threadLims[i][3]-threadLims[i][2]+1][nodes[0].length];
		}
		public void run()
		{
			for(int i = threadLims[thisT][2]; i <= threadLims[thisT][3]; i++) 										//Updates each Node
			{
				double[] numVec = new double[nodes[i].length]; 						//initialize a vector for sum of all datapoints*weight for weighted average
				double sumDenom = 0;												//SumDenom = sum of weight factors used as denominator in weighted average
				
				for(int j = 0; j < neighborly[i].length; j++)        				//Cycles through neighborhood  (all nodes) for each node
				{
					if(neighborly[i][j]<maxDist)
					{
						double nj = dpcount[j];
						double h = weightsByHood[neighborly[i][j]]; 							//neighborly = degree of separation between nodes				
						double weight= nj*h;
						
						for(int k = 0; k<dpSize; k++)									//Cycles through the data points assigned to each Node in each shell
						{
							if(assignments[k][j])										//if currently examines node has give data points assigned to it, calculate
							{
								for(int l = 0; l<dp[k].length; l++)
								{
									numVec[l] += dp[k][l]*weight;
								}
								sumDenom += weight;
							}
						}
					}
				}
				for(int l = 0; l<numVec.length; l++) 						//Takes weighted average = updated vector for node
				{
					numVec[l] = numVec[l]/sumDenom;
				}
				partOfNeighbors[i-threadLims[thisT][2]] = numVec;								//Node vectors are updated in order, but reassignment happens after all are updated -> batched SOM
			}
		}		
	}
	public class AssignThread extends Thread
	{
		public int[] pp;
		public boolean[][] pAssign;
		public int thisT;
		public AssignThread(int i)
		{
			super();
			thisT = i;
			pp = new int[nodeNum];
			pAssign = new boolean[threadLims[i][1]-threadLims[i][0]+1][nodeNum];
		}
		public void run()
		{
			if(countIntraChromosomal)
			{
				for(int i = threadLims[thisT][0]; i <= threadLims[thisT][1]; i++)
				{
					double initdist = -1;
					
					if(cos)
						initdist = cosineSim(0, i);
					else
						initdist = pearson(0,i);
					
					int bestNode = 0;
					for(int j = 0; j < nodeNum; j++) 					//Each data point goes through each Node looking for most similar
					{
						double dist = 0;
						if(cos)
							dist = cosineSim(j,i);
						else
							dist = pearson(j,i);
						
						if(dist>initdist)  								//Node with highest cosine similarity value to data point = most similar
						{
							initdist = dist;
							bestNode = j;
						}
					}
					if(initdist == 0)
						System.err.print("error initdist==0");
					pAssign[i-threadLims[thisT][0]][bestNode] = true;
					pp[bestNode]++;
				}
			}
			else
			{
				for(int i = threadLims[thisT][0]; i <= threadLims[thisT][1]; i++)
				{
					double initdist = -1;
					int chr = dpChr[i];
					double[] dpSmall = new double[dpSize-dpPerChr[chr]];
					double[] nodeSmall = new double[dpSize-dpPerChr[chr]];
					int counter = 0;
					for(int j = 0; j < dpSize; j++)
					{
						if(chr != dpChr[j])
						{
							dpSmall[counter] = dp[i][j];
							counter++;
						}
					}
					double magD = 0;
					if(cos)
					{
						for(int u = 0; u<dpSmall.length; u++)
						{
							magD += (dpSmall[u])*(dpSmall[u]);
						}
						magD = Math.sqrt(magD);
					}
					
					
					/// [count] vs [k]
					int bestNode = 0;
					for(int j = 0; j < nodeNum; j++) 					//Each data point goes through each Node looking for most similar
					{
						int county = 0;
						for(int k = 0; k < dpSize; k++)
						{
							if(chr != dpChr[k])
							{
								nodeSmall[county] = nodes[j][k];
								county++;
							} 
						}

						double magN  = 0;
						if(cos)
						{
							for(int u = 0; u<nodeSmall.length; u++)
							{
								magN += (nodeSmall[u])*(nodeSmall[u]);
							}
							magN = Math.sqrt(magN);
						}
						
						double dist = 0;
						if(cos)
							dist = cosineSim(nodeSmall, dpSmall, magN, magD);
						else
							dist = pearson(nodeSmall, dpSmall);
						
						if(dist>initdist)  								//Node with highest cosine similarity value to data point = most similar
						{
							initdist = dist;
							bestNode = j;
						}
					}
					if(initdist <= 0)
					{
						System.err.println("error: initdist<=0");
					}
					pAssign[i-threadLims[thisT][0]][bestNode] = true;
					pp[bestNode]++;
				}
			}
		}
	}
	//initialize -> iterate -> terminate
	public void go() 
	{
		generateDataPoints();
		initialize();
		iterater();
		finishTraining();
	}
	
	//Reader class used to generate data points from file 
	public void generateDataPoints() 
	{
		reader.matrixReader(); 									//reader object and file location set in constructor
		ArrayList<DataPoint> points = reader.points;
		
		ArrayList<DataPoint> badPoints = new ArrayList<DataPoint>();
		
		for(int i = 0; i< points.size(); i++)
		{
			points.get(i).updateMag();
			if(points.get(i).updateMag()==0)
				badPoints.add(points.get(i));
		}
		
		int[] locations = new int[badPoints.size()];
		for(int i = 0; i< locations.length; i++)
		{
			locations[i] = points.indexOf(badPoints.get(i));
		}
		
		points.removeAll(badPoints);
		
		
		int newSize = points.size();
		if(badPoints.size()>0)
		{
			for(int i = 0; i< points.size(); i++)
			{
				points.get(i).removeDuds(newSize, locations);
			}
		}
		
		dpSize = points.size();
		dpNames = new String[dpSize];
		dpChr = new int[dpSize];
		dpMag = new double[dpSize];
		dp = new double[dpSize][dpSize];
		chrCount = 0;
		for(int i = 0; i< dpSize; i++)
		{
			String sss = points.get(i).name; 
			dpNames[i] = sss;
			String chr = sss.split(":")[0];
			if(!chr2id.containsKey(chr)){
				chr2id.put(chr, chrCount);
				chrCount++;
			}
			dpChr[i] = chr2id.get(chr);
			
			dpMag[i] = points.get(i).updateMag();
			if(dpMag[i]==0)
				System.err.println("d  "+dpMag[i]);
			dp[i] = points.get(i).g;
		}
		dpPerChr = new int[chrCount];
		for(int i = 0; i < dpSize; i++)
			dpPerChr[dpChr[i]]++;
	}
	
	//Node vectors are each set equal to a randomly selected data point's vector - Also calls for neighbor assignment
	public void initialize() 
	{
		nodes = new double[nodeNum][dpSize];
		assignments = new boolean[dpSize][nodeNum];    							/** Be Careful, this has opposite i x j convention (dp by node instead of node by dp)*/
		dpcount = new int[nodeNum];
		nMag = new double[nodeNum];
		
		for(int i = 0; i<nodeNum; i++)
			nodes[i] = dp[(int)(Math.random()*dpSize)];
		threadLims = new int[threadNum][4];
		int dpNum = (dpSize / threadNum)-1;
		int dpExtra = dpSize % threadNum;
		int nNum = (nodeNum / threadNum)-1;
		int nExtra = nodeNum % threadNum;
		int curN = 0;
		int curD=0;
		for(int i = 0; i< threadNum; i++)
		{
			threadLims[i][0]=curD;
			curD += dpNum;
			if(dpExtra > i)
				curD++;
			threadLims[i][1]=curD;
			
			threadLims[i][2]=curN;
			curN += nNum;
			if(nExtra > i)
				curN++;
			threadLims[i][3]=curN;

			curN++;
			curD++;
		}
		assignNeighbors();
		assignNodes();
	}
	//Assigns data points to nodes based on similarity metric
	public void assignNodes()
	{
		double init = System.currentTimeMillis();
		double magD = 0;
		for(int i = 0; i<nodeNum; i++) 						//calculate node magnitudes  
		{
			magD = 0;
			for(int u = 0; u<nodes[i].length; u++)
			{
				magD += (nodes[i][u])*(nodes[i][u]);
			}
			magD = Math.sqrt(magD);
			
			nMag[i] = magD;
			if(nMag[i]==0)
				System.out.println("nMag " + i + " = " + nMag[i]);
		}
		for(int i = 0; i< threadNum; i++)
		{
			AssignThread t = new AssignThread(i);
			aThreads[i] = t;
			aThreads[i].start();
		}
		for(Thread t : aThreads){try {t.join();} catch (InterruptedException e) {}}				//join all threads at completion
		assignments = new boolean[dpSize][nodeNum];
		dpcount = new int[nodeNum];
		
		synchronized (this)
		{
			for(int i = 0; i<threadNum; i++)
			{
				for(int j = 0; j < dpcount.length; j++)
				{
					dpcount[j] += aThreads[i].pp[j];
				}
				for(int j = 0; j < aThreads[i].pAssign.length; j++)
				{
					assignments[threadLims[i][0]+j] = aThreads[i].pAssign[j];
				}
			}
		}
		timerAssign += System.currentTimeMillis()-init;
	}	
	//Cosine similarity
	public double cosineSim(int iNode, int jDP)							//Cosine Similarity = (A  B)/(||A||*||B||)
	{																			//Higher values indicate more similarity (ranges from [-1, 1])
		double dot = 0;
		double magN = nMag[iNode];
		double magD = dpMag[jDP];
		for(int i = 0; i < nodes[iNode].length; i++)
		{
			dot += (nodes[iNode][i]) * (dp[jDP][i]);
		}
		dot = dot/(magN*magD);
		if(Double.isNaN(magN))
		{
			System.err.println("NaN  "+ magN + "   " + magD);
			dot = 0;
		}
		
		if(dot > 1 && dot< 1.0000001)
	    	dot=1;
	    
		return dot;
	}
	public double cosineSim(double[] nodal, double[] datal, double magNode, double magData)
	{
		double dot = 0;
		double magN = magNode;
		double magD = magData;
		for(int i = 0; i < nodal.length; i++)
		{
			dot += (nodal[i]) * (datal[i]);
		}
		dot = dot/(magN*magD);
		if(Double.isNaN(magN))
		{
			System.err.println("NaN  "+ magN + "   " + magD);
			dot = 0;
		}
		//System.out.println(dot);
		if(dot > 1 && dot< 1.0000001)
	    	dot=1;
		return dot;
	}
	
	
	public double pearson(int iNode, int jDP)
	{
		double sx = 0.0;
	    double sy = 0.0;
	    double sxx = 0.0;
	    double syy = 0.0;
	    double sxy = 0.0;

	    int n = nodes[iNode].length;
	    for(int i = 0; i < n; i++) 
	    {
	      double x = nodes[iNode][i];
	      double y = dp[jDP][i];

	      sx += x;
	      sy += y;
	      sxx += x * x;
	      syy += y * y;
	      sxy += x * y;
	    }

	    // covariation
	    double cov = sxy / n - sx * sy / n / n;
	    // standard error of x
	    double sigmax = Math.sqrt(sxx / n -  sx * sx / n / n);
	    // standard error of y
	    double sigmay = Math.sqrt(syy / n -  sy * sy / n / n);

	    // correlation is just a normalized covariation
	    
	    double rr = cov / sigmax / sigmay;
	    if(rr > 1 && rr< 1.0000001)
	    	rr=1;
	    return rr;
	}
	
	public double pearson(double[] nodal, double[] datal)
	{
		double sx = 0.0;
	    double sy = 0.0;
	    double sxx = 0.0;
	    double syy = 0.0;
	    double sxy = 0.0;

	    int n = nodal.length;
	    for(int i = 0; i < n; i++) 
	    {
	      double x = nodal[i];
	      double y = datal[i];

	      sx += x;
	      sy += y;
	      sxx += x * x;
	      syy += y * y;
	      sxy += x * y;
	    }

	    // covariation
	    double cov = (sxy / n) - ((sx/n) * (sy/n));
	    // standard error of x
	    double sigmax = Math.sqrt((sxx / n) -  ((sx/n) * (sx/n)));
	    // standard error of y
	    double sigmay = Math.sqrt((syy / n) -  ((sy/n) * (sy/n)));

	    // correlation is just a normalized covariation
	    
	    double rr = cov / (sigmax * sigmay);
	    if(rr > 1 && rr< 1.0000001)
	    	rr=1;

	    return rr;
	}
	
	//Calls iterate methods the number of times dictated by the parameter
	public void iterater()
	{
		for(double i = 0; i < iterations; i++)
		{
			//This code writes SOMs at different stages in training
//			if(i%(iterations/4) == 0)
//			{
//				finishTraining();
//				writeFile((int)i);
//			}
			nFactor(i);
			iterate(i);
		}
	}
	
	public int nFactorSize()
	{
		int i = 0;
		double sig = sgm;
		double lr = 1;
		double r = 1;
		while(r > zero)
		{
			r = lr*Math.exp(-1*((i*i)/(2*sig*sig)));
			if(r>zero)
				i++;
		}
		return i;
	}
	//Finds the weight factor of a given data point based on learning rate and neighborhood function
	public void nFactor(double itercount) 
	{   
		double sig = sgm-sgmStop;
		sig = (sig - (sig*(itercount/iterations)))+sgmStop; 												//sigma = width of Guassian kernel (sgm ->1)
		double r = 0;
		double lrStop = 0.01;
		double lr = 1-lrStop;
		lr = (lr - (lr*(itercount/iterations)))+lrStop;
		for(double o = 0; o < weightsByHood.length; o++)
		{
			r = lr*Math.exp(-1*((o*o)/(2*sig*sig)));
			weightsByHood[(int)o] = r;
		}
	}
	//Where the magic happens
	public void iterate(double itercount)
	{
		double init = System.currentTimeMillis();
		for(int i = 0; i< threadNum; i++)
		{
			IterateThread t = new IterateThread(i);
			iThreads[i] = t;
			iThreads[i].start();
		}
		for(Thread t : iThreads){try {t.join();} catch (InterruptedException e) {}}
		
		synchronized (this){
		for(int i = 0; i < threadNum; i++)
		{
			for(int j = 0; j < iThreads[i].partOfNeighbors.length; j++)
			{
				nodes[threadLims[i][2]+j] = iThreads[i].partOfNeighbors[j];
			}
		}
		}
		
		count++;
		timerIterate += System.currentTimeMillis()-init;
		assignNodes();
	}
	
	//Assigns neighbors based on hex dist
	public void assignNeighbors() 								
	{
		neighborly = new int[nodeNum][nodeNum];
		for(int i = 0; i< nodeNum; i++)
		{
			int xA = i%xNodes;
			int yA = i/yNodes;
			for(int j = i; j < nodeNum; j++)
			{
				int xB = j%xNodes;
				int yB = j/yNodes;
				int dist = hexDist(xA, yA, xB, yB);
				neighborly[i][j] = dist;
				if(dist > farthestNeighbor)
					farthestNeighbor = dist;
				neighborly[j][i] = neighborly[i][j];
			}
		}
		maxDist = Math.min(nFactorSize(), farthestNeighbor);
		weightsByHood = new double[maxDist];
	}
	
	//Called by go() after iterations are complete
	public void finishTraining()       
	{
		//writeFile();
		findPearsonQuality();
		findCosineQuality();
	}
	//Quality is defined as the average similarity between dataPoints and their assigned nodes 
	public void findPearsonQuality()
	{
		pearsonQuality = 0;
		for(int i = 0; i < assignments.length; i++)					//dp i
		{
			for(int j =0; j< assignments[i].length; j++)			//node j
			{
				if(assignments[i][j])
					pearsonQuality += pearson(j, i);
			}
		}

		pearsonQuality /= dpSize;
	}
	public void findCosineQuality()
	{
		cosineQuality = 0;
		for(int i = 0; i < assignments.length; i++)					//dp i
		{
			for(int j =0; j< assignments[i].length; j++)			//node j
			{
				if(assignments[i][j])
					cosineQuality += cosineSim(j, i);
			}
		}
		
		cosineQuality /= dpSize;
	}
	public Boolean findStability(int d, int p)
	{
		boolean doofy = false;
		int nodeD = 0;
		int nodeP = 0;
		
		for(int i = 0; i < assignments[d].length; i++)					
		{
				if(assignments[d][i])
					nodeD = i;
				if(assignments[p][i])
					nodeP = i;
		}
		
		doofy = hexDist(nodeD%xNodes, nodeD/xNodes, nodeP%xNodes, nodeP/xNodes) <= 1;
			
		return doofy;
	}
	public double degreesOfSep()
	{
		int nodes = nodeNum;
		double sepp = 0;
		
		double[][] dist = new double[nodes][nodes];
		for (int i = 0; i<nodes; i++)
		{
			int xCoordA = i%xNodes;
			int yCoordA = i/yNodes;
			for (int j = 0; j<nodes; j++)
			{
				int xCoordB = j%xNodes;
				int yCoordB = j/yNodes;
				
				double ddd = hexDist(xCoordA, yCoordA, xCoordB, yCoordB);
				dist[i][j] = ddd;
			}
		}
			double count = 0;
			for(int j = 0; j<nodes; j++)
			{
				for(int k = 0; k<nodes; k++)
				{
					sepp += dist[j][k]*dpcount[k]*dpcount[j];
					count+= dpcount[k]*dpcount[j];
				}
			}
			sepp /= count;
			return sepp;		
	}
	public int hexDist(int x1, int y1, int x2, int y2)
	{
		if(x1 == x2 && y1 == y2)
			return 0;
		else
		{
			int hm = 0;
			int vm = 0;
			int cm = 0;
			boolean right = x1-x2<0;
			
			hm = Math.abs(x1-x2);
			vm = Math.abs(y1-y2);
			if(xNodes-x1<xNodes-x2 && xNodes-x1 + x2 < hm)
			{
				right = true;
				hm =  xNodes-x1 + x2;	
			}
			else if (xNodes-x1>xNodes-x2 && xNodes-x2 +x1 < hm)
			{
				right = false;
				hm = xNodes-x2 +x1;
			}
			
			if(yNodes-y1<yNodes-y2 && yNodes-y1 + y2 < vm)
			{
				vm = yNodes-y1 + y2;
			}
			else if(yNodes-y1>yNodes-y2 && yNodes-y2 + y1 < vm)
			{
				vm = yNodes-y2 + y1;
			}
			
			if(vm %2 !=0)
			{
				if(!right && y1%2 == 0)
				{
					cm = (int) Math.min(hm, (((double)vm)/2 +.5));
				}
				else if(!right && y1%2 == 1)
				{
					cm = (int) Math.min(hm, (((double)vm)/2));
				}
				else if(right && y1%2 == 0)
				{
					cm = (int) Math.min(hm, (((double)vm)/2));
				}
				else //if(right && y1%2 == 1)
				{
					cm = (int) Math.min(hm, (((double)vm)/2 +.5));
				}
			}
			else
				cm = (int) Math.min(hm, ((((double)vm)/2)));
			return hm+vm-cm; 
		}
	}
	
	public double percentUnoccupied()
	{
		double percent = 0;
		for (int i = 0; i< nodeNum; i++)
		{
			if (dpcount[i]== 0)
			{
				percent++;
			}
		}
		percent /= nodeNum;
		percent = ((double)((int)(percent*10000))/10000);
		return percent;
	}
	//Saves trained SOM in text file
	public void writeFile(int sampleSize){		
		BufferedWriter b;
		
		String somFilename = filePrefix+".som";
		String infoFilename = filePrefix+".info";
		String nodevecFilename = filePrefix+".nodevec";
		
		//SOM file
		try 
		{
			System.out.println("Printing SOM to "+somFilename);
						
			FileWriter ff = new FileWriter(somFilename,true);
			b = new BufferedWriter(ff);
			PrintWriter printer = new PrintWriter(b);
			
			printer.print(xNodes+"x"+yNodes+ "\nSigma:" + sgm + "->" + sgmStop +"\n");
			for(int i = 0; i<xNodes; i++)
			{
				for(int j = 0; j<yNodes; j++)
				{
					printer.print("["+j +","+i+"]\t"+"(");
					int count = 0;
					int nono = i*xNodes + j%xNodes;
					for(int k = 0; k< dpSize; k++)
					{	
						if(assignments[k][nono])
						{
							printer.print(dpNames[k]);
							count++;
							if(count < dpcount[nono])
								printer.print(", ");
						}
					}
					printer.print(")" + "\n");
				}
				printer.print("\n");
			}
			printer.print("\n");
			printer.close();
		}catch (IOException e){
			e.printStackTrace();
			System.err.print("Error printing SOM file "+somFilename);
		}
		
		//Info file
		try 
		{
			System.out.println("Printing info to "+infoFilename);
						
			FileWriter ff = new FileWriter(infoFilename,true);
			b = new BufferedWriter(ff);
			PrintWriter printer = new PrintWriter(b);
			
			String simMetric = cos ? "cosine" : "pearson";
			String intra = countIntraChromosomal ? "true" : "false";
			
			printer.print("Nodes:\t"+xNodes+"x"+yNodes+"\n" +
						"Sigma:\t" + sgm + "->" + sgmStop +"\n" +
						"Iterations:\t" + iterations +"\n" +
						"SimilarityMetric:\t" + simMetric +"\n" +
						"IncludingIntraChromosomal:\t" + intra +"\n" +
						"Samples:\t" + sampleSize +"\n" +
						"CosineQuality:\t" + cosineQuality +"\n" +
						"PearsonQuality:\t" + pearsonQuality +"\n" +
						"Stability:\t" + stability +"\n" +
						"PercentUnoccupied:\t"+percentUnoccupied() +"\n");
			printer.close();
		
		}catch (IOException e){
			e.printStackTrace();
			System.err.print("Error printing info file "+infoFilename);
		}
		
		try 
		{
			System.out.println("Printing node vectors to "+nodevecFilename);
			
			FileWriter ff = new FileWriter(nodevecFilename,true);
			b = new BufferedWriter(ff);
			PrintWriter printer = new PrintWriter(b);
			
			for(int i = 0; i < nodes.length; i++)
			{
				int x = i%xNodes;
				int y = i/yNodes;
				printer.print("("+ x+","+y +")\t");
				for(int j = 0; j < nodes[i].length-1; j++)
				{
					printer.print(nodes[i][j]+"\t");
				}
				printer.print(nodes[i][nodes[i].length-1]+"\n");
			}
			printer.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
			System.err.print("Error printing node vector file "+nodevecFilename);
		}
	}
}

