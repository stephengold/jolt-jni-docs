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
import com.github.stephengold.joltjni.BoxShape
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties
import com.github.stephengold.joltjni.readonly.ConstBody
import com.github.stephengold.sportjolt.input.InputProcessor
import com.github.stephengold.sportjolt.physics.BasePhysicsApp
import com.github.stephengold.sportjolt.physics.PhysicsTickListener
import org.lwjgl.glfw.GLFW

/*
 * A simple example of rigid-body deactivation.
 *
 * Builds upon HelloStaticBodyKt.
 *
 * author:  Stephen Gold sgold@sonic.net
 */

private const val BALL_MASS = 2f
private const val BALL_RADIUS = 1f
private const val BOX_HALF_EXTENT = 0.5f
private const val CUBE_HALF_EXTENT = 1f
private const val MAX_BODIES = 3

// For simplicity, use a single broadphase layer:
private const val NUM_BP_LAYERS = 1

/**
 * A simple example of 2 colliding balls.
 */
class HelloDeactivation : BasePhysicsApp(), PhysicsTickListener {
    // *************************************************************************
    // fields

    /**
     * small, dynamic rigid body
     */
    var dynamicCube: ConstBody? = null

    /**
     * large, static rigid body
     */
    var supportCube: ConstBody? = null

    /*
     * Create the PhysicsSystem. Invoked once during initialization.
     */
    override fun createSystem(): PhysicsSystem {
        val result = createSystem(MAX_BODIES, NUM_BP_LAYERS)

        // To enable the callbacks, register the application as a tick listener:
        addTickListener(this)

        return result
    }

    /*
     * Initialize the application. Invoked once.
     */
    override fun initialize(): Unit {
        super.initialize()

        setVsync(true)
        configureInput()
    }

    /*
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    override fun populateSystem(): Unit {
        val bi = physicsSystem.getBodyInterface()

        // Create a dynamic cube and add it to the system:
        val smallCubeShape = BoxShape(BOX_HALF_EXTENT)

        val bcs = BodyCreationSettings()
        bcs.getMassPropertiesOverride().setMass(BALL_MASS)
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia)
        bcs.setPosition(0.0, 4.0, 0.0)
        bcs.setShape(smallCubeShape)
        dynamicCube = bi.createBody(bcs)
        bi.addBody(dynamicCube, EActivation.Activate)
        /*
         * Create 2 static bodies and add them to the system.
         * The top body serves as a temporary support.
         */
        val largeCubeShape = BoxShape(CUBE_HALF_EXTENT)
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving)
        bcs.setPosition(0.0, 0.0, 0.0)
        bcs.setShape(largeCubeShape)
        supportCube = bi.createBody(bcs)
        bi.addBody(supportCube, EActivation.DontActivate)

        // The bottom body serves as a visual reference point:
        val ballShape = SphereShape(BALL_RADIUS)
        bcs.setPosition(0.0, -2.0, 0.0)
        bcs.setShape(ballShape)
        val bottomBody = bi.createBody(bcs)
        bi.addBody(bottomBody, EActivation.DontActivate)

        // Visualize the shapes of all 3 bodies:
        visualizeShape(dynamicCube)
        visualizeShape(supportCube)
        visualizeShape(bottomBody)
    }
    // *************************************************************************
    // PhysicsTickListener methods

    /*
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) after the system
     * has been stepped.
     */
    override fun physicsTick(system: PhysicsSystem, timeStep: Float): Unit {
        /*
         * Once the dynamic cube gets deactivated,
         * remove the support cube from the system:
         */
        val bi = system.getBodyInterface()
        val supportId = supportCube!!.getId()
        if (bi.isAdded(supportId) && !dynamicCube!!.isActive()) {
            bi.removeBody(supportId)
        }
    }

    /*
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system
     * is stepped.
     */
    override fun prePhysicsTick(system: PhysicsSystem, timeStep: Float): Unit {
        // do nothing
    }
    // *************************************************************************
    // private methods

    /*
     * Configure keyboard input during initialization.
     */
    private fun configureInput(): Unit {
        getInputManager().add(object : InputProcessor() {
            override fun onKeyboard(glfwKeyId: Int, isPressed: Boolean): Unit {
                when (glfwKeyId) {
                    GLFW.GLFW_KEY_E -> {
                        if (isPressed) {
                            // Reactivate the dynamic cube:
                            val bi = physicsSystem.getBodyInterface()
                            bi!!.activateBody(dynamicCube!!.getId())
                        }
                        return
                    }
                }
                super.onKeyboard(glfwKeyId, isPressed)
            }
        })
    }
}

/**
 * Main entry point for the HelloDeactivationKt application.
 */
fun main() {
    val application = HelloDeactivation()
    application.start()
}
