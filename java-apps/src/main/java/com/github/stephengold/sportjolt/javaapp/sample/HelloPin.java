/*
 Copyright (c) 2019-2025 Stephen Gold and Yanis Boudiaf

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
package com.github.stephengold.sportjolt.javaapp.sample;

import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.BodyInterface;
import com.github.stephengold.joltjni.Face;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.SoftBodyCreationSettings;
import com.github.stephengold.joltjni.SoftBodySharedSettings;
import com.github.stephengold.joltjni.SphereShape;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.Vertex;
import com.github.stephengold.joltjni.VertexAttributes;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EBendType;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.readonly.ConstBody;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import com.github.stephengold.sportjolt.IndexBuffer;
import com.github.stephengold.sportjolt.Mesh;
import com.github.stephengold.sportjolt.Topology;
import com.github.stephengold.sportjolt.VertexBuffer;
import com.github.stephengold.sportjolt.mesh.ClothGrid;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.EdgesGeometry;
import com.github.stephengold.sportjolt.physics.PinsGeometry;

/**
 * A simple cloth simulation with a pinned node.
 * <p>
 * Builds upon HelloCloth.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelloPin extends BasePhysicsApp {
    // *************************************************************************
    // constructors

    /**
     * Instantiate the HelloPin application.
     * <p>
     * This no-arg constructor was made explicit to avoid javadoc warnings from
     * JDK 18+.
     */
    public HelloPin() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloPin application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        HelloPin application = new HelloPin();
        application.start();
    }
    // *************************************************************************
    // BasePhysicsApp methods

    /**
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new object
     */
    @Override
    protected PhysicsSystem createSystem() {
        // For simplicity, use a single broadphase layer:
        int numBpLayers = 1;
        int maxBodies = 2;
        PhysicsSystem result = createSystem(maxBodies, numBpLayers);

        return result;
    }

    /**
     * Initialize the application. Invoked once.
     */
    @Override
    protected void initialize() {
        super.initialize();
        setVsync(true);

        // Relocate the camera:
        cam.setLocation(0f, 1f, 8f);
    }

    /**
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    @Override
    protected void populateSystem() {
        addBall();

        // Generate a subdivided square mesh with alternating diagonals:
        int numLines = 41;
        float lineSpacing = 0.1f;
        Mesh squareGrid = new ClothGrid(numLines, numLines, lineSpacing);

        // Create a compliant soft square and add it to the physics system:
        SoftBodySharedSettings sbss = generateSharedSettings(squareGrid);

        // Pin one of the corner vertices by zeroing its inverse mass:
        int vertexIndex = 0;
        Vertex cornerVertex = sbss.getVertex(vertexIndex);
        cornerVertex.setInvMass(0f);

        int numVertices = sbss.countVertices();
        VertexAttributes[] vertexAttributes = new VertexAttributes[numVertices];
        for (int i = 0; i < numVertices; ++i) {
            vertexAttributes[i] = new VertexAttributes();
            /*
             * Make the cloth flexible by increasing
             * the shear compliance of its edges:
             */
            vertexAttributes[i].setShearCompliance(2e-4f);
        }
        sbss.createConstraints(vertexAttributes, EBendType.Distance);
        sbss.optimize();

        RVec3Arg startLocation = new RVec3(0., 3., 0.);
        SoftBodyCreationSettings sbcs = new SoftBodyCreationSettings(
                sbss, startLocation, new Quat(), objLayerMoving);
        BodyInterface bi = physicsSystem.getBodyInterface();

        ConstBody cloth = bi.createSoftBody(sbcs);
        bi.addBody(cloth, EActivation.Activate);

        // Visualize the soft-body edges and the pin:
        new EdgesGeometry(cloth);
        new PinsGeometry(cloth);
    }
    // *************************************************************************
    // private methods

    /**
     * Add a static, rigid sphere to serve as an obstacle.
     */
    private void addBall() {
        float radius = 1f;
        SphereShape shape = new SphereShape(radius);
        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setMotionType(EMotionType.Static);
        bcs.setObjectLayer(objLayerNonMoving);
        bcs.setShape(shape);

        BodyInterface bi = physicsSystem.getBodyInterface();
        ConstBody body = bi.createBody(bcs);
        bi.addBody(body, EActivation.DontActivate);

        visualizeShape(body);
    }

    /**
     * Generate a shared-settings object using the positions and faces in the
     * specified TriangleList mesh.
     *
     * @param mesh the mesh to use (not null, unaffected)
     * @return a new object
     */
    private static SoftBodySharedSettings generateSharedSettings(Mesh mesh) {
        assert mesh.topology() == Topology.TriangleList;

        SoftBodySharedSettings result = new SoftBodySharedSettings();

        VertexBuffer locations = mesh.getPositions();
        int numVertices = locations.capacity() / 3;
        Vec3 tmpLocation = new Vec3();
        Vertex tmpVertex = new Vertex();
        for (int i = 0; i < numVertices; ++i) {
            locations.get(3 * i, tmpLocation);
            tmpVertex.setPosition(tmpLocation);
            result.addVertex(tmpVertex);
        }

        IndexBuffer indices = mesh.getIndexBuffer();
        int numFaces = indices.capacity() / Mesh.vpt;
        Face tmpFace = new Face();
        for (int i = 0; i < numFaces; ++i) {
            for (int j = 0; j < Mesh.vpt; ++j) {
                int index = indices.get(Mesh.vpt * i + j);
                tmpFace.setVertex(j, index);
            }
            result.addFace(tmpFace);
        }

        return result;
    }
}
