/*
 Copyright (c) 2025 Stephen Gold and Yanis Boudiaf

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

import com.github.stephengold.joltjni.Body
import com.github.stephengold.joltjni.BodyCreationSettings
import com.github.stephengold.joltjni.BroadPhaseLayerInterfaceTable
import com.github.stephengold.joltjni.ObjectLayerPairFilterTable
import com.github.stephengold.joltjni.ObjectVsBroadPhaseLayerFilterTable
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.Plane
import com.github.stephengold.joltjni.PlaneShape
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.Vec3
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.sportjolt.physics.BasePhysicsApp

/*
 * Drop a dynamic sphere onto a horizontal surface and visualize them both using
 * Sport-Jolt graphics.
 *
 * Builds upon HelloJoltJni.
 *
 * author:  Stephen Gold sgold@sonic.net
 */

private const val BALL_RADIUS = 0.3f
private const val GROUND_Y = -1f

private const val MAX_BODIES = 5_000
private const val NUM_BODY_MUTEXES = 0  // 0 means "use the default number"
private const val MAX_BODY_PAIRS = 65_536
private const val MAX_CONTACTS = 20_480

// For simplicity, use a single broadphase layer:
private const val NUM_BP_LAYERS = 1

private var ball: Body? = null

class HelloSport : BasePhysicsApp() {
    /*
     * Create the PhysicsSystem. Invoked once during initialization.
     */
    override fun createSystem(): PhysicsSystem {
        val ovoFilter = ObjectLayerPairFilterTable(numObjLayers)
        // Enable collisions between 2 moving bodies:
        ovoFilter.enableCollision(objLayerMoving, objLayerMoving)
        // Enable collisions between a moving body and a non-moving one:
        ovoFilter.enableCollision(objLayerMoving, objLayerNonMoving)
        // Disable collisions between 2 non-moving bodies:
        ovoFilter.disableCollision(objLayerNonMoving, objLayerNonMoving)

        // Map both object layers to broadphase layer 0:
        val layerMap = BroadPhaseLayerInterfaceTable(numObjLayers, NUM_BP_LAYERS)
        layerMap.mapObjectToBroadPhaseLayer(objLayerMoving, 0)
        layerMap.mapObjectToBroadPhaseLayer(objLayerNonMoving, 0)

        // Rules for colliding object layers with broadphase layers:
        val ovbFilter = ObjectVsBroadPhaseLayerFilterTable(
            layerMap, NUM_BP_LAYERS, ovoFilter, numObjLayers)

        val result = PhysicsSystem()

        // Set high limits, even though this sample app uses only 2 bodies:
        result.init(MAX_BODIES, NUM_BODY_MUTEXES, MAX_BODY_PAIRS, MAX_CONTACTS,
            layerMap, ovbFilter, ovoFilter)

        return result
    }

    /*
     * Initialize the application. Invoked once.
     */
    override fun initialize() {
        super.initialize()
        setVsync(true)
    }

    /*
     * Populate the PhysicsSystem with bodies. Invoked once during initialization.
     */
    override fun populateSystem() {
        val bi = physicsSystem.getBodyInterface()

        // Add a static horizontal plane at y=-1:
        val normal = Vec3.sAxisY()
        val plane = Plane(normal, -GROUND_Y)
        val floorShape = PlaneShape(plane)
        val bcs = BodyCreationSettings()
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(objLayerNonMoving)
        bcs.setShape(floorShape)
        val floor = bi.createBody(bcs)
        bi.addBody(floor, EActivation.DontActivate)

        // Add a sphere-shaped, dynamic, rigid body at the origin:
        val ballShape = SphereShape(BALL_RADIUS)
        bcs.setMotionType(EMotionType.Dynamic)
        bcs.setObjectLayer(objLayerMoving)
        bcs.setShape(ballShape)
        ball = bi.createBody(bcs)
        bi.addBody(ball, EActivation.Activate)

        // Visualize the shapes of both rigid bodies:
        visualizeShape(floor)
        visualizeShape(ball)
    }
}

/**
 * Main entry point for the HelloSportKt application.
 */
fun main() {
    val application = HelloSport()
    application.start()
    /*
     * During initialization, BasePhysicsApp loads the native library
     * and invokes createSystem() and populateSystem().
     */
}
