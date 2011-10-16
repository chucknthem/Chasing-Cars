package com.hackathon.chasingcars.entity;

import android.util.Log;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.apache.http.client.RedirectException;

/**
 * Created by IntelliJ IDEA.
 * User: charlesma
 * Date: 10/15/11
 * Time: 2:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class Player extends Rectangle implements IUpdateHandler {

    private static final float PLAYER_WIDTH = 40f;
    private static final float PLAYER_HEIGHT = 40f;

    private static final float PLAYER_DECELERATION = 100f; // per second.
    private static final float PLAYER_ACCELERATION = .1f;
    
    public static enum COLOR {
        RED,
        BLUE,
        YELLOW,
        GREEN,
        BLACK
    }

    private String name;
    private String id;
   
    private double mDirection = 0; // Radians.
    private float mVelocity = 0;

    public Player(String name, String id,  final float startX, final float startY) {
        super(startX, startY, PLAYER_WIDTH, PLAYER_HEIGHT);
 
        this.id = id;
        this.name = name;
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
        this.mX += moveX;
        this.mY += moveY;

        mVelocity = mVelocity - PLAYER_DECELERATION * pSecondsElapsed;
        if (mVelocity < 0) mVelocity = 0;
        
        super.onManagedUpdate(pSecondsElapsed);
    }

    public void accelerate(final float x, final float y) {
        float newX = mVelocity * (float) Math.cos(mDirection) + x * PLAYER_ACCELERATION;
        float newY = mVelocity * (float) Math.sin(mDirection) + y * PLAYER_ACCELERATION;

        mVelocity = (float) Math.sqrt((double)(newX * newX + newY * newY));
        mDirection = Math.atan2((double)newX, (double) newY);
    }
}
