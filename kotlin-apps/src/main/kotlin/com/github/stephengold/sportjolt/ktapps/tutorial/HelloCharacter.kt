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

package com.github.stephengold.sportjolt.ktapps.tutorial

import com.github.stephengold.joltjni.Body
import com.github.stephengold.joltjni.BodyCreationSettings
import com.github.stephengold.joltjni.BoxShape
import com.github.stephengold.joltjni.CapsuleShape
import com.github.stephengold.joltjni.CharacterSettings
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.Quat
import com.github.stephengold.joltjni.RVec3
import com.github.stephengold.joltjni.Vec3
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.sportjolt.Constants
import com.github.stephengold.sportjolt.input.RotateMode
import com.github.stephengold.sportjolt.physics.BasePhysicsApp
import com.github.stephengold.sportjolt.physics.PhysicsTickListener

/*
 * A simple example of character physics.
 *
 * Builds upon HelloStaticBodyKt.
 *
 * author:  Stephen Gold sgold@sonic.net
 */

private const val CAPSULE_RADIUS = 0.5f  // meters
private const val CAPSULE_HALF_HEIGHT = 0.5f  // meters
private const val GROUND_HALF_EXTENT = 4f
private const val GROUND_Y = -2f
private const val HALF_THICKNESS = 0.1f

private const val MAX_BODIES = 2
private const val MAX_SEPARATION = 0.1f  // meters above the floor

// For simplicity, use a single broadphase layer:
private const val NUM_BP_LAYERS = 1

private const val START_X = 0.0
private const val START_Y = 2.0
private const val START_Z = 0.0
private const val USER_DATA = 0L

/**
 * A simple example of 2 colliding balls.
 */
class HelloCharacter : BasePhysicsApp(), PhysicsTickListener {
    // *************************************************************************
    // fields

    /**
     * character being tested
     */
    private var character: com.github.stephengold.joltjni.Character? = null

    /*
     * Create the PhysicsSystem. Invoked once during initialization.
     */
    override fun createSystem(): PhysicsSystem {
        val result = createSystem(MAX_BODIES, NUM_BP_LAYERS)

        // To enable the callbacks, register the application as a tick listener:
        addTickListener(this)

        return result
    }

    /*
     * Initialize the application. Invoked once.
     */
    override fun initialize(): Unit {
        super.initialize()

        setVsync(true)
        getCameraInputProcessor().setRotationMode(RotateMode.DragLMB)
        setBackgroundColor(Constants.SKY_BLUE)
    }

    /*
     * Populate the PhysicsSystem. Invoked once during initialization.
     */
    override fun populateSystem(): Unit {
        // Create a character with a capsule shape and add it to the system:
        val shape = CapsuleShape(CAPSULE_HALF_HEIGHT, CAPSULE_RADIUS)

        val settings = CharacterSettings()
        settings.setShape(shape)

        val startLocation = RVec3(START_X, START_Y, START_Z)
        character = com.github.stephengold.joltjni.Character(
            settings, startLocation, Quat(), USER_DATA, physicsSystem)
        character!!.addToPhysicsSystem()

        // Add a static square to represent the ground:
        val ground = addSquare(GROUND_HALF_EXTENT, GROUND_Y)

        // Visualize the shapes of both physics objects:
        visualizeShape(character)
        visualizeShape(ground)
    }
    // *************************************************************************
    // PhysicsTickListener methods

    /*
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) after the system
     * has been stepped.
     *
     * @param system the system that was just stepped (not null)
     * @param timeStep the duration of the simulation step (in seconds, &ge;0)
     */
    override fun physicsTick(system: PhysicsSystem, timeStep: Float): Unit {
        // Update the character:
        character!!.postSimulation(MAX_SEPARATION)
    }

    /*
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system
     * is stepped.
     *
     * @param system the system that's about to be stepped (not null)
     * @param timeStep the duration of the simulation step (in seconds, &ge;0)
     */
    override fun prePhysicsTick(system: PhysicsSystem, timeStep: Float): Unit {
        // If the character is supported, cause it to jump:
        if (character!!.isSupported) {
            character!!.setLinearVelocity(Vec3(0f, 8f, 0f))
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
    private fun addSquare(halfExtent: Float, y: Float): Body {
        // Create a static rigid body with a square shape:
        val shape = BoxShape(halfExtent, HALF_THICKNESS, halfExtent)
        val bcs = BodyCreationSettings()
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(objLayerNonMoving)
        bcs.setPosition(0.0, (y - HALF_THICKNESS).toDouble(), 0.0)
        bcs.setShape(shape)

        val bi = physicsSystem.getBodyInterface()
        val result = bi.createBody(bcs)
        bi.addBody(result, EActivation.DontActivate)

        return result
    }
}

/**
 * Main entry point for the HelloCharacterKt application.
 */
fun main() {
    val application = HelloCharacter()
    application.start()
}
