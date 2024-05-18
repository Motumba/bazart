package ru.zagarov.matvei_dima;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

public class MyGdxGame extends ApplicationAdapter {
	SpriteBatch batch;
	Texture ballTexture;
	Texture backgroundTexture;
	BitmapFont font;
	World world;
	Box2DDebugRenderer debugRenderer;
	Array<Body> balls;
	int score;

	private static final float BALL_RADIUS = 0.25f * 5; // Увеличенный радиус шаров
	private static final float PIXELS_PER_METER = 100f;
	private static final float BALL_DIAMETER_PIXELS = BALL_RADIUS * 2 * PIXELS_PER_METER;

	@Override
	public void create() {
		batch = new SpriteBatch();
		ballTexture = new Texture("ball.png");
		backgroundTexture = new Texture("background.png");
		world = new World(new Vector2(0, -9.8f), true);
		debugRenderer = new Box2DDebugRenderer();
		balls = new Array<>();
		score = 0;

		// Инициализация шрифта
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("Roboto-Italic.ttf"));
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.size = 36;
		parameter.color = com.badlogic.gdx.graphics.Color.BLACK;
		font = generator.generateFont(parameter);
		generator.dispose();

		createWalls();

		Gdx.input.setInputProcessor(new InputAdapter() {
			@Override
			public boolean touchDown(int screenX, int screenY, int pointer, int button) {
				final Vector2 touchPoint = new Vector2(screenX / PIXELS_PER_METER, (Gdx.graphics.getHeight() - screenY) / PIXELS_PER_METER);

				// Проверка нажатия на шарик
				final boolean[] touchedBall = {false};
				world.QueryAABB(new QueryCallback() {
					@Override
					public boolean reportFixture(Fixture fixture) {
						if ("ball".equals(fixture.getUserData())) {
							if (fixture.testPoint(touchPoint.x, touchPoint.y)) {
								fixture.getBody().setUserData("remove");
								score++;
								touchedBall[0] = true;
								return false; // прекратить проверку
							}
						}
						return true;
					}
				}, touchPoint.x - 0.01f, touchPoint.y - 0.01f, touchPoint.x + 0.01f, touchPoint.y + 0.01f);

				// Если не было нажатия на шарик, создаем новый шарик
				if (!touchedBall[0]) {
					createBall(screenX, Gdx.graphics.getHeight() - screenY);
				}

				return true;
			}
		});
	}

	private void createBall(float x, float y) {
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyDef.BodyType.DynamicBody;
		bodyDef.position.set(x / PIXELS_PER_METER, y / PIXELS_PER_METER);

		CircleShape shape = new CircleShape();
		shape.setRadius(BALL_RADIUS);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = shape;
		fixtureDef.density = 1.0f;
		fixtureDef.friction = 0.2f;
		fixtureDef.restitution = 0.5f;

		Body body = world.createBody(bodyDef);
		Fixture fixture = body.createFixture(fixtureDef);
		fixture.setUserData("ball");

		shape.dispose();

		balls.add(body);
	}

	private void createWalls() {
		float w = Gdx.graphics.getWidth() / PIXELS_PER_METER;
		float h = Gdx.graphics.getHeight() / PIXELS_PER_METER;

		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyDef.BodyType.StaticBody;

		// Create floor
		EdgeShape shape = new EdgeShape();
		shape.set(new Vector2(0, 0), new Vector2(w, 0));
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = shape;
		Body floor = world.createBody(bodyDef);
		floor.createFixture(fixtureDef);

		// Create ceiling
		shape.set(new Vector2(0, h), new Vector2(w, h));
		Body ceiling = world.createBody(bodyDef);
		ceiling.createFixture(fixtureDef);

		// Create left wall
		shape.set(new Vector2(0, 0), new Vector2(0, h));
		Body leftWall = world.createBody(bodyDef);
		leftWall.createFixture(fixtureDef);

		// Create right wall
		shape.set(new Vector2(w, 0), new Vector2(w, h));
		Body rightWall = world.createBody(bodyDef);
		rightWall.createFixture(fixtureDef);

		shape.dispose();
	}

	@Override
	public void render() {
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		world.step(1 / 60f, 6, 2);

		// Remove bodies marked for removal
		Array<Body> bodiesToRemove = new Array<>();
		for (Body body : balls) {
			if ("remove".equals(body.getUserData())) {
				bodiesToRemove.add(body);
			}
		}
		for (Body body : bodiesToRemove) {
			balls.removeValue(body, true);
			world.destroyBody(body);
		}

		batch.begin();
		batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		for (Body body : balls) {
			Vector2 position = body.getPosition();
			batch.draw(ballTexture, position.x * PIXELS_PER_METER - BALL_DIAMETER_PIXELS / 2, position.y * PIXELS_PER_METER - BALL_DIAMETER_PIXELS / 2, BALL_DIAMETER_PIXELS, BALL_DIAMETER_PIXELS);
		}
		font.draw(batch, "Score: " + score, Gdx.graphics.getWidth() / 2 - 50, Gdx.graphics.getHeight() - 20);
		batch.end();

		debugRenderer.render(world, batch.getProjectionMatrix().cpy().scale(PIXELS_PER_METER, PIXELS_PER_METER, 0));
	}

	@Override
	public void dispose() {
		batch.dispose();
		ballTexture.dispose();
		backgroundTexture.dispose();
		font.dispose();
		world.dispose();
		debugRenderer.dispose();
	}
}
