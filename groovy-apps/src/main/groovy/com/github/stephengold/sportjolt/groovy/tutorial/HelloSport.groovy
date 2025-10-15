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
package com.github.stephengold.sportjolt.groovy.tutorial

import com.github.stephengold.joltjni.Body
import com.github.stephengold.joltjni.BodyCreationSettings
import com.github.stephengold.joltjni.BodyInterface
import com.github.stephengold.joltjni.BroadPhaseLayerInterfaceTable
import com.github.stephengold.joltjni.ObjectLayerPairFilterTable
import com.github.stephengold.joltjni.ObjectVsBroadPhaseLayerFilter
import com.github.stephengold.joltjni.ObjectVsBroadPhaseLayerFilterTable
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.Plane
import com.github.stephengold.joltjni.PlaneShape
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.Vec3
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.joltjni.readonly.ConstBody
import com.github.stephengold.joltjni.readonly.ConstPlane
import com.github.stephengold.joltjni.readonly.ConstShape
import com.github.stephengold.joltjni.readonly.Vec3Arg
import com.github.stephengold.sportjolt.physics.BasePhysicsApp
import groovy.transform.CompileStatic

/**
 * Drop a dynamic sphere onto a horizontal surface and visualize them both using
 * Sport-Jolt graphics.
 * <p>
 * Builds upon HelloJoltJni.
 *
 * @author Stephen Gold sgold@sonic.net
 */
@CompileStatic
final public class HelloSport extends BasePhysicsApp {

    // *************************************************************************
    // fields

    /**
     * falling rigid body
     */
    private static ConstBody ball
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloSport application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    static void main(String[] arguments) {
        var application = new HelloSport()
        application.start()
        /*
         * During initialization, BasePhysicsApp loads the native library
         * and invokes createSystem() and populateSystem().
         */
    }
    // *************************************************************************
    // BasePhysicsApp methods

    /**
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new object
     */
    @Override
    protected PhysicsSystem createSystem() {
        // For simplicity, use a single broadphase layer:
        var numBpLayers = 1

        var ovoFilter = new ObjectLayerPairFilterTable(numObjLayers)
        // Enable collisions between 2 moving bodies:
        ovoFilter.enableCollision(objLayerMoving, objLayerMoving)
        // Enable collisions between a moving body and a non-moving one:
        ovoFilter.enableCollision(objLayerMoving, objLayerNonMoving)
        // Disable collisions between 2 non-moving bodies:
        ovoFilter.disableCollision(objLayerNonMoving, objLayerNonMoving)

        // Map both object layers to broadphase layer 0:
        var layerMap = new BroadPhaseLayerInterfaceTable(numObjLayers, numBpLayers)
        layerMap.mapObjectToBroadPhaseLayer(objLayerMoving, 0)
        layerMap.mapObjectToBroadPhaseLayer(objLayerNonMoving, 0)

        // Rules for colliding object layers with broadphase layers:
        var ovbFilter = new ObjectVsBroadPhaseLayerFilterTable(
            layerMap, numBpLayers, ovoFilter, numObjLayers)

        var result = new PhysicsSystem()

        // Set high limits, even though this sample app uses only 2 bodies:
        var maxBodies = 5_000
        var numBodyMutexes = 0 // 0 means "use the default number"
        var maxBodyPairs = 65_536
        var maxContacts = 20_480
        result.init(maxBodies, numBodyMutexes, maxBodyPairs, maxContacts,
            layerMap, ovbFilter, ovoFilter)

        return result
    }

    /**
     * Initialize the application. Invoked once.
     */
    @Override
    protected void initialize() {
        super.initialize()
        vsync = true
    }

    /**
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    @Override
    protected void populateSystem() {
        var bi = physicsSystem.bodyInterface

        // Add a static horizontal plane at y=-1:
        var groundY = -1f
        var normal = Vec3.sAxisY()
        var plane = new Plane(normal, -groundY)
        var floorShape = new PlaneShape(plane)
        var bcs = new BodyCreationSettings()
        bcs.motionType = EMotionType.Static
        bcs.objectLayer = objLayerNonMoving
        bcs.shape = floorShape
        var floor = bi.createBody(bcs)
        bi.addBody(floor, EActivation.DontActivate)

        // Add a sphere-shaped, dynamic, rigid body at the origin:
        var ballRadius = 0.3f
        var ballShape = new SphereShape(ballRadius)
        bcs.motionType = EMotionType.Dynamic
        bcs.objectLayer = objLayerMoving
        bcs.shape = ballShape
        ball = bi.createBody(bcs)
        bi.addBody(ball, EActivation.Activate)

        // Visualize the shapes of both rigid bodies:
        visualizeShape(floor)
        visualizeShape(ball)
    }

}
