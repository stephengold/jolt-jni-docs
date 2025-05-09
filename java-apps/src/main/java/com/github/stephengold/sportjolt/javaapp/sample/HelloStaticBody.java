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
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.SphereShape;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;

/**
 * A simple example combining static and dynamic rigid bodies.
 * <p>
 * Builds upon HelloRigidBody.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelloStaticBody extends BasePhysicsApp {
    // *************************************************************************
    // constructors

    /**
     * Instantiate the HelloStaticBody application.
     * <p>
     * This no-arg constructor was made explicit to avoid javadoc warnings from
     * JDK 18+.
     */
    public HelloStaticBody() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloStaticBody application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        HelloStaticBody application = new HelloStaticBody();
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
    protected PhysicsSystem createSystem() {
        // For simplicity, use a single broadphase layer:
        int numBpLayers = 1;
        int maxBodies = 2;
        PhysicsSystem result = createSystem(maxBodies, numBpLayers);

        return result;
    }

    /**
     * Initialize the application. Invoked once.
     */
    @Override
    protected void initialize() {
        super.initialize();
        setVsync(true);
    }

    /**
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    @Override
    protected void populateSystem() {
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

        // Create a static body and add it to the system:
        bcs.setMotionType(EMotionType.Static);
        bcs.setObjectLayer(objLayerNonMoving);
        bcs.setPosition(0.1, 0., 0.);
        Body statBall = bi.createBody(bcs);
        bi.addBody(statBall, EActivation.DontActivate);

        assert statBall.isStatic();

        // Visualize the shapes of both bodies:
        visualizeShape(dynaBall);
        visualizeShape(statBall);
    }
}
