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
import com.github.stephengold.joltjni.BodyFilter;
import com.github.stephengold.joltjni.BodyInterface;
import com.github.stephengold.joltjni.BoxShape;
import com.github.stephengold.joltjni.BroadPhaseLayerFilter;
import com.github.stephengold.joltjni.CapsuleShape;
import com.github.stephengold.joltjni.CharacterVirtual;
import com.github.stephengold.joltjni.CharacterVirtualRef;
import com.github.stephengold.joltjni.CharacterVirtualSettings;
import com.github.stephengold.joltjni.CharacterVirtualSettingsRef;
import com.github.stephengold.joltjni.ExtendedUpdateSettings;
import com.github.stephengold.joltjni.ObjectLayerFilter;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.ShapeFilter;
import com.github.stephengold.joltjni.ShapeRefC;
import com.github.stephengold.joltjni.TempAllocator;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.readonly.ConstBody;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import com.github.stephengold.joltjni.readonly.Vec3Arg;
import com.github.stephengold.sportjolt.Constants;
import com.github.stephengold.sportjolt.input.RotateMode;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.PhysicsTickListener;

/**
 * A simple example of character physics.
 * <p>
 * Builds upon HelloCharacterVirtual.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class HelloCharacterVirtual
        extends BasePhysicsApp
        implements PhysicsTickListener {
    // *************************************************************************
    // fields

    /**
     * body filter for character collisions
     */
    private static BodyFilter allBodies;
    /**
     * character being tested
     */
    private static CharacterVirtualRef character;
    /**
     * settings for updating the character
     */
    private static ExtendedUpdateSettings updateSettings;
    /**
     * shape filter for character collisions
     */
    private static ShapeFilter allShapes;
    // *************************************************************************
    // constructors

    /**
     * Instantiate the HelloCharacterVirtual application.
     * <p>
     * This no-arg constructor was made explicit to avoid javadoc warnings from
     * JDK 18+.
     */
    public HelloCharacterVirtual() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloCharacterVirtual application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        HelloCharacterVirtual application = new HelloCharacterVirtual();
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
        int maxBodies = 1;
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
        getCameraInputProcessor().setRotationMode(RotateMode.DragLMB);
        setBackgroundColor(Constants.SKY_BLUE);

        updateSettings = new ExtendedUpdateSettings();
        updateSettings.setStickToFloorStepDown(Vec3.sZero());
        updateSettings.setWalkStairsStepUp(Vec3.sZero());

        allBodies = new BodyFilter();
        allShapes = new ShapeFilter();
    }

    /**
     * Populate the PhysicsSystem. Invoked once during initialization.
     */
    @Override
    protected void populateSystem() {
        // Create a character with a capsule shape:
        float capsuleRadius = 0.5f; // meters
        float capsuleHeight = 1f; // meters
        ConstShape sh = new CapsuleShape(capsuleHeight / 2f, capsuleRadius);
        ShapeRefC shape = sh.toRefC();

        CharacterVirtualSettingsRef settings
                = new CharacterVirtualSettings().toRef();
        settings.setShape(shape);
        assert settings.getShape().equals(sh);

        RVec3Arg startLocation = new RVec3(0., 2., 0.);
        long userData = 0L;
        character = new CharacterVirtual(
                settings, startLocation, new Quat(), userData, physicsSystem)
                .toRef();

        // Add a static square to represent the ground:
        float halfExtent = 4f;
        float y = -2f;
        ConstBody ground = addSquare(halfExtent, y);

        // Visualize the shapes of both physics objects:
        visualizeShape(character);
        visualizeShape(ground);
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
        Vec3Arg gravity = system.getGravity();
        BroadPhaseLayerFilter bplFilter
                = system.getDefaultBroadPhaseLayerFilter(objLayerMoving);
        ObjectLayerFilter olFilter
                = system.getDefaultLayerFilter(objLayerMoving);
        TempAllocator tempAllocator = getTempAllocator();
        character.extendedUpdate(timeStep, gravity, updateSettings, bplFilter,
                olFilter, allBodies, allShapes, tempAllocator);
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

        // Apply gravity:
        velocity.setY(velocity.getY() - 9.81f * timeStep);

        // If the character is supported, cause it to jump:
        if (character.isSupported()) {
            velocity.set(0f, 8f, 0f);
        }

        character.setLinearVelocity(velocity);
    }
    // *************************************************************************
    // private methods

    /**
     * Add a static horizontal-square rigid body to the system.
     *
     * @param halfExtent half of the desired side length (in meters)
     * @param y the desired elevation of the body's upper top face (in system
     * coordinates)
     * @return the new body (not null)
     */
    private ConstBody addSquare(float halfExtent, float y) {
        // Create a static rigid body with a square shape:
        float halfThickness = 0.1f;
        ShapeRefC shape
                = new BoxShape(halfExtent, halfThickness, halfExtent).toRefC();
        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setMotionType(EMotionType.Static);
        bcs.setObjectLayer(objLayerNonMoving);
        bcs.setPosition(0., y - halfThickness, 0.);
        bcs.setShape(shape);

        BodyInterface bi = physicsSystem.getBodyInterface();
        ConstBody result = bi.createBody(bcs);
        bi.addBody(result, EActivation.DontActivate);

        return result;
    }
}
