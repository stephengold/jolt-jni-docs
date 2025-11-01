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

import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.BodyFilter;
import com.github.stephengold.joltjni.BodyInterface;
import com.github.stephengold.joltjni.BoxShape;
import com.github.stephengold.joltjni.BroadPhaseLayerFilter;
import com.github.stephengold.joltjni.CapsuleShape;
import com.github.stephengold.joltjni.CharacterVirtual;
import com.github.stephengold.joltjni.CharacterVirtualSettings;
import com.github.stephengold.joltjni.ExtendedUpdateSettings;
import com.github.stephengold.joltjni.ObjectLayerFilter;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.ShapeFilter;
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
import com.github.stephengold.sportjolt.physics.FunctionalPhysicsApp;

/**
 * A simple example of character physics.
 * <p>
 * Builds upon HelloCharacterVirtual.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class HelloCharacterVirtual {
    // *************************************************************************
    // fields

    /**
     * body filter for character collisions
     */
    private static BodyFilter allBodies;
    /**
     * character being tested
     */
    private static CharacterVirtual character;
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
     * A private constructor to inhibit instantiation of this class.
     */
    private HelloCharacterVirtual() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloCharacterVirtual application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        FunctionalPhysicsApp fpa = new FunctionalPhysicsApp();

        fpa.setCreateSystem((app) -> {
            // For simplicity, use a single broadphase layer:
            int maxBodies = 1;
            int numBpLayers = 1;
            PhysicsSystem result = app.createSystem(maxBodies, numBpLayers);

            return result;
        });

        fpa.setInitialize((app) -> {
            BasePhysicsApp.setVsync(true);
            BasePhysicsApp.getCameraInputProcessor()
                    .setRotationMode(RotateMode.DragLMB);
            BasePhysicsApp.setBackgroundColor(Constants.SKY_BLUE);

            updateSettings = new ExtendedUpdateSettings();
            updateSettings.setStickToFloorStepDown(Vec3.sZero());
            updateSettings.setWalkStairsStepUp(Vec3.sZero());

            allBodies = new BodyFilter();
            allShapes = new ShapeFilter();
        });

        fpa.setPopulateSystem((app) -> {
            PhysicsSystem physicsSystem = app.getPhysicsSystem();
            BodyInterface bi = physicsSystem.getBodyInterface();

            // Create a character with a capsule shape:
            float capsuleRadius = 0.5f; // meters
            float capsuleHeight = 1f; // meters
            ConstShape shape
                    = new CapsuleShape(capsuleHeight / 2f, capsuleRadius);

            CharacterVirtualSettings settings = new CharacterVirtualSettings();
            settings.setShape(shape);
            assert settings.getShape().equals(shape);

            RVec3Arg startLocation = new RVec3(0., 2., 0.);
            long userData = 0L;
            character = new CharacterVirtual(settings, startLocation,
                    new Quat(), userData, physicsSystem);

            // Add a static square to represent the ground:
            float halfExtent = 4f;
            float y = -2f;
            ConstBody ground = addSquare(bi, halfExtent, y);

            // Visualize the shapes of both physics objects:
            BasePhysicsApp.visualizeShape(character);
            BasePhysicsApp.visualizeShape(ground);
        });

        fpa.setPostPhysicsTick((app, system, timeStep) -> {
            // Update the character:
            Vec3Arg gravity = system.getGravity();
            BroadPhaseLayerFilter bplFilter
                    = system.getDefaultBroadPhaseLayerFilter(
                            BasePhysicsApp.objLayerMoving);
            ObjectLayerFilter olFilter
                    = system.getDefaultLayerFilter(
                            BasePhysicsApp.objLayerMoving);
            TempAllocator tempAllocator = app.getTempAllocator();
            character.extendedUpdate(timeStep, gravity, updateSettings,
                    bplFilter, olFilter, allBodies, allShapes, tempAllocator);
        });

        fpa.setPrePhysicsTick((app, system, timeStep) -> {
            Vec3 velocity = character.getLinearVelocity();

            // Apply gravity:
            velocity.setY(velocity.getY() - 9.81f * timeStep);

            // If the character is supported, cause it to jump:
            if (character.isSupported()) {
                velocity.set(0f, 8f, 0f);
            }

            character.setLinearVelocity(velocity);
        });

        fpa.start("HelloCharacterVirtual");
    }
    // *************************************************************************
    // private methods

    /**
     * Add a static horizontal-square rigid body to the system.
     *
     * @param bi the system's body interface (not {@code null})
     * @param halfExtent half of the desired side length (in meters)
     * @param y the desired elevation of the body's upper top face (in system
     * coordinates)
     * @return the new body (not null)
     */
    private static ConstBody addSquare(
            BodyInterface bi, float halfExtent, float y) {
        // Create a static rigid body with a square shape:
        float halfThickness = 0.1f;
        ConstShape shape = new BoxShape(halfExtent, halfThickness, halfExtent);
        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setMotionType(EMotionType.Static);
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving);
        bcs.setPosition(0., y - halfThickness, 0.);
        bcs.setShape(shape);

        ConstBody result = bi.createBody(bcs);
        bi.addBody(result, EActivation.DontActivate);

        return result;
    }
}
