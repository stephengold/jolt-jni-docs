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
import com.github.stephengold.joltjni.BoxShape;
import com.github.stephengold.joltjni.BroadPhaseLayerInterface;
import com.github.stephengold.joltjni.MapObj2Bp;
import com.github.stephengold.joltjni.ObjVsBpFilter;
import com.github.stephengold.joltjni.ObjVsObjFilter;
import com.github.stephengold.joltjni.ObjectLayerPairFilter;
import com.github.stephengold.joltjni.ObjectVsBroadPhaseLayerFilter;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.SphereShape;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.sportjolt.input.InputProcessor;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import org.lwjgl.glfw.GLFW;

/**
 * A simple demonstration of contact response.
 * <p>
 * Press the E key to disable the ball's contact response. Once this happens,
 * the gray (static) box no longer exerts any contact force on the ball. Gravity
 * takes over, and the ball falls through.
 * <p>
 * Builds upon HelloStaticBody.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelloContactResponse extends BasePhysicsApp {
    // *************************************************************************
    // fields

    /**
     * collision object for the dynamic ball
     */
    private static Body ball;
    // *************************************************************************
    // constructors

    /**
     * Instantiate the HelloContactResponse application.
     * <p>
     * This no-arg constructor was made explicit to avoid javadoc warnings from
     * JDK 18+.
     */
    public HelloContactResponse() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloContactResponse application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        HelloContactResponse application = new HelloContactResponse();
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

        return result;
    }

    /**
     * Initialize the application. Invoked once.
     */
    @Override
    public void initialize() {
        super.initialize();
        configureInput();
    }

    /**
     * Populate the PhysicsSystem. Invoked once during initialization.
     */
    @Override
    public void populateSystem() {
        BodyInterface bi = physicsSystem.getBodyInterface();

        // Add a static box to the system, to serve as a horizontal platform.
        float boxHalfExtent = 3f;
        ConstShape boxShape = new BoxShape(boxHalfExtent);
        BodyCreationSettings bcs1 = new BodyCreationSettings();
        bcs1.setMotionType(EMotionType.Static);
        bcs1.setObjectLayer(objLayerNonMoving);
        bcs1.setPosition(0., -4., 0.);
        bcs1.setShape(boxShape);
        Body box = bi.createBody(bcs1);
        bi.addBody(box, EActivation.DontActivate);

        // Add a dynamic ball to the system.
        float ballRadius = 1f;
        ConstShape ballShape = new SphereShape(ballRadius);
        BodyCreationSettings bcs2 = new BodyCreationSettings();
        bcs2.getMassPropertiesOverride().setMass(2f);
        bcs2.setAllowSleeping(false);
        bcs2.setOverrideMassProperties(
                EOverrideMassProperties.CalculateInertia);
        bcs2.setPosition(0., 4., 0.);
        bcs2.setShape(ballShape);
        ball = bi.createBody(bcs2);
        bi.addBody(ball, EActivation.Activate);

        // Visualize the shapes of both bodies.
        visualizeShape(ball);
        visualizeShape(box);
    }
    // *************************************************************************
    // private methods

    /**
     * Configure keyboard input during startup.
     */
    private void configureInput() {
        getInputManager().add(new InputProcessor() {
            @Override
            public void onKeyboard(int glfwKeyId, boolean isPressed) {
                if (glfwKeyId == GLFW.GLFW_KEY_E) {
                    if (isPressed) {
                        // Disable the ball's contact response.
                        ball.setIsSensor(true);
                    }
                    return;
                }
                super.onKeyboard(glfwKeyId, isPressed);
            }
        });
    }
}
