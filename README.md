# quadtree

A QuadTree implemented in Java.

Information about Quadtrees, see https://en.wikipedia.org/wiki/Quadtree

This is a Quadtree implementation. Each QuadTree contains 4 quadrants. Each quadrant may contain another QuadTree object, etc.
So a quadtree is actually created by a tree of QuadTree objects.
A user constructs a QuadTree that has depth 0 by default. This is typically the object the user is interfacing with.
 
This quadtree makes it possible to find a shape, do something with it, and then remove it almost in an instant.
More info about this, see the functions:
- public QuadTree findShape(IShape s) 
- public boolean remove(IShape s, QuadTree t)

# building
Copy paste the files into your java project and use freely.
