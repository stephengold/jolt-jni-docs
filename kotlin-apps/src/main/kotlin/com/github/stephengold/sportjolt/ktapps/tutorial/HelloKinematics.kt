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
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.Quat
import com.github.stephengold.joltjni.RVec3
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties
import com.github.stephengold.sportjolt.physics.BasePhysicsApp
import com.github.stephengold.sportjolt.physics.PhysicsTickListener

/*
 * A simple example combining kinematic and dynamic rigid bodies.
 *
 * Builds upon HelloStaticBodyKt.
 *
 * author:  Stephen Gold sgold@sonic.net
 */

private const val BALL_MASS = 2f
private const val BALL_RADIUS = 1f
private const val BALL_Z = 0.0
private const val MAX_BODIES = 2

// For simplicity, use a single broadphase layer:
private const val NUM_BP_LAYERS = 1

private const val ORBIT_RADIUS = 0.4  // meters
private const val ORBITAL_PERIOD = 0.8  // seconds
private const val TWO_PI = 2.0 * Math.PI

/**
 * A simple example of 2 colliding balls.
 */
class HelloKinematics : BasePhysicsApp(), PhysicsTickListener {
    // *************************************************************************
    // fields

    /**
     * kinematic ball, orbiting the origin
     */
    private var kineBall: Body? = null
    // *************************************************************************
    // BasePhysicsApp methods

    /**
     * Create the PhysicsSystem. Invoked once during initialization.
     */
    override fun createSystem(): PhysicsSystem {
        val result = createSystem(MAX_BODIES, NUM_BP_LAYERS)

        // To enable the callbacks, register the application as a tick listener:
        addTickListener(this)

        return result
    }

    /**
     * Initialize the application. Invoked once.
     */
    override fun initialize() {
        super.initialize()
        setVsync(true)
    }

    /**
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    override fun populateSystem() {
        val bi = physicsSystem.getBodyInterface()

        // Create a collision shape for balls:
        val ballShape = SphereShape(BALL_RADIUS)

        val bcs = BodyCreationSettings()
        bcs.getMassPropertiesOverride().setMass(BALL_MASS)
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia)
        bcs.setShape(ballShape)

        // Create a dynamic body and add it to the system:
        bcs.setPosition(0.0, 4.0, BALL_Z)
        val dynaBall = bi.createBody(bcs)
        bi.addBody(dynaBall, EActivation.Activate)

        // Create a kinematic body and add it to the system:
        bcs.setMotionType(EMotionType.Kinematic)
            .setPosition(0.0, 0.0, BALL_Z)
        kineBall = bi.createBody(bcs)
        bi.addBody(kineBall, EActivation.Activate)
        assert(kineBall!!.isKinematic())

        // Visualize the shapes of both rigid bodies:
        visualizeShape(dynaBall)
        visualizeShape(kineBall)
    }
    // *************************************************************************
    // PhysicsTickListener methods

    /**
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) after the system
     * has been stepped.
     */
    override fun physicsTick(system: PhysicsSystem, timeStep: Float) {
        // do nothing
    }

    /**
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system
     * is stepped.
     */
    override fun prePhysicsTick(system: PhysicsSystem, timeStep: Float) {
        // Make the kinematic ball orbit the origin:
        val phaseAngle = totalSimulatedTime() * TWO_PI / ORBITAL_PERIOD

        val x = ORBIT_RADIUS * Math.sin(phaseAngle)
        val y = ORBIT_RADIUS * Math.cos(phaseAngle)
        val location = RVec3(x, y, BALL_Z)
        kineBall!!.moveKinematic(location, Quat(), timeStep)
    }
}

/**
 * Main entry point for the HelloKinematicsKt application.
 */
fun main() {
    val application = HelloKinematics()
    application.start()
}
