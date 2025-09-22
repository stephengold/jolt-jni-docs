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
 * Drop a dynamic sphere onto a horizontal surface and visualize them both using
 * Sport-Jolt graphics.
 *
 * Builds upon HelloJoltJni.
 *
 * author:  Stephen Gold sgold@sonic.net
 */
var HelloSport = Java.extend(BasePhysicsApp);
var application = new HelloSport() {
    /*
     * Create the PhysicsSystem. Invoked once during initialization.
     */
    createSystem: function () {
        // For simplicity, use a single broadphase layer:
        var numBpLayers = 1;

        var ovoFilter = new ObjectLayerPairFilterTable(BasePhysicsApp.numObjLayers);
        // Enable collisions between 2 moving bodies:
        ovoFilter.enableCollision(BasePhysicsApp.objLayerMoving, BasePhysicsApp.objLayerMoving);
        // Enable collisions between a moving body and a non-moving one:
        ovoFilter.enableCollision(BasePhysicsApp.objLayerMoving, BasePhysicsApp.objLayerNonMoving);
        // Disable collisions between 2 non-moving bodies:
        ovoFilter.disableCollision(BasePhysicsApp.objLayerNonMoving, BasePhysicsApp.objLayerNonMoving);

        // Map both object layers to broadphase layer 0:
        var layerMap = new BroadPhaseLayerInterfaceTable(BasePhysicsApp.numObjLayers, numBpLayers);
        layerMap.mapObjectToBroadPhaseLayer(BasePhysicsApp.objLayerMoving, 0);
        layerMap.mapObjectToBroadPhaseLayer(BasePhysicsApp.objLayerNonMoving, 0);

        // Rules for colliding object layers with broadphase layers:
        var ovbFilter = new ObjectVsBroadPhaseLayerFilterTable(
                layerMap, numBpLayers, ovoFilter, BasePhysicsApp.numObjLayers);

        var result = new PhysicsSystem();

        // Set high limits, even though this sample app uses only 2 bodies:
        var maxBodies = 5000;
        var numBodyMutexes = 0; // 0 means "use the default number"
        var maxBodyPairs = 65536;
        var maxContacts = 20480;
        result.init(maxBodies, numBodyMutexes, maxBodyPairs, maxContacts, layerMap, ovbFilter, ovoFilter);

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

        // Add a static horizontal plane at y=-1:
        var groundY = -1;
        var normal = Vec3.sAxisY();
        var plane = new Plane(normal, -groundY);
        var floorShape = new PlaneShape(plane);
        var bcs = new BodyCreationSettings();
        bcs.setMotionType(EMotionType.Static);
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving);
        bcs.setShape(floorShape);
        var floor = bi.createBody(bcs);
        bi.addBody(floor, EActivation.DontActivate);

        // Add a sphere-shaped, dynamic, rigid body at the origin:
        var ballRadius = 0.3;
        var ballShape = new SphereShape(ballRadius);
        bcs.setMotionType(EMotionType.Dynamic);
        bcs.setObjectLayer(BasePhysicsApp.objLayerMoving);
        bcs.setShape(ballShape);
        ball = bi.createBody(bcs);
        bi.addBody(ball, EActivation.Activate);

        // Visualize the shapes of both bodies:
        BasePhysicsApp.visualizeShape(floor);
        BasePhysicsApp.visualizeShape(ball);
    }
};
application.start("HelloSport");
