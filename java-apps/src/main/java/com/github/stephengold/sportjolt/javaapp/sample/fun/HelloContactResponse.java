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
package com.github.stephengold.sportjolt.javaapp.sample.fun;

import com.github.stephengold.joltjni.Body;
import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.BodyInterface;
import com.github.stephengold.joltjni.BoxShape;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.SphereShape;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties;
import com.github.stephengold.joltjni.readonly.ConstBody;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.sportjolt.BaseApplication;
import com.github.stephengold.sportjolt.input.InputProcessor;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.FunctionalPhysicsApp;
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
final public class HelloContactResponse {
    // *************************************************************************
    // fields

    /**
     * falling ball
     */
    private static Body ball;
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private HelloContactResponse() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloContactResponse application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        FunctionalPhysicsApp fpa = new FunctionalPhysicsApp();

        fpa.setCreateSystem((app) -> {
            // For simplicity, use a single broadphase layer:
            int maxBodies = 2;
            int numBpLayers = 1;
            PhysicsSystem result = app.createSystem(maxBodies, numBpLayers);

            return result;
        });

        fpa.setInitialize((app) -> {
            BaseApplication.setVsync(true);
            configureInput();
        });

        fpa.setPopulateSystem((app) -> {
            BodyInterface bi = app.getPhysicsSystem().getBodyInterface();

            // Add a static box to the system, to serve as a platform:
            float boxHalfExtent = 3f;
            ConstShape boxShape = new BoxShape(boxHalfExtent);
            BodyCreationSettings bcs1 = new BodyCreationSettings();
            bcs1.setMotionType(EMotionType.Static);
            bcs1.setObjectLayer(BasePhysicsApp.objLayerNonMoving);
            bcs1.setPosition(0., -4., 0.);
            bcs1.setShape(boxShape);
            ConstBody box = bi.createBody(bcs1);
            bi.addBody(box, EActivation.DontActivate);

            // Add a dynamic ball to the system:
            float ballRadius = 1f;
            ConstShape ballShape = new SphereShape(ballRadius);
            BodyCreationSettings bcs2 = new BodyCreationSettings();
            bcs2.getMassPropertiesOverride().setMass(2f);
            bcs2.setAllowSleeping(false); // Disable sleeping for clarity.
            bcs2.setOverrideMassProperties(
                    EOverrideMassProperties.CalculateInertia);
            bcs2.setPosition(0., 4., 0.);
            bcs2.setShape(ballShape);
            ball = bi.createBody(bcs2);
            bi.addBody(ball, EActivation.Activate);

            // Visualize the shapes of both rigid bodies:
            BasePhysicsApp.visualizeShape(ball);
            BasePhysicsApp.visualizeShape(box);
        });

        fpa.start("HelloContactResponse");
    }
    // *************************************************************************
    // private methods

    /**
     * Configure keyboard input during initialization.
     */
    private static void configureInput() {
        BaseApplication.getInputManager().add(new InputProcessor() {
            @Override
            public void onKeyboard(int glfwKeyId, boolean isPressed) {
                if (glfwKeyId == GLFW.GLFW_KEY_E) {
                    if (isPressed) {
                        // Disable the ball's contact response:
                        ball.setIsSensor(true);
                    }
                    return;
                }
                super.onKeyboard(glfwKeyId, isPressed);
            }
        });
    }
}
