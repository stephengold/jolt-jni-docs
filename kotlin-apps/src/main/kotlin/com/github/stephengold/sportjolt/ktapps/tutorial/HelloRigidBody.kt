/*
 Copyright (c) 2025 Stephen Gold and Yanis Boudiaf

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
import com.github.stephengold.joltjni.BodyInterface
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties
import com.github.stephengold.joltjni.readonly.ConstBody
import com.github.stephengold.joltjni.readonly.ConstShape
import com.github.stephengold.sportjolt.physics.BasePhysicsApp

/*
 * A simple example of 2 colliding balls, illustrating the 5 basic features of
 * responsive, dynamic, rigid bodies:
 *
 * + rigidity (fixed shape),
 * + inertia (resistance to changes of motion),
 * + dynamics (motion determined by forces, torques, and impulses),
 * + gravity (continual downward force), and
 * + contact response (avoid intersecting with other bodies).
 *
 * Builds upon HelloSportKt.
 *
 * author:  Stephen Gold sgold@sonic.net
 */

private const val BALL_RADIUS = 1f
private const val MAX_BODIES = 2

// For simplicity, use a single broadphase layer:
private const val NUM_BP_LAYERS = 1

/**
 * A simple example of 2 colliding balls.
 */
class HelloRigidBody : BasePhysicsApp() {
    /*
     * Create the PhysicsSystem. Invoked once during initialization.
     */
    override fun createSystem(): PhysicsSystem {
        val result = createSystem(MAX_BODIES, NUM_BP_LAYERS)

        return result
    }

    /*
     * Initialize the application. Invoked once.
     */
    override fun initialize(): Unit {
        super.initialize()
        setVsync(true)
    }

    /*
     * Populate the PhysicsSystem with bodies. Invoked once during initialization.
     */
    override fun populateSystem(): Unit {
        val bi = physicsSystem.getBodyInterface()

        // Create a collision shape for balls:
        val ballShape = SphereShape(BALL_RADIUS)

        val bcs = BodyCreationSettings()
        bcs.getMassPropertiesOverride().setMass(2f)
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia)
        bcs.setShape(ballShape)

        // Create 2 balls (dynamic rigid bodies) and add them to the system:
        bcs.setPosition(1.0, 1.0, 0.0)
        val ball1 = bi.createBody(bcs)
        bi.addBody(ball1, EActivation.Activate)

        bcs.setPosition(5.0, 1.0, 0.0)
        val ball2 = bi.createBody(bcs)
        bi.addBody(ball2, EActivation.Activate)

        assert(ball2.isDynamic())
        val actualMass = 1f / ball2.getMotionProperties().getInverseMass()
        assert(Math.abs(actualMass - 2f) < 1e-6f)

        // Apply an impulse to ball2 to put it on a collision course with ball1:
        ball2.addImpulse(-25f, 0f, 0f)

        // Visualize the shapes of both rigid bodies:
        visualizeShape(ball1)
        visualizeShape(ball2)
    }

    /*
     * Advance the physics simulation by the specified amount. Invoked during
     * each update.
     */
    override fun updatePhysics(wallClockSeconds: Float): Unit {
        // For clarity, simulate at 1/10th normal speed:
        val simulateSeconds = 0.1f * wallClockSeconds
        super.updatePhysics(simulateSeconds)
    }
}

/**
 * Main entry point for the HelloRigidBodyKt application.
 */
fun main() {
    val application = HelloRigidBody()
    application.start()
}
