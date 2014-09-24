package ca.uwaterloo.Lab4_203_05;

import java.util.ArrayList;
import java.util.List;

import mapper.MapView;
import mapper.NavigationalMap;
import mapper.PositionListener;
import mapper.VectorUtils;
import android.graphics.Color;
import android.graphics.PointF;
import android.widget.TextView;

public class Listener implements PositionListener {

	// origin and destination point 
	// Global Variables
	TextView tv;
	PointF origin = new PointF(0,0);
	PointF destination = new PointF(0,0);
	PointF update = new PointF(0,0);
	PointF sweepLeft;
	PointF sweepRight;
	PointF sweeper;
	PointF stageLeft;
	PointF stageRight;
	PointF parallelDest ;
	MapView mapView;
	NavigationalMap map = new NavigationalMap();
	boolean wall = true;
	boolean reset = false;
	boolean pathFound = false;
	boolean parallel = false;
	boolean longWayRight = false;
	boolean longWayLeft = false;
	int stage = 0;

	List<PointF> seg = new ArrayList<PointF>(); 

	// contructor
	public Listener (NavigationalMap map, MapView mapView, TextView tv) 
	{
		this.map = map;
		this.mapView = mapView;
		this.tv = tv;
	}

	@Override
	public void originChanged(MapView source, PointF loc) {

		origin = loc;
		source.setUserPoint(loc);
		List<PointF> pp = new ArrayList<PointF>();
		pp.add(loc);
		pp.add(destination);
		//source.setUserPath(pp); // call origin first so don't require
	}

	@Override
	public void destinationChanged(MapView source, PointF dest) {

		destination = dest;
		List<PointF> pp = new ArrayList<PointF>();
		// source.setUserPoint(dest);
		if (!reset) // detour away from assignment to avoid path finder through wall after reset
		{
			pp.add(dest);
			pp.add(origin);
		}
		pathFinder(source, pp);
	}

	// updates the red pointer
	public void updateLocation(MapView source, PointF disp) {

		update = disp;
		collisionDetection(source, disp);
		roadBlock();
		giveDirections();
	}

	// detects for border conditions
	public void collisionDetection(MapView source, PointF coord) {

		if (map.calculateIntersections(source.getUserPoint(),coord).size() == 0) {
			source.setUserPoint(coord);
			List<PointF> pp = new ArrayList<PointF>();
			pp.add(coord);
			pp.add(destination);
			//source.setUserPath(pp);
		}

	}

	// auto generates efficient path
	public void pathFinder(MapView source, List<PointF> pp)
	{
		List<PointF> openRight = new ArrayList<PointF>();
		List<PointF> openLeft = new ArrayList<PointF>();

		if (!initialBlock()) // check starting case
		{
			source.setUserPath(pp); // just connect the dots. Easy
		}
		else if (roadBlock()) 
		{
			for (float i = 0.0f; i < 50; i += 0.2f)  // scans map horizontally
			{

				PointF checkRight = new PointF (origin.x + 3.7f,(origin.y)+i);
				PointF checkRightDown = new PointF (origin.x + 3.7f,(origin.y)-i);
				PointF checkLeft = new PointF (origin.x - 3.7f,(origin.y)+i);
				PointF checkLeftDown = new PointF (origin.x - 3.7f,(origin.y)-i);
				PointF sweeper = new PointF(origin.x,(origin.y)+i); // small steps to test for "mines" (border/edge)
				PointF sweeperDown = new PointF(origin.x,(origin.y)-i); // small steps to test for "mines" (border/edge)
				parallelDest = new PointF (destination.x, origin.y);
				if (map.calculateIntersections(origin, parallelDest).size() == 0) { // special case if not obstructions parallel to origin.

					seg.add(origin);
					seg.add(parallelDest);
					seg.add(destination);
					source.setUserPath(seg);
					seg.clear(); // empties list
					pathFound = true;
					parallel = true;
					break;
				}

				else 
				{
					if (map.calculateIntersections(origin, sweeper).size() > 0) { 
						break;
					}

					if (map.calculateIntersections(origin, sweeperDown).size() <= 0){ // fixes boundary overflow bug

						if (map.calculateIntersections(sweeperDown, checkRightDown).size() == 0)
						{
							openRight.add(sweeperDown);

						}
						if (map.calculateIntersections(sweeperDown, checkLeftDown).size() == 0)
						{
							openLeft.add(sweeperDown);
						}

					}
					else{
						// do nothing
					}

					if (map.calculateIntersections(sweeper, checkRight).size() == 0) 
					{
						openRight.add(sweeper);

					}

					if (map.calculateIntersections(sweeper, checkLeft).size() == 0)
					{
						openLeft.add(sweeper);
					}
				}	
			}
			// when no direct linear path can be established
			if (!pathFound)
			{
				for (float i = 0.0f; i < 100; i += 0.2f){

					if (destination.x <= origin.x){
						longWayLeft = true;
						// int median = (int)(openLeft.size()/2); // NOTE: assumes this specific map. Not dynamic
						PointF stage1 = openLeft.get(0);
						stageLeft = stage1;
						sweepLeft = new PointF (stage1.x - i,stage1.y);
						if (map.calculateIntersections(sweepLeft, destination).size() == 0) 
						{
							seg.add(origin);
							seg.add(stage1);
							seg.add(sweepLeft);
							break;
						}
					}	

					else{
						longWayRight = true; // destination is to the right of starting 
						// int median = (int)(openRight.size()/2); // NOTE: assumes this specific map. Not dynamic
						PointF stage1 = openRight.get(0);
						stageRight = stage1;
						sweepRight = new PointF (stage1.x + i,stage1.y);
						if (map.calculateIntersections(sweepRight, destination).size() == 0) // checks for walls
						{
							seg.add(origin);
							seg.add(stage1);
							seg.add(sweepRight);
							break;
						}
					}	
				}
				seg.add(destination);
				source.setUserPath(seg);
				seg.clear(); // empties list
			}
		}
		giveDirections(); // navigate
	}
	// gps navigation instructions
	public void giveDirections()
	{
		String output = "";
		float radians = 0;
		if(!initialBlock()) { // not blocked
			radians = VectorUtils.angleBetween(origin, update, destination);
		}
		else if (parallel) // special path case
		{	
			if (origin.x < destination.x) {

				if (update.x < destination.x) // stage assignment
				{
					radians = VectorUtils.angleBetween(origin, update, new PointF (destination.x, origin.y)); // calls new PointF to fix output error
				}
				else 
					stage = 1;
			}
			else if (origin.x >= destination.x) {

				if (update.x >= destination.x) // stage assignment
				{
					radians = VectorUtils.angleBetween(origin, update, new PointF (destination.x, origin.y));
				}
				else 
					stage = 2;
			}
			if (stage == 1) {
				radians = VectorUtils.angleBetween(new PointF (destination.x, origin.y), update, destination);
			}
			else if (stage == 2) {
				radians = VectorUtils.angleBetween(new PointF (destination.x, origin.y), destination, update);
			}
		}
		else if (longWayRight) // third case with destination to the right
		{	
				if (update.x > origin.x && update.x <= sweepRight.x) // stage assignment
				{
					stage = 1;
				}
				else if (update.x > sweepRight.x) {
					stage = 2;
				}
				if (stage == 0){
					radians = VectorUtils.angleBetween(origin, update, stageRight);
				}
				if (stage == 1){
					radians = VectorUtils.angleBetween(stageRight, update, sweepRight);
				}
				if (stage == 2){
					radians = VectorUtils.angleBetween(sweepRight, update, destination);
				}
		}
			else if (longWayLeft) // fourth case with destination to the left
			{
				if (update.x < origin.x && update.x >= sweepLeft.x) // stage assignment MAYBE CHANGE TO Y
				{
					stage = 1;
				}
				else if (update.x < sweepLeft.x) {
					stage = 2;
				}
				
				if (stage == 0){
					radians = VectorUtils.angleBetween(origin, update, stageLeft);
				}
				if (stage == 1){
					radians = VectorUtils.angleBetween(stageLeft, update, sweepLeft);
				}
				if (stage == 2){
					radians = VectorUtils.angleBetween(sweepLeft, update, destination);
				}
			}

			if ((VectorUtils.distance(update,destination) < 1f)) { // range of acceptable arrival 

				output = "ARRIVED AT DESTINATION!";
				tv.setTextColor(Color.GREEN); 
				tv.setText(output);
			}
			else
			{
				tv.setTextColor(Color.BLACK); 
				if (Math.toDegrees(radians) <= -2) {
					tv.setText(String.format("Directions: Turn %f Degrees LEFT!", Math.abs(Math.toDegrees(radians))));
				}
				else if (Math.toDegrees(radians) < 2 && Math.toDegrees(radians) > -2){
					tv.setText(String.format("Directions: Go Straight Ahead!"));
				}
				else if (-173f >= Math.toDegrees(radians) && Math.toDegrees(radians) >= -180f || 173f <= Math.toDegrees(radians) && Math.toDegrees(radians) <= 180f) {
					tv.setText(String.format("Directions: TURN AROUND!"));
				}
				else {
					tv.setText(String.format("Directions: Turn %f Degrees RIGHT!", Math.abs(Math.toDegrees(radians))));
				}
			}

		}

		public boolean initialBlock() // check for interception
		{	
			if (map.calculateIntersections(origin,destination).size() > 0) // check for intersections
			{
				System.out.println(map.calculateIntersections(origin,destination).size());
				wall = true;
			}
			else {
				wall = false;
			}
			return wall;
		}

		public boolean roadBlock() // check for walls after initial block evaluates to false
		{	
			if (map.calculateIntersections(update,destination).size() >= 0) // check for intersections
			{
				//System.out.println(map.calculateIntersections(update,destination).size());
				wall = true;
			}

			else 
			{
				wall = false;
			}
			return wall;
		}

		public void resetPointer() // resets the map and variables
		{
			origin.set(0,0);
			destination.set(0,0);
			update.set(0,0);
			wall = true;
			reset = false;
			pathFound = false;
			parallel = false;
			longWayRight = false;
			longWayLeft = false;
			reset = true; // fixes reset path finding through wall bug
			originChanged(mapView, origin);
			destinationChanged(mapView, destination);
			updateLocation(mapView, update);
			reset = false; // change back after once the detour is complete
			
		}
	} 