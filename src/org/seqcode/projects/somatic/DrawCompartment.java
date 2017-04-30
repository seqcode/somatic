package org.seqcode.projects.somatic;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.text.Document;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.w3c.dom.DOMImplementation;

public class DrawCompartment extends JPanel
{
	private static final long serialVersionUID = 1L;
	public double gini;
	public int nodes,xs,ys,xNodes,yNodes, maxDataPoints, minDataPoints, colorNum, winW, winH, binSize;
	public ArrayList<Node> coords;
	public ArrayList<DataPoint> dataPoints;
	public ArrayList<MiniNode> nodeList;
	public ArrayList<Color> colors;
	MiniSystem nodeSystem;
	public boolean nodeCallsOnly, multi, weighting, norm, equalWeight;
	int swap, col, www;
	public double sep, cutoff;
	String map, directory;
	public DrawHex d;
	public String fold, mapper, der;
	public int maxBins, minBins, pValnVal, locCol, weightCol;
	public ArrayList<DataPoint> bins;
	public ArrayList<String> searchers,compNames;
	public double[][] compCount;
	public DrawCompartment(String s, String ss, boolean filesContainNodes)
	{
		nodeCallsOnly = filesContainNodes;
		map = s;
		directory = ss;
		cutoff = 0.01;
		multi = false;
		norm = false;//true;
		swap=0;
		gini=0;
		sep = 0;
		weighting = false;
		String ffs = s;
		yNodes=0;
		xNodes=0;
		reader(ffs);
		nodes = yNodes*xNodes;
    	colors = new ArrayList<Color>();
    	colorNum=100;
    	binSize = dataPoints.get(0).maxLocus - dataPoints.get(0).minLocus;
    	col = 0;
    	www = 0;
    	equalWeight = true;
    	String mapp = System.getProperty("user.dir")+"/"+map;
    	
    	searchers = new ArrayList<String>();
    	compNames = new ArrayList<String>();
    	mapReader(mapp);
		File folder = new File(System.getProperty("user.dir")+"/"+directory);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) 
		{
		    if (file.isFile() && (file.getName().indexOf(".txt") != -1 || file.getName().indexOf(".domains")!=-1)) 
		    {
		    	searchers.add(file.getPath());
		    	compNames.add(file.getName());
		    }
		}
		nodes = yNodes*xNodes;
		if(!filesContainNodes)
		{
			compCount = new double[nodes][searchers.size()];
			for(int i = 0; i < searchers.size(); i++)
			{
				search(searchers.get(i), i);
				System.out.println(searchers.get(i));
			}
		}
		else
		{
			colors = new ArrayList<Color>();
			for(int i = 0; i <nodeList.size(); i++)
			{
				nodeList.get(i).color = Color.DARK_GRAY;
			}
			for(int i = 0; i < searchers.size(); i++)
			{
				colors.add(new Color((int)(255*Math.random()),(int)(255*Math.random()),(int)(255*Math.random())));
				searchNodes(searchers.get(i),i);
			}
		}
    	//System.out.println(binSize);
	}
	public void searchNodes(String s, int ind)
	{
		ArrayList<String> strings = inputRead(s);
		for(String whole: strings)
		{
			String[] wholes = whole.split("\n");
			for(String node:wholes)
			{
				if(node.contains("(")&&node.contains(","))
				{
					String strr = node.substring(node.indexOf("(")+1,node.indexOf(","));
					String strr2 = node.substring(node.indexOf(",")+1,node.indexOf(")"));
					int x = Integer.parseInt(strr);
					int y = Integer.parseInt(strr2);
					nodeSystem.get(x,y).color = colors.get(ind);
				}
			}
		}
	}
	
	public void saveImg(String name, String file, int w, int h)
	{
	    BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
	    Graphics2D gg = bi.createGraphics();
	    gg.setClip(100, 100, 300, 300);
	    print((Graphics) gg.getClip());
	    try 
	    {
	        File outputfile = new File(file+"/"+name+".png");
	        ImageIO.write(bi, "png", outputfile);
	    } catch (IOException e) {}
	}
	
	public void mapReader(String f)
	{
		ArrayList<String> StringMat = new ArrayList<String>();
		try 
		{
			Scanner in = new Scanner(new FileReader(f));
			String sizer = in.next();
			int xo = Integer.parseInt(sizer.substring(0,sizer.indexOf("x")));
			int yo = Integer.parseInt(sizer.substring(sizer.indexOf("x")+1,sizer.length()));
			//System.out.println(xo + "  x  "+ yo);
			in.next();
			while(in.hasNext())
			{
				StringMat.add(in.next());
			}
			createNoder(StringMat, xo, yo);
		} catch (FileNotFoundException e) {e.printStackTrace();}
	}
	public void swap()
	{
		if(swap >= 3)
		{
			swap = 0;
			heatMapping();
		}
		else
		{
			swap ++;
			heatMapping();
		}
		repaint();
	}
	public void search(String file, int ind)
	{
		for(int i = 0; i < nodeSystem.size(); i++)
		{
			MiniNode mini = nodeSystem.get(i);
			mini.counting.clear();
			mini.weight = 0;
		}
		ArrayList<String> strings = inputRead(file);
		for(String whole: strings)
		{
			int chr = 0;
			int locus1 = 0; int locus2 = 0;
			double weight = 0;
			String[] wholes = whole.split("\t");
			//System.out.println(whole);
			if (whole.contains("\t") && whole.contains(":")&&whole.contains("chr")&& wholes[col].contains("-") && !whole.contains("_") && !whole.substring(whole.indexOf("chr")+3,whole.indexOf(":")).equalsIgnoreCase("M"))
			{
				weighting = true;
				if(wholes[col].substring(whole.indexOf("chr")+3,whole.indexOf(":")).equalsIgnoreCase("X")||wholes[col].substring(whole.indexOf("chr")+3,whole.indexOf(":")).equalsIgnoreCase("Y"))
						chr = 23;
				else 
					chr  = Integer.parseInt(wholes[col].substring(wholes[col].indexOf("chr")+3,wholes[col].indexOf(":")));
				
				String loc1 = wholes[col].substring(wholes[col].indexOf(":")+1, wholes[col].indexOf("-"));
				locus1 = Integer.parseInt(loc1);
				locus2 = Integer.parseInt(wholes[col].substring(wholes[col].indexOf("-")+1));
				if(equalWeight)
					weight = 1.0;
				else
					weight = Double.parseDouble(wholes[www]);     											/** This needs to be taken as an argument somehow, not hard coded*/
				int locus = locus1;
				ArrayList<Integer> inds = new ArrayList<Integer>();
				int count = 0;
				while(locus<locus2)
				{
					for(int i = 0; i< dataPoints.size(); i++)
					{
						if(dataPoints.get(i).chrome == chr && dataPoints.get(i).minLocus <= locus && dataPoints.get(i).maxLocus>=locus)
						{
							count++;
							inds.add(i);
						}
					}
					locus+=binSize;
				}
				weight/=count;
				for(Integer i: inds)
				{
					dataPoints.get(i).myMini.counting.add(dataPoints.get(i));
					dataPoints.get(i).myMini.weight += weight;
				}
			}
		}
		for(int i = 0; i < nodeSystem.size(); i++)
		{
			compCount[i][ind] = nodeSystem.get(i).weight;
		}
	}
	public void search(String file, int locCol, int weightCol)
	{
		multi = false;
		for(int i = 0; i < nodeSystem.size(); i++)
		{
			MiniNode mini = nodeSystem.get(i);
			mini.counting.clear();
			mini.weight = 0;
		}
		ArrayList<String> strings = inputRead(file);
		for(String whole: strings)
		{
			int chr = 0;
			int locus1 = 0; int locus2 = 0;
			double weight = 0;
			String[] wholes = whole.split("\t");
			//System.out.println(whole);
			if(whole.contains("\t") && whole.contains(":")&&whole.contains("chr")&& !wholes[locCol].contains("-") && !whole.contains("_"))
			{
				weighting = true;
				if(wholes[col].substring(whole.indexOf("chr")+3,whole.indexOf(":")).equalsIgnoreCase("X")||wholes[col].substring(whole.indexOf("chr")+3,whole.indexOf(":")).equalsIgnoreCase("Y"))
						chr = 23;
				else 
					chr  = Integer.parseInt(wholes[locCol].substring(whole.indexOf("chr")+3,whole.indexOf(":")));
				locus1 = Integer.parseInt(wholes[locCol].substring(whole.indexOf(":")+1));
				//locus2 = Integer.parseInt(whole.substring(whole.indexOf("-")+1,whole.indexOf("\t")));
				int locus = locus1;//(locus1 +locus2)/2;
				if(equalWeight)
					weight = 1.0;
				else
					weight = Double.parseDouble(wholes[weightCol]);     											/** This needs to be taken as an argument somehow, not hard coded*/
				for(int i = 0; i< dataPoints.size(); i++)
				{
					if(dataPoints.get(i).chrome == chr && dataPoints.get(i).minLocus <= locus && dataPoints.get(i).maxLocus>=locus)
					{
						//System.out.println(chr + " " + locus + "  " + i);
						dataPoints.get(i).myMini.counting.add(dataPoints.get(i));
						dataPoints.get(i).myMini.weight += weight;
					}
				}
			}
			else if (whole.contains("\t") && whole.contains(":")&&whole.contains("chr")&& wholes[locCol].contains("-") && !whole.contains("_") && !whole.substring(whole.indexOf("chr")+3,whole.indexOf(":")).equalsIgnoreCase("M"))
			{
				weighting = true;
				if(wholes[locCol].substring(whole.indexOf("chr")+3,whole.indexOf(":")).equalsIgnoreCase("X")||wholes[locCol].substring(whole.indexOf("chr")+3,whole.indexOf(":")).equalsIgnoreCase("Y"))
						chr = 23;
				else 
					chr  = Integer.parseInt(wholes[locCol].substring(wholes[locCol].indexOf("chr")+3,wholes[locCol].indexOf(":")));
				
				String loc1 = wholes[locCol].substring(wholes[locCol].indexOf(":")+1, wholes[locCol].indexOf("-"));
				locus1 = Integer.parseInt(loc1);
				locus2 = Integer.parseInt(wholes[locCol].substring(wholes[locCol].indexOf("-")+1));
				if(equalWeight)
					weight = 1.0;
				else
					weight = Double.parseDouble(wholes[weightCol]);     											/** This needs to be taken as an argument somehow, not hard coded*/
				int locus = locus1;
				ArrayList<Integer> inds = new ArrayList<Integer>();
				int count = 0;
				while(locus<locus2)
				{
					for(int i = 0; i< dataPoints.size(); i++)
					{
						if(dataPoints.get(i).chrome == chr && dataPoints.get(i).minLocus <= locus && dataPoints.get(i).maxLocus>=locus)
						{
							count++;
							inds.add(i);
						}
					}
					locus+=binSize;
				}
				weight/=count;
				for(Integer i: inds)
				{
					dataPoints.get(i).myMini.counting.add(dataPoints.get(i));
					dataPoints.get(i).myMini.weight += weight;
				}
			}
		}
		heatMapping();
		repaint();
		
	}
	
	public ArrayList<String> inputRead(String file)
	{
		ArrayList<String> StringMat = new ArrayList<String>();
		try 
		{
			Scanner in = new Scanner(new FileReader(file));
			String sizer = in.next();
			//System.out.println(xo + "  x  "+ yo);
			in.next();
			while(in.hasNextLine())
			{
				StringMat.add(in.nextLine());
			}
		} catch (FileNotFoundException e) {e.printStackTrace();}
		return StringMat;

	}
	public void countingDPS(int chr)
	{
		multi = false;
		weighting = false;
		for(int i =0; i<nodeSystem.size();i++)
		{
			MiniNode mini = nodeSystem.get(i);
			mini.counting.clear();
			for(int j =0; j<mini.bins.size(); j++)
			{
				if(chr <= -1)
				{
					if(Math.random()>.9)
						mini.counting.add(mini.bins.get(j));
				}
				else if (chr == 0)
					mini.counting.add(mini.bins.get(j));
				else if (mini.bins.get(j).chrome == chr)
					mini.counting.add(mini.bins.get(j));
			}
			//System.out.println(mini.counting.size());
		}
	    maxDataPoints = 0;
	    minDataPoints = 0;
		heatMapping();
		repaint();
	}
	public void reader(String f)
	{
		ArrayList<String> StringMat = new ArrayList<String>();
		try 
		{
			Scanner in = new Scanner(new FileReader(f));
			String sizer = in.next();
			int xo = Integer.parseInt(sizer.substring(0,sizer.indexOf("x")));
			int yo = Integer.parseInt(sizer.substring(sizer.indexOf("x")+1,sizer.length()));
			//System.out.println(xo + "  x  "+ yo);
			in.next();
			while(in.hasNext())
			{
				StringMat.add(in.next());
			}
			createNoder(StringMat, xo, yo);
		} catch (FileNotFoundException e) {e.printStackTrace();}
	}
	public void createNoder(ArrayList<String> strings, int x, int y)
	{
		nodeList = new ArrayList<MiniNode>(x*y);
		dataPoints = new ArrayList<DataPoint>();
		MiniNode current = null;
		xNodes = x;
		yNodes = y;
		for(String whole: strings)
		{
			if(whole.contains("["))
			{
				current = new MiniNode(whole);
				nodeList.add(current);
			}
			else
				current.addDP(whole);
		}
		for(int i = 0; i<nodeList.size(); i++)
		{
			for(int j = 0; j < nodeList.get(i).bins.size(); j++)
			{
				dataPoints.add(nodeList.get(i).bins.get(j));
			}
		}
		//System.out.println(dataPoints.size());
		nodeSystem = new MiniSystem(nodeList,xNodes,yNodes);
	}
	public void paintComponent(Graphics g)
	  {
	    super.paintComponent(g);
	    //colorBar(g);
	    setBackground(Color.GRAY);
	    
	    int centerNode = 2450;
//	    System.out.println(centerNode);
	    for(int i = 0; i<nodeList.size(); i++)
	    {	
	    	g.setColor(nodeList.get(i).color);
	    	g.fillPolygon(nodeList.get(i).p);
//	    	Graphics2D g2 = (Graphics2D) g;
//	        g2.setStroke(new BasicStroke(2));
	    	g.setColor(nodeList.get(i).outline);
		    g.drawPolygon(nodeList.get(i).p);
	    }
	    for(int i = 0; i<colors.size(); i++)
	    {
	    	g.setColor(colors.get(i));
	    	int xCenter = (i*50)+50;
	    	int xLabel = (i*50)+57;
	    	int yCenter = 650;
	    	g.fillRect(xCenter-5, yCenter-5, 10, 10);
	    	g.setColor(Color.BLACK);
	    	g.setFont(new Font("Serif", 5, 9));
	    	g.drawString(compNames.get(i), xLabel, yCenter+4);
	    }
    	//g.drawString("Sep = "+ sep, getWidth()-400, 15);
	}
	public void nodeBuild(int winW, int winH)
	{  
		int xMin = (int)(.025 * winW);
	    int yMin = (int)(.025 * winH);
	    int xMax = (int)(.90 * winW);
	    int yMax = (int)(.85 * winH);
		
	    double winWidth = xMax-xMin;
	    double winHeight = yMax-yMin;
	    
	    double height = winHeight/yNodes;
	    double width = winWidth/xNodes;
	    height = height/3;
	    width = width/2;
	    int x = (int) (width);
	    int y = (int) (height);
	    int w = (int) (winWidth/width);
	    int h = (int)(winHeight/height);
	    for(int i = 0; i<h; i++)
		{
	    	for(int m = 0; m<w; m++)
			{
				if((i%6==1 && m%2==0)||(i%6==4 && m%2 == 1))
				{
					MiniNode p = nodeSystem.get(m/2,i/3);
					p.xLoc = m*x+xMin;
					p.yLoc = i*y+yMin;
				}
			}
		}
	   for(int c = 0; c<nodeList.size(); c++)
	   {
		  MiniNode m = nodeList.get(c);
		  
		  int[] xPoints = new int[6];
		  int[] yPoints = new int[6];
		  
		  xPoints[0] = m.xLoc;
		  yPoints[0] = m.yLoc-(2*y)+1;
		  
		  xPoints[1] = m.xLoc+x-1;
		  yPoints[1] = m.yLoc-y+1;
		  
		  xPoints[2] = m.xLoc+x-1;
		  yPoints[2] = m.yLoc+y-1;
			
		  xPoints[3] = m.xLoc;
		  yPoints[3] = m.yLoc+(2*y)-1;

		  xPoints[4] = m.xLoc-x+1;
		  yPoints[4] = m.yLoc+y-1;
			
		  xPoints[5] = m.xLoc-x+1;
		  yPoints[5] = m.yLoc-y+1;
		  
		  m.polygonMaker(xPoints, yPoints, 6);
	   }
	    
	    
//	    for(int c = 0; c<nodeList.size(); c++)
//		   {
//			  MiniNode m = nodeList.get(c);
//			  
//			  int[] xPoints = new int[4];
//			  int[] yPoints = new int[4];
//			  
//			  xPoints[0] = m.xLoc+x;
//			  yPoints[0] = m.yLoc-y;
//			  
//			  xPoints[1] = m.xLoc+x;
//			  yPoints[1] = m.yLoc+y;
//
//			  xPoints[2] = m.xLoc-x;
//			  yPoints[2] = m.yLoc+y;
//				
//			  xPoints[3] = m.xLoc-x;
//			  yPoints[3] = m.yLoc-y;
//			  
//			  m.polygonMaker(xPoints, yPoints, 4);
//		   }
	}
	public void degreesOfSep()
	{
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
//				if(Math.random()<=.0001)
//					System.out.println("(" + xCoordA+ ", " + yCoordA+ ")   ->   ("  +xCoordB + ", " +  yCoordB +")  = " + ddd);
			}
		}
			double count = 0;
			for(int j = 0; j<nodes; j++)
			{
				for(int k = 0; k<nodes; k++)
				{
					sepp += dist[j][k]*(nodeList.get(k).counting.size())*(nodeList.get(j).counting.size());
					count+= nodeList.get(k).counting.size()*(nodeList.get(j).counting.size());
				}
			}
			sepp /= count;
			sep = sepp;
			
		/*for(int i = 0; i<nodes; i++)
		{
			for(int j = 0; j<nodes; j++)
			{
				System.out.print(dist[i][j] + "\t");
			}
			System.out.println("\n \n");
		}*/
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
					//System.out.println("(" + x1+ ", " + y1+ ")   ->   ("  +x2 + ", " +  y2 +")  = " + (hm-cm+vm-cm+cm));
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
					//System.out.println("(" + x1+ ", " + y1+ ")   ->   ("  +x2 + ", " +  y2 +")  = " + (hm-cm+vm-cm+cm));
				}
			}
			else
				cm = (int) Math.min(hm, ((((double)vm)/2)));
			//System.out.println("(" + x1+ ", " + y1+ ")   ->   ("  +x2 + ", " +  y2 +")  = " + "right? " + right + "     " + (hm) + " + " + vm + " - " + cm + "  ===   " + (hm + vm -cm));
			return hm+vm-cm; 
		}
	}
//	public void colors()
//	{
//		for(int k = 0; k<colorNum;k++)
//		{
//			int red = 255;//(int)(k*255/colorNum);
//			int blue = (int)(255-(k*255/colorNum));
//			int green = (int)(255-(k*255/colorNum));
//			Color c = new Color(red, green, blue);
//			colors.add(c);
//		}
//	}
	public void heatMapping()
	{
		if(nodeCallsOnly)
			return;
		if(colors.size()<7)
		{
			colors.add(Color.GREEN);colors.add(Color.YELLOW);colors.add(Color.BLUE);
			colors.add(Color.CYAN);colors.add(Color.RED);colors.add(Color.MAGENTA);colors.add(Color.GRAY);
		}
//		colors.add(Color.RED);colors.add(Color.RED);colors.add(Color.BLUE);
//		colors.add(Color.BLUE);colors.add(Color.BLUE);colors.add(Color.BLUE); colors.add(Color.WHITE);
		
//		for(int i = 0; i < compCount[0].length; i++)
//		{
//			int R = (int)(Math.random()*256);
//			int G = (int)(Math.random()*256);
//			int B= (int)(Math.random()*256);
//			colors.add(new Color(R, G, B));
//		}
		double[] largest = new double[compCount[0].length];
		for(int i = 0; i < nodeSystem.size(); i++)
		{
			for(int j = 0; j < compCount[i].length; j++)
			{
				if(compCount[i][j]>largest[j])
					largest[j] = compCount[i][j];
			}
		}
		for(int i = 0; i<nodeSystem.size(); i++)
		{
			double best = 0;
			int colorInd = 0;
			for(int j = 0; j < compCount[i].length; j++)
			{
				if(compCount[i][j]/largest[j]>best/largest[j])
				{
					best = compCount[i][j];
					colorInd = j;
				}
				if(best > 0 )
				{
					double ratio = best/largest[colorInd];
					int R = (int) ((colors.get(colorInd).getRed()*ratio) + (Color.WHITE.getRed()*(1-ratio)));
					int G = (int) ((colors.get(colorInd).getGreen()*ratio) + (Color.WHITE.getGreen()*(1-ratio)));
					int B = (int) ((colors.get(colorInd).getBlue()*ratio) + (Color.WHITE.getBlue()*(1-ratio)));
					nodeSystem.get(i).color = new Color(R,G,B);//colors.get(colorInd);
					nodeSystem.get(i).outline = colors.get(colorInd);
				}
				else
					nodeSystem.get(i).color = Color.GRAY;
			}
		}
	}
}
