package de.ecspride;

import android.app.Activity;
import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.telephony.SmsManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @testcase_name LocationLeak3
 * @version 0.1
 * @author Secure Software Engineering Group (SSE), European Center for Security and Privacy by Design (EC SPRIDE) 
 * @author_mail steven.arzt@cased.de
 * 
 * @description This example contains a location information leakage in the onResume() callback method.
 *  The data source is placed into the onLocationChanged() callback method in a separate class
 *  which sets the data into a field of the activity. Activity and callback are decoupled using an
 *  interface.
 * @dataflow onLocationChanged: source -> data -> onResume -> sink 
 * @number_of_leaks 1
 * @challenges the analysis must be able to emulate the Android activity lifecycle correctly,
 *  integrate the callback method onLocationChanged, detect the callback methods as source
 *  and connect the callback class to the activity via the interface.
 */
public class LocationLeak3 extends Activity implements IDataProvider {

	private String data = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_multi_handlers1);

		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}
		
        LocationListener locationListener = new MyLocationListener(this);  
        LocationManager locationManager = (LocationManager) 
        		getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
	}

	@Override
    protected void onResume (){
    	super.onResume();
    	Log.d("Location", "Location: " + data); //sink, leak
		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage("+49", null, "Location: " + data, null, null);  //sink, leak

		try {
			connect(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	private void connect(String data) throws IOException {
		String URL = "http://www.google.de/search?q=";
		URL = URL.concat(data);
		java.net.URL url = new URL(URL);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection(); //sink, leak
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		// Starts the query
		conn.connect();

		InputStream is = conn.getInputStream();
		if (is == null)
			return;
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = null;
		try {
			while ((line = br.readLine()) != null)
				sb.append(line);
		} finally {
			br.close();
			is.close();
		}
		Log.d(getClass().getSimpleName(), sb.toString());
	}

	@Override
	public void setData(String data) {
		this.data = data;
	}
}
