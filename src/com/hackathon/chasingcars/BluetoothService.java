/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hackathon.chasingcars;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothService {
    // Debugging
    private static final String TAG = "chasingcars:BluetoothChatService";
    private static final boolean D = true;

    final static int NUM_UUIDS = 8;

    private Activity owner;

    private static final UUID[] MY_UUIDS = new UUID[] {
            UUID.fromString("0DD4F882-F75A-11E0-ADFB-C8BC4824019B"),
            UUID.fromString("27A2AED0-F75A-11E0-95E3-D1BC4824019B"),
            UUID.fromString("2B5E207C-F75A-11E0-821D-E0BC4824019B"),
            UUID.fromString("2E508DC4-F75A-11E0-939C-E1BC4824019B"),
            UUID.fromString("30E682B4-F75A-11E0-9876-E2BC4824019B"),
            UUID.fromString("33C0ACF8-F75A-11E0-8A65-E3BC4824019B"),
            UUID.fromString("483F02EC-F75A-11E0-B119-E7BC4824019B"),
            UUID.fromString("4B32F986-F75A-11E0-B283-EBBC4824019B")
    };

    private ConnectedThread[] serverClients = new ConnectedThread[NUM_UUIDS];
    private ServerListenThread serverListenThread;

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;

    private ClientThread clientThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_DISCONNECTED = 4; // disconnected

    // Constants for message types
    public static final int MESSAGE_STATECHANGE = 1;
    public static final int MESSAGE_READ = 2;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param owner  The UI Activity
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothService(Activity owner, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
        this.owner = owner;
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1000);
            owner.startActivity(discoverableIntent);
        }
    }


    public void setupServer() {
        ensureDiscoverable();
        setState(STATE_LISTEN);

        if (serverListenThread != null) {
            serverListenThread.cancel();
            serverListenThread = null;
        }
        serverListenThread = new ServerListenThread();
        serverListenThread.start();
    }


    public void setupClient(BluetoothDevice device) {
        // we try to connect to each of the UUID
        setState(STATE_CONNECTING);
        
        if (clientThread != null) {
            clientThread.cancel();
            clientThread = null;
        }

        clientThread = new ClientThread(device);
        clientThread.start();
    }



    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MESSAGE_STATECHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized ConnectedThread connected(BluetoothSocket socket, BluetoothDevice device, int clientNum) {
        if (D) Log.d(TAG, "connected to device: " + device.getName() + ";" + device.getAddress());

        ConnectedThread connectedThread = new ConnectedThread(socket, clientNum);

        // Start the thread to manage the connection and perform transmissions
        connectedThread.start();

        // Send the name of the connected device back to the UI Activity
        setState(STATE_CONNECTED);

        return connectedThread;
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (clientThread != null) {
            clientThread.cancel();
            clientThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (serverListenThread != null) {
            serverListenThread.cancel();
            serverListenThread = null;
        }

        for(int i = 0; i < serverClients.length; i++) {
            if (serverClients[i] != null) {
                serverClients[i].cancel();
                serverClients[i] = null;
            }
        }

        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see BluetoothService.ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                Log.d(TAG, "write called, can't send as state != connecteD: " + mState);
            }

            r = mConnectedThread;
        }

        if (r == null) {
            // send from server to everyone
            serverSendToAll(out, out.length, -1);
        } else {
            // Perform the write unsynchronized
            r.write(out);
        }
    }

    public String getDeviceAddress() {
        return mAdapter.getAddress();
    }

    public boolean isServer() {
        return serverListenThread != null;
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        setState(STATE_DISCONNECTED);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        setState(STATE_DISCONNECTED);
    }

    private void serverSendToAll(byte[] buffer, int numBytes, int fromClient) {
        if (D) Log.d(TAG, "serverSendToAll: bytes: " + buffer + " numByteS: "+ numBytes + " fromClient: " + fromClient);

        for(int i = 0; i < serverClients.length; i++) {
            if (i != fromClient) {
                if (serverClients[i] != null) {
                    serverClients[i].write(buffer, numBytes);
                }
            }
        }
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class ServerListenThread extends Thread {
        // The local server socket
        private BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public ServerListenThread() {
        }

        public void run() {

            // listen for multiple connections
            for(int i = 0; i < MY_UUIDS.length; i++) {
                try {
                    BluetoothServerSocket tmp = null;
                    mmServerSocket = null;
                    if (D) Log.d(TAG, "Server listening on: " + MY_UUIDS[i]);
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(Common.MY_APP_NAME, MY_UUIDS[i]);

                    if (D) Log.d(TAG, "Socket Created. BEGIN waiting for accept" + this);

                    // now we wait for a connection
                    mmServerSocket = tmp;
                    BluetoothSocket socket = tmp.accept();

                    if (D) Log.d(TAG, "Got a socket");
                    if (socket != null) {
                        serverClients[i] = connected(socket, socket.getRemoteDevice(), i);
                    } else {
                        Log.e(TAG, "Got an empty socket??");
                    }

                    // stop listening on that now
                    if (D) Log.d(TAG, "Close tmp socket");
                    tmp.close();
                } catch (IOException e) {
                    Log.e(TAG, "ServerListenThread failed", e);
                }
            }
        }

        public void cancel() {
            if (D) Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                if (mmServerSocket != null) {
                    mmServerSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ClientThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mDevice;

        public ClientThread(BluetoothDevice device) {
            mDevice = device;
        }

        public void run() {
            try {
                mAdapter.cancelDiscovery();

                int retryCount = 100;
                for(int j = 0; j < retryCount; j++) {
                    for(int i = 0; i < MY_UUIDS.length; i++) {
                        UUID uuid = MY_UUIDS[i];

                        if(D) Log.w(TAG, "ClientThread attempting create socket for UUID: " + uuid);
                        mmSocket = mDevice.createInsecureRfcommSocketToServiceRecord(uuid);

                        try {
                            if (D) Log.d(TAG, "Trying to connect to the above UUID");
                            // This is a blocking call and will only return on a
                            // successful connection or an exception
                            mmSocket.connect();

                            if (D) Log.d(TAG, "ClientThread mmSocket connected! : " + mmSocket + "; about to connect thread");

                            // Start the connected thread
                            mConnectedThread = connected(mmSocket, mDevice, -1);
                            return;
                        } catch (IOException e) {
                            // Close the socket
                            Log.e(TAG, "ClientThread connect failed", e);
                            try {
                                mmSocket.close();
                            } catch (IOException e2) {
                                Log.e(TAG, "unable to close() socket during connection failure", e2);
                            }
                        }
                        Thread.sleep(300);
                    }

                    Log.w(TAG, "MYUID loop iteration over, trying again. Upto iteration " + j + " out of " + retryCount);
                    Thread.sleep(1000);
                }

                connectionFailed();
            } catch (IOException e) {
                Log.w(TAG, "ClientThread run error", e);
            } catch (InterruptedException e) {
                Log.w(TAG, "ClientThread interrupted", e);
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final int clientNum;

        public ConnectedThread(BluetoothSocket socket, int clientNum) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            this.clientNum = clientNum;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // if we are the server, we need to send this to everyone else
                    // except the person who sent it!
                    if (serverListenThread != null) {
                        serverSendToAll(buffer, bytes, clientNum);
                    }

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }

            connectionLost();
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void write(byte[] buffer, int numBytes) {
            try {
                mmOutStream.write(buffer, 0, numBytes);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }



        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
