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

import com.github.stephengold.joltjni.BodyCreationSettings
import com.github.stephengold.joltjni.CylinderShape
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EMotionQuality
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties
import com.github.stephengold.joltjni.readonly.ConstBody
import com.github.stephengold.sportjolt.physics.BasePhysicsApp

/*
 * A simple example of continuous collision detection (CCD).
 *
 * Builds upon HelloStaticBody.
 *
 * author:  Stephen Gold sgold@sonic.net
 */

private const val BALL_MASS = 2f
private const val BALL_RADIUS = 0.1f
private const val CONVEX_RADIUS = 0.02f
private const val DISC_HALF_THICKNESS = 0.025f
private const val DISC_RADIUS = 2f

private const val MAX_BODIES = 3

// For simplicity, use a single broadphase layer:
private const val NUM_BP_LAYERS = 1

private const val RELATIVE_SPEED = 0.1f

/**
 * A simple example of 2 colliding balls.
 */
class HelloCcd : BasePhysicsApp() {
    /*
     * Create the PhysicsSystem. Invoked once during initialization.
     */
    override fun createSystem(): PhysicsSystem {
        val result = createSystem(MAX_BODIES, NUM_BP_LAYERS)

        // Increase gravity to make the balls fall faster:
        result.setGravity(0f, -100f, 0f)

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
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    override fun populateSystem(): Unit {
        val bi = physicsSystem.getBodyInterface()

        // Create a collision shape for balls:
        val ballShape = SphereShape(BALL_RADIUS)

        val bcs = BodyCreationSettings()
        bcs.getMassPropertiesOverride().setMass(BALL_MASS)
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia)
        bcs.setShape(ballShape)
        /*
         * Create 2 dynamic balls, one with LinearCast CCD and one without,
         * and add them to the system:
         */
        bcs.setMotionQuality(EMotionQuality.LinearCast)
        bcs.setPosition(-1.0, 4.0, 0.0)
        val ccdBall = bi.createBody(bcs)
        bi.addBody(ccdBall, EActivation.Activate)

        bcs.setMotionQuality(EMotionQuality.Discrete)
        bcs.setPosition(1.0, 4.0, 0.0)
        val controlBall = bi.createBody(bcs)
        bi.addBody(controlBall, EActivation.Activate)

        // Verify the motion quality of each ball:
        val ccdProperties = ccdBall.getMotionProperties()
        assert(ccdProperties.getMotionQuality() == EMotionQuality.LinearCast)
        val controlProperties = controlBall.getMotionProperties()
        assert(controlProperties.getMotionQuality() == EMotionQuality.Discrete)

        // Add an obstacle:
        val disc = addDisc()

        // Visualize the shapes of all 3 rigid bodies:
        visualizeShape(ccdBall)
        visualizeShape(controlBall)
        visualizeShape(disc).setProgram("Unshaded/Monochrome")
    }

    /**
     * Advance the physics simulation by the specified amount. Invoked during
     * each update.
     *
     * @param wallClockSeconds the elapsed wall-clock time since the previous
     * invocation of {@code updatePhysics} (in seconds, &ge;0)
     */
    override fun updatePhysics(wallClockSeconds: Float): Unit {
        // For clarity, simulate at 1/10th normal speed:
        val simulateSeconds = RELATIVE_SPEED * wallClockSeconds
        super.updatePhysics(simulateSeconds)
    }

    /**
     * Add a thin static disc to serve as an obstacle.
     *
     * @return the new object
     */
    private fun addDisc(): ConstBody {
        val discShape = CylinderShape(
            DISC_HALF_THICKNESS, DISC_RADIUS, CONVEX_RADIUS)

        val bcs = BodyCreationSettings()
            .setMotionType(EMotionType.Static)
            .setObjectLayer(objLayerNonMoving)
            .setShape(discShape)

        val bi = physicsSystem.getBodyInterface()
        val result = bi.createBody(bcs)
        bi.addBody(result, EActivation.DontActivate)

        return result
    }
}

/**
 * Main entry point for the HelloCcdKt application.
 */
fun main() {
    val application = HelloCcd()
    application.start()
}
