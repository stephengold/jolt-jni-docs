/*
 Copyright (c) 2025 Stephen Gold and Yanis Boudiaf

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
 * A simple example of 2 colliding balls, illustrating the 5 basic features of
 * responsive, dynamic, rigid bodies:
 *
 * + rigidity (fixed shape),
 * + inertia (resistance to changes of motion),
 * + dynamics (motion determined by forces, torques, and impulses),
 * + gravity (continual downward force), and
 * + contact response (avoid intersecting with other bodies).
 *
 * Builds upon HelloSport.
 *
 * author:  Stephen Gold sgold@sonic.net
 */
var HelloRigidBody = Java.extend(BasePhysicsApp);
var application = new HelloRigidBody() {
    /*
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new object
     */
    createSystem: function () {
        // For simplicity, use a single broadphase layer:
        var maxBodies = 2;
        var numBpLayers = 1;
        var result = Java.super(application).createSystem(maxBodies, numBpLayers);

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
        var ballRadius = 1;
        var ballShape = new SphereShape(ballRadius);

        var bcs = new BodyCreationSettings();
        bcs.getMassPropertiesOverride().setMass(2);
        bcs.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia);
        bcs.setShape(ballShape);

        // Create 2 balls (dynamic rigid bodies) and add them to the system:
        bcs.setPosition(1., 1., 0.);
        var ball1 = bi.createBody(bcs);
        bi.addBody(ball1, EActivation.Activate);

        bcs.setPosition(5., 1., 0.);
        var ball2 = bi.createBody(bcs);
        bi.addBody(ball2, EActivation.Activate);

        // Apply an impulse to ball2 to put it on a collision course with ball1:
        ball2.addImpulse(-25, 0, 0);

        // Visualize the shapes of both bodies:
        BasePhysicsApp.visualizeShape(ball1);
        BasePhysicsApp.visualizeShape(ball2);
    },

    /**
     * Advance the physics simulation by the specified amount. Invoked during
     * each update.
     *
     * @param wallClockSeconds the elapsed wall-clock time since the previous
     * invocation of {@code updatePhysics} (in seconds, &ge;0)
     */
    updatePhysics: function (wallClockSeconds) {
        // For clarity, simulate at 1/10th normal speed:
        var simulateSeconds = 0.1 * wallClockSeconds;
        Java.super(application).updatePhysics(simulateSeconds);
    }
};
application.start("HelloRigidBody");
