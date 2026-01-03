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

/*
 * A simple example of continuous collision detection (CCD).
 *
 * Builds upon HelloStaticBody.
 *
 * author:  Stephen Gold sgold@sonic.net
 */
var HelloCcd = Java.extend(BasePhysicsApp);
var application = new HelloCcd() {
    /*
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * return:  a new object
     */
    createSystem: function () {
        // For simplicity, use a single broadphase layer:
        var maxBodies = 3;
        var numBpLayers = 1;
        var result = Java.super(application).createSystem(maxBodies, numBpLayers);

        // Increase gravity to make the balls fall faster:
        result.setGravity(0., -100., 0.);

        return result;
    },

    /*
     * Initialize the application. Invoked once.
     */
    initialize: function () {
        Java.super(application).initialize();
        BaseApplication.setVsync(true);
    },

    /*
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    populateSystem: function () {
        var sup = Java.super(application);
        var bi = sup.getPhysicsSystem().getBodyInterface();

        // Create a collision shape for balls:
        var ballRadius = 0.1;
        var ballShape = new SphereShape(ballRadius);

        var bcs = new BodyCreationSettings();
        bcs.getMassPropertiesOverride().setMass(2.);
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia);
        bcs.setShape(ballShape);
        /*
         * Create 2 dynamic balls, one with LinearCast CCD and one without,
         * and add them to the system:
         */
        bcs.setMotionQuality(EMotionQuality.LinearCast);
        bcs.setPosition(-1., 4., 0.);
        var ccdBall = bi.createBody(bcs);
        bi.addBody(ccdBall, EActivation.Activate);

        bcs.setMotionQuality(EMotionQuality.Discrete);
        bcs.setPosition(1., 4., 0.);
        var controlBall = bi.createBody(bcs);
        bi.addBody(controlBall, EActivation.Activate);

        // Verify the motion quality of each ball:
        var ccdProperties = ccdBall.getMotionProperties();
        //assert ccdProperties.getMotionQuality() == EMotionQuality.LinearCast;
        var controlProperties
                = controlBall.getMotionProperties();
        //assert controlProperties.getMotionQuality() == EMotionQuality.Discrete;

        // Add an obstacle:
        var disc = this.addDisc();

        // Visualize the shapes of all 3 rigid bodies:
        BasePhysicsApp.visualizeShape(ccdBall);
        BasePhysicsApp.visualizeShape(controlBall);
        BasePhysicsApp.visualizeShape(disc).setProgram("Unshaded/Monochrome");
    },

    /*
     * Advance the physics simulation by the specified amount. Invoked during
     * each update.
     */
    updatePhysics: function (wallClockSeconds) {
        // For clarity, simulate at 1/10th normal speed:
        var simulateSeconds = 0.1 * wallClockSeconds;
        Java.super(application).updatePhysics(simulateSeconds);
    },

    /*
     * Add a thin static disc to serve as an obstacle.
     */
    addDisc: function () {
        var discRadius = 2.;
        var discThickness = 0.05;
        var discConvexRadius = 0.02;
        var discShape = new CylinderShape(
                discThickness / 2., discRadius, discConvexRadius);

        var bcs = new BodyCreationSettings();
        bcs.setMotionType(EMotionType.Static);
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving);
        bcs.setShape(discShape);

        var sup = Java.super(application);
        var bi = sup.getPhysicsSystem().getBodyInterface();
        var result = bi.createBody(bcs);
        bi.addBody(result, EActivation.DontActivate);

        return result;
    }
};
application.start("HelloCcd");
