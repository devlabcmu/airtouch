package lx.interaction.dollar;

import java.util.*;

public class Template
{
	String Name;
	Vector<Point> Points;

	Template(String name, Vector<Point> points) 
	{
		this.Name = name;
		this.Points = Utils.Resample(points, DollarRecognizer.NumPoints);
		this.Points = Utils.RotateToZero(this.Points);
		this.Points = Utils.ScaleToSquare(this.Points, DollarRecognizer.SquareSize);
		this.Points = Utils.TranslateToOrigin(this.Points);		
	}
}
