package ca.uwaterloo.Lab4_203_05;


import java.util.Arrays;

import mapper.MapLoader;
import mapper.MapView;
import mapper.NavigationalMap;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import ca.uwaterloo.sensortoy.LineGraphView;

public class MainActivity extends Activity { 

	LineGraphView graph; // declare the graph
	MapView mapView;

	// global variables
	int counter = 0;  // keeps track of the step counter
	float xCounter = 0.0f;
	float yCounter = 0.0f;
	double xDisp = 0.0f;
	double yDisp = 0.0f;
	int currentState = 0; // keep track of the current stage for the finite state machine  
	float filteredOrientation = 0.0f;  // filtered angle
	Listener l;
	
	PointF pp = new PointF(0,0); // mapview

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        
		super.onCreate(savedInstanceState); 
		setContentView(R.layout.activity_main); // set button to main activity
		final LinearLayout linearlayout = (LinearLayout) findViewById(R.id.linearlayout); 
		mapView = new MapView(getApplicationContext(), 1920, 500, 35, 35);
		Button clearButton = new Button(getApplicationContext()); // create new button called clearButton
		linearlayout.addView(clearButton); // adds a button view
		clearButton.setText("Reset Step Counter"); // button message
		registerForContextMenu(mapView); // creates the menu for long press
		NavigationalMap map = MapLoader.loadMap(getExternalFilesDir(null), "Lab-room.svg");
		//NavigationalMap map = MapLoader.loadMap(getExternalFilesDir(null), "Lab-room-peninsula.svg");
		mapView.setMap(map);
		linearlayout.addView(mapView);
		TextView tv7 = new TextView(getApplicationContext()); // textview for Listener
		l = new Listener(map, mapView, tv7); // innitalize it here because need NagivationalMap for parameters
		mapView.addListener(l); // Call addListener() on your MapView with a PositionListener.

		
		l.originChanged(mapView, pp);

		//float test = pp.x; // test
		
		// create new OnClickListener and declare it's function
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				graph.purge(); // resets graph
				counter = 0; // resets step counters
				xCounter = 0.0f;
				yCounter = 0.0f;
				l.resetPointer();
			}
		});


		graph = new LineGraphView(getApplicationContext(),
				100,
				Arrays.asList("x", "y", "z"));
		linearlayout.addView(graph); graph.setVisibility(View.VISIBLE); // adds graph to world

		linearlayout.setOrientation(LinearLayout.VERTICAL); // set orientation

		TextView tv1 = new TextView(getApplicationContext());
		TextView tv2 = new TextView(getApplicationContext());
		TextView tv3 = new TextView(getApplicationContext());
		TextView tv4 = new TextView(getApplicationContext());
		TextView tv5 = new TextView(getApplicationContext());

		// create new position listener object
	
		mapView.addListener(l);
		
		// displays the respective outputs to the screen in order as shown
		linearlayout.addView(tv7);
		linearlayout.addView(tv1);
		linearlayout.addView(tv2);
		linearlayout.addView(tv3);
		linearlayout.addView(tv5);
		linearlayout.addView(tv4);

		// initializing the sensors
		SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		// Accelerometer
		Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // used for mGravity
		Sensor accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION); // finite state machine uses this sensor

		// Magnetic Field
		Sensor magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD); // used for mGeomagnetic

		SensorEventListener combined = new CombinedSensorEventListener(tv1, tv2); // combined listener used for both accelerometer and magnetic field
		SensorEventListener a = new AccelerometerSensorEventListener(tv3,tv4,tv5); // listens for linear acceleration (3 textviews for counter and displacement)

		// note: used game delay because it proved smoother through experimentation
		sensorManager.registerListener(combined, accelerometerSensor,SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(combined, magneticFieldSensor,SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(a, accelerationSensor,SensorManager.SENSOR_DELAY_GAME);

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
	super.onCreateContextMenu(menu, v, menuInfo);
	mapView.onCreateContextMenu(menu, v, menuInfo); }
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	return super.onContextItemSelected(item) || mapView.onContextItemSelected(item); }

	
	// accelerometor class 
	class CombinedSensorEventListener implements SensorEventListener {
		// delcare variables
		TextView output1;
		TextView output2;
		float azimuth = 0.0f;
		float R[] = new float[9];
		float I[] = new float[9];
		float[] mGravity = null;
		float[] mGeomagnetic = null;

		// constructor 
		CombinedSensorEventListener(TextView outputView1, TextView outputView2){
			//innitalizes variables 
			output1 = outputView1;
			output2 = outputView2;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
		}
		@Override
		public void onSensorChanged(SensorEvent se) {

			if (se.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			{
				mGravity = se.values;
			}
			if (se.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) 
			{
				mGeomagnetic = se.values;
			}
			if (mGravity != null && mGeomagnetic != null) { 

				boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic); // getRotationMatrix returns boolean 

				if (success) {  
					float orientation[] = new float[3]; // size 3 array to hold azimuth, roll and pitch
					SensorManager.getOrientation(R, orientation); // orientation in respect to magnetic North
					float azimuth = (float)Math.toDegrees((double)orientation[0]); // interested only in azimuth value 
					filteredOrientation = calculateFilteredAngle(azimuth - 0.21f,filteredOrientation); // filter noise // NOTE: accounted for lab room angle
					output2.setTextColor(Color.BLACK); 
					output2.setText(String.format("Azimuth:" + filteredOrientation));
				}
				mGravity=null; //oblige full new refresh
				mGeomagnetic=null; //oblige full new refresh
			}
		}
	}

	// accelerometor class for linear acceleration
	class AccelerometerSensorEventListener implements SensorEventListener {
		// delcare variables
		TextView stepCounter;
		TextView xSteps;
		TextView ySteps;
		float[] filteredValues = new float[3]; // refined values after filtering from low pass filtering

		// constructor 
		AccelerometerSensorEventListener(TextView stepCounter,TextView xSteps, TextView ySteps){
			// innitalizes variables 
			this.stepCounter = stepCounter;
			this.xSteps = xSteps;
			this.ySteps = ySteps;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
		}
		@Override
		public void onSensorChanged(SensorEvent se) {
			if (se.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) 
			{
				filteredValues = lowpass(se.values); // send values to lowpass for noise filtering
				stepCounter.setTextColor(Color.BLACK); // change font color
				graph.addPoint(filteredValues); // sends values to graph
				checkStep(filteredValues); // checks for valid steps using the finite state machine
				stepCounter.setText(String.format("Steps: %d", counter)); // output step counter to user
				xSteps.setTextColor(Color.RED); 
				ySteps.setTextColor(Color.BLUE); 
				
				// display the displacement accordingly to counter 
				if (xCounter >= 0.0f)
				{
					xSteps.setText(String.format("East: %f", xCounter));
				}
				else
				{
					xSteps.setText(String.format("West: %f", Math.abs(xCounter)));
				}
				if (yCounter >= 0.0f)
				{
					ySteps.setText(String.format("North: %f", yCounter));
				}
				else
				{
					ySteps.setText(String.format("South: %f", Math.abs(yCounter)));
				}
			}       
		}
	}
	

	// low-pass filtering for noise reduction
	public float[] lowpass(float[] in)
	{ 
		float[] out = new float[in.length];
		float alpha = 0.21F; // constant obtained through trail and error
		out[0] = 0;
		for(int i = 1; i < in.length; i++) 
		{ 
			out[i] = alpha * in[i] + (1-alpha) * out[i-1]; // algorithm
		}
		return out; // return filtered value
	}


	// finite state machine for valid step checking
	public void checkStep (float[] values)
	{
		// stage 1: check rise and peak in acceleration. 1st characteristic of a step
		if(values[1] > 0.1 && currentState == 0) // number gathered through experimentation 
		{
			currentState = 1; // proceed to next stage 	
		}
		if (values[1] < -0.1 && currentState == 1)
		{
			currentState=0; // reset 
			counter++; // increment counter
			displacement(filteredOrientation); // calculate displacement when a valid step is registered
		}
	}

	public void updateLoc(float x, float y) {
		
		PointF user = mapView.getUserPoint(); // locates current position
		//double vectorDisp = 0.0d; 
		x = (float)(0.73*x + user.x); // increments onto the position for displacement
		y = (float)(0.73*y + user.y); // negative because -y means going north and +y is going south.
		PointF vectorDisp = new PointF(x,y); // mapview
		l.updateLocation(mapView, vectorDisp);
		
	}
	
	// method to calculate displacement based off from the azimuth angle
	public void displacement (float angle)
	{
		if (angle >= 0.0f)
		{
			if (angle <= 90.0f) // N and E
			{
				xDisp = Math.abs(Math.sin(Math.toRadians(angle))); // Choose East to be positive on x-axis
				yDisp = Math.abs(Math.cos(angle)); // Choose North to be positve on y-axis
			}
			else // E and S
			{
				xDisp = Math.abs(Math.sin(Math.toRadians(180.0f-angle))); // East  
				yDisp = - Math.abs(Math.cos(Math.toRadians(180.0f-angle))); // South
			}
		}
		else 
		{

			if (angle >= -90.0f) // N and W  
			{
				xDisp = - Math.abs(Math.sin(Math.abs(Math.toRadians(angle)))); // West
				yDisp = Math.abs(Math.cos(Math.abs(Math.toRadians(angle)))); // North
			}
			else // W and S
			{
				xDisp = - Math.abs(Math.cos(180.0f-Math.abs(Math.toRadians(angle)))); // West
				yDisp = - Math.abs(Math.sin(180.0f-Math.abs(Math.toRadians(angle)))); // South
			}	
		}
		xCounter += xDisp;
		yCounter += yDisp;
		updateLoc((float)xDisp, -(float)yDisp); //NOTE: negative because South is +Y and North is -Y
		xDisp = 0.0d; // reset
		yDisp = 0.0d;
	}

	// preventive measure in the case of bogus angles
	private float restrictAngle(float tmpAngle){
		while(tmpAngle>=180) tmpAngle-=360;
		while(tmpAngle<-180) tmpAngle+=360;
		return tmpAngle;
	}

	//x is a raw angle value from getOrientation(...)
	//y is the current filtered angle value
	private float calculateFilteredAngle(float x, float y){ 

		final float alpha = 0.3f;
		float diff = x-y;
		
		//here, we ensure that abs(diff)<=180
		diff = restrictAngle(diff);
		
		y += alpha*diff;
		//ensure that y stays within [-180, 180[ bounds
		y = restrictAngle(y);

		return y;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);//
		return true; 
	}
}
