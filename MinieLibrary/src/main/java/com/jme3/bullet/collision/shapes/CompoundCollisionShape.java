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
package com.jme3.bullet.collision.shapes;

import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * A CollisionShape formed by combining convex child shapes, based on Bullet's
 * btCompoundShape.
 *
 * @author normenhansen
 */
public class CompoundCollisionShape extends CollisionShape {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger2
            = Logger.getLogger(CompoundCollisionShape.class.getName());
    /**
     * local copy of {@link com.jme3.math.Matrix3f#IDENTITY}
     */
    final private static Matrix3f matrixIdentity = new Matrix3f();
    /**
     * field names for serialization
     */
    final private static String tagChildren = "children";
    // *************************************************************************
    // fields

    /**
     * child shapes of this shape
     */
    private ArrayList<ChildCollisionShape> children = new ArrayList<>(6);
    // *************************************************************************
    // constructors

    /**
     * Instantiate an empty compound shape (with no children).
     */
    public CompoundCollisionShape() {
        createEmpty();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a child shape with the specified local translation.
     *
     * @param shape the child shape to add (not null, not a compound shape,
     * alias created)
     * @param location the local coordinates of the child shape's center (not
     * null, unaffected)
     */
    public void addChildShape(CollisionShape shape, Vector3f location) {
        addChildShape(shape, location, matrixIdentity);
    }

    /**
     * Add a child shape with the specified local translation and orientation.
     *
     * @param shape the child shape to add (not null, not a compound shape,
     * alias created)
     * @param location the local coordinates of the child shape's center (not
     * null, unaffected)
     * @param rotation the local orientation of the child shape (not null,
     * unaffected)
     */
    public void addChildShape(CollisionShape shape, Vector3f location,
            Matrix3f rotation) {
        if (shape instanceof CompoundCollisionShape) {
            throw new IllegalArgumentException(
                    "CompoundCollisionShapes cannot have CompoundCollisionShapes as children!");
        }
        ChildCollisionShape child
                = new ChildCollisionShape(location, rotation, shape);
        children.add(child);
        long parentId = getObjectId();
        addChildShape(parentId, shape.getObjectId(), location, rotation);
    }

    /**
     * Add a child shape with the specified local transform. The transform scale
     * is ignored.
     *
     * @param shape the child shape to add (not null, not a compound shape,
     * alias created)
     * @param transform the local transform of the child shape (not null,
     * unaffected)
     */
    public void addChildShape(CollisionShape shape, Transform transform) {
        Vector3f offset = transform.getTranslation();
        Matrix3f orientation = transform.getRotation().toRotationMatrix();
        addChildShape(shape, offset, orientation);
    }

    /**
     * Count the child shapes.
     *
     * @return the count (&ge;0)
     */
    public int countChildren() {
        int numChildren = children.size();
        return numChildren;
    }

    /**
     * Find the first child with the specified shape.
     *
     * @param childShape the shape to search for
     * @return the index of the child if found, otherwise -1
     */
    public int findIndex(CollisionShape childShape) {
        int result = -1;
        for (int index = 0; index < children.size(); ++index) {
            ChildCollisionShape ccs = children.get(index);
            CollisionShape shape = ccs.getShape();
            if (shape == childShape) {
                result = index;
                break;
            }
        }

        return result;
    }

    /**
     * Enumerate the child shapes.
     *
     * @return a new array of pre-existing child shapes (not null)
     */
    public ChildCollisionShape[] listChildren() {
        int numChildren = children.size();
        ChildCollisionShape[] result = new ChildCollisionShape[numChildren];
        children.toArray(result);

        return result;
    }

    /**
     * Remove a child from this shape.
     *
     * @param shape the child shape to remove (not null)
     */
    public void removeChildShape(CollisionShape shape) {
        long parentId = getObjectId();
        removeChildShape(parentId, shape.getObjectId());
        for (Iterator<ChildCollisionShape> it = children.iterator();
                it.hasNext();) {
            ChildCollisionShape childCollisionShape = it.next();
            if (childCollisionShape.getShape() == shape) {
                it.remove();
            }
        }
    }
    // *************************************************************************
    // CollisionShape methods

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned shape into a deep-cloned one, using the specified Cloner
     * and original to resolve copied fields.
     *
     * @param cloner the Cloner that's cloning this shape (not null)
     * @param original the instance from which this shape was shallow-cloned
     * (not null, unaffected)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);
        children = cloner.clone(children);
        createEmpty();
        loadChildren();
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public CompoundCollisionShape jmeClone() {
        try {
            CompoundCollisionShape clone
                    = (CompoundCollisionShape) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * De-serialize this shape from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from the importer
     */
    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);
        children = capsule.readSavableArrayList(tagChildren, null);
        loadChildren();
    }

    /**
     * Recalculate this shape's bounding box if necessary.
     */
    @Override
    protected void recalculateAabb() {
        long nativeId = getObjectId();
        recalcAabb(nativeId);
    }

    /**
     * Serialize this shape to the specified exporter, for example when saving
     * to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);
        OutputCapsule capsule = exporter.getCapsule(this);
        capsule.writeSavableArrayList(children, tagChildren, null);
    }
    // *************************************************************************
    // private methods

    /**
     * Instantiate an empty btCompoundShape.
     */
    private void createEmpty() {
        long shapeId = createShape();
        setNativeId(shapeId);

        setScale(scale);
        setMargin(margin);
    }

    /**
     * Add the configured children to the empty btCompoundShape.
     */
    private void loadChildren() {
        long parentId = getObjectId();

        for (ChildCollisionShape child : children) {
            addChildShape(parentId, child.getShape().getObjectId(),
                    child.getLocation(null), child.getRotation(null));
        }
    }
    // *************************************************************************
    // native methods

    native private long addChildShape(long compoundId, long childId,
            Vector3f location, Matrix3f rotation); // TODO should return void

    native private long createShape();

    native private void recalcAabb(long shapeId);

    native private long removeChildShape(long compoundId,
            long childId); // TODO should return void
}
