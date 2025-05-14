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

import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.BodyInterface;
import com.github.stephengold.joltjni.CapsuleShape;
import com.github.stephengold.joltjni.CharacterRef;
import com.github.stephengold.joltjni.CharacterSettings;
import com.github.stephengold.joltjni.HeightFieldShapeSettings;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.ShapeRefC;
import com.github.stephengold.joltjni.ShapeSettings;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.readonly.ConstBody;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import com.github.stephengold.joltjni.readonly.Vec3Arg;
import com.github.stephengold.sportjolt.Constants;
import com.github.stephengold.sportjolt.Utils;
import com.github.stephengold.sportjolt.input.InputProcessor;
import com.github.stephengold.sportjolt.input.RotateMode;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.PhysicsTickListener;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.lwjgl.glfw.GLFW;

/**
 * A simple example of character physics.
 * <p>
 * Press the W key to walk. Press the space bar to jump.
 * <p>
 * Builds upon HelloCharacter.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelloWalk extends BasePhysicsApp implements PhysicsTickListener {
    // *************************************************************************
    // fields

    /**
     * true when the spacebar is pressed, otherwise false
     */
    private static volatile boolean jumpRequested;
    /**
     * true when the W key is pressed, otherwise false
     */
    private static volatile boolean walkRequested;
    /**
     * character being tested
     */
    private static CharacterRef character;
    // *************************************************************************
    // constructors

    /**
     * Instantiate the HelloWalk application.
     * <p>
     * This no-arg constructor was made explicit to avoid javadoc warnings from
     * JDK 18+.
     */
    public HelloWalk() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloWalk application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        HelloWalk application = new HelloWalk();
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

        // To enable the callbacks, register the application as a tick listener:
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

        RVec3Arg startLocation = new RVec3(-73.6, 19.09, -45.58);
        long userData = 0L;
        character = new com.github.stephengold.joltjni.Character(
                settings, startLocation, new Quat(), userData, physicsSystem)
                .toRef();
        character.addToPhysicsSystem();

        // Add a static heightmap to represent the ground:
        ConstBody ground = addTerrain();

        // Visualize the shapes of both physics objects:
        visualizeShape(character);
        Vector4fc darkGreen = new Vector4f(0f, 0.3f, 0f, 1f);
        visualizeShape(ground)
                .setColor(darkGreen)
                .setSpecularColor(Constants.BLACK);
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

        RVec3Arg location = character.getPosition();
        cam.setLocation(location.toVec3());
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
                velocity.setY(8f);

            } else if (walkRequested) {
                // Walk in the camera's forward direction:
                Vec3Arg forward = cam.getDirection();
                float walkSpeed = 7f;
                velocity.setX(walkSpeed * forward.getX());
                velocity.setZ(walkSpeed * forward.getZ());
            }
        }

        character.setLinearVelocity(velocity);
    }
    // *************************************************************************
    // private methods

    /**
     * Add a static heightfield rigid body to the system.
     *
     * @return the new body (not null)
     */
    private ConstBody addTerrain() {
        // Generate an array of heights from a PNG image on the classpath:
        String resourceName = "/Textures/Terrain/splat/mountains512.png";
        BufferedImage image = Utils.loadResourceAsImage(resourceName);

        float maxHeight = 51f;
        FloatBuffer heightBuffer = Utils.toHeightBuffer(image, maxHeight);

        // Construct a static rigid body based on the array of heights:
        int numFloats = heightBuffer.capacity();

        Vec3Arg offset = new Vec3(-256f, 0f, -256f);
        Vec3Arg scale = new Vec3(1f, 1f, 1f);
        int sampleCount = 512;
        assert numFloats == sampleCount * sampleCount : numFloats;
        ShapeSettings ss = new HeightFieldShapeSettings(
                heightBuffer, offset, scale, sampleCount);

        ShapeRefC shapeRef = ss.create().get();
        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setMotionType(EMotionType.Static);
        bcs.setObjectLayer(objLayerNonMoving);
        bcs.setShape(shapeRef);

        BodyInterface bi = physicsSystem.getBodyInterface();
        ConstBody result = bi.createBody(bcs);
        bi.addBody(result, EActivation.DontActivate);

        return result;
    }

    /**
     * Configure the projection and CIP during startup.
     */
    private static void configureCamera() {
        getCameraInputProcessor().setRotationMode(RotateMode.DragLMB);
        getProjection().setFovyDegrees(30f);

        // Bring the near plane closer to reduce clipping:
        getProjection().setZClip(0.1f, 1_000f);
    }

    /**
     * Configure keyboard input during startup.
     */
    private void configureInput() {
        getInputManager().add(new InputProcessor() {
            @Override
            public void onKeyboard(int glfwKeyId, boolean isPressed) {
                switch (glfwKeyId) {
                    case GLFW.GLFW_KEY_SPACE:
                        jumpRequested = isPressed;
                        return;

                    case GLFW.GLFW_KEY_W:
                        walkRequested = isPressed;
                        // This overrides the CameraInputProcessor.
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
        setLightColor(0.3f, 0.3f, 0.3f);
        setLightDirection(7f, 3f, 5f);

        // Set the background color to light blue:
        setBackgroundColor(Constants.SKY_BLUE);
    }
}
