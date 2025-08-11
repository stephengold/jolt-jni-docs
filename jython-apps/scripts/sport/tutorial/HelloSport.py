"""
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
"""

"""
 * Drop a dynamic sphere onto a horizontal surface and visualize them both using
 * Sport-Jolt graphics.
 *
 * Builds upon HelloJoltJni.
 *
 * author:  Stephen Gold sgold@sonic.net
"""


class HelloSport(BasePhysicsApp):

    # Create the PhysicsSystem. Invoked once during initialization.
    def createSystem(self):
        # For simplicity, use a single broadphase layer:
        numBpLayers = 1

        ovoFilter = ObjectLayerPairFilterTable(BasePhysicsApp.numObjLayers)
        # Enable collisions between 2 moving bodies:
        ovoFilter.enableCollision(
            BasePhysicsApp.objLayerMoving, BasePhysicsApp.objLayerMoving
        )
        # Enable collisions between a moving body and a non-moving one:
        ovoFilter.enableCollision(
            BasePhysicsApp.objLayerMoving, BasePhysicsApp.objLayerNonMoving
        )
        # Disable collisions between 2 non-moving bodies:
        ovoFilter.disableCollision(
            BasePhysicsApp.objLayerNonMoving, BasePhysicsApp.objLayerNonMoving
        )

        # Map both object layers to broadphase layer 0:
        layerMap = BroadPhaseLayerInterfaceTable(
            BasePhysicsApp.numObjLayers, numBpLayers
        )
        layerMap.mapObjectToBroadPhaseLayer(BasePhysicsApp.objLayerMoving, 0)
        layerMap.mapObjectToBroadPhaseLayer(BasePhysicsApp.objLayerNonMoving, 0)

        # Rules for colliding object layers with broadphase layers:
        ovbFilter = ObjectVsBroadPhaseLayerFilterTable(
            layerMap, numBpLayers, ovoFilter, BasePhysicsApp.numObjLayers
        )

        result = PhysicsSystem()

        # Set high limits, even though this sample app uses only 2 bodies:
        maxBodies = 5000
        numBodyMutexes = 0  # 0 means "use the default number"
        maxBodyPairs = 65536
        maxContacts = 20480
        result.init(
            maxBodies,
            numBodyMutexes,
            maxBodyPairs,
            maxContacts,
            layerMap,
            ovbFilter,
            ovoFilter,
        )

        return result

    # Populate the PhysicsSystem with bodies. Invoked once during initialization.
    def populateSystem(self):
        bi = self.physicsSystem.getBodyInterface()

        # Add a static horizontal plane at y=-1:
        groundY = -1
        normal = Vec3.sAxisY()
        plane = Plane(normal, -groundY)
        floorShape = PlaneShape(plane)
        bcs = BodyCreationSettings()
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving)
        bcs.setShape(floorShape)
        floor = bi.createBody(bcs)
        bi.addBody(floor, EActivation.DontActivate)

        # Add a sphere-shaped, dynamic, rigid body at the origin:
        ballRadius = 0.3
        ballShape = SphereShape(ballRadius)
        bcs.setMotionType(EMotionType.Dynamic)
        bcs.setObjectLayer(BasePhysicsApp.objLayerMoving)
        bcs.setShape(ballShape)
        global ball
        ball = bi.createBody(bcs)
        bi.addBody(ball, EActivation.Activate)

        # Visualize the shapes of both bodies:
        BasePhysicsApp.visualizeShape(floor)
        BasePhysicsApp.visualizeShape(ball)


application = HelloSport()
application.start()
