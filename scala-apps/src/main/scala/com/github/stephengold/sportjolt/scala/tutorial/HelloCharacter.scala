/*
 Copyright (c) 2026 Stephen Gold and Yanis Boudiaf

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
package com.github.stephengold.sportjolt.scala.tutorial

import com.github.stephengold.joltjni.Body
import com.github.stephengold.joltjni.BodyCreationSettings
import com.github.stephengold.joltjni.BodyInterface
import com.github.stephengold.joltjni.BoxShape
import com.github.stephengold.joltjni.CapsuleShape
import com.github.stephengold.joltjni.CharacterSettings
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.Quat
import com.github.stephengold.joltjni.RVec3
import com.github.stephengold.joltjni.Vec3
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.sportjolt.BaseApplication
import com.github.stephengold.sportjolt.Constants
import com.github.stephengold.sportjolt.input.RotateMode
import com.github.stephengold.sportjolt.physics.BasePhysicsApp
import com.github.stephengold.sportjolt.physics.PhysicsTickListener

/**
 * A simple example of character physics.
 * <p>
 * Builds upon HelloStaticBody.
 *
 * @author Stephen Gold sgold@sonic.net
 */
object HelloCharacter {
    // *************************************************************************
    // fields

    /**
     * character being tested
     */
    private var character: com.github.stephengold.joltjni.Character = null
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloCharacter application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    def main(arguments: Array[String]): Unit = {
        val application = new HelloCharacter
        application.start
    }
}

class HelloCharacter extends BasePhysicsApp, PhysicsTickListener {
    /**
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new object
     */
    override def createSystem: PhysicsSystem = {
        // For simplicity, use a single broadphase layer:
        val maxBodies = 2
        val numBpLayers = 1
        val result = createSystem(maxBodies, numBpLayers)

        // To enable the callbacks, register the application as a tick listener:
        addTickListener(this)

        return result
    }

    /**
     * Initialize the application. Invoked once.
     */
    override def initialize: Unit = {
        super.initialize

        BaseApplication.setVsync(true)
        BaseApplication.getCameraInputProcessor.setRotationMode(RotateMode.DragLMB)
        BaseApplication.setBackgroundColor(Constants.SKY_BLUE)
    }

    /**
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    override def populateSystem: Unit = {
        // Create a character with a capsule shape and add it to the system:
        val capsuleRadius = 0.5f // meters
        val capsuleHeight = 1f // meters
        val shape = new CapsuleShape(capsuleHeight / 2f, capsuleRadius)

        val settings = new CharacterSettings
        settings.setShape(shape)

        val startLocation = new RVec3(0.0, 2.0, 0.0)
        val userData = 0L
        HelloCharacter.character = new com.github.stephengold.joltjni.Character(
                settings, startLocation, new Quat, userData, physicsSystem)
        HelloCharacter.character.addToPhysicsSystem

        // Add a static square to represent the ground:
        val halfExtent = 4f
        val y = -2f
        val ground = addSquare(halfExtent, y)

        // Visualize the shapes of both physics objects:
        BasePhysicsApp.visualizeShape(HelloCharacter.character)
        BasePhysicsApp.visualizeShape(ground)
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
    override def physicsTick(system: PhysicsSystem, timeStep: Float): Unit = {
        // Update the character:
        val maxSeparation = 0.1f // meters above the floor
        HelloCharacter.character.postSimulation(maxSeparation)
    }

    /**
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system
     * is stepped.
     *
     * @param system the system that's about to be stepped (not null)
     * @param timeStep the duration of the simulation step (in seconds, &ge;0)
     */
    override def prePhysicsTick(system: PhysicsSystem, timeStep: Float): Unit = {
        // If the character is supported, cause it to jump:
        if (HelloCharacter.character.isSupported) {
            HelloCharacter.character.setLinearVelocity(new Vec3(0f, 8f, 0f))
        }
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
    private def addSquare(halfExtent: Float, y: Float): Body = {
        // Create a static rigid body with a square shape:
        val halfThickness = 0.1f
        val shape = new BoxShape(halfExtent, halfThickness, halfExtent)
        val bcs = new BodyCreationSettings
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving)
        bcs.setPosition(0.0, y - halfThickness, 0.0)
        bcs.setShape(shape)

        val bi = physicsSystem.getBodyInterface
        val result = bi.createBody(bcs)
        bi.addBody(result, EActivation.DontActivate)

        return result
    }
}
