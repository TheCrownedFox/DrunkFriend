// App Name: Drunk Friend
// Author: Austin Diviness
// Last Updated: 2/17/2013

package com.austindiviness.drunkfriend;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class AllContactsActivity extends Activity {
	public ArrayList<ContactData> data;
	public LocationListener locationListener;
	public LocationManager locationManager;
	public String numberToText;
	public String contactName;
	public String message1;
	public ProgressDialog loading;
    public int mainMenuId = 1;
    public int settingsId = Menu.FIRST;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact_list);
		Bundle extraData = getIntent().getExtras();
		data = (ArrayList<ContactData>) extraData.get("contactsData");
		ArrayList<String> names = new ArrayList<String>();
		for (ContactData item: data) {
			names.add(item.getName());
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.textview, names);
		ListView listView = (ListView) findViewById(R.id.listview1);
		listView.setAdapter(adapter);
		// set click listener
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				numberToText = "Invalid";
				TextView textView = (TextView) view;
				contactName = textView.getText().toString(); // insert alert dialog here to make sure the user wants to text the user they picked
				AlertDialog.Builder checkCorrectContact = new AlertDialog.Builder(AllContactsActivity.this);
                checkCorrectContact.setTitle("Confirm Contact");
                checkCorrectContact.setMessage("Do you want to send pick up message to " + contactName + "?");
                checkCorrectContact.setNegativeButton("No", new DialogInterface.OnClickListener() {
                	@Override
                    public void onClick(DialogInterface dialog, int which) {
                		// no, close the dialog
                        dialog.dismiss();
                    }
                });
                checkCorrectContact.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                	@Override
                    public void onClick(DialogInterface dialog, int which) {
                        // yes, send the message
                		// get number of selected contact
        				for(ContactData contact: data) {
        					if (contact.getName().equalsIgnoreCase(contactName)) {
        						numberToText = contact.getNumber();
        						break;
        					}
        				}
                        loading = new ProgressDialog(getBaseContext()).show(AllContactsActivity.this, "Gathering GPS Data", "This may take a little while...", true);
        				sendMessage(numberToText);
                    }
                });
                checkCorrectContact.show();
			}
		});
	}

	public void sendMessage(String contactNumber) {
		// get location
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationListener = new MyLocationListener();
		String locationProvider = LocationManager.GPS_PROVIDER;
		locationManager.requestLocationUpdates(locationProvider, 2000, 50, locationListener);
		//Toast.makeText(getBaseContext(), "Gathering GPS information, this will take a little while...", Toast.LENGTH_LONG).show();
		// pause for 15 seconds, before running rest of code inside handler
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
				Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    //Toast.makeText(getBaseContext(), "GPS either turned off or cannot contact satellites", Toast.LENGTH_LONG).show();
                    AlertDialog.Builder badSignal = new AlertDialog.Builder(AllContactsActivity.this);
                    loading.dismiss();
                    badSignal.setTitle("No Satellite Coverage");
                    badSignal.setMessage("Insufficient satellite information is available. Perhaps try again outside?");
                    badSignal.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    	@Override
                    	public void onClick(DialogInterface dialog, int which) {
                    		// bad GPS signal, turn off location manager and exit
                    		locationManager.removeUpdates(locationListener);
                    		locationManager = null;
                    		finish();
                    	}
                    });
                    badSignal.show();
                }
                else {
					String lat = String.valueOf(location.getLatitude());
					String lon = String.valueOf(location.getLongitude());
					// correct order to display values is apparently lat, lon, even though it seems pretty ambiguous
					String message2 = "https://maps.google.com/maps?z=12&t=m&q=" + lat + "," + lon; 
					SmsManager sms = SmsManager.getDefault();
					// set message1 to the string stored in shared preferences, somewhere
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AllContactsActivity.this);
					message1 = prefs.getString("message_to_text", "ERROR reading custom message. GPS location as follows:");
					// send messages
					if (message1.length() > 160) {
						int numberOfMessages = message1.length() / 160;
						if ((160 * numberOfMessages) < message1.length()) {
							++numberOfMessages;
						}
						for (int i = 0; i < numberOfMessages - 1; ++i) {
							sms.sendTextMessage(numberToText, null, message1.substring(i * 160, (i + 1) * 160), null, null);
						}
						sms.sendTextMessage(numberToText, null, message1.substring((numberOfMessages - 1) * 160, message1.length()), null, null);
					}
					else {
						sms.sendTextMessage(numberToText, null, message1, null, null);
					}
					sms.sendTextMessage(numberToText, null, message2, null, null);
					AlertDialog.Builder messageSent = new AlertDialog.Builder(AllContactsActivity.this);
					messageSent.setTitle("Success!");
					messageSent.setMessage("Successfully sent SMS message to " + contactName + " regarding your situation and present location.");
					messageSent.setNeutralButton("OK", new DialogInterface.OnClickListener() {						
						@Override
						public void onClick(DialogInterface dialog, int which) {
			                // message sent, turn off location manager and exit
			                locationManager.removeUpdates(locationListener);
			                locationManager = null;
							finish();	
						}
					});
					loading.dismiss();
					messageSent.show();
                }
			}
		}, 15000);
		// end of handler code
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(mainMenuId, settingsId, 0, "Settings");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case Menu.FIRST:
                //Toast.makeText(MainActivity.this, "settings button clicked", Toast.LENGTH_SHORT).show();
                Intent settingsIntent = new Intent(AllContactsActivity.this, SettingsActivity.class);
				settingsIntent.putExtra("contactsData", data);
                AllContactsActivity.this.startActivity(settingsIntent);
        }
        return true;
    }

}


