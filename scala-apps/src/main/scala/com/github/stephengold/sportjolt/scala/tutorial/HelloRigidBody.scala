/*
 Copyright (c) 2025-2026 Stephen Gold and Yanis Boudiaf

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
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties
import com.github.stephengold.sportjolt.BaseApplication
import com.github.stephengold.sportjolt.physics.BasePhysicsApp

/**
 * A simple example of 2 colliding balls, illustrating the 5 basic features of
 * responsive, dynamic, rigid bodies:<ul>
 * <li>rigidity (fixed shape),</li>
 * <li>inertia (resistance to changes of motion),</li>
 * <li>dynamics (motion determined by forces, torques, and impulses),</li>
 * <li>gravity (continual downward force), and </li>
 * <li>contact response (avoid intersecting with other bodies).</li>
 * </ul>
 * <p>
 * Builds upon HelloSport.
 *
 * @author Stephen Gold sgold@sonic.net
 */
object HelloRigidBody {
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloSport application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    def main(arguments: Array[String]): Unit = {
        val application = new HelloRigidBody
        application.start
    }
}

class HelloRigidBody extends BasePhysicsApp {
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

        // Create 2 balls (dynamic rigid bodies) and add them to the system:
        bcs.setPosition(1.0, 1.0, 0.0)
        val ball1 = bi.createBody(bcs)
        bi.addBody(ball1, EActivation.Activate)

        bcs.setPosition(5.0, 1.0, 0.0)
        val ball2 = bi.createBody(bcs)
        bi.addBody(ball2, EActivation.Activate)

        assert(ball2.isDynamic)
        val actualMass = 1f / ball2.getMotionProperties.getInverseMass
        assert(Math.abs(actualMass - 2f) < 1e-6f, "actualMass = " + actualMass)

        // Apply an impulse to ball2 to put it on a collision course with ball1:
        ball2.addImpulse(-25f, 0f, 0f)

        // Visualize the shapes of both bodies:
        BasePhysicsApp.visualizeShape(ball1)
        BasePhysicsApp.visualizeShape(ball2)
    }

    /**
     * Advance the physics simulation by the specified amount. Invoked during
     * each update.
     *
     * @param wallClockSeconds the elapsed wall-clock time since the previous
     * invocation of {@code updatePhysics} (in seconds, &ge;0)
     */
    override def updatePhysics(wallClockSeconds: Float): Unit = {
        // For clarity, simulate at 1/10th normal speed:
        val simulateSeconds = 0.1f * wallClockSeconds
        super.updatePhysics(simulateSeconds)
    }
}
