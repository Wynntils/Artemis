/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.type;

import com.wynntils.utils.mc.type.Location;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.joml.Vector2f;

public record BoundingPolygon(List<Vector2f> vertices, List<Vector2f> axes) implements BoundingShape {
    public static BoundingPolygon fromLocations(List<Location> locations) {
        return BoundingPolygon.fromVertices(locations.stream()
                .map(location -> new Vector2f(location.x(), location.z()))
                .toList());
    }

    public static BoundingPolygon fromBox(BoundingBox box) {
        return BoundingPolygon.fromVertices(List.of(
                new Vector2f(box.x1(), box.z1()),
                new Vector2f(box.x2(), box.z1()),
                new Vector2f(box.x2(), box.z2()),
                new Vector2f(box.x1(), box.z2())));
    }

    public static BoundingPolygon fromVertices(List<Vector2f> vertices) {
        List<Vector2f> verticeList = Collections.unmodifiableList(vertices);
        List<Vector2f> axes = Collections.unmodifiableList(computeAxes(verticeList));

        // Assert that the polygon has at least 3 vertices
        assert verticeList.size() >= 3;

        // Assert that the polygon is ordered in a counterclockwise orientation and is convex
        for (int i = 0; i < verticeList.size(); i++) {
            Vector2f a = verticeList.get(i);
            Vector2f b = verticeList.get((i + 1) % verticeList.size());
            Vector2f c = verticeList.get((i + 2) % verticeList.size());
            assert (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x()) > 0;
        }

        return new BoundingPolygon(verticeList, axes);
    }

    public Vector2f centroid() {
        float area = 0;
        float centroidX = 0;
        float centroidY = 0;

        int numVertices = vertices.size();
        for (int i = 0; i < numVertices; i++) {
            Vector2f current = vertices.get(i);
            Vector2f next = vertices.get((i + 1) % numVertices);

            float crossProduct = current.x * next.y - next.x * current.y;
            area += crossProduct;

            centroidX += (current.x + next.x) * crossProduct;
            centroidY += (current.y + next.y) * crossProduct;
        }

        area *= 0.5f;
        centroidX /= (6 * area);
        centroidY /= (6 * area);

        return new Vector2f(centroidX, centroidY);
    }

    public float maxWidth() {
        float minX = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;

        for (Vector2f vertex : vertices) {
            if (vertex.x < minX) {
                minX = vertex.x;
            }
            if (vertex.x > maxX) {
                maxX = vertex.x;
            }
        }

        return maxX - minX;
    }

    @Override
    public boolean contains(float x, float z) {
        // Ray casting algorithm / crossing number algorithm

        // Count the number of times a ray from the point to the right crosses a polygon edge
        int count = 0;

        for (int i = 0; i < vertices.size(); i++) {
            Vector2f a = vertices.get(i);
            Vector2f b = vertices.get((i + 1) % vertices.size());

            // Check if the ray crosses the edge
            if ((a.y() > z) != (b.y() > z) && x < (b.x() - a.x()) * (z - a.y()) / (b.y() - a.y()) + a.x()) {
                count++;
            }
        }

        // If the number of crossings is odd, the point is inside the polygon
        return count % 2 == 1;
    }

    @Override
    public boolean intersects(BoundingBox boundingBox) {
        return BoundingShape.intersects(boundingBox, this);
    }

    @Override
    public boolean intersects(BoundingCircle boundingCircle) {
        // Separating Axis Theorem algorithm
        Vector2f circleCenter = new Vector2f(boundingCircle.x(), boundingCircle.z());
        for (Vector2f axis : axes) {
            Projection projection1 = projectCircle(boundingCircle, axis);
            Projection projection2 = projectPolygon(this, axis);
            if (!projection1.overlaps(projection2)) {
                return false;
            }
        }

        // Check additional axes from the circle center to each vertex
        for (Vector2f vertex : vertices) {
            Vector2f axis = vertex.sub(circleCenter, new Vector2f()).normalize();
            Projection projection1 = projectCircle(boundingCircle, axis);
            Projection projection2 = projectPolygon(this, axis);
            if (!projection1.overlaps(projection2)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean intersects(BoundingPolygon boundingPolygon) {
        // Separating Axis Theorem algorithm
        for (Vector2f axis : axes) {
            Projection projection1 = projectPolygon(this, axis);
            Projection projection2 = projectPolygon(boundingPolygon, axis);
            if (!projection1.overlaps(projection2)) {
                return false;
            }
        }

        for (Vector2f axis : boundingPolygon.axes()) {
            Projection projection1 = projectPolygon(this, axis);
            Projection projection2 = projectPolygon(boundingPolygon, axis);
            if (!projection1.overlaps(projection2)) {
                return false;
            }
        }

        return true;
    }

    private static Projection projectPolygon(BoundingPolygon polygon, Vector2f axis) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        for (Vector2f vertex : polygon.vertices()) {
            double p = axis.dot(vertex);
            if (p < min) {
                min = p;
            }
            if (p > max) {
                max = p;
            }
        }

        return new Projection(min, max);
    }

    private static Projection projectCircle(BoundingCircle circle, Vector2f axis) {
        Vector2f circleCenter = new Vector2f(circle.x(), circle.z());
        float centerProjection = circleCenter.dot(axis);
        float radiusProjection = circle.radius() * axis.length();
        return new Projection(centerProjection - radiusProjection, centerProjection + radiusProjection);
    }

    private static List<Vector2f> computeAxes(List<Vector2f> vertices) {
        List<Vector2f> axes = new ArrayList<>();

        for (int i = 0; i < vertices.size(); i++) {
            Vector2f vertex1 = vertices.get(i);
            Vector2f vertex2 = vertices.get((i + 1) % vertices.size());
            Vector2f edge = vertex2.sub(vertex1, new Vector2f());
            axes.add(edge.perpendicular());
        }

        return axes;
    }

    private record Projection(double min, double max) {
        public boolean overlaps(Projection other) {
            return max >= other.min && other.max >= min;
        }
    }
}
