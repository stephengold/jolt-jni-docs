/*
 Copyright (c) 2020-2025 Stephen Gold and Yanis Boudiaf

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
import com.github.stephengold.joltjni.readonly.ConstBody
import com.github.stephengold.sportjolt.BaseApplication
import com.github.stephengold.sportjolt.physics.BasePhysicsApp

/**
 * Drop a dynamic sphere onto a horizontal surface and visualize them both using
 * Sport-Jolt graphics.
 * <p>
 * Builds upon HelloJoltJni.
 *
 * @author Stephen Gold sgold@sonic.net
 */
object HelloSport {
    // *************************************************************************
    // fields

    /**
     * falling rigid body
     */
    private var ball: ConstBody = null
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloSport application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    def main(arguments: Array[String]): Unit = {
        val application = new HelloSport
        application.start
        /*
         * During initialization, BasePhysicsApp loads the native library
         * and invokes createSystem and populateSystem.
         */
    }
}

class HelloSport extends BasePhysicsApp {
    /**
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new object
     */
    override def createSystem: PhysicsSystem = {
        // For simplicity, use a single broadphase layer:
        val numBpLayers = 1

        val ovoFilter = new ObjectLayerPairFilterTable(BasePhysicsApp.numObjLayers)
        // Enable collisions between 2 moving bodies:
        ovoFilter.enableCollision(BasePhysicsApp.objLayerMoving, BasePhysicsApp.objLayerMoving)
        // Enable collisions between a moving body and a non-moving one:
        ovoFilter.enableCollision(BasePhysicsApp.objLayerMoving, BasePhysicsApp.objLayerNonMoving)
        // Disable collisions between 2 non-moving bodies:
        ovoFilter.disableCollision(BasePhysicsApp.objLayerNonMoving, BasePhysicsApp.objLayerNonMoving)

        // Map both object layers to broadphase layer 0:
        val layerMap = new BroadPhaseLayerInterfaceTable(BasePhysicsApp.numObjLayers, numBpLayers)
        layerMap.mapObjectToBroadPhaseLayer(BasePhysicsApp.objLayerMoving, 0)
        layerMap.mapObjectToBroadPhaseLayer(BasePhysicsApp.objLayerNonMoving, 0)

        // Rules for colliding object layers with broadphase layers:
        val ovbFilter = new ObjectVsBroadPhaseLayerFilterTable(
                        layerMap, numBpLayers, ovoFilter, BasePhysicsApp.numObjLayers)

        val result = new PhysicsSystem

        // Set high limits, even though this sample app uses only 2 bodies:
        val maxBodies = 5_000
        val numBodyMutexes = 0 // 0 means "use the default number"
        val maxBodyPairs = 65_536
        val maxContacts = 20_480
        result.init(maxBodies, numBodyMutexes, maxBodyPairs, maxContacts,
                layerMap, ovbFilter, ovoFilter)
        result
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

        // Add a static horizontal plane at y=-1:
        val groundY = -1f
        val normal = Vec3.sAxisY
        val plane = new Plane(normal, -groundY)
        val floorShape = new PlaneShape(plane)
        val bcs = new BodyCreationSettings
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving)
        bcs.setShape(floorShape)
        val floor = bi.createBody(bcs)
        bi.addBody(floor, EActivation.DontActivate)

        // Add a sphere-shaped, dynamic, rigid body at the origin:
        val ballRadius = 0.3f
        val ballShape = new SphereShape(ballRadius)
        bcs.setMotionType(EMotionType.Dynamic)
        bcs.setObjectLayer(BasePhysicsApp.objLayerMoving)
        bcs.setShape(ballShape)
        HelloSport.ball = bi.createBody(bcs)
        bi.addBody(HelloSport.ball, EActivation.Activate)

        // Visualize the shapes of both bodies:
        BasePhysicsApp.visualizeShape(floor)
        BasePhysicsApp.visualizeShape(HelloSport.ball)
    }
}
