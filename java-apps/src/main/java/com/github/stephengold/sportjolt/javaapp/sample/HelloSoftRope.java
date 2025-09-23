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

import com.github.stephengold.joltjni.BodyInterface;
import com.github.stephengold.joltjni.Edge;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.SoftBodyCreationSettings;
import com.github.stephengold.joltjni.SoftBodySharedSettings;
import com.github.stephengold.joltjni.SoftBodySharedSettingsRef;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.Vertex;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.operator.Op;
import com.github.stephengold.joltjni.readonly.ConstBody;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import com.github.stephengold.joltjni.readonly.Vec3Arg;
import com.github.stephengold.sportjolt.IndexBuffer;
import com.github.stephengold.sportjolt.Mesh;
import com.github.stephengold.sportjolt.Topology;
import com.github.stephengold.sportjolt.VertexBuffer;
import com.github.stephengold.sportjolt.mesh.DividedLine;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.EdgesGeometry;
import com.github.stephengold.sportjolt.physics.PinsGeometry;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A simple rope simulation using a soft body.
 * <p>
 * Builds upon HelloPin.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class HelloSoftRope extends BasePhysicsApp {
    // *************************************************************************
    // constructors

    /**
     * Instantiate the HelloSoftRope application.
     * <p>
     * This no-arg constructor was made explicit to avoid javadoc warnings from
     * JDK 18+.
     */
    public HelloSoftRope() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloSoftRope application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        HelloSoftRope application = new HelloSoftRope();
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
        int maxBodies = 1;
        int numBpLayers = 1;
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
        // Generate a subdivided line-segment mesh:
        int numSegments = 40;
        Vector3fc endPoint1 = new Vector3f(0f, 4f, 0f);
        Vector3fc endPoint2 = new Vector3f(2f, 4f, 2f);
        Mesh lineMesh = new DividedLine(endPoint1, endPoint2, numSegments);

        // Create a soft body and add it to the physics system:
        SoftBodySharedSettingsRef sbss = generateSharedSettings(lineMesh);

        // Pin one of the end vertices by zeroing its inverse mass:
        int vertexIndex = 0;
        Vertex endVertex = sbss.getVertex(vertexIndex);
        endVertex.setInvMass(0f);

        sbss.optimize();

        RVec3Arg startLocation = new RVec3(0., 0., 0.);
        SoftBodyCreationSettings sbcs = new SoftBodyCreationSettings(
                sbss, startLocation, new Quat(), objLayerMoving);
        BodyInterface bi = physicsSystem.getBodyInterface();

        ConstBody rope = bi.createSoftBody(sbcs);
        bi.addBody(rope, EActivation.Activate);

        // Visualize the soft-body edges and the pin:
        new EdgesGeometry(rope);
        new PinsGeometry(rope);
    }
    // *************************************************************************
    // private methods

    /**
     * Generate a shared-settings object using the positions and lines in the
     * specified LineList mesh.
     *
     * @param mesh the mesh to use (not null, unaffected)
     * @return a new object
     */
    private static SoftBodySharedSettingsRef generateSharedSettings(Mesh mesh) {
        assert mesh.topology() == Topology.LineList;

        SoftBodySharedSettingsRef result = new SoftBodySharedSettings().toRef();

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
        int numEdges = indices.capacity() / Mesh.vpe;
        Edge tmpEdge = new Edge();
        Vec3 tmpLocation2 = new Vec3();
        for (int i = 0; i < numEdges; ++i) {
            int vertexIndex1 = indices.get(Mesh.vpe * i);
            tmpEdge.setVertex(0, vertexIndex1);

            int vertexIndex2 = indices.get(Mesh.vpe * i + 1);
            tmpEdge.setVertex(1, vertexIndex2);

            locations.get(3 * vertexIndex1, tmpLocation);
            locations.get(3 * vertexIndex2, tmpLocation2);
            Vec3Arg offset = Op.minus(tmpLocation2, tmpLocation);
            float length = offset.length();
            tmpEdge.setRestLength(length);

            result.addEdgeConstraint(tmpEdge);
        }

        return result;
    }
}
