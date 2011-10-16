package com.hackathon.chasingcars;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class HomePage extends Activity {
	private static final String TAG = "chasingcars:HomePage";
	final static boolean D = true;
    private Context homeContext;

    private static final int REQUEST_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    Button btnJoin;
    Button btnCreate;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
		if (D) Log.e(TAG, "++ ON CREATE ++");

        // Set up the window layout
        setContentView(R.layout.home);
        homeContext = this;

        // set up listener for the buttons
        btnJoin = (Button)findViewById(R.id.btnJoin);
        btnCreate = (Button)findViewById(R.id.btnCreate);

        btnJoin.setOnClickListener(buttonHandler);
        btnCreate.setOnClickListener(buttonHandler);
	}

    private Button.OnClickListener buttonHandler = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == btnCreate.getId()) {
                // create button clicked
                Intent intent = new Intent(homeContext, ProtocolDebugger.class);
                intent.putExtra("mode", Common.BluetoothMode.Server.toString());
                startActivity(intent);
            } else {
                // show the device list, and pick a device
                Intent i = new Intent(homeContext, DeviceListActivity.class);
                startActivityForResult(i, REQUEST_DEVICE);
            }
        }
    };

	@Override
	public void onStart() {
	    super.onStart();
	    if(D) Log.e(TAG, "++ ON START ++");

        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

	}

	@Override
	public synchronized void onResume() {
	    super.onResume();
	    if(D) Log.e(TAG, "+ ON RESUME +");

	}


	@Override
	public synchronized void onPause() {
	    super.onPause();
	    if(D) Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
	    super.onStop();
	    if(D) Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
	    super.onDestroy();
	    if(D) Log.e(TAG, "--- ON DESTROY ---");
	}


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so we're ok
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled!");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        case REQUEST_DEVICE:

            if (resultCode == Activity.RESULT_OK) {
                String device = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                Intent intent = new Intent(homeContext, ProtocolDebugger.class);
                intent.putExtra("mode", Common.BluetoothMode.Client.toString());
                intent.putExtra("device", device);
                startActivity(intent);
            } else {
                Log.d(TAG, "No one chosen!");
                Toast.makeText(this, "Please choose device to connect to!", Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        }
    }
}
