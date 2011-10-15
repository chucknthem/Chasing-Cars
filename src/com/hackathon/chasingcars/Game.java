package com.hackathon.chasingcars;
import java.util.Random;

import com.hackathon.chasingcars.entity.Player;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.primitive.Line;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.opengl.texture.TextureManager;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 11:54:51 - 03.04.2010
 */
public class Game extends BaseChasingCarActivity {
    // ===========================================================
    // Constants
    // ===========================================================

    /* Initializing the Random generator produces a comparable result over different versions. */
    private static final long RANDOM_SEED = 1234567890;

    private static final int MAP_WIDTH = 5000;
    private static final int MAP_HEIGHT = 5000;
    
    private static final int CAMERA_WIDTH = 1000;
    private static final int CAMERA_HEIGHT = 800;

    private static final int LINE_COUNT = 100;

    private static final int BG_RED = 1;
    private static final int BG_GREEN = 1;
    private static final int BG_BLUE = 1;


    // ===========================================================
    // Fields
    // ===========================================================

    private static TextureManager mTextureManager;

    private Camera mCamera;

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
        this.mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
        this.mTextureManager = new TextureManager();
        
        return new Engine(new EngineOptions(true, ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(MAP_WIDTH, MAP_HEIGHT), this.mCamera));
    }

    @Override
    public void onLoadResources() {

    }

    @Override
    public Scene onLoadScene() {
        this.mEngine.registerUpdateHandler(new FPSLogger());

        final Scene scene = new Scene();
        scene.setBackground(new ColorBackground(BG_RED, BG_GREEN, BG_BLUE));

        final Random random = new Random(RANDOM_SEED);

        for(int i = 0; i < LINE_COUNT; i++) {
            final float x1 = random.nextFloat() * CAMERA_WIDTH;
            final float x2 = random.nextFloat() * CAMERA_WIDTH;
            final float y1 = random.nextFloat() * CAMERA_HEIGHT;
            final float y2 = random.nextFloat() * CAMERA_HEIGHT;
            final float lineWidth = random.nextFloat() * 5;

            final Player rec = new Player("bob", "1", x1, y1);

            rec.setColor(random.nextFloat(), random.nextFloat(), random.nextFloat());

            scene.attachChild(rec);
        }

        Player player = new Player("bob", "1", 0, 0);
        //Rectangle player = new Rectangle(0, 0, 10, 10);
        player.setColor(1.0f, 0.0f, 0.0f);
        scene.attachChild(player);
        return scene;
    }

    @Override
    public void onLoadComplete() {

    }

    // ===========================================================
    // Methods
    // ===========================================================

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}