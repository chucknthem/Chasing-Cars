package com.hackathon.chasingcars;

import android.text.format.Time;
import android.util.Log;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.hackathon.chasingcars.protocol.Messages;

public class MessageProtocol {
    private static final String TAG = "chasingcars:MessageProtocol";

    // debug the protocol?
    private static final boolean D_P = false;

    BluetoothService service = null;

    final static int MESSAGE_WHOAMI = 0;
    final static int MESSAGE_YOUARE = 1;
    final static int MESSAGE_PLAYERPOS = 2;
    final static int MESSAGE_PLAYERCOINS = 3;
    final static int MESSAGE_GAMESTART = 4;
    final static int MESSAGE_GAMEEND = 5;

    boolean useGeneric = false;

    public MessageProtocol(BluetoothService service) {
        this.service = service;
    }

    public MessageProtocol(BluetoothService service, boolean useGeneric) {
        this(service);
        this.useGeneric = useGeneric;
    }

    private void send(int type, com.google.protobuf.GeneratedMessage msg) {
        // convert the length to a 4 byte array
        byte[] bytes = msg.toByteArray();
        if (bytes.length > 200) {
            throw new IllegalArgumentException("Can't handle large messages");
        }

        byte[] message = new byte[bytes.length + 2];
        message[0] = (byte)bytes.length;
        message[1] = (byte)type;

        for(int i = 0; i < bytes.length; i++) {
            message[i + 2] = bytes[i];
        }

        if (D_P) {
            System.out.println("Sending bytes:");
            dumpBytes(message);
        }

        service.write(message);
    }

    public void sendPlayerPos(int player, int x, int y) {
        Messages.PlayerPos playerPos = Messages.PlayerPos.newBuilder()
                .setPlayer(player)
                .setX(x)
                .setY(y)
                .build();
        send(MESSAGE_PLAYERPOS, playerPos);
    }

    public String getDevice() {
        return service.getDeviceAddress();
    }

    public void whoAmI() {
        // send the device address
        Messages.WhoAmI who = Messages.WhoAmI.newBuilder()
                .setDeviceAddr(getDevice())
                .build();
        send(MESSAGE_WHOAMI, who);
    }

    public void youAre(String device, int player) {
        Messages.YouAre youAre = Messages.YouAre.newBuilder()
                .setPlayer(player)
                .setDeviceAddr(device)
                .build();

        send(MESSAGE_YOUARE, youAre);
    }

    public void sendPlayerCoins(int player, int coins) {
        Messages.PlayerCoins coinsM = Messages.PlayerCoins.newBuilder()
                .setPlayer(player)
                .setCoins(coins)
                .build();
        send(MESSAGE_PLAYERCOINS, coinsM);
    }

    public void gameStart() {
        Time now = new Time();
        now.setToNow();

        Messages.GameStart start = Messages.GameStart.newBuilder()
                .setTime(now.format2445())
                .build();
        send(MESSAGE_GAMESTART, start);
    }

    public void gameEnd() {
        Time now = new Time();
        now.setToNow();

        Messages.GameEnd end = Messages.GameEnd.newBuilder()
                .setTime(now.format2445())
                .build();
        send(MESSAGE_GAMEEND, end);
    }

    byte[] previous = null;

    private void dumpBytes(byte[] buf) {
        int sum = -1;
        for(int i = 0; i < buf.length; i++) {
            if (i % 16 == 0) {
                System.out.println();
                if (sum == 0) break;
                sum = 0;
            }
            sum += buf[i];
            System.out.print(" " + buf[i]);
        }
    }

    public void parseBytes(byte[] buf, MessageHandler handler) {
        // parse magic
        
        if (D_P) {
            Log.e(TAG, "parseBytes magically");
            dumpBytes(buf);
        }

        if (previous != null) {
            if (D_P) {
                Log.e(TAG, "previous not null!");
                dumpBytes(previous);
            }

            byte[] newBuf = new byte[previous.length + buf.length];
            int newBufI = 0;
            for(int i = 0; i < previous.length; i++) {
                newBuf[newBufI++] = previous[i];
            }
            for(int i = 0; i < buf.length; i++) {
                newBuf[newBufI++] = buf[i];
            }

            if (D_P) {
                Log.e(TAG, "newBuf: ");
                dumpBytes(newBuf);
            }

            buf = newBuf;
        }

        // for now, assume messages can be clustered but that's it
        int bufI = 0;
        while(bufI < buf.length) {
            byte msgLength = buf[bufI++];
            byte msgType = buf[bufI++];

            if (msgLength == 0) {
                // this isn't a message!
                if (D_P) {
                    Log.e(TAG, "msg length of 0 found?");
                }
                break;
            }

            if (bufI + msgLength + 2 > buf.length) {
                // incomplete message, put in buffer
                previous = new byte[buf.length - bufI];
                for(int j = 0; j < previous.length; j++) {
                    previous[j] = buf[bufI++];
                }

                if (D_P) {
                    Log.e(TAG, "shoved into previous");
                    dumpBytes(previous);
                }
            } else {
                // all good, we can parse it
                byte[] current = new byte[msgLength];
                for(int j = 0; j < msgLength; j++) {
                    current[j] = buf[bufI++];
                }

                if (D_P) {
                    Log.e(TAG, "parseIntoMsg");
                }

                // parse the damn thing
                parseIntoMsg(msgType, current, handler);
            }
        }
    }

    private void parseIntoMsg(byte msgType, byte[] buf, MessageHandler handler) {
        try {
            switch(msgType) {
                case MESSAGE_GAMEEND:
                    Messages.GameEnd ge = Messages.GameEnd.parseFrom(buf);
                    if (useGeneric) {
                        handler.handleMessage(msgType, ge);
                    } else {
                        handler.handleGameEnd(ge.getTime());
                    }
                    break;
                case MESSAGE_GAMESTART:
                    Messages.GameStart gs = Messages.GameStart.parseFrom(buf);
                    if (useGeneric) {
                        handler.handleMessage(msgType, gs);
                    } else {
                        handler.handleGameStart(gs.getTime());
                    }
                    break;
                case MESSAGE_PLAYERCOINS:
                    Messages.PlayerCoins cs = Messages.PlayerCoins.parseFrom(buf);
                    if (useGeneric) {
                        handler.handleMessage(msgType, cs);
                    } else {
                        handler.handlePlayerCoins(cs.getPlayer(), cs.getCoins());
                    }
                    break;
                case MESSAGE_PLAYERPOS:
                    Messages.PlayerPos ps = Messages.PlayerPos.parseFrom(buf);
                    if (useGeneric) {
                        handler.handleMessage(msgType, ps);
                    } else {
                        handler.handlePlayerPos(ps.getPlayer(),  ps.getX(), ps.getY());
                    }
                    break;
                case MESSAGE_WHOAMI:
                    Messages.WhoAmI who = Messages.WhoAmI.parseFrom(buf);
                    if (useGeneric) {
                        handler.handleMessage(msgType, who);
                    } else {
                        handler.handleWhoAmI(who.getDeviceAddr());
                    }
                    break;
                case MESSAGE_YOUARE:
                    Messages.YouAre you = Messages.YouAre.parseFrom(buf);
                    if (useGeneric) {
                        handler.handleMessage(msgType, you);
                    } else {
                        handler.handleYouAre(you.getDeviceAddr(), you.getPlayer());
                    }
                    break;
            }
        } catch (InvalidProtocolBufferException e) {
            Log.e(TAG, "parseIntoMsg invalid", e);
        }
    }

    public interface MessageHandler {
        void handleWhoAmI(String device);
        void handleYouAre(String device, int player);
        void handlePlayerPos(int player, int x, int y);
        void handlePlayerCoins(int player, int coins);
        void handleGameStart(String time);
        void handleGameEnd(String time);

        // generic handler if you want to avoid calling each of the above ones..
        void handleMessage(byte messageType, Object message);
    }
}
