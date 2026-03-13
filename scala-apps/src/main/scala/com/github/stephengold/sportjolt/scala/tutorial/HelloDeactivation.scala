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
import com.github.stephengold.joltjni.BoxShape
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties
import com.github.stephengold.joltjni.readonly.ConstBody
import com.github.stephengold.sportjolt.BaseApplication
import com.github.stephengold.sportjolt.input.InputProcessor
import com.github.stephengold.sportjolt.physics.BasePhysicsApp
import com.github.stephengold.sportjolt.physics.PhysicsTickListener
import org.lwjgl.glfw.GLFW

/**
 * A simple example of rigid-body deactivation.
 * <p>
 * Builds upon HelloStaticBody.
 *
 * @author Stephen Gold sgold@sonic.net
 */
object HelloDeactivation {
    // *************************************************************************
    // fields

    /**
     * small, dynamic rigid body
     */
    private var dynamicCube: ConstBody = null
    /**
     * large, static rigid body
     */
    private var supportCube: ConstBody = null
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloDeactivation application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    def main(arguments: Array[String]): Unit = {
        val application = new HelloDeactivation
        application.start
    }
}

class HelloDeactivation extends BasePhysicsApp, PhysicsTickListener {
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

        return result
    }

    /**
     * Initialize the application. Invoked once.
     */
    override def initialize: Unit = {
        super.initialize

        BaseApplication.setVsync(true)
        configureInput
    }

    /**
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    override def populateSystem: Unit = {
        val bi = physicsSystem.getBodyInterface

        // Create a dynamic cube and add it to the system:
        val boxHalfExtent = 0.5f
        val smallCubeShape = new BoxShape(boxHalfExtent)
        val bcs = new BodyCreationSettings
        bcs.getMassPropertiesOverride.setMass(2f)
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia)
        bcs.setPosition(0.0, 4.0, 0.0)
        bcs.setShape(smallCubeShape)
        HelloDeactivation.dynamicCube = bi.createBody(bcs)
        bi.addBody(HelloDeactivation.dynamicCube, EActivation.Activate)
        /*
         * Create 2 static bodies and add them to the system.
         * The top body serves as a temporary support.
         */
        val cubeHalfExtent = 1f
        val largeCubeShape = new BoxShape(cubeHalfExtent)
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving)
        bcs.setPosition(0.0, 0.0, 0.0)
        bcs.setShape(largeCubeShape)
        HelloDeactivation.supportCube = bi.createBody(bcs)
        bi.addBody(HelloDeactivation.supportCube, EActivation.DontActivate)

        // The bottom body serves as a visual reference point:
        val ballRadius = 0.5f
        val ballShape = new SphereShape(ballRadius)
        bcs.setPosition(0.0, -2.0, 0.0)
        bcs.setShape(ballShape)
        val bottomBody = bi.createBody(bcs)
        bi.addBody(bottomBody, EActivation.DontActivate)

        // Visualize the shapes of all 3 bodies:
        BasePhysicsApp.visualizeShape(HelloDeactivation.dynamicCube)
        BasePhysicsApp.visualizeShape(HelloDeactivation.supportCube)
        BasePhysicsApp.visualizeShape(bottomBody)
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
        /*
         * Once the dynamic cube gets deactivated,
         * remove the support cube from the system:
         */
        val bi = system.getBodyInterface
        val supportId = HelloDeactivation.supportCube.getId
        if (bi.isAdded(supportId) && !HelloDeactivation.dynamicCube.isActive) {
            bi.removeBody(supportId)
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
        // do nothing
    }
    // *************************************************************************
    // private methods

    /**
     * Configure keyboard input during initialization.
     */
    private def configureInput: Unit = {
        BaseApplication.getInputManager.add(new InputProcessor {
            override def onKeyboard(glfwKeyId: Int, isPressed: Boolean): Unit = {
                glfwKeyId match {
                    case GLFW.GLFW_KEY_E =>
                        if (isPressed) {
                            // Reactivate the dynamic cube:
                            val bi = physicsSystem.getBodyInterface
                            bi.activateBody(HelloDeactivation.dynamicCube.getId)
                        }
                        return

                    case _ =>
                }
                super.onKeyboard(glfwKeyId, isPressed)
            }
        })
    }
}
