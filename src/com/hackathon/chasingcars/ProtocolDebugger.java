package com.hackathon.chasingcars;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.*;

public class ProtocolDebugger extends Activity {
    private static final String TAG = "chasingcars:ProtocolDebugger";
    final static boolean D = true;
    private Context homeContext;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothChatService mChatService;
    private MessageProtocol messager;

    // layout stuff
    Button btnWhoAmI, btnPlayerPos, btnPlayerCoins, btnGameEnd, btnStart;
    Spinner spinPlayer;
    EditText txtX, txtY, txtCoins;
    TextView txtWhoAmI, txtGameStatus, txtAll, txtConnectStatus;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (D) Log.e(TAG, "++ ON CREATE ++");

        // Set up the window layout
        setContentView(R.layout.protocol_debugger);
        homeContext = this;

        // set up listener for the buttons
        //Button btnWhoAmI, btnPlayerPos, btnPlayerCoins, btnGameEnd, btnGameStart;
        btnWhoAmI = (Button)findViewById(R.id.btnWhoAmI);
        btnPlayerPos = (Button)findViewById(R.id.btnPlayerPos);
        btnPlayerCoins = (Button)findViewById(R.id.btnPlayerCoin);
        btnGameEnd = (Button)findViewById(R.id.btnGameEnd);
        btnStart = (Button)findViewById(R.id.btnStart);

        btnWhoAmI.setOnClickListener(buttonHandler);
        btnPlayerPos.setOnClickListener(buttonHandler);
        btnPlayerCoins.setOnClickListener(buttonHandler);
        btnGameEnd.setOnClickListener(buttonHandler);
        btnStart.setOnClickListener(buttonHandler);

        spinPlayer = (Spinner)findViewById(R.id.spinPlayer);
        txtX = (EditText)findViewById(R.id.txtX);
        txtY = (EditText)findViewById(R.id.txtY);
        txtCoins = (EditText)findViewById(R.id.txtCoins);
        txtWhoAmI = (TextView)findViewById(R.id.txtWhoAmI);
        txtGameStatus = (TextView)findViewById(R.id.txtGameStatus);
        txtAll = (TextView)findViewById(R.id.txtAll);
        txtConnectStatus = (TextView)findViewById(R.id.txtConnectStatus);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String[] spinnerArray = new String[] {"1", "2", "3", "4", "5"};
        ArrayAdapter spinnerArrayAdapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item,
                spinnerArray);

        spinPlayer.setAdapter(spinnerArrayAdapter);
    }
    

    private Button.OnClickListener buttonHandler = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            int player = Integer.parseInt(spinPlayer.getSelectedItem().toString());
            if (view.getId() == btnWhoAmI.getId()) {
                // send who am i
                messager.whoAmI();
            } else if (view.getId() == btnGameEnd.getId()) {
                messager.gameEnd();
            } else if (view.getId() == btnStart.getId()) {
                messager.gameStart();
            } else if (view.getId() == btnPlayerCoins.getId()) {
                int coins = Integer.parseInt(txtCoins.getText().toString());
                messager.sendPlayerCoins(player, coins);
            } else if (view.getId() == btnPlayerPos.getId()) {
                int x = Integer.parseInt(txtX.getText().toString());
                int y = Integer.parseInt(txtY.getText().toString());
                messager.sendPlayerPos(player, x, y);
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        if (mBluetoothAdapter.isEnabled()) {
            setupChat();
        } else {
            finish();
            // can't do anything without bluetooth
            Toast.makeText(this, "We require bluetooth to start!", Toast.LENGTH_SHORT).show();
        }
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

    private MessageProtocol.MessageHandler handler = new MessageProtocol.MessageHandler() {
        @Override
        public void handleWhoAmI(String device) {
            txtWhoAmI.setText("Request who: " + device);
        }

        @Override
        public void handleYouAre(String device, int player) {
            txtWhoAmI.setText("You are: " + device + " for: " + player);
        }

        @Override
        public void handlePlayerPos(int player, int x, int y) {
            txtAll.append("Player: " + player + " is at: " + x + "," + y);
            txtAll.append("\n");
        }

        @Override
        public void handlePlayerCoins(int player, int coins) {
            txtAll.append("Player: " + player + " has coins: " + coins);
            txtAll.append("\n");
        }

        @Override
        public void handleGameStart(String time) {
            txtGameStatus.setText("Game started at: " + time);
        }

        @Override
        public void handleGameEnd(String time) {
            txtGameStatus.setText("Game ended at: " + time);
        }
    };

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothChatService.MESSAGE_STATECHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    txtConnectStatus.setText("Connected");
                    messager = new MessageProtocol(mChatService);
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    txtConnectStatus.setText("Connecting");
                    break;
                case BluetoothChatService.STATE_LISTEN:
                    txtConnectStatus.setText("Listening");
                    break;
                case BluetoothChatService.STATE_DISCONNECTED:
                    txtConnectStatus.setText("Disconnected");
                    Toast.makeText(getApplicationContext(), "Disconnected!", Toast.LENGTH_SHORT);
                    finish();
                    break;
                case BluetoothChatService.STATE_NONE:
                    txtConnectStatus.setText("Not Connected");
                    break;
                }
                break;
            case BluetoothChatService.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                messager.parseBytes(readBuf, handler);
                break;
            }
        }
    };

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // start up the bluetooth chat service
        Common.BluetoothMode mode = Enum.valueOf(Common.BluetoothMode.class,
                getIntent().getExtras().getString("mode"));
        if (mode == Common.BluetoothMode.Server) {
            if(D) Log.w(TAG, "setupChat starting server");
            mChatService.setupServer();
        } else {
            // get the device
            String deviceAddr = getIntent().getExtras().getString("device");
            if(D) Log.w(TAG, "setupChat starting client for addr: " + deviceAddr);
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddr);
            mChatService.setupClient(device);
        }
    }


}
