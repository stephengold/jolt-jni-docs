/*
 Copyright (c) 2020-2025 Stephen Gold and Yanis Boudiaf

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. Neither the name of the copyright holder nor the names of its
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.stephengold.sportjolt.javaapp.sample;

import com.github.stephengold.joltjni.AllHitCollideShapeCollector;
import com.github.stephengold.joltjni.Body;
import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.BodyInterface;
import com.github.stephengold.joltjni.BroadPhaseLayerFilter;
import com.github.stephengold.joltjni.CapsuleShape;
import com.github.stephengold.joltjni.CharacterRef;
import com.github.stephengold.joltjni.CharacterSettings;
import com.github.stephengold.joltjni.CollideShapeSettings;
import com.github.stephengold.joltjni.ConvexHullShapeSettings;
import com.github.stephengold.joltjni.ObjectLayerFilter;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Plane;
import com.github.stephengold.joltjni.PlaneShape;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RMat44;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.SpecifiedObjectLayerFilter;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.operator.Op;
import com.github.stephengold.joltjni.readonly.ConstNarrowPhaseQuery;
import com.github.stephengold.joltjni.readonly.ConstPlane;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.joltjni.readonly.RMat44Arg;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import com.github.stephengold.joltjni.readonly.Vec3Arg;
import com.github.stephengold.sportjolt.Constants;
import com.github.stephengold.sportjolt.Geometry;
import com.github.stephengold.sportjolt.Mesh;
import com.github.stephengold.sportjolt.TextureKey;
import com.github.stephengold.sportjolt.input.InputProcessor;
import com.github.stephengold.sportjolt.input.RotateMode;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.PhysicsTickListener;
import com.github.stephengold.sportjolt.physics.ShapeSummary;
import org.lwjgl.glfw.GLFW;

/**
 * A simple example of a narrow-phase query.
 * <p>
 * Press the arrow keys to walk. Press the space bar to jump.
 * <p>
 * Builds upon HelloBroadPhase.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class HelloNarrowPhase
        extends BasePhysicsApp
        implements PhysicsTickListener {
    // *************************************************************************
    // constants

    /**
     * location of the ghost's center
     */
    final private static Vec3Arg ghostCenter = new Vec3(15f, 0f, -13f);
    // *************************************************************************
    // fields

    /**
     * reusable hit collector
     */
    private static AllHitCollideShapeCollector collector;
    /**
     * true when the space bar is pressed, otherwise false
     */
    private static volatile boolean jumpRequested;
    /**
     * true when the DOWN key is pressed, otherwise false
     */
    private static volatile boolean walkBackward;
    /**
     * true when the UP key is pressed, otherwise false
     */
    private static volatile boolean walkForward;
    /**
     * true when the LEFT key is pressed, otherwise false
     */
    private static volatile boolean walkLeft;
    /**
     * true when the RIGHT key is pressed, otherwise false
     */
    private static volatile boolean walkRight;
    /**
     * broadphase-layer filter that has no effect
     */
    private static BroadPhaseLayerFilter filterBpLayerNoOp;
    /**
     * character to trigger the ghost shape
     */
    private static CharacterRef character;
    /**
     * ghost shape for detecting intrusions
     */
    private static ConstShape ghost;
    /**
     * visualize the ghost shape
     */
    private static Geometry ghostGeometry;
    /**
     * object-layer filter that accepts only moving bodies
     */
    private static ObjectLayerFilter filterObjLayerMoving;
    // *************************************************************************
    // constructors

    /**
     * Instantiate the HelloNarrowphase application.
     * <p>
     * This no-arg constructor was made explicit to avoid javadoc warnings from
     * JDK 18+.
     */
    public HelloNarrowPhase() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloNarrowphase application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        HelloNarrowPhase application = new HelloNarrowPhase();
        application.start();
    }
    // *************************************************************************
    // BasePhysicsApp methods

    /**
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new object
     */
    @Override
    protected PhysicsSystem createSystem() {
        // For simplicity, use a single broadphase layer:
        int maxBodies = 2;
        int numBpLayers = 1;
        PhysicsSystem result = createSystem(maxBodies, numBpLayers);

        // To enable the callbacks, register this app as a tick listener:
        addTickListener(this);

        return result;
    }

    /**
     * Initialize the application. Invoked once.
     */
    @Override
    protected void initialize() {
        super.initialize();

        setVsync(true);
        configureCamera();
        configureInput();
        configureLighting();

        // Initialize collector and filters:
        collector = new AllHitCollideShapeCollector();
        filterBpLayerNoOp = new BroadPhaseLayerFilter();
        filterObjLayerMoving = new SpecifiedObjectLayerFilter(objLayerMoving);

        // Create a ghost shape:
        float radius = 10f;
        ConvexHullShapeSettings ss = new ConvexHullShapeSettings(
                new Vec3(radius, 0f, 0f), new Vec3(-radius, 0f, 0f),
                new Vec3(0f, radius, 0f), new Vec3(0f, -radius, 0f),
                new Vec3(0f, 0f, radius), new Vec3(0f, 0f, -radius));
        ghost = ss.create().get();

        // Visualize the ghost shape:
        ShapeSummary summary = new ShapeSummary(ghost, "low/Facet");
        Mesh mesh = meshForShape(ghost, summary);
        ghostGeometry = new Geometry(mesh)
                .setLocation(ghostCenter);
    }

    /**
     * Populate the PhysicsSystem. Invoked once during initialization.
     */
    @Override
    protected void populateSystem() {
        // Create a character with a capsule shape and add it to the system:
        float capsuleRadius = 3f; // meters
        float capsuleHeight = 4f; // meters
        ConstShape shape = new CapsuleShape(capsuleHeight / 2f, capsuleRadius);

        CharacterSettings settings = new CharacterSettings();
        settings.setShape(shape);

        RVec3Arg startLocation = new RVec3(0., 3., 0.);
        long userData = 0L;
        character = new com.github.stephengold.joltjni.Character(
                settings, startLocation, new Quat(), userData, physicsSystem)
                .toRef();
        character.addToPhysicsSystem();

        // Visualize the character:
        visualizeShape(character);

        // Add a plane to represent the ground:
        float groundY = -2f;
        addPlane(groundY);
    }
    // *************************************************************************
    // PhysicsTickListener methods

    /**
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) after the system
     * has been stepped.
     *
     * @param system the system that was just stepped (not null)
     * @param timeStep the duration of the simulation step (in seconds, &ge;0)
     */
    @Override
    public void physicsTick(PhysicsSystem system, float timeStep) {
        // Update the character:
        float maxSeparation = 0.1f; // meters above the ground
        character.postSimulation(maxSeparation);

        // Collect all movable bodies intersecting the ghost shape:
        ConstNarrowPhaseQuery query = physicsSystem.getNarrowPhaseQuery();
        Vec3Arg scale = Vec3.sOne();
        RMat44Arg transform = RMat44.sTranslation(ghostCenter.toRVec3());
        CollideShapeSettings settings = new CollideShapeSettings();
        RVec3Arg base = ghostCenter.toRVec3();
        collector.reset();
        query.collideShape(ghost, scale, transform, settings, base,
                collector, filterBpLayerNoOp, filterObjLayerMoving);

        // Update the color of the ghost:
        int numHits = collector.countHits();
        if (numHits > 0) {
            // Intruder detected!
            ghostGeometry.setColor(Constants.RED);
        } else {
            ghostGeometry.setColor(Constants.YELLOW);
        }
    }

    /**
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system
     * is stepped.
     *
     * @param system the system that's about to be stepped (not null)
     * @param timeStep the duration of the simulation step (in seconds, &ge;0)
     */
    @Override
    public void prePhysicsTick(PhysicsSystem system, float timeStep) {
        Vec3 velocity = character.getLinearVelocity();

        // Clear any horizontal motion from the previous simulation step:
        velocity.setX(0f);
        velocity.setZ(0f);

        // If the character is supported, cause it respond to keyboard input:
        if (character.isSupported()) {
            if (jumpRequested) {
                // Cause the character to jump:
                velocity.setY(18f);

            } else {
                // Walk as directed by the arrow keys:
                Vec3 component1 = cam.getDirection();
                float backward = walkBackward ? 1f : 0f;
                float forward = walkForward ? 1f : 0f;
                component1.scaleInPlace(forward - backward);

                float right = walkRight ? 1f : 0f;
                float left = walkLeft ? 1f : 0f;
                Vec3 component2 = cam.getRight();
                component2.scaleInPlace(right - left);
                Op.assign(velocity, Op.plus(component1, component2));

                velocity.setY(0f);
                if (velocity.length() > 0f) {
                    float scale = 7f / velocity.length();
                    velocity.scaleInPlace(scale);
                }
            }
        }

        character.setLinearVelocity(velocity);
    }
    // *************************************************************************
    // private methods

    /**
     * Add a static horizontal plane body to the system.
     *
     * @param y the desired elevation (in system coordinates)
     */
    private void addPlane(float y) {
        ConstPlane plane = new Plane(0f, 1f, 0f, -y);
        PlaneShape shape = new PlaneShape(plane);
        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setMotionType(EMotionType.Static);
        bcs.setObjectLayer(objLayerNonMoving);
        bcs.setShape(shape);

        BodyInterface bi = physicsSystem.getBodyInterface();
        Body body = bi.createBody(bcs);
        bi.addBody(body, EActivation.DontActivate);

        // Visualize the body:
        String resourceName = "/Textures/greenTile.png";
        float maxAniso = 16f;
        TextureKey textureKey
                = new TextureKey("classpath://" + resourceName, maxAniso);
        visualizeShape(body, 0.1f)
                .setSpecularColor(Constants.DARK_GRAY)
                .setTexture(textureKey);
    }

    /**
     * Configure the camera, projection, and CIP during initialization.
     */
    private static void configureCamera() {
        getCameraInputProcessor().setRotationMode(RotateMode.DragLMB);
        cam.setAzimuth(-1.9f)
                .setLocation(35f, 35f, 60f)
                .setUpAngle(-0.5f);
        getProjection().setFovyDegrees(30f);
    }

    /**
     * Configure keyboard input during initialization.
     */
    private void configureInput() {
        getInputManager().add(new InputProcessor() {
            @Override
            public void onKeyboard(int glfwKeyId, boolean isPressed) {
                switch (glfwKeyId) {
                    case GLFW.GLFW_KEY_SPACE:
                        jumpRequested = isPressed;
                        return;

                    case GLFW.GLFW_KEY_DOWN:
                        walkBackward = isPressed;
                        return;
                    case GLFW.GLFW_KEY_LEFT:
                        walkLeft = isPressed;
                        return;
                    case GLFW.GLFW_KEY_RIGHT:
                        walkRight = isPressed;
                        return;
                    case GLFW.GLFW_KEY_UP:
                        walkForward = isPressed;
                        return;

                    default:
                }
                super.onKeyboard(glfwKeyId, isPressed);
            }
        });
    }

    /**
     * Configure lighting and the background color.
     */
    private void configureLighting() {
        setLightDirection(7f, 3f, 5f);

        // Set the background color to light blue:
        setBackgroundColor(Constants.SKY_BLUE);
    }
}
