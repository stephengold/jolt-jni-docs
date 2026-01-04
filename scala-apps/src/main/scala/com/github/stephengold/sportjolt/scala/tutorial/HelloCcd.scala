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

import com.github.stephengold.joltjni.BodyCreationSettings
import com.github.stephengold.joltjni.BodyInterface
import com.github.stephengold.joltjni.CylinderShape
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EMotionQuality
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties
import com.github.stephengold.joltjni.readonly.ConstBody
import com.github.stephengold.sportjolt.BaseApplication
import com.github.stephengold.sportjolt.physics.BasePhysicsApp

/**
 * A simple example of continuous collision detection (CCD).
 * <p>
 * Builds upon HelloStaticBody.
 *
 * @author Stephen Gold sgold@sonic.net
 */
object HelloCcd {
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloCcd application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    def main(arguments: Array[String]): Unit = {
        val application = new HelloCcd
        application.start
    }
}

class HelloCcd extends BasePhysicsApp {
    /**
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new object
     */
    override def createSystem: PhysicsSystem = {
        // For simplicity, use a single broadphase layer:
        val maxBodies = 3
        val numBpLayers = 1
        val result = createSystem(maxBodies, numBpLayers)

        // Increase gravity to make the balls fall faster:
        result.setGravity(0f, -100f, 0f)

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
        val ballRadius = 0.1f
        val ballShape = new SphereShape(ballRadius)

        val bcs = new BodyCreationSettings
        bcs.getMassPropertiesOverride().setMass(2f)
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
        BasePhysicsApp.visualizeShape(ccdBall)
        BasePhysicsApp.visualizeShape(controlBall)
        BasePhysicsApp.visualizeShape(disc).setProgram("Unshaded/Monochrome")
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
    // *************************************************************************
    // private methods

    /**
     * Add a thin static disc to serve as an obstacle.
     *
     * @return the new object
     */
    private def addDisc(): ConstBody = {
        val discRadius = 2f
        val discThickness = 0.05f
        val discConvexRadius = 0.02f
        val discShape = new CylinderShape(
                discThickness / 2f, discRadius, discConvexRadius)

        val bcs = new BodyCreationSettings()
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving)
        bcs.setShape(discShape)

        val bi = physicsSystem.getBodyInterface()
        val result = bi.createBody(bcs)
        bi.addBody(result, EActivation.DontActivate)

        return result
    }
}
