package com.hackathon.chasingcars;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.hackathon.chasingcars.MultiplayerService.SERVICE;

public class MultiplayerHome extends Activity {
    private static final String TAG = "chasingcars:MultiplayerHome";
    final static boolean D = true;
    private Context homeContext;

    private BluetoothAdapter mBluetoothAdapter;
    private MessageProtocol messager;

    boolean isServer;

    TextView txtStatus;
    ListView lstPlayers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the window layout
        setContentView(R.layout.multi_home);

        // load views
        txtStatus = (TextView)findViewById(R.id.txtStatus);
        lstPlayers = (ListView)findViewById(R.id.lstPlayers);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "No bluetooth adapter found!");
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (mBluetoothAdapter.isEnabled()) {
            setupCommunication();
        } else {
            finish();
            // can't do anything without bluetooth
            Toast.makeText(this, "We require bluetooth to start!", Toast.LENGTH_SHORT).show();
        }

        // load up the list
        List<String> players = new ArrayList<String>();
        lstPlayers.setAdapter(new ArrayAdapter<String>(this, R.layout.list_player_item, players));
    }

    int currentPlayerCount = 0;
    HashMap<String, Integer> players = new HashMap<String, Integer>();

    private MessageProtocol.MessageHandler handler = new MessageProtocol.MessageHandler() {
        @Override
        public void handleWhoAmI(String device) {
            // as a server, we add this device to our list, and we give it id
            if (D) Log.e(TAG, "handleWhoAmI: " + device);

            if (isServer) {
                players.put(device, currentPlayerCount);

                // manually add the player to our list
                handler.handleYouAre(device, currentPlayerCount);

                currentPlayerCount++;

                // messager can be null when we first do the fake whoAmI for the server
                if (messager != null) {
                    // and we send a list of all players
                    for(String deviceAddr : players.keySet()) {
                        messager.youAre(deviceAddr, players.get(deviceAddr));
                    }
                }
            }
        }

        @Override
        public void handleYouAre(String device, int player) {
            // if we don't have this player in our list, it's new
            if (D) Log.e(TAG, "Got a youAre: " + device + ";" + player);
            boolean shouldAdd = true;

            // server already knows
            if (!isServer) {
                shouldAdd = !players.containsKey(device);
                players.put(device, player);
            }

            if (shouldAdd) {
                // add to the GUI list
                ArrayAdapter<String> adapter = (ArrayAdapter<String>)lstPlayers.getAdapter();
                adapter.add("Player: " + player + " maps to " + device);
            }
        }

        @Override
        public void handlePlayerPos(int player, int x, int y) {
        }

        @Override
        public void handlePlayerCoins(int player, int coins) {
        }

        @Override
        public void handleGameStart(String time) {
        }

        @Override
        public void handleGameEnd(String time) {
        }

        @Override
        public void handleMessage(byte messageType, Object message) {
        }
    };

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothService.MESSAGE_STATECHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                    txtStatus.setText("Connected");

                    // we can enable the chat for the group
                    messager = new MessageProtocol(SERVICE.BTS);

                    if (loading != null) {
                        loading.cancel();
                    }

                    // if we're a server, we have to fake a whoAmI sent to ourselves
                    // but we don't do anything about it yet
                    if (!isServer) {
                        // we ask for who we are
                        Log.e(TAG, "I AM ATRIX! " + messager.getDevice());
                        messager.whoAmI();
                    }
                    break;
                case BluetoothService.STATE_CONNECTING:
                    txtStatus.setText("Connecting");
                    showLoading();
                    break;
                case BluetoothService.STATE_LISTEN:
                    handler.handleWhoAmI(SERVICE.BTS.getDeviceAddress());
                    txtStatus.setText("Listening");
                    break;
                case BluetoothService.STATE_DISCONNECTED:
                    txtStatus.setText("Disconnected");
                    Toast.makeText(getApplicationContext(), "Disconnected!", Toast.LENGTH_SHORT);
                    if (loading != null) {
                        loading.cancel();
                    }
                    finish();
                    break;
                case BluetoothService.STATE_NONE:
                    txtStatus.setText("Not Connected");
                    break;
                }
                break;
            case BluetoothService.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                messager.parseBytes(readBuf, handler);
                break;
            }
        }
    };

    ProgressDialog loading = null;

    private void showLoading() {
        loading = new ProgressDialog(this);
        loading.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loading.setTitle("Connecting to bluetooth");
        loading.setMessage("Connecting to device <blah>");
        loading.show();
    }

    private void setupCommunication() {
        Log.d(TAG, "setupCommunication()");

        // create our static multiplayer service
        MultiplayerService.createService();

        // Initialize the BluetoothService to perform bluetooth connections
        SERVICE.initBTS(this);
        SERVICE.setNewHandler(mHandler);
        
        // start up the bluetooth chat service
        Common.BluetoothMode mode = Enum.valueOf(Common.BluetoothMode.class,
                getIntent().getExtras().getString("mode"));
        if (mode == Common.BluetoothMode.Server) {
            if(D) Log.w(TAG, "setupCommunication starting server");
            SERVICE.BTS.setupServer();
            isServer = true;

            // TODO: show a please wait for clients
        } else {
            // get the device
            String deviceAddr = getIntent().getExtras().getString("device");
            if(D) Log.w(TAG, "setupCommunication starting client for addr: " + deviceAddr);
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddr);
            SERVICE.BTS.setupClient(device);
        }
    }

}
