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
import com.github.stephengold.joltjni.ConvexHullShapeSettings
import com.github.stephengold.joltjni.Jolt
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.Plane
import com.github.stephengold.joltjni.PlaneShape
import com.github.stephengold.joltjni.Vec3
import com.github.stephengold.joltjni.VehicleCollisionTesterRay
import com.github.stephengold.joltjni.VehicleConstraint
import com.github.stephengold.joltjni.VehicleConstraintSettings
import com.github.stephengold.joltjni.WheelSettingsWv
import com.github.stephengold.joltjni.WheeledVehicleController
import com.github.stephengold.joltjni.WheeledVehicleControllerSettings
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties
import com.github.stephengold.joltjni.readonly.Vec3Arg
import com.github.stephengold.sportjolt.BaseApplication
import com.github.stephengold.sportjolt.Constants
import com.github.stephengold.sportjolt.TextureKey
import com.github.stephengold.sportjolt.input.RotateMode
import com.github.stephengold.sportjolt.physics.BasePhysicsApp
import org.joml.Vector3f

/**
 * A simple example of vehicle physics.
 * <p>
 * Builds upon HelloStaticBody.
 *
 * @author Stephen Gold sgold@sonic.net
 */
object HelloVehicle {
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloVehicle application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    def main(arguments: Array[String]): Unit = {
        val application = new HelloVehicle
        application.start
    }
}

class HelloVehicle extends BasePhysicsApp {
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

        BaseApplication.getCameraInputProcessor
                .setRotationMode(RotateMode.DragLMB)
        BaseApplication.cam.setLocation(-4.2f, 6.8f, 36f)
                .setLookDirection(new Vector3f(0.49f, -0.36f, -0.8f)) // TODO

        BaseApplication.setBackgroundColor(Constants.SKY_BLUE)
    }

    /**
     * Populate the PhysicsSystem with bodies and constraints. Invoked once
     * during initialization.
     */
    override def populateSystem: Unit = {
        // Add a static horizontal plane at y=-0.65 to represent the ground:
        val groundY = -0.65f
        addPlane(groundY)
        /*
         * Create a wedge-shaped body with a low center of gravity.
         * The local forward direction is +Z.
         */
        val noseZ = 1.4f           // offset from body's center
        val spoilerY = 0.5f        // offset from body's center
        val tailZ = -0.7f          // offset from body's center
        val undercarriageY = -0.1f // offset from body's center
        val halfWidth = 0.4f
        val cornerLocations: Array[Vec3Arg] = Array.ofDim(6)
        cornerLocations(0) = new Vec3(+halfWidth, undercarriageY, noseZ)
        cornerLocations(1) = new Vec3(-halfWidth, undercarriageY, noseZ)
        cornerLocations(2) = new Vec3(+halfWidth, undercarriageY, tailZ)
        cornerLocations(3) = new Vec3(-halfWidth, undercarriageY, tailZ)
        cornerLocations(4) = new Vec3(+halfWidth, spoilerY, tailZ)
        cornerLocations(5) = new Vec3(-halfWidth, spoilerY, tailZ)
        val ss = new ConvexHullShapeSettings(cornerLocations*)
        val wedgeShape = ss.create.get

        val bcs = new BodyCreationSettings
        bcs.getMassPropertiesOverride.setMass(200f)
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia)
                .setShape(wedgeShape)

        val bi = physicsSystem.getBodyInterface
        val body = bi.createBody(bcs)
        bi.addBody(body, EActivation.Activate)

        // Configure 4 wheels, 2 in the front (for steering) and 2 in the rear:
        val frontAxleZ = 0.7f * noseZ // offset from body's origin
        val rearAxleZ = 0.8f * tailZ // offset from body's origin
        val xOffset = 0.9f * halfWidth
        val wheels: Array[WheelSettingsWv] = Array.ofDim(4)
        for (i <- 0 to 3) {
            wheels(i) = new WheelSettingsWv
        }
        wheels(0).setPosition(new Vec3(-xOffset, 0f, frontAxleZ)) // left front
        wheels(1).setPosition(new Vec3(xOffset, 0f, frontAxleZ))
        wheels(2).setPosition(new Vec3(-xOffset, 0f, rearAxleZ))
        wheels(3).setPosition(new Vec3(xOffset, 0f, rearAxleZ)) // right rear

        // The rear wheels aren't used for steering:
        wheels(2).setMaxSteerAngle(0f)
        wheels(3).setMaxSteerAngle(0f)
        /*
         * Configure a controller with a single differential,
         * for rear-wheel drive:
         */
        val wvcs = new WheeledVehicleControllerSettings
        wvcs.setNumDifferentials(1)
        val vds = wvcs.getDifferential(0)
        vds.setLeftWheel(2)
        vds.setRightWheel(3)

        val vcs = new VehicleConstraintSettings
        vcs.addWheels(wheels*)
        vcs.setController(wvcs)
        val vehicle = new VehicleConstraint(body, vcs)
        val objectLayer = body.getObjectLayer
        val tester = new VehicleCollisionTesterRay(objectLayer)
        vehicle.setVehicleCollisionTester(tester)
        physicsSystem.addConstraint(vehicle)
        physicsSystem.addStepListener(vehicle.getStepListener)

        // Visualize the vehicle:
        BasePhysicsApp.visualizeShape(vehicle)
        BasePhysicsApp.visualizeWheels(vehicle)

        // Apply a steering angle of 4 degrees left (to both front wheels):
        val right = -Jolt.degreesToRadians(4f)

        // Apply a constant forward acceleration:
        val forward = 1f
        val brake = 0f
        val handBrake = 0f
        val controller = vehicle.getController.asInstanceOf[WheeledVehicleController]
        controller.setDriverInput(forward, right, brake, handBrake)
    }
    // *************************************************************************
    // private methods

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
        val textureKey
                = new TextureKey("classpath://" + resourceName, maxAniso)
        BasePhysicsApp.visualizeShape(body, 0.1f)
                .setSpecularColor(Constants.DARK_GRAY)
                .setTexture(textureKey)
    }
}
