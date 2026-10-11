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
import com.badlogic.gdx.utils.Array;
import javax.imageio.ImageIO;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Texture mapTex;
    private Pixmap mapPixmap;

    private String hero = "kris", dir = "down"; 
    private float x = 415, y = 430, time = 0f;
    private boolean moving = false;
    private Animation<TextureRegion> anim;

    @Override
    public void create() {
        batch = new SpriteBatch();
        mapTex = new Texture(Gdx.files.internal("assets/school.png"));
        mapPixmap = new Pixmap(Gdx.files.internal("assets/school.png"));
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600); 
        camera.zoom = 0.25f; 
        loadAnim();
    }

    private void loadAnim() {
        try {
            javax.imageio.ImageReader r = ImageIO.getImageReadersByFormatName("gif").next();
            javax.imageio.stream.ImageInputStream s = ImageIO.createImageInputStream(Gdx.files.internal("assets/" + hero + "_lw_walkcycle_" + dir + ".gif").file());
            r.setInput(s);
            Array<TextureRegion> frames = new Array<>();
            for (int i = 0; i < r.getNumImages(true); i++) {
                java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
                ImageIO.write(r.read(i), "png", os);
                byte[] b = os.toByteArray();
                Pixmap p = new Pixmap(b, 0, b.length);
                frames.add(new TextureRegion(new Texture(p)));
                p.dispose();
            }
            anim = new Animation<>(0.15f, frames, Animation.PlayMode.LOOP);
            r.dispose(); s.close();
        } catch (Exception e) { anim = null; }
    }

    @Override
    public void render() {
        boolean wasMoving = moving; String oldDir = dir; moving = false;
        float nx = x, ny = y, d = 140f * Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) { nx -= d; dir = "left"; moving = true; }
        else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) { nx += d; dir = "right"; moving = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) { ny += d; dir = "up"; moving = true; }
        else if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) { ny -= d; dir = "down"; moving = true; }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) { hero = "kris"; loadAnim(); }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) { hero = "noelle"; loadAnim(); }

        if (moving) {
            int px = (int) nx + 10, py = mapPixmap.getHeight() - 1 - (int) ny;
            if (px >= 0 && px < mapPixmap.getWidth() && py >= 0 && py < mapPixmap.getHeight() && (mapPixmap.getPixel(px, py) & 0xffffff00) != 0) {
                x = nx; y = ny; time += Gdx.graphics.getDeltaTime();
            } else { moving = false; }
        }

        if (!dir.equals(oldDir) || moving != wasMoving) loadAnim();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (anim != null) {
            TextureRegion f = anim.getKeyFrame(time);
            camera.position.set(x + f.getRegionWidth()/2f, y + f.getRegionHeight()/2f, 0);
        }
        camera.position.x = Math.max(100, Math.min(camera.position.x, mapTex.getWidth() - 100));
        camera.position.y = Math.max(75, Math.min(camera.position.y, mapTex.getHeight() - 75));
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(mapTex, 0, 0);
        if (anim != null) batch.draw(anim.getKeyFrame(time), x, y);
        batch.end();
    }

    @Override
    public void dispose() { batch.dispose(); mapTex.dispose(); mapPixmap.dispose(); }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration c = new Lwjgl3ApplicationConfiguration();
        c.setTitle("Deltarune Sandbox"); c.setWindowedMode(800, 600); c.useVsync(true);
        new Lwjgl3Application(new Main(), c);
    }
}
