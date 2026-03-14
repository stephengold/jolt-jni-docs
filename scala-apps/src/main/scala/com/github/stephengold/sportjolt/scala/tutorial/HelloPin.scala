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
package com.github.stephengold.sportjolt.scala.tutorial

import com.github.stephengold.joltjni.BodyCreationSettings
import com.github.stephengold.joltjni.Face
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.Quat
import com.github.stephengold.joltjni.RVec3
import com.github.stephengold.joltjni.SoftBodyCreationSettings
import com.github.stephengold.joltjni.SoftBodySharedSettings
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.Vec3
import com.github.stephengold.joltjni.Vertex
import com.github.stephengold.joltjni.VertexAttributes
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EBendType
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.joltjni.readonly.ConstVertexAttributes
import com.github.stephengold.sportjolt.BaseApplication
import com.github.stephengold.sportjolt.Mesh
import com.github.stephengold.sportjolt.Topology
import com.github.stephengold.sportjolt.mesh.ClothGrid
import com.github.stephengold.sportjolt.physics.BasePhysicsApp
import com.github.stephengold.sportjolt.physics.EdgesGeometry
import com.github.stephengold.sportjolt.physics.PinsGeometry

/**
 * A simple cloth simulation with a pinned node.
 * <p>
 * Builds upon HelloCloth.
 *
 * @author Stephen Gold sgold@sonic.net
 */
object HelloPin {
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloPin application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    def main(arguments: Array[String]): Unit = {
        val application = new HelloPin
        application.start
    }
}

class HelloPin extends BasePhysicsApp {
    /**
     * Create the PhysicsSystem. Invoked once during initialization.
     *
     * @return a new object
     */
    override def createSystem: PhysicsSystem = {
        // For simplicity, use a single broadphase layer:
        val maxBodies = 2
        val numBpLayers = 1
        val result = createSystem(maxBodies, numBpLayers)

        return result
    }

    /**
     * Initialize the application. Invoked once.
     */
    override def initialize: Unit = {
        super.initialize
        BaseApplication.setVsync(true)

        // Relocate the camera:
        BaseApplication.cam.setLocation(0f, 1f, 8f)
    }

    /**
     * Populate the PhysicsSystem with bodies. Invoked once during
     * initialization.
     */
    override def populateSystem: Unit = {
        addBall

        // Generate a subdivided square mesh with alternating diagonals:
        val numLines = 41
        val lineSpacing = 0.1f
        val squareGrid = new ClothGrid(numLines, numLines, lineSpacing)

        // Create a compliant soft square and add it to the physics system:
        val sbss = generateSharedSettings(squareGrid)

        // Pin one of the corner vertices by zeroing its inverse mass:
        val vertexIndex = 0
        val cornerVertex = sbss.getVertex(vertexIndex)
        cornerVertex.setInvMass(0f)

        val numVertices = sbss.countVertices
        val vertexAttributes: Array[ConstVertexAttributes]
                = Array.ofDim(numVertices)
        for (i <- 0 to numVertices - 1) {
            val atts = new VertexAttributes
            /*
             * Make the cloth flexible by increasing
             * the shear compliance of its edges:
             */
            atts.setShearCompliance(2e-4f)
            vertexAttributes(i) = atts
        }
        sbss.createConstraints(vertexAttributes, EBendType.Distance)
        sbss.optimize

        val startLocation = new RVec3(0.0, 3.0, 0.0)
        val sbcs = new SoftBodyCreationSettings(
                sbss, startLocation, new Quat, BasePhysicsApp.objLayerMoving)

        val bi = physicsSystem.getBodyInterface
        val cloth = bi.createSoftBody(sbcs)
        bi.addBody(cloth, EActivation.Activate)

        // Visualize the soft-body edges and the pin:
        new EdgesGeometry(cloth)
        new PinsGeometry(cloth)
    }
    // *************************************************************************
    // private methods

    /**
     * Add a static, rigid sphere to serve as an obstacle.
     */
    private def addBall: Unit = {
        val radius = 1f
        val shape = new SphereShape(radius)
        val bcs = new BodyCreationSettings
        bcs.setMotionType(EMotionType.Static)
        bcs.setObjectLayer(BasePhysicsApp.objLayerNonMoving)
        bcs.setShape(shape)

        val bi = physicsSystem.getBodyInterface
        val body = bi.createBody(bcs)
        bi.addBody(body, EActivation.DontActivate)

        BasePhysicsApp.visualizeShape(body)
    }

    /**
     * Generate a shared-settings object using the positions and faces in the
     * specified TriangleList mesh.
     *
     * @param mesh the mesh to use (not null, unaffected)
     * @return a new object
     */
    private def generateSharedSettings(mesh: Mesh): SoftBodySharedSettings = {
        assert (mesh.topology == Topology.TriangleList)

        val result = new SoftBodySharedSettings

        val locations = mesh.getPositions
        val numVertices = locations.capacity / 3
        val tmpLocation = new Vec3
        val tmpVertex = new Vertex
        for (i <- 0 to numVertices - 1) {
            locations.get(3 * i, tmpLocation)
            tmpVertex.setPosition(tmpLocation)
            result.addVertex(tmpVertex)
        }

        val indices = mesh.getIndexBuffer
        val numFaces = indices.capacity / Mesh.vpt
        val tmpFace = new Face
        for (i <- 0 to numFaces - 1) {
            for (j <- 0 to Mesh.vpt - 1) {
                val index = indices.get(Mesh.vpt * i + j)
                tmpFace.setVertex(j, index)
            }
            result.addFace(tmpFace)
        }

        return result
    }
}
