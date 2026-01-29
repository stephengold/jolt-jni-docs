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
import com.github.stephengold.joltjni.CapsuleShape
import com.github.stephengold.joltjni.CharacterSettings
import com.github.stephengold.joltjni.HeightFieldShapeSettings
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.Quat
import com.github.stephengold.joltjni.RVec3
import com.github.stephengold.joltjni.Vec3
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.joltjni.readonly.ConstBody
import com.github.stephengold.sportjolt.BaseApplication
import com.github.stephengold.sportjolt.Constants
import com.github.stephengold.sportjolt.Utils
import com.github.stephengold.sportjolt.input.InputProcessor
import com.github.stephengold.sportjolt.input.RotateMode
import com.github.stephengold.sportjolt.physics.BasePhysicsApp
import com.github.stephengold.sportjolt.physics.PhysicsTickListener
import org.joml.Vector4f
import org.lwjgl.glfw.GLFW

/**
 * A simple example of character physics.
 * <p>
 * Press the W key to walk. Press the space bar to jump.
 * <p>
 * Builds upon HelloCharacter.
 *
 * @author Stephen Gold sgold@sonic.net
 */
object HelloWalk {
    // *************************************************************************
    // fields

    /**
     * true when the space bar is pressed, otherwise false
     */
    private var jumpRequested: Boolean = false
    /**
     * true when the W key is pressed, otherwise false
     */
    private var walkRequested: Boolean = false
    /**
     * character being tested
     */
    private var character: com.github.stephengold.joltjni.Character = null
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloWalk application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    def main(arguments: Array[String]): Unit = {
        val application = new HelloWalk
        application.start
    }
}

class HelloWalk extends BasePhysicsApp, PhysicsTickListener {
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

        val startLocation = new RVec3(-73.6, 19.09, -45.58)
        val userData = 0L
        HelloWalk.character = new com.github.stephengold.joltjni.Character(
                settings, startLocation, new Quat, userData, physicsSystem)
        HelloWalk.character.addToPhysicsSystem

        // Add a static heightmap to represent the ground:
        val ground = addTerrain

        // Visualize the shapes of both physics objects:
        BasePhysicsApp.visualizeShape(HelloWalk.character)
        val darkGreen = new Vector4f(0f, 0.3f, 0f, 1f)
        BasePhysicsApp.visualizeShape(ground)
                .setColor(darkGreen)
                .setSpecularColor(Constants.BLACK)
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
        HelloWalk.character.postSimulation(maxSeparation)

        val location = HelloWalk.character.getPosition
        BaseApplication.cam.setLocation(location)
    }

    /**
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system
     * is stepped.
     *
     * @param system the system that's about to be stepped (not null)
     * @param timeStep the duration of the simulation step (in seconds, &ge;0)
     */
    override def prePhysicsTick(system: PhysicsSystem, timeStep: Float): Unit = {
        val velocity = HelloWalk.character.getLinearVelocity

        // Clear any horizontal motion from the previous simulation step:
        velocity.setX(0f)
        velocity.setZ(0f)

        // If the character is supported, make it respond to keyboard input:
        if (HelloWalk.character.isSupported) {
            if (HelloWalk.jumpRequested) {
                // Cause the character to jump:
                velocity.setY(8f)

            } else if (HelloWalk.walkRequested) {
                // Walk in the camera's forward direction:
                val forward = BaseApplication.cam.getDirection
                val walkSpeed = 7f
                velocity.setX(walkSpeed * forward.getX)
                velocity.setZ(walkSpeed * forward.getZ)
            }
        }

        HelloWalk.character.setLinearVelocity(velocity)
    }
    // *************************************************************************
    // private methods

    /**
     * Add a static heightfield rigid body to the system.
     *
     * @return the new body (not null)
     */
    private def addTerrain: ConstBody = {
       // Generate an array of heights from a PNG image on the classpath:
        val resourceName = "/Textures/Terrain/splat/mountains512.png"
        val image = Utils.loadResourceAsImage(resourceName)

        val maxHeight = 51f
        val heightBuffer = Utils.toHeightBuffer(image, maxHeight)

        // Construct a static rigid body based on the array of heights:
        val numFloats = heightBuffer.capacity

        val offset = new Vec3(-256f, 0f, -256f)
        val scale = new Vec3(1f, 1f, 1f)
        val sampleCount = 512
        assert(numFloats == sampleCount * sampleCount , numFloats)
        val ss = new HeightFieldShapeSettings(
                heightBuffer, offset, scale, sampleCount)

        val shapeRef = ss.create.get
        val bcs = new BodyCreationSettings
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving)
        bcs.setShape(shapeRef)

        val bi = physicsSystem.getBodyInterface
        val result = bi.createBody(bcs)
        bi.addBody(result, EActivation.DontActivate)

        return result
    }

    /**
     * Configure the projection and CIP during initialization.
     */
    private def configureCamera: Unit = {
        BaseApplication.getCameraInputProcessor.setRotationMode(RotateMode.DragLMB)
        BaseApplication.getProjection.setFovyDegrees(30f)

        // Bring the near plane closer to reduce clipping:
        BaseApplication.getProjection.setZClip(0.1f, 1_000f)
    }

    /**
     * Configure keyboard input during initialization.
     */
    private def configureInput: Unit = {
        BaseApplication.getInputManager.add(new InputProcessor {
            override def onKeyboard(glfwKeyId: Int, isPressed: Boolean): Unit = {
                glfwKeyId match {
                    case GLFW.GLFW_KEY_SPACE =>
                        HelloWalk.jumpRequested = isPressed
                        return

                    case GLFW.GLFW_KEY_W =>
                        HelloWalk.walkRequested = isPressed
                        // This overrides the CameraInputProcessor.
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
        BaseApplication.setLightColor(0.3f, 0.3f, 0.3f)
        BaseApplication.setLightDirection(7f, 3f, 5f)

        // Set the background color to light blue:
        BaseApplication.setBackgroundColor(Constants.SKY_BLUE)
    }
}
