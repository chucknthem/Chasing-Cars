package com.hackathon.chasingcars.entity;


import org.anddev.andengine.entity.primitive.Rectangle;

public class Coin extends Rectangle {
    private static final float COIN_WIDTH = 40f;
    private static final float COIN_HEIGHT = 40f;

    private boolean mTaken = false;
    public Coin(final float startX, final float startY) {
        super(startX, startY, COIN_WIDTH, COIN_HEIGHT);
    }

    public void take() {
        mTaken = true;
    }

    public boolean isTaken() {
        return mTaken;
    }
}
