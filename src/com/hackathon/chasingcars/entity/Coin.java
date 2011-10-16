package com.hackathon.chasingcars.entity;


import org.anddev.andengine.entity.primitive.Rectangle;

public class Coin extends Rectangle {
    private static final float COIN_WIDTH = 40f;
    private static final float COIN_HEIGHT = 40f;

    private boolean mTaken = false;
    public Coin(final float startX, final float startY) {
        super(startX, startY, COIN_WIDTH, COIN_HEIGHT);
        this.setColor(0.8f, 1.0f, 0.0f, 1.0f);
    }

    public void take() {
        mTaken = true;
    }

    public boolean isTaken() {
        return mTaken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Coin coin = (Coin) o;

        if (mTaken == coin.mTaken && coin.getX() == this.getX() && coin.getY() == this.getY()) return true;

        return false;
    }

    @Override
    public int hashCode() {
        return ((int) (this.getX() * 31 + this.getY() * 7));
    }
}
