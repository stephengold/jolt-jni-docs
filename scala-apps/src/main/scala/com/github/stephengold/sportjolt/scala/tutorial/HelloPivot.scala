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
import com.github.stephengold.joltjni.BoxShape
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.Quat
import com.github.stephengold.joltjni.RVec3
import com.github.stephengold.joltjni.SixDofConstraintSettings
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EAxis
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties
import com.github.stephengold.sportjolt.BaseApplication
import com.github.stephengold.sportjolt.Projection
import com.github.stephengold.sportjolt.Utils
import com.github.stephengold.sportjolt.input.RotateMode
import com.github.stephengold.sportjolt.physics.BasePhysicsApp
import com.github.stephengold.sportjolt.physics.ConstraintGeometry
import com.github.stephengold.sportjolt.physics.LocalAxisGeometry
import com.github.stephengold.sportjolt.physics.PhysicsTickListener
import org.joml.Vector3f

/**
 * A simple example of a constraint that isn't centered on a body.
 * <p>
 * Builds upon HelloConstraint.
 *
 * @author Stephen Gold sgold@sonic.net
 */
object HelloPivot {
    // *************************************************************************
    // fields

    /**
     * mouse-controlled kinematic ball
     */
    private var kineBall: Body = null
    /**
     * latest X-Z plane location indicated by the mouse cursor
     */
    private val mouseLocation: Vector3f = new Vector3f
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloPivot application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    def main(arguments: Array[String]): Unit = {
        val application = new HelloPivot
        application.start
    }
}

class HelloPivot extends BasePhysicsApp with PhysicsTickListener {
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

        // Reduce the time step for better accuracy:
        this.timePerStep = 0.005f // seconds

        return result
    }

    /**
     * Initialize the application. Invoked once.
     */
    override def initialize: Unit = {
        super.initialize
        BaseApplication.setVsync(true)

        configureCamera
        BaseApplication.setLightDirection(7f, 3f, 5f)

        // Disable VSync for more frequent mouse-position updates:
        BaseApplication.setVsync(false)
    }

    /**
     * Populate the PhysicsSystem with bodies and constraints. Invoked once
     * during initialization.
     */
    override def populateSystem: Unit = {
        // Add a mouse-controlled kinematic ball:
        HelloPivot.kineBall = addKineBall

        // Add a dynamic box:
        val rotor = addBox

        // Constrain the box to rotate around an offset Y axis:
        val settings = new SixDofConstraintSettings
        settings.makeFixedAxis(EAxis.RotationX)
        // Y-axis rotation remains free.
        settings.makeFixedAxis(EAxis.RotationZ)
        settings.makeFixedAxis(EAxis.TranslationX)
        settings.makeFixedAxis(EAxis.TranslationY)
        settings.makeFixedAxis(EAxis.TranslationZ)
        val pivotInWorld = new RVec3(0.0, 0.0, 2.0)
        settings.setPosition1(pivotInWorld)
        settings.setPosition2(pivotInWorld)

        val fixedToWorld = Body.sFixedToWorld
        val constraint = settings.create(fixedToWorld, rotor)
        physicsSystem.addConstraint(constraint)

        // Visualize the constraint:
        new ConstraintGeometry(constraint, 2); // ballBody is its 2nd end
    }

    /**
     * Callback invoked during each iteration of the render loop.
     */
    override def render: Unit = {
        val screenXy = BaseApplication.getInputManager.locateCursor
        if (screenXy != null) {
            /*
             * Calculate the X-Z plane location (if any)
             * indicated by the mouse cursor:
             */
            val nearLocation
                    = BaseApplication.cam.clipToWorld(screenXy, Projection.nearClipZ, null)
            val farLocation
                    = BaseApplication.cam.clipToWorld(screenXy, Projection.farClipZ, null)
            val nearY = nearLocation.y
            val farY = farLocation.y
            if (nearY > 0f && farY < 0f) {
                val dy = nearY - farY
                val t = nearY / dy
                Utils.lerp(t, nearLocation, farLocation, HelloPivot.mouseLocation)
            }
        }

        super.render
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
        // Relocate the kinematic ball based on the mouse location:
        val bodyLocation = Utils.toLocationVector(HelloPivot.mouseLocation)
        HelloPivot.kineBall.moveKinematic(bodyLocation, new Quat, timeStep)
    }
    // *************************************************************************
    // private methods

    /**
     * Create a dynamic rigid body with a box shape and add it to the system.
     *
     * @return the new body
     */
    private def addBox: Body = {
        val shape = new BoxShape(0.3f, 1f, 1f)

        val bcs = new BodyCreationSettings
        bcs.setAllowSleeping(false) // Disable sleep (deactivation).
        bcs.getMassPropertiesOverride.setMass(0.2f)
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia)
        bcs.setShape(shape)

        val bi = physicsSystem.getBodyInterface
        val result = bi.createBody(bcs)
        bi.addBody(result, EActivation.Activate)

        BasePhysicsApp.visualizeShape(result)

        return result
    }

    /**
     * Create a kinematic body with a sphere shape and add it to the system.
     *
     * @return the new body
     */
    private def addKineBall: Body = {
        val ballRadius = 1f
        val shape = new SphereShape(ballRadius)

        val bcs = new BodyCreationSettings
        bcs.setAllowSleeping(false) // Disable sleep (deactivation).
        bcs.setMotionType(EMotionType.Kinematic) // default=Dynamic
        bcs.setShape(shape)

        val bi = physicsSystem.getBodyInterface
        val result = bi.createBody(bcs)
        bi.addBody(result, EActivation.Activate)

        BasePhysicsApp.visualizeShape(result)

        return result
    }

    /**
     * Configure the Camera and CIP during initialization.
     */
    private def configureCamera: Unit = {
        BaseApplication.getCameraInputProcessor.setRotationMode(RotateMode.None)

        BaseApplication.cam.setLocation(0f, 5f, 10f)
        BaseApplication.cam.setUpAngle(-0.6f)
        BaseApplication.cam.setAzimuth(-1.6f)
    }
}
