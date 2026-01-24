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
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.Quat
import com.github.stephengold.joltjni.RVec3
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties
import com.github.stephengold.sportjolt.BaseApplication
import com.github.stephengold.sportjolt.physics.BasePhysicsApp
import com.github.stephengold.sportjolt.physics.PhysicsTickListener

/**
 * A simple example combining kinematic and dynamic rigid bodies.
 * <p>
 * Builds upon HelloStaticBody.
 *
 * @author Stephen Gold sgold@sonic.net
 */
object HelloKinematics {
    // *************************************************************************
    // fields

    /**
     * kinematic ball, orbiting the origin
     */
    private var kineBall: Body = null
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloKinematics application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    def main(arguments: Array[String]): Unit = {
        val application = new HelloKinematics
        application.start
    }
}

class HelloKinematics extends BasePhysicsApp with PhysicsTickListener {
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

        // To enable the callbacks, register this app as a tick listener:
        addTickListener(this)

        return result
    }

    /**
     * Initialize the application. Invoked once.
     */
    override def initialize: Unit = {
        super.initialize
        BaseApplication.setVsync(true)
    }

    /**
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    override def populateSystem: Unit = {
        val bi = physicsSystem.getBodyInterface

        // Create a collision shape for balls:
        val ballRadius = 1f
        val ballShape = new SphereShape(ballRadius)

        val bcs = new BodyCreationSettings
        bcs.getMassPropertiesOverride.setMass(2f)
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia)
        bcs.setShape(ballShape)

        // Create a dynamic body and add it to the system:
        bcs.setPosition(0.0, 4.0, 0.0)
        val dynaBall = bi.createBody(bcs)
        bi.addBody(dynaBall, EActivation.Activate)

        // Create a kinematic body and add it to the system:
        bcs.setMotionType(EMotionType.Kinematic)
        bcs.setPosition(0.0, 0.0, 0.0)
        HelloKinematics.kineBall = bi.createBody(bcs)
        bi.addBody(HelloKinematics.kineBall, EActivation.Activate)
        assert(HelloKinematics.kineBall.isKinematic)

        // Visualize the shapes of both rigid bodies:
        BasePhysicsApp.visualizeShape(dynaBall)
        BasePhysicsApp.visualizeShape(HelloKinematics.kineBall)
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
        // do nothing
    }

    /**
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system
     * is stepped.
     *
     * @param system the system that's about to be stepped (not null)
     * @param timeStep the duration of the simulation step (in seconds, &ge;0)
     */
    override def prePhysicsTick(system: PhysicsSystem, timeStep: Float): Unit = {
        // Make the kinematic ball orbit the origin:
        val orbitalPeriod = 0.8 // seconds
        val phaseAngle = 2.0 * BasePhysicsApp.totalSimulatedTime * Math.PI / orbitalPeriod

        val orbitRadius = 0.4 // meters
        val x = orbitRadius * Math.sin(phaseAngle)
        val y = orbitRadius * Math.cos(phaseAngle)
        val location = new RVec3(x, y, 0.0)
        HelloKinematics.kineBall.moveKinematic(location, new Quat, timeStep)
    }
}
