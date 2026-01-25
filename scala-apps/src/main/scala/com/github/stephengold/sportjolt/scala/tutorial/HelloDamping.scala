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
import com.github.stephengold.joltjni.RVec3
import com.github.stephengold.joltjni.Vec3
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties
import com.github.stephengold.joltjni.operator.Op
import com.github.stephengold.sportjolt.BaseApplication
import com.github.stephengold.sportjolt.physics.BasePhysicsApp

/**
 * A simple example illustrating the effect of damping on dynamic rigid bodies.
 * <p>
 * Builds upon HelloRigidBody.
 *
 * @author Stephen Gold sgold@sonic.net
 */
object HelloDamping {
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloDamping application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    def main(arguments: Array[String]): Unit = {
        val application = new HelloDamping
        application.start
    }
}

class HelloDamping extends BasePhysicsApp {
    /**
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new object
     */
    override def createSystem: PhysicsSystem = {
        // For simplicity, use a single broadphase layer:
        val maxBodies = 4
        val numBpLayers = 1
        val result = createSystem(maxBodies, numBpLayers)

        // For clarity, disable gravity:
        result.setGravity(0f, 0f, 0f)

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

        // Create a collision shape for unit cubes:
        val cubeHalfExtent = 0.5f
        val cubeShape = new BoxShape(cubeHalfExtent)

        val bcs = new BodyCreationSettings
        bcs.setAllowSleeping(false) // Disable sleeping for clarity.
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia)
                .getMassPropertiesOverride.setMass(2f)
        bcs.setObjectLayer(BasePhysicsApp.objLayerMoving)
                .setShape(cubeShape)
        /*
         * Create 4 cubes (dynamic rigid bodies) and add them to the system.
         * Give each cube its own set of damping parameters.
         * Locate the cubes 4 meters apart, center to center.
         */
        val numCubes = 4
        val cube = new Array[Body](numCubes)

        bcs.setAngularDamping(0f)
        bcs.setLinearDamping(0f)
        bcs.setPosition(0.0, +2.0, 0.0)
        cube(0) = bi.createBody(bcs)
        bi.addBody(cube(0), EActivation.Activate)

        bcs.setAngularDamping(0.9f)
        bcs.setLinearDamping(0f)
        bcs.setPosition(4.0, +2.0, 0.0)
        cube(1) = bi.createBody(bcs)
        bi.addBody(cube(1), EActivation.Activate)

        bcs.setAngularDamping(0f)
        bcs.setLinearDamping(0.9f)
        bcs.setPosition(0.0, -2.0, 0.0)
        cube(2) = bi.createBody(bcs)
        bi.addBody(cube(2), EActivation.Activate)

        bcs.setAngularDamping(0.9f)
        bcs.setLinearDamping(0.9f)
        bcs.setPosition(4.0, -2.0, 0.0)
        cube(3) = bi.createBody(bcs)
        bi.addBody(cube(3), EActivation.Activate)

        val angDamping = cube(2).getMotionProperties.getAngularDamping
        assert(angDamping == 0f, "angDamping = " + angDamping)
        val linDamping = cube(2).getMotionProperties.getLinearDamping
        assert(linDamping == 0.9f, "linDamping = " + linDamping)
        /*
         * Apply an off-center impulse to each cube,
         * causing it to drift and spin:
         */
        val impulse = new Vec3(-1f, 0f, 0f)
        val offset = new RVec3(0.0, 1.0, 1.0)
        for (cubeIndex <- 0 to 3) do
            val center = cube(cubeIndex).getCenterOfMassPosition
            cube(cubeIndex).addImpulse(impulse, Op.plus(center, offset))

        // Visualize the shapes of all 4 cubes:
        for (cubeIndex <- 0 to 3) do
            BasePhysicsApp.visualizeShape(cube(cubeIndex))
    }
}
