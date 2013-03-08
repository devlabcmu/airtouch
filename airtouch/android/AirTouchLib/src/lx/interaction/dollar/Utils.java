package lx.interaction.dollar;

//TODO: Most of the code here could be significantly optimized. This was a quick port from the C# version of the library

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Utils
{	 
	public static Vector<Point>Resample(Vector<Point> points, int n)
	{		
		double I = PathLength(points) / (n - 1); // interval length
		double D = 0.0;
		
		Vector<Point> srcPts = new Vector<Point>(points.size());
		for (int i = 0; i < points.size(); i++)
			srcPts.addElement(points.elementAt(i));
		
		Vector<Point> dstPts = new Vector<Point>(n);
		dstPts.addElement(srcPts.elementAt(0));	//assumes that srcPts.size() > 0
		
		for (int i = 1; i < srcPts.size(); i++)
		{
			Point pt1 = (Point) srcPts.elementAt(i - 1);
			Point pt2 = (Point) srcPts.elementAt(i);

			double d = Distance(pt1, pt2);
			if ((D + d) >= I)
			{
				double qx = pt1.X + ((I - D) / d) * (pt2.X - pt1.X);
				double qy = pt1.Y + ((I - D) / d) * (pt2.Y - pt1.Y);
				Point q = new Point(qx, qy);
				dstPts.addElement(q); // append new point 'q'
				srcPts.insertElementAt(q, i); // insert 'q' at position i in points s.t. 'q' will be the next i
				D = 0.0;
			}
			else
			{
				D += d;
			}
		}
		// somtimes we fall a rounding-error short of adding the last point, so add it if so
		if (dstPts.size() == n - 1)
		{
			dstPts.addElement(srcPts.elementAt(srcPts.size() - 1));
		}

		return dstPts;
	}

	
	public static Vector<Point>RotateToZero(Vector<Point>points)
	{	return RotateToZero(points, null, null);	}

	
	public static Vector<Point>RotateToZero(Vector<Point>points, Point centroid, Rectangle boundingBox)
	{
		Point c = Centroid(points);
		Point first = (Point)points.elementAt(0);
		//double theta = Trigonometric.atan2(c.Y - first.Y, c.X - first.X);
		double theta = Math.atan2(c.Y - first.Y, c.X - first.X);
		
		if (centroid != null)
			centroid.copy(c);
		
		if (boundingBox != null)
			BoundingBox(points, boundingBox);
		
		return RotateBy(points, -theta);
	}		
	
	public static Vector<Point>RotateBy(Vector<Point>points, double theta)
	{
		return RotateByRadians(points, theta);
	}
	
	// rotate the points by the given radians about their centroid
	public static Vector<Point>RotateByRadians(Vector<Point>points, double radians)
	{
		Vector<Point>newPoints = new Vector<Point>(points.size());
		Point c = Centroid(points);

		double _cos = Math.cos(radians);
		double _sin = Math.sin(radians);

		double cx = c.X;
		double cy = c.Y;

		for (int i = 0; i < points.size(); i++)
		{
			Point p = (Point) points.elementAt(i);

			double dx = p.X - cx;
			double dy = p.Y - cy;

			newPoints.addElement(
				new Point(	dx * _cos - dy * _sin + cx, 
							dx * _sin + dy * _cos + cy )
							);
		}
		return newPoints;
	}

	public static Vector<Point>ScaleToSquare(Vector<Point>points, double size)
	{
		return ScaleToSquare(points, size, null);
	}				

	public static Vector<Point>ScaleToSquare(Vector<Point>points, double size, Rectangle boundingBox)
	{
		Rectangle B = BoundingBox(points);
		Vector<Point>newpoints = new Vector<Point>(points.size());
		for (int i = 0; i < points.size(); i++)
		{
			Point p = (Point)points.elementAt(i);
			double qx = p.X * (size / B.Width);
			double qy = p.Y * (size / B.Height);
			newpoints.addElement(new Point(qx, qy));
		}
		
		if (boundingBox != null) //this will probably not be used as we are more interested in the pre-rotated bounding box -> see RotateToZero
			boundingBox.copy(B);
		
		return newpoints;
	}			
	
	public static Vector<Point>TranslateToOrigin(Vector<Point>points)
	{
		Point c = Centroid(points);
		Vector<Point>newpoints = new Vector<Point>(points.size());
		for (int i = 0; i < points.size(); i++)
		{
			Point p = (Point)points.elementAt(i);
			double qx = p.X - c.X;
			double qy = p.Y - c.Y;
			newpoints.addElement(new Point(qx, qy));
		}
		return newpoints;
	}			
	
	public static double DistanceAtBestAngle(Vector<Point>points, Template T, double a, double b, double threshold)
	{
		double Phi = DollarRecognizer.Phi;
	
		double x1 = Phi * a + (1.0 - Phi) * b;
		double f1 = DistanceAtAngle(points, T, x1);
		double x2 = (1.0 - Phi) * a + Phi * b;
		double f2 = DistanceAtAngle(points, T, x2);
		
		while (Math.abs(b - a) > threshold)
		{
			if (f1 < f2)
			{
				b = x2;
				x2 = x1;
				f2 = f1;
				x1 = Phi * a + (1.0 - Phi) * b;
				f1 = DistanceAtAngle(points, T, x1);
			}
			else
			{
				a = x1;
				x1 = x2;
				f1 = f2;
				x2 = (1.0 - Phi) * a + Phi * b;
				f2 = DistanceAtAngle(points, T, x2);
			}
		}
		return Math.min(f1, f2);
	}			

	public static double DistanceAtAngle(Vector<Point>points, Template T, double theta)
	{
		Vector<Point>newpoints = RotateBy(points, theta);
		return PathDistance(newpoints, T.Points);
	}		

//	#region Lengths and Rects	
	
	public static Rectangle BoundingBox(Vector<Point>points)
	{
		double minX = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;
	
		Enumeration<Point>e = points.elements();
		
//		foreach (Point p in points)
		while (e.hasMoreElements())
		{
			Point p = (Point)e.nextElement();
		
			if (p.X < minX)
				minX = p.X;
			if (p.X > maxX)
				maxX = p.X;
		
			if (p.Y < minY)
				minY = p.Y;
			if (p.Y > maxY)
				maxY = p.Y;
		}
	
		return new Rectangle(minX, minY, maxX - minX, maxY - minY);
	}

	public static void BoundingBox(Vector<Point>points, Rectangle dst)
	{
		double minX = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;
	
		Enumeration<Point>e = points.elements();
		
//		foreach (Point p in points)
		while (e.hasMoreElements())
		{
			Point p = (Point)e.nextElement();
		
			if (p.X < minX)
				minX = p.X;
			if (p.X > maxX)
				maxX = p.X;
		
			if (p.Y < minY)
				minY = p.Y;
			if (p.Y > maxY)
				maxY = p.Y;
		}
	
		dst.X = minX;
		dst.Y = minY;
		dst.Width = maxX - minX;
		dst.Height = maxY - minY;
	}	
	
	public static double Distance(Point p1, Point p2)
	{
		double dx = p2.X - p1.X;
		double dy = p2.Y - p1.Y;
		return Math.sqrt(dx * dx + dy * dy);
	}

	// compute the centroid of the points given
	public static Point Centroid(Vector<Point>points)
	{
		double xsum = 0.0;
		double ysum = 0.0;
		
		Enumeration<Point> e = points.elements();
		
//		foreach (Point p in points)
		while (e.hasMoreElements())
		{
			Point p = (Point)e.nextElement();
			xsum += p.X;
			ysum += p.Y;
		}
		return new Point(xsum / points.size(), ysum / points.size());
	}

	public static double PathLength(Vector<Point>points)
	{
		double length = 0;
		for (int i = 1; i < points.size(); i++)
		{
			//length += Distance((Point) points[i - 1], (Point) points[i]);
			length += Distance((Point) points.elementAt(i - 1), (Point) points.elementAt(i));
		}
		return length;
	}

	// computes the 'distance' between two point paths by summing their corresponding point distances.
	// assumes that each path has been resampled to the same number of points at the same distance apart.
	public static double PathDistance(Vector<Point>path1, Vector<Point>path2)
	{            
		double distance = 0;
		for (int i = 0; i < path1.size(); i++)
		{
			distance += Distance((Point) path1.elementAt(i), (Point) path2.elementAt(i));
		}
		return distance / path1.size();
	}

	public static DOMSource gestureToDOMSource(Vector<Point> points, String name)
	{
		Document doc = null;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Element root = doc.createElement("Gesture");
		root.setAttribute("Name", name);
		root.setAttribute("NumPts", "" + points.size());
		for (Point point : points) {
			Element pt = doc.createElement("Point");
			pt.setAttribute("X", "" + point.X);
			pt.setAttribute("Y", "" + point.Y);
			root.appendChild(pt);
		}
		return new DOMSource(doc);
	}
	
	public static void writeGestureToStream(Vector<Point> points, String name, StreamResult s)
	{
		try {
			TransformerFactory.newInstance().newTransformer().transform(gestureToDOMSource(points, name), s);
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		}
	}
	
	public static void writeGestureToSysout(Vector<Point> points, String name, StreamResult s)
	{
		writeGestureToStream(points, name, new StreamResult(System.out));
	}
	
	public static void writeGestureToFile(Vector<Point> points, String name, File f)
	{
		writeGestureToStream(points, name, new StreamResult(f));
	}

	public static Template loadTemplateFromXml(InputStream f)
	{
		Document doc;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
			doc.getDocumentElement().normalize();
			Vector<Point> resultPoints = new Vector<Point>();
			NodeList points = doc.getElementsByTagName("Point");
			for (int i = 0; i < points.getLength(); i++) {
				Element e = (Element) points.item(i);
				resultPoints.add(new Point(Integer.parseInt(e.getAttribute("X")), Integer.parseInt(e.getAttribute("Y")) ));
			}
			
			return new Template(doc.getDocumentElement().getAttribute("Name"), new Vector<Point>());
			
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
}
