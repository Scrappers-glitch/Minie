/*
 * Copyright (c) 2009-2018 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.bullet.util;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Transform;
import com.jme3.math.Triangle;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import jme3utilities.math.MyBuffer;
import jme3utilities.math.MyVolume;
import jme3utilities.math.RectangularSolid;
import jme3utilities.math.VectorSet;
import jme3utilities.math.VectorSetUsingBuffer;

/**
 * Temporary objects used to return debug meshes from native Bullet.
 *
 * @author normenhansen
 */
class DebugMeshCallback {
    // *************************************************************************
    // constants and loggers

    /**
     * number of axes in a vector
     */
    final private static int numAxes = 3;
    /**
     * number of vertices per triangle
     */
    final private static int vpt = 3;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(DebugMeshCallback.class.getName());
    // *************************************************************************
    // fields

    /**
     * list of vertex locations (typically includes many duplicates)
     */
    final private ArrayList<Vector3f> list = new ArrayList<>(250);
    // *************************************************************************
    // new methods exposed

    /**
     * Estimate the footprint of the vertex locations in world coordinates.
     *
     * @param meshToWorld the transform from mesh coordinates to world
     * coordinates (not null, unaffected)
     * @return a new array containing the corner locations of a rectangle (in
     * world coordinates)
     */
    Vector3f[] footprint(Transform meshToWorld) {
        assert meshToWorld != null;
        /*
         * Copy the location list, removing all duplicates in the process.
         */
        VectorSet distinct = new VectorSetUsingBuffer(list.size());
        for (Vector3f vector : list) {
            distinct.add(vector);
        }
        /*
         * Transform vertex locations to world coordinates and set all the
         * Y coordinates to the minimum coordinate value.
         */
        FloatBuffer floatBuffer = distinct.toBuffer();
        int numFloats = floatBuffer.limit();
        MyBuffer.transform(floatBuffer, 0, numFloats, meshToWorld);

        float minY = Float.POSITIVE_INFINITY;
        int numVectors = numFloats / numAxes;
        for (int vectorIndex = 0; vectorIndex < numVectors; ++vectorIndex) {
            int position = vectorIndex * numAxes + PhysicsSpace.AXIS_Y;
            float y = floatBuffer.get(position);
            if (y < minY) {
                minY = y;
            }
        }

        for (int vectorIndex = 0; vectorIndex < numVectors; ++vectorIndex) {
            int position = vectorIndex * numAxes + PhysicsSpace.AXIS_Y;
            floatBuffer.put(position, minY);
        }
        /*
         * Fit a rotated rectangular solid to the vertex locations.
         */
        RectangularSolid solid
                = new RectangularSolid(floatBuffer, 0, numFloats);
        Vector3f maxima = solid.maxima(null);
        Vector3f minima = solid.minima(null);
        /*
         * Enumerate the local coordinates (within the solid) of the 4 corners.
         * Assume that the local X axis corresponds to the world Y axis, since
         * those are the axes with the least variance.
         */
        float midX = (minima.x + maxima.x) / 2f;
        Vector3f[] cornerLocations = new Vector3f[4];
        cornerLocations[0] = new Vector3f(midX, maxima.y, maxima.z);
        cornerLocations[1] = new Vector3f(midX, minima.y, maxima.z);
        cornerLocations[2] = new Vector3f(midX, maxima.y, minima.z);
        cornerLocations[3] = new Vector3f(midX, minima.y, minima.z);
        /*
         * Transform corner locations into the world coordinate system.
         */
        for (Vector3f location : cornerLocations) {
            solid.localToWorld(location, location);
        }

        return cornerLocations;
    }

    /**
     * Calculate face normals and store them in a FloatBuffer.
     *
     * @return a new buffer (not null)
     */
    FloatBuffer getFaceNormals() {
        int numVertices = list.size();
        int numTriangles = numVertices / 3;
        assert numTriangles * vpt == numVertices : numVertices;

        int numFloats = 3 * numVertices;
        FloatBuffer buffer = BufferUtils.createFloatBuffer(numFloats);

        Triangle triangle = new Triangle();
        for (int triIndex = 0; triIndex < numTriangles; ++triIndex) {
            int firstVertex = vpt * triIndex;
            Vector3f pos1 = list.get(firstVertex);
            Vector3f pos2 = list.get(firstVertex + 1);
            Vector3f pos3 = list.get(firstVertex + 2);
            triangle.set(pos1, pos2, pos3);
            triangle.setNormal(null); // work around JME issue #957
            Vector3f normal = triangle.getNormal();
            for (int j = 0; j < vpt; ++j) {
                buffer.put(normal.x);
                buffer.put(normal.y);
                buffer.put(normal.z);
            }
        }

        return buffer;
    }

    /**
     * Calculate smooth normals and store them in a FloatBuffer.
     *
     * @return a new buffer (not null)
     */
    FloatBuffer getSmoothNormals() {
        int numVertices = list.size();
        int numTriangles = numVertices / 3;
        assert numTriangles * vpt == numVertices : numVertices;

        Map<Vector3f, Integer> indexMap = new HashMap<>(numVertices);
        int numDistinct = 0;
        for (Vector3f vector : list) {
            if (!indexMap.containsKey(vector)) {
                indexMap.put(vector, numDistinct);
                ++numDistinct;
            }
        }
        /*
         * Initialize the averaging data for each distinct vertex.
         */
        int[] numFaces = new int[numDistinct];
        Vector3f[] totals = new Vector3f[numDistinct];
        for (int i = 0; i < numDistinct; ++i) {
            numFaces[i] = 0;
            totals[i] = new Vector3f(0f, 0f, 0f);
        }

        Triangle triangle = new Triangle();
        for (int triIndex = 0; triIndex < numTriangles; ++triIndex) {
            int firstVertex = vpt * triIndex;
            Vector3f loc1 = list.get(firstVertex);
            Vector3f loc2 = list.get(firstVertex + 1);
            Vector3f loc3 = list.get(firstVertex + 2);
            triangle.set(loc1, loc2, loc3);
            triangle.setNormal(null); // work around JME issue #957
            Vector3f faceNormal = triangle.getNormal();

            int i1 = indexMap.get(loc1);
            totals[i1].addLocal(faceNormal);
            ++numFaces[i1];

            int i2 = indexMap.get(loc2);
            totals[i2].addLocal(faceNormal);
            ++numFaces[i2];

            int i3 = indexMap.get(loc3);
            totals[i3].addLocal(faceNormal);
            ++numFaces[i3];
        }
        /*
         * Average and re-normalize the face normals for each distinct vertex.
         */
        for (int i = 0; i < totals.length; ++i) {
            assert numFaces[i] > 0 : numFaces[i];
            float factor = 1f / numFaces[i];
            totals[i].multLocal(factor);
            totals[i].normalizeLocal();
        }

        int numFloats = 3 * numVertices;
        FloatBuffer buffer = BufferUtils.createFloatBuffer(numFloats);

        for (Vector3f vertexLocation : list) {
            int vertexIndex = indexMap.get(vertexLocation);
            Vector3f normal = totals[vertexIndex];
            buffer.put(normal.x);
            buffer.put(normal.y);
            buffer.put(normal.z);
        }

        return buffer;
    }

    /**
     * Copy the vertex locations to a FloatBuffer.
     *
     * @return a new buffer (not null)
     */
    FloatBuffer getVertices() {
        int numFloats = 3 * list.size();
        FloatBuffer buffer = BufferUtils.createFloatBuffer(numFloats);
        for (Vector3f location : list) {
            buffer.put(location.x);
            buffer.put(location.y);
            buffer.put(location.z);
        }

        return buffer;
    }

    /**
     * Calculate volume of the mesh, assuming it's closed and convex.
     *
     * @return the volume (in cubic mesh units)
     */
    float volumeConvex() {
        int numVertices = list.size();
        int numTriangles = numVertices / 3; // TODO vpt
        assert numTriangles * vpt == numVertices : numVertices;

        double total = 0.0;
        if (numTriangles > 0) {
            Vector3f fixed = list.get(0);
            for (int triIndex = 0; triIndex < numTriangles; ++triIndex) {
                int firstVertex = vpt * triIndex;
                Vector3f pos1 = list.get(firstVertex);
                Vector3f pos2 = list.get(firstVertex + 1);
                Vector3f pos3 = list.get(firstVertex + 2);
                double tetraVolume
                        = MyVolume.tetrahedronVolume(pos1, pos2, pos3, fixed);
                total += tetraVolume;
            }
        }
        float volume = (float) total;

        assert volume >= 0f : volume;
        return volume;
    }
    // *************************************************************************
    // private methods

    /**
     * Add a vertex to the mesh under construction.
     * <p>
     * This method is invoked from native code.
     *
     * @param x the local X coordinate of the new vertex
     * @param y the local Y coordinate of the new vertex
     * @param z the local Z coordinate of the new vertex
     * @param part ignored
     * @param index ignored
     */
    private void addVector(float x, float y, float z, int part, int index) {
        list.add(new Vector3f(x, y, z));
    }
}
