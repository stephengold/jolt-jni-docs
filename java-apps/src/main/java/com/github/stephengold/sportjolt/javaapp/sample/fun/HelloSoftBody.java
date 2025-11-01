/*
 Copyright (c) 2020-2025 Stephen Gold and Yanis Boudiaf

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
package com.github.stephengold.sportjolt.javaapp.sample.fun;

import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.BodyInterface;
import com.github.stephengold.joltjni.BoxShape;
import com.github.stephengold.joltjni.Face;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.SoftBodyCreationSettings;
import com.github.stephengold.joltjni.SoftBodySharedSettings;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.Vertex;
import com.github.stephengold.joltjni.VertexAttributes;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EBendType;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.readonly.ConstBody;
import com.github.stephengold.joltjni.readonly.ConstShape;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import com.github.stephengold.sportjolt.BaseApplication;
import com.github.stephengold.sportjolt.IndexBuffer;
import com.github.stephengold.sportjolt.Mesh;
import com.github.stephengold.sportjolt.VertexBuffer;
import com.github.stephengold.sportjolt.mesh.IcosphereMesh;
import com.github.stephengold.sportjolt.physics.BasePhysicsApp;
import com.github.stephengold.sportjolt.physics.EdgesGeometry;
import com.github.stephengold.sportjolt.physics.FacesGeometry;
import com.github.stephengold.sportjolt.physics.FunctionalPhysicsApp;

/**
 * A simple example of a soft body colliding with a static rigid body.
 * <p>
 * Builds upon HelloStaticBody.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class HelloSoftBody {
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private HelloSoftBody() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloSoftBody application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        FunctionalPhysicsApp fpa = new FunctionalPhysicsApp();

        fpa.setInitialize((app) -> {
            BaseApplication.setVsync(true);

            // Relocate the camera:
            BaseApplication.getCamera().setLocation(0f, 1f, 8f);
        });

        fpa.setCreateSystem((app) -> {
            // For simplicity, use a single broadphase layer:
            int maxBodies = 2;
            int numBpLayers = 1;
            PhysicsSystem result = app.createSystem(maxBodies, numBpLayers);

            return result;
        });

        fpa.setPopulateSystem((app) -> {
            BodyInterface bi = app.getPhysicsSystem().getBodyInterface();
            addBox(bi);

            // A mesh is used to generate the shape and topology of the body:
            int numRefinementIterations = 3;
            boolean indexed = true;
            Mesh mesh = new IcosphereMesh(numRefinementIterations, indexed);

            // Create a soft ball and add it to the physics system:
            SoftBodySharedSettings sbss = new SoftBodySharedSettings();

            VertexBuffer locations = mesh.getPositions();
            int numVertices = locations.capacity() / 3;
            Vec3 tmpLocation = new Vec3();
            Vertex tmpVertex = new Vertex();
            for (int i = 0; i < numVertices; ++i) {
                locations.get(3 * i, tmpLocation);
                tmpVertex.setPosition(tmpLocation);
                sbss.addVertex(tmpVertex);
            }

            IndexBuffer indices = mesh.getIndexBuffer();
            int numFaces = indices.capacity() / Mesh.vpt;
            Face tmpFace = new Face();
            for (int i = 0; i < numFaces; ++i) {
                for (int j = 0; j < Mesh.vpt; ++j) {
                    int index = indices.get(Mesh.vpt * i + j);
                    tmpFace.setVertex(j, index);
                }
                sbss.addFace(tmpFace);
            }

            VertexAttributes[] vertexAttributes
                    = new VertexAttributes[numVertices];
            for (int i = 0; i < numVertices; ++i) {
                vertexAttributes[i] = new VertexAttributes();
            }
            sbss.createConstraints(vertexAttributes, EBendType.Distance);
            sbss.optimize();

            RVec3Arg startLocation = new RVec3(0., 3., 0.);
            SoftBodyCreationSettings sbcs = new SoftBodyCreationSettings(sbss,
                    startLocation, new Quat(), BasePhysicsApp.objLayerMoving);

            // Configure the ball to resist deformation:
            sbcs.setPressure(30_000f); // default=0

            ConstBody body = bi.createSoftBody(sbcs);
            bi.addBody(body, EActivation.Activate);

            // Visualize the soft body:
            new FacesGeometry(body);
            new EdgesGeometry(body);
        });

        fpa.start("HelloSoftBody");
    }
    // *************************************************************************
    // private methods

    /**
     * Add a large static cube to serve as a platform.
     *
     * @param bi the system's body interface (not {@code null})
     */
    private static void addBox(BodyInterface bi) {
        float halfExtent = 3f;
        ConstShape shape = new BoxShape(halfExtent);
        BodyCreationSettings bcs = new BodyCreationSettings();
        bcs.setMotionType(EMotionType.Static);
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving);
        bcs.setPosition(0., -halfExtent, 0.);
        bcs.setShape(shape);

        ConstBody body = bi.createBody(bcs);
        bi.addBody(body, EActivation.DontActivate);

        BasePhysicsApp.visualizeShape(body);
    }
}
