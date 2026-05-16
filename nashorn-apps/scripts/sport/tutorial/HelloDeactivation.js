/*
 Copyright (c) 2025-2026 Stephen Gold and Yanis Boudiaf

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

/*
 * A simple example of rigid-body deactivation.
 *
 * Builds upon HelloStaticBody.
 *
 * author:  Stephen Gold sgold@sonic.net
 */

// small, dynamic rigid body
var dynamicCube;

// large, static rigid body
var supportCube;

var HelloDeactivation = Java.extend(BasePhysicsApp, PhysicsTickListener);
var application = new HelloDeactivation() {
    /*
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * return:  a new object
     */
    createSystem: function () {
        // For simplicity, use a single broadphase layer:
        var sup = Java.super(application);
        var maxBodies = 3;
        var numBpLayers = 1;
        var result = sup.createSystem(maxBodies, numBpLayers);

        // To enable the callbacks, register the application as a tick listener:
        sup.addTickListener(application);

        return result;
    },

    /*
     * Initialize the application. Invoked once.
     */
    initialize: function () {
        Java.super(application).initialize();

        BaseApplication.setVsync(true);
        this.configureInput();
    },

    /*
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    populateSystem: function () {
        var sup = Java.super(application);
        var bi = sup.getPhysicsSystem().getBodyInterface();

        // Create a dynamic cube and add it to the system:
        var boxHalfExtent = 0.5;
        var smallCubeShape = new BoxShape(boxHalfExtent);
        var bcs = new BodyCreationSettings();
        bcs.getMassPropertiesOverride().setMass(2.);
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia);
        bcs.setPosition(0., 4., 0.);
        bcs.setShape(smallCubeShape);
        dynamicCube = bi.createBody(bcs);
        bi.addBody(dynamicCube, EActivation.Activate);
        /*
         * Create 2 static bodies and add them to the system.
         * The top body serves as a temporary support.
         */
        var cubeHalfExtent = 1.;
        var largeCubeShape = new BoxShape(cubeHalfExtent);
        bcs.setMotionType(EMotionType.Static)
                .setObjectLayer(BasePhysicsApp.objLayerNonMoving)
                .setPosition(0., 0., 0.)
                .setShape(largeCubeShape);
        supportCube = bi.createBody(bcs);
        bi.addBody(supportCube, EActivation.DontActivate);

        // The bottom body serves as a visual reference point:
        var ballRadius = 0.5;
        var ballShape = new SphereShape(ballRadius);
        bcs.setPosition(0., -2., 0.);
        bcs.setShape(ballShape);
        var bottomBody = bi.createBody(bcs);
        bi.addBody(bottomBody, EActivation.DontActivate);

        // Visualize the shapes of all 3 bodies:
        BasePhysicsApp.visualizeShape(dynamicCube);
        BasePhysicsApp.visualizeShape(supportCube);
        BasePhysicsApp.visualizeShape(bottomBody);
    },

    /*
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) after the system
     * has been stepped.
     */
    physicsTick: function (system, timeStep) {
        /*
         * Once the dynamic cube gets deactivated,
         * remove the support cube from the system:
         */
        var bi = system.getBodyInterface();
        var supportId = supportCube.getId();
        if (bi.isAdded(supportId) && !dynamicCube.isActive()) {
            bi.removeBody(supportId);
        }
    },

    /*
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system
     * is stepped.
     */
    prePhysicsTick: function (system, timeStep) {
        // do nothing
    },

    /*
     * Configure keyboard input during initialization.
     */
    configureInput: function () {
        var processor = new InputProcessor() {
            onKeyboard: function (glfwKeyId, isPressed) {
                if (glfwKeyId == GLFW.GLFW_KEY_E) {
                    if (isPressed) {
                        // Reactivate the dynamic cube:
                        var sup = Java.super(application);
                        var bi = sup.getPhysicsSystem().getBodyInterface();
                        bi.activateBody(dynamicCube.getId());
                    }
                    return;
                }

                var sup = Java.super(processor);
                sup.onKeyboard(glfwKeyId, isPressed);
            }
        };
        BaseApplication.getInputManager().add(processor);
    }
};
application.start("HelloDeactivation");
