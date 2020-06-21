/*
 Copyright (c) 2020, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

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
package jme3utilities.tutorial;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Sphere;

/**
 * A simple example using RigidBodyControl.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelloRbc extends SimpleApplication {
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloRbc application.
     *
     * @param ignored array of command-line arguments (not null)
     */
    public static void main(String[] ignored) {
        HelloRbc application = new HelloRbc();
        application.start();
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void simpleInitApp() {
        // Light the scene with directional and ambient lights.
        AmbientLight ambient = new AmbientLight(ColorRGBA.White.mult(0.2f));
        rootNode.addLight(ambient);

        Vector3f direction = new Vector3f(-0.7f, -0.3f, -0.5f).normalizeLocal();
        DirectionalLight sun = new DirectionalLight(direction, ColorRGBA.White);
        rootNode.addLight(sun);

        // Create a material and a mesh for balls.
        float ballRadius = 1f;
        Material ballMaterial = new Material(assetManager, Materials.LIGHTING);
        Mesh ballMesh = new Sphere(16, 32, ballRadius);

        // Create a geometries for a dynamic ball and a static ball
        // and add them to the scene graph.
        Geometry dyna = new Geometry("dyna", ballMesh);
        dyna.setMaterial(ballMaterial);
        rootNode.attachChild(dyna);

        Geometry stat = new Geometry("stat", ballMesh);
        stat.setMaterial(ballMaterial);
        rootNode.attachChild(stat);

        // Create controls for both balls and add them to the geometries.
        float mass = 2f;
        RigidBodyControl dynaRbc = new RigidBodyControl(mass);
        dyna.addControl(dynaRbc);

        RigidBodyControl statRbc
                = new RigidBodyControl(PhysicsBody.massForStatic);
        stat.addControl(statRbc);

        // Set up Bullet physics and create a physics space.
        BulletAppState bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        PhysicsSpace physicsSpace = bulletAppState.getPhysicsSpace();

        // Add the controls to the physics space.
        physicsSpace.add(dynaRbc);
        physicsSpace.add(statRbc);

        // Position the balls in physics space.
        dynaRbc.setPhysicsLocation(new Vector3f(0f, 4f, 0f));
        statRbc.setPhysicsLocation(new Vector3f(0.1f, 0f, 0f));

        // Minie's BulletAppState simulates the dynamics...
    }
}
