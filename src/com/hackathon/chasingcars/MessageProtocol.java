package com.hackathon.chasingcars;

import android.nfc.Tag;
import android.text.format.Time;
import android.text.method.DateTimeKeyListener;
import android.util.Log;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hackathon.chasingcars.protocol.Messages;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public class MessageProtocol {
    private static final String TAG = "chasingcars:MessageProtocol";

    BluetoothChatService service = null;

    final static int MESSAGE_WHOAMI = 0;
    final static int MESSAGE_YOUARE = 1;
    final static int MESSAGE_PLAYERPOS = 2;
    final static int MESSAGE_PLAYERCOINS = 3;
    final static int MESSAGE_GAMESTART = 4;
    final static int MESSAGE_GAMEEND = 5;

    public MessageProtocol(BluetoothChatService service) {
        this.service = service;
    }

    private void send(int type, com.google.protobuf.GeneratedMessage msg) {
        // convert the length to a 4 byte array
        byte[] bytes = msg.toByteArray();
        if (bytes.length > 200) {
            throw new IllegalArgumentException("Can't handle large messages");
        }

        byte msgType = (byte)type;
        byte msgLength = (byte)bytes.length;

        byte[] header = new byte[]{msgLength, msgType};
        service.write(header);
        service.write(msg.toByteArray());

        //System.out.println("Sending bytes:");
        //dumpBytes(header);
        //dumpBytes(msg.toByteArray());
    }

    public void sendPlayerPos(int player, int x, int y) {
        Messages.PlayerPos playerPos = Messages.PlayerPos.newBuilder()
                .setPlayer(player)
                .setX(x)
                .setY(y)
                .build();
        send(MESSAGE_PLAYERPOS, playerPos);
    }

    public void whoAmI() {
        // send the device address
        String addr = service.getDeviceAddress();
        Messages.WhoAmI who = Messages.WhoAmI.newBuilder()
                .setDeviceAddr(addr)
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
        for(int i = 0; i < buf.length; i++) {
            if (i % 16 == 0) {
                System.out.println();
            }

            System.out.print(" " + buf[i]);
        }
    }

    public void parseBytes(byte[] buf, MessageHandler handler) {
        // parse magic
        //System.out.println("parseBytes magically");
        //dumpBytes(buf);

        if (previous != null) {
            byte[] newBuf = new byte[previous.length + buf.length];
            int newBufI = 0;
            for(int i = 0; i < previous.length; i++) {
                newBuf[newBufI++] = previous[i];
            }
            for(int i = 0; i < buf.length; i++) {
                newBuf[newBufI++] = buf[i];
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
                break;
            }

            if (bufI + msgLength + 2 > buf.length) {
                // incomplete message, put in buffer
                previous = new byte[buf.length - bufI];
                for(int j = 0; j < previous.length; j++) {
                    previous[j] = buf[bufI++];
                }
            } else {
                // all good, we can parse it
                byte[] current = new byte[msgLength];
                for(int j = 0; j < msgLength; j++) {
                    current[j] = buf[bufI++];
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
                    handler.handleGameEnd(ge.getTime());
                    break;
                case MESSAGE_GAMESTART:
                    Messages.GameStart gs = Messages.GameStart.parseFrom(buf);
                    handler.handleGameStart(gs.getTime());
                    break;
                case MESSAGE_PLAYERCOINS:
                    Messages.PlayerCoins cs = Messages.PlayerCoins.parseFrom(buf);
                    handler.handlePlayerCoins(cs.getPlayer(), cs.getCoins());
                    break;
                case MESSAGE_PLAYERPOS:
                    Messages.PlayerPos ps = Messages.PlayerPos.parseFrom(buf);
                    handler.handlePlayerPos(ps.getPlayer(),  ps.getX(), ps.getY());
                    break;
                case MESSAGE_WHOAMI:
                    Messages.WhoAmI who = Messages.WhoAmI.parseFrom(buf);
                    handler.handleWhoAmI(who.getDeviceAddr());
                    break;
                case MESSAGE_YOUARE:
                    Messages.YouAre you = Messages.YouAre.parseFrom(buf);
                    handler.handleYouAre(you.getDeviceAddr(), you.getPlayer());
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
    }
}
