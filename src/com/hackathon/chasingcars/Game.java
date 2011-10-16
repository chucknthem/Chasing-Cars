package com.hackathon.chasingcars;
import java.util.*;

import android.util.Log;
import android.view.MotionEvent;
import com.hackathon.chasingcars.entity.Coin;
import com.hackathon.chasingcars.entity.Player;
import org.anddev.andengine.collision.RectangularShapeCollisionChecker;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.camera.SmoothCamera;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.scene.background.RepeatingSpriteBackground;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.TextureManager;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.AssetBitmapTextureAtlasSource;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;

import javax.microedition.khronos.opengles.GL10;

public class Game extends BaseChasingCarActivity implements Scene.IOnSceneTouchListener, IUpdateHandler {
    // ===========================================================
    // Constants
    // ===========================================================

    /* Initializing the Random generator produces a comparable result over different versions. */
    private static final long RANDOM_SEED = 1234567890;

    public static final float MAP_WIDTH = 5000;
    public static final float MAP_HEIGHT = 5000;

    private static final float STARTX = MAP_WIDTH/2;
    private static final float STARTY = MAP_HEIGHT/2;

    public static final float CAMERA_WIDTH = 1000;
    public static final float CAMERA_HEIGHT = 800;
    private static final float CAM_MAX_VELOCITY_X = 350f;
    private static final float CAM_MAX_VELOCITY_Y = 350f;
    private static final float CAM_ZOOM_FACTOR = 5f;
    
    private static final float COIN_COUNT = 100;

    private static final int BG_RED = 1;
    private static final int BG_GREEN = 1;
    private static final int BG_BLUE = 1;



    // ===========================================================
    // Fields
    // ===========================================================

    private static TextureManager mTextureManager;

    private SmoothCamera mCamera;

    private List<Player> mPlayers = new LinkedList<Player>();

    private List<Coin> mCoins = new LinkedList<Coin>();

    private Player mThisPlayer;

    private boolean mIsAccelerating = false;



    // Touch
    private float mTouchX, mTouchY, mTouchOffsetX, mTouchOffsetY;

    private BitmapTextureAtlas mAutoParallaxBackgroundTexture;
    private TextureRegion mParallaxLayerFront;
    private BitmapTextureAtlas mBitmapTextureAtlas;
    private TiledTextureRegion mPlayerTextureRegion;
    private RepeatingSpriteBackground mGrassBackground;

    // ===========================================================
    // Constructors
    // ===========================================================

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    @Override
    public Engine onLoadEngine() {
        this.mCamera = new SmoothCamera(STARTX, STARTY, CAMERA_WIDTH, CAMERA_HEIGHT, CAM_MAX_VELOCITY_X, CAM_MAX_VELOCITY_Y, CAM_ZOOM_FACTOR);
        this.mTextureManager = new TextureManager();
        
        return new Engine(new EngineOptions(true, ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(MAP_WIDTH, MAP_HEIGHT), this.mCamera));
    }

    @Override
    public void onLoadResources() {
        this.mGrassBackground = new RepeatingSpriteBackground(CAMERA_WIDTH + 200, CAMERA_HEIGHT + 200, this.mEngine.getTextureManager(), new AssetBitmapTextureAtlasSource(this, "grass3.png")) {
            public void onDraw(GL10 pGL, Camera pCamera) {
                pGL.glPushMatrix();

                float camX = (int)pCamera.getMinX() % 64;
                camX += (pCamera.getMinX() - (int)pCamera.getMinX());
                float camY = (int)pCamera.getMinY() % 64;
                camY += (pCamera.getMinY() - (int)pCamera.getMinY());

                pGL.glTranslatef(-1 * camX, -1 * camY, 0);
                super.onDraw(pGL, pCamera);
                pGL.glPopMatrix();
            }
        };
    }

    @Override
    public Scene onLoadScene() {
        this.mEngine.registerUpdateHandler(new FPSLogger());
        this.mCamera.setCenter(STARTX, STARTY);
        //this.mEngine.registerUpdateHandler(this);

        final Scene scene = new Scene();
        scene.setBackground(new ColorBackground(BG_RED, BG_GREEN, BG_BLUE));
        scene.setBackground(this.mGrassBackground);

        mThisPlayer = new Player("bob", "1", STARTX, STARTY);
        mThisPlayer.setColor(Player.COLOR.RED);

        scene.attachChild(mThisPlayer);

        mPlayers.add(mThisPlayer);

        /// Randomly distribute some coins.
        Random rGen = new Random(RANDOM_SEED);
        for (int i = 0; i < COIN_COUNT; i++) {
            float x = rGen.nextFloat() * MAP_WIDTH;
            float y = rGen.nextFloat() * MAP_HEIGHT;
            Coin coin = new Coin(x, y);
            scene.attachChild(coin);
            mCoins.add(coin);
        }

        Collections.sort(mCoins, new Comparator<Coin>() {
            @Override
            public int compare(Coin coin1, Coin coin2) {
                return (int) (coin1.getX() - coin2.getX());
            }
        });
        scene.registerUpdateHandler(this);

        // Register activity to handle touch events.
        scene.setOnSceneTouchListener(this);

        return scene;
    }

    @Override
    public void onLoadComplete() {

    }

    /**
     * Called when a {@link org.anddev.andengine.input.touch.TouchEvent} is dispatched to a {@link org.anddev.andengine.entity.scene.Scene}.
     *
     * @param pScene           The {@link org.anddev.andengine.entity.scene.Scene} that the {@link org.anddev.andengine.input.touch.TouchEvent} has been dispatched to.
     * @param pSceneTouchEvent The {@link org.anddev.andengine.input.touch.TouchEvent} object containing full information about the event.
     * @return <code>true</code> if this {@link org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener} has consumed the {@link org.anddev.andengine.input.touch.TouchEvent}, <code>false</code> otherwise.
     */
    @Override
    public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
        int action = pSceneTouchEvent.getAction();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            this.mIsAccelerating = true;
            //this.mTouchX = pSceneTouchEvent.getMotionEvent().getX(); // relative to camera
            //this.mTouchY = pSceneTouchEvent.getMotionEvent().getY(); // relative to camera
            this.mTouchX = pSceneTouchEvent.getX(); // scene global
            this.mTouchY = pSceneTouchEvent.getY(); // scene global
        } else if (action == MotionEvent.ACTION_UP) {
            this.mIsAccelerating = false;
        }
        return true;
    }

    // ===========================================================
    // Methods
    // ===========================================================

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    @Override
    public void onUpdate(float pSecondsElapsed) {
        if (mIsAccelerating) {
            float aX = mTouchX - mThisPlayer.getX();
            float aY = mTouchY - mThisPlayer.getY();

            mThisPlayer.accelerate(aX, aY);
        }

        List<Coin> toRemove = new ArrayList<Coin>();
        for (Coin coin : mCoins) {
            if (Math.abs(coin.getX() - mThisPlayer.getX()) < 40 && Math.abs(coin.getY() - mThisPlayer.getY()) < 40) {
                mThisPlayer.takeCoin(coin);
                mEngine.getScene().detachChild(coin);
                toRemove.add(coin);
            } else if (coin.getX() > mThisPlayer.getX()) {
                break;
            }
            /*
            float dx = coin.getX() - mThisPlayer.getX();
            float dy = coin.getY() - mThisPlayer.getY();
            float d = (float) Math.sqrt(dx*dx + dy*dy);

            if(d < 40) {
                mThisPlayer.takeCoin(coin);
                mEngine.getScene().detachChild(coin);
                toRemove.add(coin);
                Log.w("Detach", coin.getX() + " " + coin.getY());
            }*/
        }

        for (Coin coin : toRemove) {
            mCoins.remove(coin);
            mEngine.getScene().detachChild(coin);
        }
        this.mCamera.setCenter(mThisPlayer.getX(), mThisPlayer.getY());
    }

    @Override
    public void reset() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}