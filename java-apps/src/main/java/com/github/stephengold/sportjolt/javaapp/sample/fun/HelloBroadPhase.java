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
package com.github.stephengold.sportjolt.javaapp.sample.fun;

import com.github.stephengold.joltjni.AaBox;
import com.github.stephengold.joltjni.AllHitCollideShapeBodyCollector;
import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.BodyInterface;
import com.github.stephengold.joltjni.BroadPhaseLayerFilter;
import com.github.stephengold.joltjni.CapsuleShape;
import com.github.stephengold.joltjni.CharacterSettings;
import com.github.stephengold.joltjni.ObjectLayerFilter;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Plane;
import com.github.stephengold.joltjni.PlaneShape;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.SpecifiedObjectLayerFilter;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.operator.Op;
import com.github.stephengold.joltjni.readonly.ConstAaBox;
import com.github.stephengold.joltjni.readonly.ConstBody;
import com.github.stephengold.joltjni.readonly.ConstBroadPhaseQuery;
import com.github.stephengold.joltjni.readonly.ConstPlane;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import com.github.stephengold.joltjni.readonly.Vec3Arg;
import com.github.stephengold.sportjolt.BaseApplication;
import com.github.stephengold.sportjolt.Camera;
import com.github.stephengold.sportjolt.Constants;
import com.github.stephengold.sportjolt.Geometry;
import com.github.stephengold.sportjolt.TextureKey;
import com.github.stephengold.sportjolt.input.InputProcessor;
import com.github.stephengold.sportjolt.input.RotateMode;
import com.github.stephengold.sportjolt.mesh.BoxMesh;
import com.github.stephengold.sportjolt.physics.AabbGeometry;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.FunctionalPhysicsApp;
import org.lwjgl.glfw.GLFW;

/**
 * A simple example of a broadphase query.
 * <p>
 * Press the arrow keys to walk. Press the space bar to jump.
 * <p>
 * Builds upon HelloSensor.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class HelloBroadPhase {
    // *************************************************************************
    // fields

    /**
     * reusable hit collector
     */
    private static AllHitCollideShapeBodyCollector collector;
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
     * character to trigger the ghost box
     */
    private static com.github.stephengold.joltjni.Character character;
    /**
     * ghost box for detecting intrusions
     */
    private static ConstAaBox ghost;
    /**
     * visualize the ghost box
     */
    private static Geometry ghostGeometry;
    /**
     * object-layer filter that accepts only moving bodies
     */
    private static ObjectLayerFilter filterObjLayerMoving;
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private HelloBroadPhase() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloBroadPhase application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        FunctionalPhysicsApp fpa = new FunctionalPhysicsApp();

        fpa.setCreateSystem((app) -> {
            // For simplicity, use a single broadphase layer:
            int maxBodies = 2;
            int numBpLayers = 1;
            PhysicsSystem result = app.createSystem(maxBodies, numBpLayers);

            return result;
        });

        fpa.setInitialize((app) -> {
            BaseApplication.setVsync(true);
            configureCamera();
            configureInput();
            configureLighting();

            // Initialize collector and filters:
            collector = new AllHitCollideShapeBodyCollector();
            filterBpLayerNoOp = new BroadPhaseLayerFilter();
            filterObjLayerMoving = new SpecifiedObjectLayerFilter(
                    BasePhysicsApp.objLayerMoving);

            // Create a ghost box:
            Vec3Arg center = new Vec3(15f, 0f, -13f);
            float radius = 10f;
            ghost = new AaBox(center, radius);

            // Visualize the ghost box:
            BoxMesh boxMesh = BoxMesh.getMesh();
            ghostGeometry = new Geometry(boxMesh)
                    .setLocation(center)
                    .setScale(radius);
        });

        fpa.setPopulateSystem((app) -> {
            PhysicsSystem physicsSystem = app.getPhysicsSystem();
            BodyInterface bi = physicsSystem.getBodyInterface();

            // Create a character with a capsule shape and add it to the system:
            float capsuleRadius = 3f; // meters
            float capsuleHeight = 4f; // meters
            ConstShape shape
                    = new CapsuleShape(capsuleHeight / 2f, capsuleRadius);

            CharacterSettings settings = new CharacterSettings();
            settings.setShape(shape);

            RVec3Arg startLocation = new RVec3(0., 3., 0.);
            long userData = 0L;
            character = new com.github.stephengold.joltjni.Character(settings,
                    startLocation, new Quat(), userData, physicsSystem);
            character.addToPhysicsSystem();

            // Visualize the character:
            BasePhysicsApp.visualizeShape(character);
            new AabbGeometry(character); // outline the character's AABB

            // Add a plane to represent the ground:
            float groundY = -2f;
            addPlane(bi, groundY);
        });

        fpa.setPostPhysicsTick((app, system, timeStep) -> {
            // Update the character:
            float maxSeparation = 0.1f; // meters above the ground
            character.postSimulation(maxSeparation);

            // Collect all movable bodies with AABBs overlapping the ghost box:
            ConstBroadPhaseQuery query = system.getBroadPhaseQuery();
            collector.reset();
            query.collideAaBox(
                    ghost, collector, filterBpLayerNoOp, filterObjLayerMoving);

            // Update the color of the ghost:
            int numHits = collector.countHits();
            if (numHits > 0) {
                // Intruder detected!
                ghostGeometry.setColor(Constants.RED);
            } else {
                ghostGeometry.setColor(Constants.YELLOW);
            }
        });

        fpa.setPrePhysicsTick((app, system, timeStep) -> {
            Vec3 velocity = character.getLinearVelocity();

            // Clear any horizontal motion from the previous simulation step:
            velocity.setX(0f);
            velocity.setZ(0f);

            // If the character is supported, make it respond to keyboard input:
            if (character.isSupported()) {
                if (jumpRequested) {
                    // Cause the character to jump:
                    velocity.setY(18f);

                } else {
                    Camera cam = BaseApplication.getCamera();

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
        });

        fpa.start("HelloBroadPhase");
    }
    // *************************************************************************
    // private methods

    /**
     * Add a static horizontal plane body to the system.
     *
     * @param bi the system's body interface (not {@code null})
     * @param y the desired elevation (in system coordinates)
     */
    private static void addPlane(BodyInterface bi, float y) {
        ConstPlane plane = new Plane(0f, 1f, 0f, -y);
        ConstShape shape = new PlaneShape(plane);
        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setMotionType(EMotionType.Static);
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving);
        bcs.setShape(shape);

        ConstBody body = bi.createBody(bcs);
        bi.addBody(body, EActivation.DontActivate);

        // Visualize the body:
        String resourceName = "/Textures/greenTile.png";
        float maxAniso = 16f;
        TextureKey textureKey
                = new TextureKey("classpath://" + resourceName, maxAniso);
        BasePhysicsApp.visualizeShape(body, 0.1f)
                .setSpecularColor(Constants.DARK_GRAY)
                .setTexture(textureKey);
    }

    /**
     * Configure the camera, projection, and CIP during initialization.
     */
    private static void configureCamera() {
        BasePhysicsApp.getCameraInputProcessor()
                .setRotationMode(RotateMode.DragLMB);
        BasePhysicsApp.getCamera()
                .setAzimuth(-1.9f)
                .setLocation(35f, 35f, 60f)
                .setUpAngle(-0.5f);
        BasePhysicsApp.getProjection()
                .setFovyDegrees(30f);
    }

    /**
     * Configure keyboard input during initialization.
     */
    private static void configureInput() {
        BasePhysicsApp.getInputManager().add(new InputProcessor() {
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
    private static void configureLighting() {
        BasePhysicsApp.setLightDirection(7f, 3f, 5f);

        // Set the background color to light blue:
        BasePhysicsApp.setBackgroundColor(Constants.SKY_BLUE);
    }
}
