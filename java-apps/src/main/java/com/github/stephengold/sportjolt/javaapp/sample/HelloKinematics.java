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
package com.github.stephengold.sportjolt.javaapp.sample;

import com.github.stephengold.joltjni.Body;
import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.BodyInterface;
import com.github.stephengold.joltjni.BroadPhaseLayerInterface;
import com.github.stephengold.joltjni.MapObj2Bp;
import com.github.stephengold.joltjni.ObjVsBpFilter;
import com.github.stephengold.joltjni.ObjVsObjFilter;
import com.github.stephengold.joltjni.ObjectLayerPairFilter;
import com.github.stephengold.joltjni.ObjectVsBroadPhaseLayerFilter;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.SphereShape;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.PhysicsTickListener;

/**
 * A simple example combining kinematic and dynamic rigid bodies.
 * <p>
 * Builds upon HelloStaticBody.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelloKinematics
        extends BasePhysicsApp
        implements PhysicsTickListener {
    // *************************************************************************
    // fields

    /**
     * kinematic ball, orbiting the origin
     */
    private static Body kineBall;
    /**
     * physics-simulation time (in seconds, &ge;0)
     */
    private static float elapsedTime = 0f;
    // *************************************************************************
    // constructors

    /**
     * Instantiate the HelloKinematics application.
     * <p>
     * This no-arg constructor was made explicit to avoid javadoc warnings from
     * JDK 18+.
     */
    public HelloKinematics() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloKinematics application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        HelloKinematics application = new HelloKinematics();
        application.start();
    }
    // *************************************************************************
    // BasePhysicsApp methods

    /**
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new object
     */
    @Override
    public PhysicsSystem createSystem() {
        // a single broadphase layer for simplicity:
        int numBpLayers = 1;
        BroadPhaseLayerInterface mapObj2Bp
                = new MapObj2Bp(numObjLayers, numBpLayers)
                        .add(objLayerNonMoving, 0)
                        .add(objLayerMoving, 0);
        ObjectVsBroadPhaseLayerFilter objVsBpFilter
                = new ObjVsBpFilter(numObjLayers, numBpLayers);
        ObjectLayerPairFilter objVsObjFilter = new ObjVsObjFilter(numObjLayers)
                .disablePair(objLayerNonMoving, objLayerNonMoving);

        int maxBodies = 2;
        int numBodyMutexes = 0; // 0 means "use the default number"
        int maxBodyPairs = 3;
        int maxContacts = 3;
        PhysicsSystem result = new PhysicsSystem();
        result.init(maxBodies, numBodyMutexes, maxBodyPairs, maxContacts,
                mapObj2Bp, objVsBpFilter, objVsObjFilter);

        // To enable the callbacks, register this app as a tick listener:
        addTickListener(this);

        return result;
    }

    /**
     * Populate the PhysicsSystem. Invoked once during initialization.
     */
    @Override
    public void populateSystem() {
        BodyInterface bi = physicsSystem.getBodyInterface();

        // Create a collision shape for balls:
        float ballRadius = 1f;
        ConstShape ballShape = new SphereShape(ballRadius);

        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.getMassPropertiesOverride().setMass(2f);
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia);
        bcs.setShape(ballShape);

        // Create a dynamic body and add it to the system:
        bcs.setPosition(0., 4., 0.);
        Body dynaBall = bi.createBody(bcs);
        bi.addBody(dynaBall, EActivation.Activate);

        // Create a kinematic body and add it to the system:
        bcs.setMotionType(EMotionType.Kinematic);
        bcs.setPosition(0., 0., 0.);
        kineBall = bi.createBody(bcs);
        bi.addBody(kineBall, EActivation.Activate);

        assert kineBall.isKinematic();

        // Visualize the shapes of both bodies.
        visualizeShape(dynaBall);
        visualizeShape(kineBall);
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
    @Override
    public void physicsTick(PhysicsSystem system, float timeStep) {
        // do nothing
    }

    /**
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system
     * is stepped.
     *
     * @param system the system that's about to to be stepped (not null)
     * @param timeStep the duration of the simulation step (in seconds, &ge;0)
     */
    @Override
    public void prePhysicsTick(PhysicsSystem system, float timeStep) {
        // Make the kinematic ball orbit the origin:
        double orbitalPeriod = 0.8; // seconds
        double phaseAngle = 2. * elapsedTime * Math.PI / orbitalPeriod;

        double orbitRadius = 0.4; // meters
        double x = orbitRadius * Math.sin(phaseAngle);
        double y = orbitRadius * Math.cos(phaseAngle);
        RVec3 location = new RVec3(x, y, 0.);
        kineBall.moveKinematic(location, new Quat(), timeStep);

        elapsedTime += timeStep;
    }
}
