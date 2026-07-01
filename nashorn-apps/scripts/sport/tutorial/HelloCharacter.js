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
 * A simple example of character physics.
 *
 * Builds upon HelloStaticBody.
 *
 * author:  Stephen Gold sgold@sonic.net
 */
var HelloCharacter = Java.extend(BasePhysicsApp, PhysicsTickListener);
var application = new HelloCharacter() {
    /*
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * return:  a new object
     */
    createSystem: function () {
        // For simplicity, use a single broadphase layer:
        var maxBodies = 2;
        var numBpLayers = 1;
        var sup = Java.super(application)
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
        BaseApplication.getCameraInputProcessor().setRotationMode(RotateMode.DragLMB);
        BaseApplication.setBackgroundColor(Constants.SKY_BLUE);
    },

    /*
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    populateSystem: function () {
        var sup = Java.super(application);
        var physicsSystem = sup.getPhysicsSystem();

        // Create a character with a capsule shape and add it to the system:
        var capsuleRadius = 0.5; // meters
        var capsuleHeight = 1.; // meters
        var shape = new CapsuleShape(capsuleHeight / 2., capsuleRadius);

        var settings = new CharacterSettings();
        settings.setShape(shape);

        var startLocation = new RVec3(0., 2., 0.);
        var userData = 0;
        character = new com.github.stephengold.joltjni.Character(
                settings, startLocation, new Quat(), userData, physicsSystem);
        character.addToPhysicsSystem();

        // Add a static square to represent the ground:
        var halfExtent = 4.;
        var y = -2.;
        var ground = this.addSquare(halfExtent, y);

        // Visualize the shapes of both physics objects:
        BasePhysicsApp.visualizeShape(character);
        BasePhysicsApp.visualizeShape(ground);
    },

    /*
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) after the system
     * has been stepped.
     */
    physicsTick: function (system, timeStep) {
        // Update the character:
        var maxSeparation = 0.1; // meters above the floor
        character.postSimulation(maxSeparation);
    },

    /*
     * Callback invoked (by Sport-Jolt, not by Jolt Physics) before the system
     * is stepped.
     */
    prePhysicsTick: function (system, timeStep) {
        // If the character is supported, cause it to jump:
        if (character.isSupported()) {
            character.setLinearVelocity(0., 8., 0.);
        }
    },

    /*
     * Add a static horizontal-square rigid body to the system.
     *
     * return:  the new body (not null)
     */
    addSquare: function (halfExtent, y) {
        // Create a static rigid body with a square shape:
        var halfThickness = 0.1;
        var shape = new BoxShape(halfExtent, halfThickness, halfExtent);
        var bcs = new BodyCreationSettings();
        bcs.setMotionType(EMotionType.Static);
        bcs.setObjectLayer(BaseApplication.objLayerNonMoving);
        bcs.setPosition(0., y - halfThickness, 0.);
        bcs.setShape(shape);

        var sup = Java.super(application);
        var physicsSystem = sup.getPhysicsSystem();
        var bi = physicsSystem.getBodyInterface();
        var result = bi.createBody(bcs);
        bi.addBody(result, EActivation.DontActivate);

        return result;
    }
};
application.start("HelloCharacter");
