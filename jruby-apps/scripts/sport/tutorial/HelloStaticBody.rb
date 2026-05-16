=begin
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
=end

=begin
    A simple example combining static and dynamic rigid bodies.

    Builds upon HelloRigidBody.

    author:  Stephen Gold sgold@sonic.net
=end

application = Class.new(BasePhysicsApp) {
    alias_method :super_createSystem, :createSystem
    alias_method :super_updatePhysics, :updatePhysics

    # Create the PhysicsSystem. Invoked once during initialization.
    def createSystem()

        # For simplicity, use a single broadphase layer:
        max_bodies = 2
        num_bp_layers = 1
        super_createSystem(max_bodies, num_bp_layers)
    end

    # Initialize the application. Invoked once.
    def initialize()
        super
        BasePhysicsApp.setVsync(true)
    end

    # Populate the PhysicsSystem with bodies. Invoked once during initialization.
    def populateSystem()
        bi = physics_system.getBodyInterface()

        # Create a collision shape for balls:
        ball_radius = 1
        ball_shape = SphereShape.new(ball_radius)

        bcs = BodyCreationSettings.new()
        bcs.getMassPropertiesOverride().setMass(2)
        bcs.setOverrideMassProperties(EOverrideMassProperties::CalculateInertia)
        bcs.setShape(ball_shape)

        # Create a dynamic body and add it to the system:
        bcs.setPosition(0, 4, 0)
        dynaBall = bi.createBody(bcs)
        bi.addBody(dynaBall, EActivation::Activate)

        # Create a static body and add it to the system:
        bcs.setMotionType(EMotionType::Static) # default=Dynamic
        bcs.setObjectLayer(BasePhysicsApp::objLayerNonMoving) # default=0
        bcs.setPosition(0.1, 0, 0)
        statBall = bi.createBody(bcs)
        bi.addBody(statBall, EActivation::DontActivate)
        #assert statBall.isStatic()

        # Visualize the shapes of both rigid bodies:
        BasePhysicsApp.visualizeShape(dynaBall)
        BasePhysicsApp.visualizeShape(statBall)
    end
}.new()

application.start("HelloStaticBody")
