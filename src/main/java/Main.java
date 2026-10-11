import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import java.io.InputStream;
import java.util.HashMap;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Texture mapTex;
    private Pixmap mapPixmap;
    private final Array<Texture> loadedTextures = new Array<>();
    private HashMap<String, Animation<TextureRegion>> animations;
    
    private String currentHero = "kris";
    private String dir = "down"; 
    private float x = 415, y = 430, stateTime = 0f;
    private boolean moving = false;

    private static final int VIRTUAL_WIDTH = 320;
    private static final int VIRTUAL_HEIGHT = 240;
    private static final float SPEED = 140f; 

    @Override
    public void create() {
        batch = new SpriteBatch();
        mapTex = new Texture(Gdx.files.internal("assets/school.png"));
        
        byte[] bytes = Gdx.files.internal("assets/school.png").readBytes();
        mapPixmap = new Pixmap(bytes, 0, bytes.length);
        
        camera = new OrthographicCamera();
        camera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        animations = new HashMap<>();
        loadHeroAnimations("kris");
        loadHeroAnimations("noelle");
    }

    private void loadHeroAnimations(String heroName) {
        String[] directions = {"down", "left", "right", "up"};
        for (String d : directions) {
            InputStream stream = null;
            javax.imageio.stream.ImageInputStream s = null;
            try {
                stream = Gdx.files.internal("assets/" + heroName + "_lw_walkcycle_" + d + ".gif").read();
                javax.imageio.ImageReader r = javax.imageio.ImageIO.getImageReadersByFormatName("gif").next();
                s = javax.imageio.ImageIO.createImageInputStream(stream);
                r.setInput(s);
                Array<TextureRegion> frames = new Array<>();
                int numImages = r.getNumImages(true);
                for (int i = 0; i < numImages; i++) {
                    java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
                    javax.imageio.ImageIO.write(r.read(i), "png", os);
                    byte[] b = os.toByteArray();
                    Pixmap p = new Pixmap(b, 0, b.length);
                    Texture tex = new Texture(p);
                    loadedTextures.add(tex);
                    frames.add(new TextureRegion(tex));
                    p.dispose();
                }
                Animation<TextureRegion> anim = new Animation<>(0.15f, frames, Animation.PlayMode.LOOP);
                animations.put(heroName + "_" + d, anim);
                r.dispose();
            } catch (Exception e) {
                Gdx.app.error("ANIM_LOAD", "Could not load animation for: " + heroName + "_" + d);
            } finally {
                try { if (s != null) s.close(); } catch (Exception ignored) {}
                try { if (stream != null) stream.close(); } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void render() {
        handleInput();
        updateCamera();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(mapTex, 0, 0);
        
        Animation<TextureRegion> currentAnim = animations.get(currentHero + "_" + dir);
        if (currentAnim != null) {
            TextureRegion currentFrame = moving ? currentAnim.getKeyFrame(stateTime, true) : currentAnim.getKeyFrame(0);
            batch.draw(currentFrame, x, y);
        }
        batch.end();
    }

    private void handleInput() {
        Vector2 velocity = new Vector2(0, 0);

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A))   velocity.x = -1;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D))  velocity.x = 1;
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W))     velocity.y = 1;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S))   velocity.y = -1;

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) currentHero = "kris";
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) currentHero = "noelle";

        if (velocity.isZero()) {
            moving = false;
        } else {
            moving = true;
            if (velocity.x < 0) dir = "left";
            else if (velocity.x > 0) dir = "right";
            else if (velocity.y > 0) dir = "up";
            else if (velocity.y < 0) dir = "down";

            velocity.nor().scl(SPEED * Gdx.graphics.getDeltaTime());

            float nextX = x + velocity.x;
            float nextY = y + velocity.y;

            int checkX = (int) nextX + 10; 
            int checkY = mapPixmap.getHeight() - 1 - (int) nextY;

            if (checkX >= 0 && checkX < mapPixmap.getWidth() && checkY >= 0 && checkY < mapPixmap.getHeight()) {
                if ((mapPixmap.getPixel(checkX, checkY) & 0xffffff00) != 0) {
                    x = nextX;
                    y = nextY;
                    stateTime += Gdx.graphics.getDeltaTime();
                }
            }
        }
    }

    private void updateCamera() {
        Animation<TextureRegion> currentAnim = animations.get(currentHero + "_" + dir);
        float widthOffset = (currentAnim != null) ? currentAnim.getKeyFrame(0).getRegionWidth() / 2f : 0;
        float heightOffset = (currentAnim != null) ? currentAnim.getKeyFrame(0).getRegionHeight() / 2f : 0;

        camera.position.set(x + widthOffset, y + heightOffset, 0);

        float minCamX = camera.viewportWidth / 2f;
        float maxCamX = mapTex.getWidth() - minCamX;
        float minCamY = camera.viewportHeight / 2f;
        float maxCamY = mapTex.getHeight() - minCamY;

        camera.position.x = Math.max(minCamX, Math.min(camera.position.x, maxCamX));
        camera.position.y = Math.max(minCamY, Math.min(camera.position.y, maxCamY));
        camera.update();
    }

    @Override
    public void dispose() {
        batch.dispose();
        mapTex.dispose();
        mapPixmap.dispose();
        for (Texture tex : loadedTextures) {
            tex.dispose();
        }
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration c = new Lwjgl3ApplicationConfiguration();
        c.setTitle("Deltarune Sandbox");
        c.setWindowedMode(800, 600);
        c.useVsync(true);
        new Lwjgl3Application(new Main(), c);
    }
}
