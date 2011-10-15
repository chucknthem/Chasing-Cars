package com.hackathon.chasingcars.entity;

import org.anddev.andengine.entity.primitive.Rectangle;
import org.apache.http.client.RedirectException;

/**
 * Created by IntelliJ IDEA.
 * User: charlesma
 * Date: 10/15/11
 * Time: 2:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class Player extends Rectangle {

    private static final float PLAYER_WIDTH = 40f;
    private static final float PLAYER_HEIGHT = 40f;
    public static enum COLOR {
        RED,
        BLUE,
        YELLOW,
        GREEN,
        BLACK
    }

    private String name;
    private String id;

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
}
