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
import com.github.stephengold.joltjni.Plane
import com.github.stephengold.joltjni.PlaneShape
import com.github.stephengold.joltjni.Quat
import com.github.stephengold.joltjni.RVec3
import com.github.stephengold.joltjni.SixDofConstraintSettings
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EAxis
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties
import com.github.stephengold.joltjni.operator.Op
import com.github.stephengold.sportjolt.BaseApplication
import com.github.stephengold.sportjolt.Constants
import com.github.stephengold.sportjolt.Projection
import com.github.stephengold.sportjolt.TextureKey
import com.github.stephengold.sportjolt.Utils
import com.github.stephengold.sportjolt.input.RotateMode
import com.github.stephengold.sportjolt.physics.BasePhysicsApp
import com.github.stephengold.sportjolt.physics.ConstraintGeometry
import com.github.stephengold.sportjolt.physics.PhysicsTickListener
import org.joml.Vector3f

/**
 * A simple example of a double-ended SixDofBodyConstraint.
 * <p>
 * Builds upon HelloPivot.
 *
 * @author Stephen Gold sgold@sonic.net
 */
object HelloDoubleEnded {
    // *************************************************************************
    // constants

    /**
     * system Y coordinate of the ground plane
     */
    private val groundY = -4f
    /**
     * half the height of the paddle (in meters)
     */
    private val paddleHalfHeight = 1f
    // *************************************************************************
    // fields

    /**
     * mouse-controlled kinematic paddle
     */
    private var paddleBody: Body = null
    /**
     * latest ground-plane location indicated by the mouse cursor
     */
    private val mouseLocation: Vector3f = new Vector3f
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloDoubleEnded application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    def main(arguments: Array[String]): Unit = {
        val application = new HelloDoubleEnded
        application.start
    }
}

class HelloDoubleEnded extends BasePhysicsApp with PhysicsTickListener {
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
        // Add a static plane to represent the ground:
        addPlane(HelloDoubleEnded.groundY)

        // Add a dynamic box:
        HelloDoubleEnded.paddleBody = addBox

        // Add a dynamic ball:
        val ballBody = addBall

        // Add a double-ended constraint to connect the ball to the paddle:
        val pivotInBall = new RVec3(0.0, 3.0, 0.0)
        val pivotInPaddle = new RVec3(0.0, 3.0, 0.0)

        val settings = new SixDofConstraintSettings
        settings.makeFixedAxis(EAxis.TranslationX)
        settings.makeFixedAxis(EAxis.TranslationY)
        settings.makeFixedAxis(EAxis.TranslationZ)
        settings.setPosition1(pivotInPaddle)
        settings.setPosition2(pivotInBall)

        val constraint
                = settings.create(HelloDoubleEnded.paddleBody, ballBody)
        physicsSystem.addConstraint(constraint)

        // Visualize the constraint:
        new ConstraintGeometry(constraint, 1) // paddleBody is 1st end
        new ConstraintGeometry(constraint, 2) // ballBody is 2nd end
    }

    /**
     * Callback invoked during each iteration of the render loop.
     */
    override def render: Unit = {
        val screenXy = BaseApplication.getInputManager.locateCursor
        if (screenXy != null) {
            /*
             * Calculate the ground-plane location (if any)
             * indicated by the mouse cursor:
             */
            val nearLocation
                    = BaseApplication.cam.clipToWorld(screenXy, Projection.nearClipZ, null)
            val farLocation
                    = BaseApplication.cam.clipToWorld(screenXy, Projection.farClipZ, null)
            val nearY = nearLocation.y
            val farY = farLocation.y
            if (nearY > HelloDoubleEnded.groundY && farY < HelloDoubleEnded.groundY) {
                val dy = nearY - farY
                val t = (nearY - HelloDoubleEnded.groundY) / dy
                Utils.lerp(t, nearLocation, farLocation, HelloDoubleEnded.mouseLocation)
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
        val mouse = Utils.toJoltVector(HelloDoubleEnded.mouseLocation)
        val bodyLocation = Op.plus(mouse,
                new RVec3(0.0, HelloDoubleEnded.paddleHalfHeight, 0.0))
        HelloDoubleEnded.paddleBody.moveKinematic(
                bodyLocation, new Quat, timeStep)
    }
    // *************************************************************************
    // private methods

    /**
     * Create a dynamic rigid body with a sphere shape and add it to the system.
     *
     * @return the new body
     */
    private def addBall: Body = {
        val radius = 0.4f
        val shape = new SphereShape(radius)

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
     * Create a kinematic body with a box shape and add it to the system.
     *
     * @return the new body
     */
    private def addBox: Body = {
        val shape = new BoxShape(0.3f, HelloDoubleEnded.paddleHalfHeight, 1f)

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
     * Add a static horizontal plane body to the system.
     *
     * @param y the desired elevation (in system coordinates)
     */
    private def addPlane(y: Float): Unit = {
        val plane = new Plane(0f, 1f, 0f, -y)
        val shape = new PlaneShape(plane)
        val bcs = new BodyCreationSettings
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving)
        bcs.setShape(shape)

        val bi = physicsSystem.getBodyInterface
        val body = bi.createBody(bcs)
        bi.addBody(body, EActivation.DontActivate)

        // Visualize the body:
        val resourceName = "/Textures/greenTile.png"
        val maxAniso = 16f
        val textureKey = new TextureKey("classpath://" + resourceName, maxAniso)
        BasePhysicsApp.visualizeShape(body, 0.1f)
                .setSpecularColor(Constants.DARK_GRAY)
                .setTexture(textureKey)
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
