package com.funrungames.containers;

import java.awt.Rectangle;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.management.InvalidAttributeValueException;

/**
 * 
 * @author bjornbon
 * 
 * Information about Quadtrees, see https://en.wikipedia.org/wiki/Quadtree
 * 
 * This is a Quadtree implementation. Each QuadTree contains 4 quadrants. Each quadrant may contain another QuadTree object, etc.
 * So a quadtree is actually created by a tree of QuadTree objects.
 * A user constructs a QuadTree that has depth 0 by default. This is typically the object the user is interfacing with.
 * 
 * This quadtree makes it possible to find a shape, do something with it, and then remove it almost in an instant.
 * More info about this, see the functions:
 * public QuadTree findShape(IShape s) 
 * public boolean remove(IShape s, QuadTree t)
 * 
 */
public class QuadTree  implements Iterable<IShape> {

	/**
	 * This is not a hard boundary but a trigger to create child objects below the current one and move Shapes to the lower level
	 */
    private static final int maxNumberOfShapesPerNode = 10;

    private int maxDepth;
    
    /**
     * Depth of the current QuadTree object. A user typically interface with the object with depth==0
     */
    private int depth;
    
    /**
     * A QuadTree object represent a certain rectangular area, called boudingBox.
     */
    private Rectangle boudingBox;
    
    /**
     * A shape is stored in this QuadTree element if it fits within the boundig box of the QuadTree
     * If the shape is not in shapes it may be in one of the sub-QuadTrees.
     */
    private List<IShape> shapes;  
    
    /**
     * A QuadTree object can have up to 4 childs (I also call this sub-QuadTrees), for every quadrant of the boudingBox of the current QuadTree object.
     * 01
     * 23
     */
    private QuadTree[] childs;  
    
    /**
     * If a QuadTree-objects is not the root one (depth == 0) it must have a parent.
     */
    private QuadTree parent;
    
    /**
     * States that are used during iteration
     */
    private int iterateState; // 0-3 search through childs, 4 search own shapes
    private Iterator<IShape> shapesIterator;
    
    /**
     * The total number of shapes in the QuadTree structure.
     */
    private int aggregatedSize = 0; 

    /**
     * 
     * @param maxDepth In my ujournals clustering software I choose a depth 6, but an optimal maxDepth is 
     *                 very use-case specific and heavily depends on the size of the boundingBox. 
     *                 See https://gamedev.stackexchange.com/questions/77432/how-should-i-choose-quadtree-depth
     * @param boudingBox Speaks for itself.
     * @throws InvalidAttributeValueException
     * 
     * Note: Some functions use recursion. A very large maxDepth (say > 100) may cause issues then.
     */
    public QuadTree(int maxDepth, Rectangle boudingBox) throws InvalidAttributeValueException
    {
    	lInitQuadTree(maxDepth, boudingBox, 0, null);
    }
    
    /**
     * 
     * @param maxDepth
     * @param boudingBox
     * @param depth The depth of this QuadTree
     * @param parent All QuadTree objects have a parent except the one with dpeth 0.
     * @throws InvalidAttributeValueException
     */
    private QuadTree(int maxDepth, Rectangle boudingBox, int depth, QuadTree parent) throws InvalidAttributeValueException
    {
    	lInitQuadTree(maxDepth, boudingBox, depth, parent);
    }
    
    private void lInitQuadTree(int maxDepth, Rectangle boudingBox, int depth, QuadTree parent) throws InvalidAttributeValueException
    {
    	if (maxDepth < 1 || (boudingBox.width < 1 && boudingBox.height < 1))
    	{
    		throw new InvalidAttributeValueException();
    	}
        this.maxDepth = maxDepth;
        this.depth = depth;
        this.parent = parent;
        this.boudingBox = boudingBox;
        childs = null;
        shapes = new LinkedList<IShape>();
    } 
    
    public void dump()
    {
    	System.out.println("dump depth: " + depth);
    	System.out.println("boudingBox=" + boudingBox.toString());
    	int i = 0;
    	for (IShape s: shapes)
    	{
    		System.out.println("shape " + i + ": " + s.toString());
    		i++;
    	}
    	if (childs != null)
    	{
	    	for (i = 0; i < 4; i++)
	    	{
	    		if (childs[i] != null) childs[i].dump();
	    	}
    	}
    }
    
    /**
     * A helper class representing a quadrant of a QuadTree.
     * A quadant has an index 0..3 as depicted below:
     * 01
     * 23
     * 
     * and a quadrant has a covered area represented by boudingBox.
     * 
     * @author bjornbon
     *
     */
    private class Quadrant
    {
    	public int index;
    	public Rectangle boudingBox;
    	public Quadrant (int index, Rectangle boudingBox)
    	{
    		this.index = index;
    		this.boudingBox = boudingBox;
    	}
    }
    
    /**
     * In the current Quadrant and given a IShape s find a sub-quadrant where the shape fits in.
     * 
     * @param s
     * @return A sub-quadrant modelled by an index and a boudingBox (area). Returns null if s does not fit in one of the quadrants.
     */
    private Quadrant lFindChildIndexboudingBox(IShape s)
    {
    	Quadrant r = null;
    	
    	int x = (int)boudingBox.getX();
    	int y = (int)boudingBox.getY();
    	int w = (int)boudingBox.getWidth();
    	int h = (int)boudingBox.getHeight();
    	
    	Rectangle[] quadrantLocations = new Rectangle[4];
    	quadrantLocations[0] = new Rectangle(x, y, w / 2, h / 2);
    	quadrantLocations[1] = new Rectangle(x + w/2, y, w - w / 2, h / 2);
    	quadrantLocations[2] = new Rectangle(x, y + h/2, w / 2, h - h / 2);
    	quadrantLocations[3] = new Rectangle(x + w/2, y + h/2, w - w / 2, h - h / 2);
    	
    	Rectangle shapeLocation = s.getBoundingBox();
    	
    	for (int i = 0; i < 4; i++)
    	{
    		if (quadrantLocations[i].contains(shapeLocation))
        	{
        		r = new Quadrant(i, quadrantLocations[i]);
        		break;
        	}    		
    	}
    	return r;
    }
    
    /**
     * Try to insert a shape s to one of the child quadrants
     * @param s
     * @return
     * @throws Exception
     */
    private boolean lInsertToChild(IShape s) throws Exception
    {
    	boolean inserted = false;
    	Quadrant ib = lFindChildIndexboudingBox(s);

    	if (ib != null)
    	{
    		if (childs[ib.index] == null)
    		{
    			childs[ib.index] = new QuadTree(maxDepth, ib.boudingBox, depth + 1, this);
    		}
    		childs[ib.index].insert(s);
    		inserted = true;
    	}
    	return inserted;
    }
    
    /*
     * This is one of the core features of any QuadTree: Move a shape a low as possible in the QuadTree structure.
     */
    private void lMoveShapesToLowerLevel() throws Exception
    {
    	assert(childs != null): "child should be not null";
    	Iterator<IShape> it = shapes.iterator();
    	{
    		while(it.hasNext())
    		{
    			IShape s = it.next();
    			boolean inserted = lInsertToChild(s);
    			if (inserted)
    			{
    				it.remove();
    			}
    		}
    	}
    }
    
    /**
     * Inserting a shape in the QuadTree
     * @param s The shape to insert
     * @throws Exception
     */
    public void insert(IShape s) throws Exception
    {
    	if (!this.boudingBox.contains(s.getBoundingBox())) throw new Exception("Out of boudingBox"); 
    	
    	if (childs != null)
    	{
    		// We're already in the state of having child-QuadTrees
    		boolean inserted = lInsertToChild(s);
    		if (!inserted)
    		{
    			// It does not fit in one of the childs so put it in the shapes of the current object
    			shapes.add(s);
    		}
    	}
    	else
    	{
    		shapes.add(s);
    		if (shapes.size() > maxNumberOfShapesPerNode && depth < maxDepth)
    		{
    			childs = new QuadTree[4];
    			lMoveShapesToLowerLevel();
    		}
    	}
    	aggregatedSize++;
    }
    
    /**
     * Clean up childs if possible. Typically called when a shape is removed.
     */
    private void lCleanChilds()
    {
    	if (shapes != null)
    	{
    		int nmbrOfEmptyChilds = 0;
    		for (int i = 0; i < 4; i++)
			{
    			if (childs[i] != null && childs[i].size() == 0)
    			{
    				childs[i].parent = null;
    				childs[i] = null;
    			}
    			if (childs[i] == null) nmbrOfEmptyChilds++;
			}
    		if (nmbrOfEmptyChilds == 4)
    		{
    			childs = null;
    		}
    	}
    }
    
    /**
     * Find a shape and remove it.
     * @param s The shapes' equals method is used to determine whether a shape is found.
     * @return
     */
    public boolean remove(IShape s)
    {
    	QuadTree t = findShape(s);
    	if (t != null)
    	{
    		remove(s, t);
    	}
    	return t != null;
    }
    
    /**
     * Removes a shape but only looks in QuadTree t. SO no iteration through the whole tree.
     * This function is a lot faster than the remove with only s as parameter,
     * 
     * @param s The shapes' equals method is used to determine whether a shape is found.
     * @param t A QuadTree (sub)-object (typically returned by findShape())
     * @return
     */
    public boolean remove(IShape s, QuadTree t)
    {
    	boolean removed = false;
    	if (t != null)
    	{
    		removed = t.shapes.remove(s);
    		if (t.shapes.size() == 0 && t.childs == null && t.parent != null)
    		{
    			t.parent.lCleanChilds();
    		}
    	}
    	if (removed) 
		{
    		QuadTree qt = this;
    		while(qt != null)
    		{
    			qt.aggregatedSize--;
    			qt = qt.parent;
    		}
		}
    	return removed;
    }
    
    /**
     * 
     * @param s The shapes' equals method is used to determine whether a shape is found.
     * @return null if not found else the sub-QuadTree where the shape has been found. The return value can be input for the 
     * remove(IShape s, QuadTree t) function to remove the found shape in an instant.
     */
    public QuadTree findShape(IShape s)
    {
    	QuadTree r = null;
    	if (childs != null)
    	{
    		// first try to find shape at deepest level of QuadTree
    		Quadrant ib = lFindChildIndexboudingBox(s);
    		if (ib != null && childs[ib.index] != null)
    		{
    			r = childs[ib.index].findShape(s);
    		}
    	}
    	
    	// seconds if it is not at a deeper level check shapes array at current level
    	if (r == null)
    	{
    		for (IShape s1: shapes)
    		{
    			if (s1.equals(s))
    			{
    				r = this;
    				break;
    			}
    		}
    	}
    	return r;
    }
    
    /**
     * Compute the total size of a QudTree by iterating through the whole structure.
     * Mainly meant for testing
     * @return Number of shapes in the QuadTree.
     */
    public int sizeSlow()
    {
    	int size = shapes.size();
    	if (childs != null)
    	{
    		for (int i = 0; i < 4; i++)
    		{
    			if (childs[i] != null)
    			{
    				size += childs[i].sizeSlow();
    			}
    		}
    	}
    	return size;
    }
    
    /**
     * 
     * @return Number of shapes in the QuadTree.
     */
    public int size()
    {
    	return aggregatedSize;
    }
    
    /**
     * Search for interseting shapes in this QuadTree and its childs.
     * 
     * @param intersectingShapes output list containing the intersecting shapes.
     * @param refShape
     */
    private void getIntersectingShapes(List<IShape> intersectingShapes, IShape refShape)
    {
    	for (IShape shape: shapes)
    	{
    		if (shape.getBoundingBox().intersects(refShape.getBoundingBox()))
    		{
    			intersectingShapes.add(shape);
    		}
    	}
    	if (childs != null)
    	{
    		for (int i = 0; i < 4; i++)
    		{
    			if (childs[i] != null && childs[i].boudingBox.intersects(refShape.getBoundingBox()))
    			{
    				childs[i].getIntersectingShapes(intersectingShapes, refShape);
    			}
    		}
    	}
    }
    
    /**
     * The function it is all about.
     * 
     * @param refShape
     * @return List of all shapes that intersect with refShape
     */
    public List<IShape> getIntersectingShapes(IShape refShape)
    {
    	List<IShape> intersetingShapes = new LinkedList<IShape>();
    	getIntersectingShapes(intersetingShapes, refShape);
    	return intersetingShapes;
    }
    
    class QuadTreeIterator implements Iterator<IShape>
    {
    	private QuadTree qt; // current QuadTree I'm iterating through.

    	protected QuadTreeIterator(QuadTree root)
    	{
    		qt = root;
    		qt.iterateState = 0;
    	}
    	
    	/**
    	 * Iterating is done by first iterating through the sub-SquadTrees (the childs),
    	 * after that iterate through the list of shapes of this QuadTree.
    	 */
		@Override
		public boolean hasNext() 
		{
			boolean r = false;
			while (qt.iterateState < 4 && !r)
			{
				if (qt.childs != null)
				{
					if (qt.childs[qt.iterateState] != null)
					{
						qt = qt.childs[qt.iterateState];
						qt.iterateState = 0;
						r = hasNext();
						return r;
					}
					else
					{
						qt.iterateState++;
						if (qt.iterateState == 4)
						{
							qt.shapesIterator = qt.shapes.iterator();
						}
					}
				}
				else
				{
					qt.iterateState = 4;
					qt.shapesIterator = qt.shapes.iterator();
				}
			}
			
			if (!r && qt.iterateState == 4)
			{
				r = qt.shapesIterator.hasNext();
				if (!r)
				{
					qt.shapesIterator = null;
					if (qt.parent != null)
					{
						qt = qt.parent;
						qt.iterateState++;
						if (qt.iterateState == 4)
						{
							qt.shapesIterator = qt.shapes.iterator();
						}
						r = hasNext();
					}
				}
			}
			return r;
		}

		@Override
		public IShape next() throws NoSuchElementException
		{
			try
			{
				return qt.shapesIterator.next();
			}
			catch (Exception e)
			{
				hasNext();
				return qt.shapesIterator.next();
			}
		}

		@Override
		public void remove() throws UnsupportedOperationException 
		{
			throw new UnsupportedOperationException();
		}

    }

	@Override
	public Iterator<IShape> iterator() {
		return new QuadTreeIterator(this);
	}
}