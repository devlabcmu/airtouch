
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

public class iPadFeatureExtractor extends PApplet
{
	String[] data;
	
	 PrintWriter out;
	 
	 
	public void setup()
	{
		background(0);
		size(1280, 800);
		frameRate(200);
		PFont myFont = createFont("Arial", 20);
		textFont(myFont);
		textAlign(CENTER);
		smooth();
		data = loadStrings("../data.csv");
		try
		{
			out = new PrintWriter(new BufferedWriter(new FileWriter("out.csv")));
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		for (int i=0;i<1606;i++)
			out.print("f"+i+",");
		
		out.println("class");
	}

	int counter = 1;
	
	public void draw()
	{
		background(255);

		String[] tokens = data[counter].split(",");
		ArrayList<PVector> blobs = parseLine(tokens);
		
		//System.out.println(blobs.size());
		
		if (blobs.size()<=0)
		{
			counter++;
			return;
		}
			
		
		
		float[] features = extractFeatures(blobs);
		
		String output = "";
		
		
		
		for (int i=0;i<features.length;i++)
		{
			output += features[i]+",";
		}
		output += tokens[1];
		
		
		
		out.println(output);
		
		out.flush();

		if (counter<data.length-1)
			counter++;
		else
			System.exit(0);
		
		//System.out.println("a" + counter);
		//System.out.println(data.length-1);
	
	
	}
	
	
	public ArrayList<PVector> parseLine(String[] tokens)
	{
		ArrayList<PVector> blobs = new ArrayList<PVector>();
		
		for (int i=2;i<tokens.length;i+=3)
		{
			float x = Float.parseFloat(tokens[i].trim());
			float y = Float.parseFloat(tokens[i+1].trim());
			float s = Float.parseFloat(tokens[i+2].trim())*20f;
		
			if (x==-1)
				break;
			
			blobs.add(new PVector(x,y,s));
		}
			
		return blobs;
	}
	
	public float[] extractFeatures(ArrayList<PVector> blobs)
	{
		float[] features = new float[1606];
		int index = 0;
		
		float TEMPLATESIZE = 40;
		
		PGraphics g = createGraphics((int)TEMPLATESIZE,(int)TEMPLATESIZE, P2D);
		
		
		float minx = 9999, maxx=0, miny=9999,maxy=0;
		
		for (int i=0;i<blobs.size();i++)
		{
			PVector v = blobs.get(i);
			
			if (v.x-v.z/2f<minx)
				minx=v.x-v.z/2f;
			if (v.x+v.z/2f>maxx)
				maxx=v.x+v.z/2f;
			if (v.y-v.z/2f<miny)
				miny=v.y-v.z/2f;
			if (v.y+v.z/2f>maxy)
				maxy=v.y+v.z/2f;
		}
		
		float bw = maxx - minx;
		float bh = maxy - miny;
		
		PGraphics temp = createGraphics((int)bw,(int)bh,P2D);
		
		temp.beginDraw();
		temp.background(0);
		temp.fill(255);
		temp.noStroke();
		
		float sizeSum = 0;
		float averageSize = 0;
		for (int i=0;i<blobs.size();i++)
		{
			PVector v = blobs.get(i);
			float rx = v.x - minx;
			float ry = v.y - miny;
			
			temp.ellipse(rx,ry,v.z,v.z);
			
			sizeSum+=v.z;
		}
		averageSize = sizeSum / blobs.size();
		
		temp.endDraw();
		
		PImage img = new PImage((int)TEMPLATESIZE,(int)TEMPLATESIZE);
		
		img.copy(temp,0,0,(int)bw,(int)bh,0,0,(int)TEMPLATESIZE,(int)TEMPLATESIZE);
		
		image(img,100,100,300,300);
		
		int filledPixels = 0;
		for (int i=0;i<TEMPLATESIZE*TEMPLATESIZE;i++)
		{
			features[index] = g.pixels[i] & 0x000001;
			
			if (features[index] == 1f)
				filledPixels++;
			
			index++;
		}
		
		features[index++] = blobs.size();
		features[index++] = filledPixels;
		features[index++] = bh;
		features[index++] = bw;
		features[index++] = sizeSum;
		features[index++] = averageSize;
		
		float distance = 0;
		float count = 0;
		for (int i=0;i<blobs.size();i++)
			for (int k=i+1;k<blobs.size();k++)
			{
				PVector a = blobs.get(i);
				PVector b = blobs.get(k);
				
				distance+= dist(a.x,a.y,b.x,b.y);
				count++;
			}
		
		fill(0);
		//textSize(20);
		//text(""+minx + ", " + maxx, 500,50);
		//features[index++] = distance/count;
		
		return features;	
	}

}	