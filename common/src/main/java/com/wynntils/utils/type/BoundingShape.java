/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.type;

public interface BoundingShape {
    boolean contains(float x, float z);

    boolean intersects(BoundingBox boundingBox);

    boolean intersects(BoundingCircle boundingCircle);

    boolean intersects(BoundingPolygon boundingPolygon);

    static boolean intersects(BoundingBox boundingBox1, BoundingBox boundingBox2) {
        return boundingBox1.intersects(boundingBox2);
    }

    static boolean intersects(BoundingCircle boundingCircle1, BoundingCircle boundingCircle2) {
        return boundingCircle1.intersects(boundingCircle2);
    }

    static boolean intersects(BoundingPolygon boundingPolygon1, BoundingPolygon boundingPolygon2) {
        return boundingPolygon1.intersects(boundingPolygon2);
    }

    static boolean intersects(BoundingBox boundingBox, BoundingCircle boundingCircle) {
        return boundingBox.intersects(boundingCircle);
    }

    static boolean intersects(BoundingBox boundingBox, BoundingPolygon boundingPolygon) {
        // This is done by turning the bounding box into a polygon and then checking for polygon-polygon intersection
        return boundingPolygon.intersects(BoundingPolygon.fromBox(boundingBox));
    }

    static boolean intersects(BoundingCircle boundingCircle, BoundingPolygon boundingPolygon) {
        return boundingPolygon.intersects(boundingCircle);
    }
}
