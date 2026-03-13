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
import com.github.stephengold.joltjni.CapsuleShape
import com.github.stephengold.joltjni.CharacterSettings
import com.github.stephengold.joltjni.CustomContactListener
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.Plane
import com.github.stephengold.joltjni.PlaneShape
import com.github.stephengold.joltjni.Quat
import com.github.stephengold.joltjni.RVec3
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.joltjni.operator.Op
import com.github.stephengold.sportjolt.BaseApplication
import com.github.stephengold.sportjolt.Constants
import com.github.stephengold.sportjolt.TextureKey
import com.github.stephengold.sportjolt.input.InputProcessor
import com.github.stephengold.sportjolt.input.RotateMode
import com.github.stephengold.sportjolt.physics.BasePhysicsApp
import com.github.stephengold.sportjolt.physics.PhysicsTickListener
import org.lwjgl.glfw.GLFW

/**
 * A simple example of a sensor body with a contact listener.
 * <p>
 * Press the arrow keys to walk. Press the space bar to jump.
 * <p>
 * Builds upon HelloWalk.
 *
 * @author Stephen Gold sgold@sonic.net
 */
object HelloSensor {
    // *************************************************************************
    // fields

    /**
     * sensor body for detecting intrusions
     */
    private var sensor: Body = null
    /**
     * record contact between the sensor and a moving body
     */
    @volatile private var hadContact: Boolean = false
    /**
     * true when the space bar is pressed, otherwise false
     */
    @volatile private var jumpRequested: Boolean = false
    /**
     * true when the DOWN key is pressed, otherwise false
     */
    @volatile private var walkBackward: Boolean = false
    /**
     * true when the UP key is pressed, otherwise false
     */
    @volatile private var walkForward: Boolean = false
    /**
     * true when the LEFT key is pressed, otherwise false
     */
    @volatile private var walkLeft: Boolean = false
    /**
     * true when the RIGHT key is pressed, otherwise false
     */
    @volatile private var walkRight: Boolean = false
    /**
     * character to trigger the sensor
     */
    private var character: com.github.stephengold.joltjni.Character = null
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloSensor application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    def main(arguments: Array[String]): Unit = {
        val application = new HelloSensor
        application.start
    }
}

class HelloSensor extends BasePhysicsApp, PhysicsTickListener {
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

        // To enable the callbacks, register the application as a tick listener
        // and set a simple contact listener:
        addTickListener(this)
        result.setContactListener(new CustomContactListener {
            override def onContactAdded(body1Va: Long, body2Va: Long,
                    manifoldVa: Long, settingsVa: Long): Unit = {
                addContact(body1Va, body2Va)
            }
        })

        return result
    }

    /**
     * Initialize the application. Invoked once.
     */
    override def initialize: Unit = {
        super.initialize

        BaseApplication.setVsync(true)
        configureCamera
        configureInput
        configureLighting
    }

    /**
     * Populate the PhysicsSystem. Invoked once during initialization.
     */
    override def populateSystem: Unit = {
        // Create a character with a capsule shape and add it to the system:
        val capsuleRadius = 3f // meters
        val capsuleHeight = 4f // meters
        val shape = new CapsuleShape(capsuleHeight / 2f, capsuleRadius)

        val settings = new CharacterSettings
        settings.setShape(shape)

        val startLocation = new RVec3(0.0, 3.0, 0.0)
        val userData = 0L
        HelloSensor.character = new com.github.stephengold.joltjni.Character(
                settings, startLocation, new Quat, userData, physicsSystem)
        HelloSensor.character.addToPhysicsSystem

        // Create a spherical sensor bubble:
        val sensorRadius = 10f
        val sensorShape = new SphereShape(sensorRadius)
        val bcs = new BodyCreationSettings
        bcs.setIsSensor(true)
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving)
        bcs.setPosition(15.0, 0.0, -13.0)
        bcs.setShape(sensorShape)
        val bi = physicsSystem.getBodyInterface
        HelloSensor.sensor = bi.createBody(bcs)
        bi.addBody(HelloSensor.sensor, EActivation.DontActivate)

        // Visualize the shapes of both physics objects:
        BasePhysicsApp.visualizeShape(HelloSensor.character)
        BasePhysicsApp.visualizeShape(HelloSensor.sensor)

        // Add a plane to represent the ground:
        val groundY = -2f
        addPlane(groundY)
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
        // Update the character:
        val maxSeparation = 0.1f // meters above the ground
        HelloSensor.character.postSimulation(maxSeparation)

        if (HelloSensor.hadContact) {
            // Intruder deptected! Pop the sensor bubble:
            val bi = physicsSystem.getBodyInterface
            val bodyId = HelloSensor.sensor.getId
            bi.removeBody(bodyId)
            HelloSensor.hadContact = false
        }
    }

    /**
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system
     * is stepped.
     *
     * @param system the system that's about to be stepped (not null)
     * @param timeStep the duration of the simulation step (in seconds, &ge;0)
     */
    override def prePhysicsTick(system: PhysicsSystem, timeStep: Float): Unit = {
        val velocity = HelloSensor.character.getLinearVelocity

        // Clear any horizontal motion from the previous simulation step:
        velocity.setX(0f)
        velocity.setZ(0f)

        // If the character is supported, make it respond to keyboard input:
        if (HelloSensor.character.isSupported) {
            if (HelloSensor.jumpRequested) {
                // Cause the character to jump:
                velocity.setY(8f)

            } else {
                // Walk as directed by the arrow keys:
                val component1 = BaseApplication.cam.getDirection
                val backward = if (HelloSensor.walkBackward) 1f else 0f
                val forward = if (HelloSensor.walkForward) 1f else 0f
                component1.scaleInPlace(forward - backward)

                val right = if (HelloSensor.walkRight) 1f else 0f
                val left = if (HelloSensor.walkLeft) 1f else 0f
                val component2 = BaseApplication.cam.getRight
                component2.scaleInPlace(right - left)
                Op.assign(velocity, Op.plus(component1, component2))

                velocity.setY(0f)
                if (velocity.length > 0f) {
                    val scale = 7f / velocity.length
                    velocity.scaleInPlace(scale)
                }
            }
        }

        HelloSensor.character.setLinearVelocity(velocity)
    }
    // *************************************************************************
    // private methods

    /**
     * Process a new contact point.
     *
     * @param body1Va the virtual address of the first contact body
     * @param body2Va the virtual address of the 2nd contact body
     */
    private def addContact(body1Va: Long, body2Va: Long): Unit = {
        val ghostVa = HelloSensor.sensor.va
        if (body1Va == ghostVa) {
            val other = new Body(body2Va) // TODO 2 args
            if (!other.isStatic) {
                HelloSensor.hadContact = true
            }

        } else if (body2Va == ghostVa) {
            val other = new Body(body1Va) // TODO 2 args
            if (!other.isStatic) {
                HelloSensor.hadContact = true
            }
        }
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
        val textureKey
                = new TextureKey("classpath://" + resourceName, maxAniso)
        BasePhysicsApp.visualizeShape(body, 0.1f)
                .setSpecularColor(Constants.DARK_GRAY)
                .setTexture(textureKey)
    }

    /**
     * Configure the camera, projection, and CIP during initialization.
     */
    private def configureCamera: Unit = {
        BaseApplication.getCameraInputProcessor.setRotationMode(RotateMode.DragLMB)
        BaseApplication.cam.setAzimuth(-1.9f)
                .setLocation(35f, 35f, 60f)
                .setUpAngle(-0.5f)
        BaseApplication.getProjection.setFovyDegrees(30f)
    }

    /**
     * Configure keyboard input during initialization.
     */
    private def configureInput: Unit = {
        BaseApplication.getInputManager.add(new InputProcessor {
            override def onKeyboard(glfwKeyId: Int, isPressed: Boolean): Unit = {
                glfwKeyId match {
                    case GLFW.GLFW_KEY_SPACE =>
                        HelloSensor.jumpRequested = isPressed
                        return

                    case GLFW.GLFW_KEY_DOWN =>
                        HelloSensor.walkBackward = isPressed
                        return
                    case GLFW.GLFW_KEY_LEFT =>
                        HelloSensor.walkLeft = isPressed
                        return
                    case GLFW.GLFW_KEY_RIGHT =>
                        HelloSensor.walkRight = isPressed
                        return
                    case GLFW.GLFW_KEY_UP =>
                        HelloSensor.walkForward = isPressed
                        return

                    case _ =>
                }
                super.onKeyboard(glfwKeyId, isPressed)
            }
        })
    }

    /**
     * Configure lighting and the background color.
     */
    private def configureLighting: Unit = {
        BaseApplication.setLightDirection(7f, 3f, 5f)

        // Set the background color to light blue:
        BaseApplication.setBackgroundColor(Constants.SKY_BLUE)
    }
}
