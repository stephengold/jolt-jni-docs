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
 * A simple example of rigid-body deactivation.
 * <p>
 * Builds upon HelloStaticBody.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class HelloDeactivation {
    // *************************************************************************
    // fields

    /**
     * small, dynamic rigid body
     */
    private static ConstBody dynamicCube;
    /**
     * large, static rigid body
     */
    private static ConstBody supportCube;
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private HelloDeactivation() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloDeactivation application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        FunctionalPhysicsApp fpa = new FunctionalPhysicsApp();

        fpa.setCreateSystem((app) -> {
            // For simplicity, use a single broadphase layer:
            int maxBodies = 3;
            int numBpLayers = 1;
            PhysicsSystem result = app.createSystem(maxBodies, numBpLayers);

            return result;
        });

        fpa.setInitialize((app) -> {
            BaseApplication.setVsync(true);
            configureInput(app);
        });

        fpa.setPopulateSystem((app) -> {
            BodyInterface bi = app.getPhysicsSystem().getBodyInterface();

            // Create a dynamic cube and add it to the system:
            float boxHalfExtent = 0.5f;
            ConstShape smallCubeShape = new BoxShape(boxHalfExtent);
            BodyCreationSettings bcs = new BodyCreationSettings();
            bcs.getMassPropertiesOverride().setMass(2f);
            bcs.setOverrideMassProperties(
                    EOverrideMassProperties.CalculateInertia);
            bcs.setPosition(0., 4., 0.);
            bcs.setShape(smallCubeShape);
            dynamicCube = bi.createBody(bcs);
            bi.addBody(dynamicCube, EActivation.Activate);
            /*
             * Create 2 static bodies and add them to the system.
             * The top body serves as a temporary support.
             */
            float cubeHalfExtent = 1f;
            ConstShape largeCubeShape = new BoxShape(cubeHalfExtent);
            bcs.setMotionType(EMotionType.Static);
            bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving);
            bcs.setPosition(0., 0., 0.);
            bcs.setShape(largeCubeShape);
            supportCube = bi.createBody(bcs);
            bi.addBody(supportCube, EActivation.DontActivate);

            // The bottom body serves as a visual reference point:
            float ballRadius = 0.5f;
            ConstShape ballShape = new SphereShape(ballRadius);
            bcs.setPosition(0., -2., 0.);
            bcs.setShape(ballShape);
            ConstBody bottomBody = bi.createBody(bcs);
            bi.addBody(bottomBody, EActivation.DontActivate);

            // Visualize all 3 bodies:
            BasePhysicsApp.visualizeShape(dynamicCube);
            BasePhysicsApp.visualizeShape(supportCube);
            BasePhysicsApp.visualizeShape(bottomBody);
        });

        fpa.setPostPhysicsTick((app, system, timeStep) -> {
            /*
             * Once the dynamic cube gets deactivated,
             * remove the support cube from the system:
             */
            BodyInterface bi = system.getBodyInterface();
            int supportId = supportCube.getId();
            if (bi.isAdded(supportId) && !dynamicCube.isActive()) {
                bi.removeBody(supportId);
            }
        });

        fpa.start("HelloDeactivation");
    }
    // *************************************************************************
    // private methods

    /**
     * Configure keyboard input during initialization.
     *
     * @param app (not null)
     */
    private static void configureInput(BasePhysicsApp app) {
        BaseApplication.getInputManager().add(new InputProcessor() {
            @Override
            public void onKeyboard(int glfwKeyId, boolean isPressed) {
                if (glfwKeyId == GLFW.GLFW_KEY_E) {
                    if (isPressed) {
                        // Reactivate the dynamic cube:
                        BodyInterface bi
                                = app.getPhysicsSystem().getBodyInterface();
                        bi.activateBody(dynamicCube.getId());
                    }
                    return;
                }
                super.onKeyboard(glfwKeyId, isPressed);
            }
        });
    }
}
