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
import com.hackathon.chasingcars.entity.Player;

public class HomePage extends Activity {
	private static final String TAG = "chasingcars:HomePage";
	final static boolean D = true;
    private Context homeContext;

    private static final int REQUEST_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 3;


    // single player
    Button btnSingle;
    Button btnAI;

    // multiplayer
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
        btnAI = (Button)findViewById(R.id.btnAI);
        btnSingle = (Button)findViewById(R.id.btnSingle);

        btnJoin.setOnClickListener(buttonHandler);
        btnCreate.setOnClickListener(buttonHandler);
        btnAI.setOnClickListener(buttonHandler);
        btnSingle.setOnClickListener(buttonHandler);
	}

    private void startGame(int numPlayers, String[] players) {
        Intent intent = new Intent(this, Game.class);
        intent.putExtra("numPlayers", numPlayers);

        for(int i = 0; i < numPlayers; i++) {
            intent.putExtra("player_" + i, players[i]);
        }

        startActivity(intent);
    }


    private Button.OnClickListener buttonHandler = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == btnCreate.getId()) {
                // create button clicked
                Intent intent = new Intent(homeContext, MultiplayerHome.class);
                intent.putExtra("mode", Common.BluetoothMode.Server.toString());
                startActivity(intent);
            } else if (view.getId() == btnAI.getId()) {
                // chose to play against the AI
                startGame(2, new String[] {
                        Player.createString("bob", "", Player.COLOR.RED),
                        Player.createString("ai", "", Player.COLOR.BLUE)});
            } else if (view.getId() == btnSingle.getId()) {
                // choose to play by himself..
                startGame(1, new String[] {
                        Player.createString("bob", "", Player.COLOR.RED)});
            } else {
                // Chose to join an existing game, first we need to pick the right device
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
	    if(D) Log.e(TAG, "+ ON RESUME LOL +");

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
                Intent intent = new Intent(homeContext, MultiplayerHome.class);
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
