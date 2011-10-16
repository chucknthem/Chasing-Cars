package com.hackathon.chasingcars.entity;

import android.util.Log;
import com.hackathon.chasingcars.Game;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.primitive.Rectangle;

public class Player extends Rectangle implements IUpdateHandler {

    private static final float PLAYER_WIDTH = 40f;
    private static final float PLAYER_HEIGHT = 40f;

    private static final float PLAYER_DECELERATION = 100f; // per second.
    private static final float PLAYER_ACCELERATION = .05f;

    public static final float PLAYER_MAX_VELOCITY = 400;

    public static enum COLOR {
        RED,
        BLUE,
        YELLOW,
        GREEN,
        BLACK
    }

    private int mCoinCount = 0;
    private String mName;
    private String mId;
   
    private double mDirection = 0; // Radians.
    private float mVelocity = 0;

    public Player(String name, String id,  final float startX, final float startY) {
        super(startX, startY, PLAYER_WIDTH, PLAYER_HEIGHT);
 
        mId = id;
        mName = name;
    }

    public void setColor(COLOR color) {
        float red, green, blue;
        red = green = blue = 0;

        switch (color) {
            case BLACK:
                break; // default
            case RED:
                red = 1;
                break;
            case GREEN:
                green = 1;
                break;
            case BLUE:
                blue = 1;
                break;
            case YELLOW:
                red = 1;
                green = 1;
                break;
        }
        this.setColor(red, green, blue);
    }
 
    @Override
    protected void onManagedUpdate(final float pSecondsElapsed) {

        float moveX = mVelocity * pSecondsElapsed * (float) Math.cos(mDirection);
        float moveY = mVelocity * pSecondsElapsed * (float) Math.sin(mDirection);
        float newX = this.mX + moveX;
        float newY = this.mY + moveY;

        mVelocity = mVelocity - PLAYER_DECELERATION * pSecondsElapsed;
        if (mVelocity < 0) mVelocity = 0;
        
        super.onManagedUpdate(pSecondsElapsed);
        if (newX > (0 + Game.CAMERA_WIDTH/2) && newX < (Game.MAP_WIDTH - Game.CAMERA_WIDTH/2) &&
            newY > (0 + Game.CAMERA_WIDTH/2) && newY < (Game.MAP_HEIGHT - Game.CAMERA_HEIGHT/2)) {
            this.mX = newX;
            this.mY = newY;
        }

    }

    public void accelerate(final float x, final float y) {
        float newX = mVelocity * (float) Math.cos(mDirection) + x * PLAYER_ACCELERATION;
        float newY = mVelocity * (float) Math.sin(mDirection) + y * PLAYER_ACCELERATION;

        mVelocity = (float) Math.sqrt((double)(newX * newX + newY * newY));
        if (mVelocity > PLAYER_MAX_VELOCITY) {
            mVelocity = PLAYER_MAX_VELOCITY;
        }
        mDirection = Math.atan2((double)newY, (double) newX);
    }

    public void takeCoin(Coin coin) {
        coin.take();
        mCoinCount++;

    }
}
