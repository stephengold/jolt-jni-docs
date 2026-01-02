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
 * A simple example illustrating the effect of damping on dynamic rigid bodies.
 * <p>
 * Builds upon HelloRigidBody.
 *
 * author:  Stephen Gold sgold@sonic.net
 */
var HelloDamping = Java.extend(BasePhysicsApp);
var application = new HelloDamping() {
    /*
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new object
     */
    createSystem: function () {
        // For simplicity, use a single broadphase layer:
        var maxBodies = 4;
        var numBpLayers = 1;
        var result = Java.super(application).createSystem(maxBodies, numBpLayers);

        // For clarity, disable gravity:
        result.setGravity(0., 0., 0.);

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

        // Create a collision shape for unit cubes:
        var cubeHalfExtent = 0.5;
        var cubeShape = new BoxShape(cubeHalfExtent);

        var bcs = new BodyCreationSettings();
        bcs.setAllowSleeping(false); // Disable sleeping for clarity.
        bcs.getMassPropertiesOverride().setMass(2.);
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia);
        bcs.setShape(cubeShape);
        /*
         * Create 4 cubes (dynamic rigid bodies) and add them to the system.
         * Give each cube its own set of damping parameters.
         * Locate the cubes 4 meters apart, center to center.
         */
        var numCubes = 4;
        var cube = [];

        bcs.setAngularDamping(0.);
        bcs.setLinearDamping(0.);
        bcs.setPosition(0., +2., 0.);
        cube[0] = bi.createBody(bcs);
        bi.addBody(cube[0], EActivation.Activate);

        bcs.setAngularDamping(0.9);
        bcs.setLinearDamping(0.);
        bcs.setPosition(4., +2., 0.);
        cube[1] = bi.createBody(bcs);
        bi.addBody(cube[1], EActivation.Activate);

        bcs.setAngularDamping(0.);
        bcs.setLinearDamping(0.9);
        bcs.setPosition(0., -2., 0.);
        cube[2] = bi.createBody(bcs);
        bi.addBody(cube[2], EActivation.Activate);

        bcs.setAngularDamping(0.9);
        bcs.setLinearDamping(0.9);
        bcs.setPosition(4., -2., 0.);
        cube[3] = bi.createBody(bcs);
        bi.addBody(cube[3], EActivation.Activate);
        /*
         * Apply an off-center impulse to each cube,
         * causing it to drift and spin:
         */
        var impulse = new Vec3(-1., 0., 0.);
        var offset = new RVec3(0., 1., 1.);
        for (var cubeIndex = 0; cubeIndex < numCubes; ++cubeIndex) {
            var center = cube[cubeIndex].getCenterOfMassPosition();
            cube[cubeIndex].addImpulse(impulse, Op.plus(center, offset));
        }

        // Visualize the shapes of all 4 cubes:
        for (var cubeIndex = 0; cubeIndex < numCubes; ++cubeIndex) {
            BasePhysicsApp.visualizeShape(cube[cubeIndex]);
        }
    }
};
application.start("HelloDamping");
