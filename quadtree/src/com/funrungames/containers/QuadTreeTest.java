package com.funrungames.containers;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author bjornbon
 *
 */
public class QuadTreeTest {

	private final int widthWorld = 10000;
	private final int heightWorld = 10000;
	private final int maxWidthOfShape = 10;
	private final int maxHeightOfShape = 10;
	
	Random rand = new Random();

	class MyShape implements IShape
	{
		Rectangle boundingBox;
		public boolean visited = false;
				
		public MyShape(int x, int y, int w, int h)
		{
			this.boundingBox = new Rectangle(x,  y, w, h);
		}
		
		public MyShape() 
		{
			int  w = rand.nextInt(maxWidthOfShape) + 1; 
			int  h = rand.nextInt(maxHeightOfShape) + 1; 
			int  x = rand.nextInt(widthWorld - w);
			int  y = rand.nextInt(heightWorld - h);
			this.boundingBox = new Rectangle(x,  y, w, h);
		}
		
		public MyShape(int tempWidthWorld, int tempHeightWorld) 
		{
			int  w = rand.nextInt(maxWidthOfShape) + 1; 
			int  h = rand.nextInt(maxHeightOfShape) + 1; 
			int  x = rand.nextInt(tempWidthWorld - w);
			int  y = rand.nextInt(tempHeightWorld - h);
			this.boundingBox = new Rectangle(x,  y, w, h);
		}
		
		public String toString()
		{
			return boundingBox.toString();
		}

		@Override
		public Rectangle getBoundingBox() {
			return boundingBox;
		}
		
		@Override
		public boolean equals(Object o)
		{
			boolean r = false;
			if (o != null && o instanceof IShape)
			{
				Rectangle bb1 = getBoundingBox();
				Rectangle bb2 = ((IShape)o).getBoundingBox();
				r = (bb1.getX() == bb2.getX() && bb1.getY() == bb2.getY() && bb1.getWidth() == bb2.getWidth() && bb1.getHeight() == bb2.getHeight());
			}
			return r;
		}
	}
	
	
	@Test
	public void testEmpty() throws Exception 
	{
		QuadTree qt = new QuadTree(5, new Rectangle(0, 0, widthWorld, heightWorld));
		Assert.assertEquals(0, qt.size());
		Assert.assertEquals(0, qt.sizeSlow());
		IShape s = new MyShape();
		Assert.assertNull(qt.findShape(s));
	}
	
	@Test
	public void testOutOfBounds() throws Exception
	{
		boolean exceptionFired = false;
		QuadTree qt = new QuadTree(5, new Rectangle(0, 0, widthWorld, heightWorld));
		MyShape ms = new MyShape(widthWorld - 1, 1, 2, 1);
		try
		{
			qt.insert(ms);
		}
		catch (Exception e)
		{
			exceptionFired = true;
		}
		Assert.assertTrue(exceptionFired);
	}
	
	/* 
	 * Remove some shapes and test iteration 
	 */
	@Test
	public void testFindAndRemove() throws Exception 
	{
		int maxshapes = 1000;
		QuadTree qt = new QuadTree(5, new Rectangle(0, 0, widthWorld, heightWorld));
		List<IShape> l = new ArrayList<IShape>(maxshapes);
		Assert.assertEquals(0, qt.size());
		Assert.assertEquals(0, qt.sizeSlow());
		for (int i = 0; i < maxshapes; i++)
		{
			IShape s = new MyShape();
			qt.insert(s);
			l.add(s);
			
			Assert.assertEquals(i + 1, qt.sizeSlow());
			Assert.assertEquals(i + 1, qt.size());

		}
		
		Assert.assertEquals(maxshapes, l.size());
		Assert.assertEquals(maxshapes, qt.size());
		
		//qt.dump();
		int size = maxshapes;
		for (IShape s: l)
		{
			QuadTree qt2 = qt.findShape(s);
			Assert.assertNotNull(qt2);
			qt.remove(s, qt2);
			Assert.assertEquals(size - 1, qt.size());
			Assert.assertEquals(size - 1, qt.sizeSlow());
			Assert.assertNull(qt.findShape(s));
			
			qt.insert(s);
			Assert.assertNotNull(qt.findShape(s));
			Assert.assertEquals(size, qt.size());
			Assert.assertEquals(size, qt.sizeSlow());
			qt.remove(s);
			Assert.assertNull(qt.findShape(s));
			Assert.assertEquals(size - 1, qt.size());
			Assert.assertEquals(size - 1, qt.sizeSlow());
			
			size--;
		}
		Assert.assertEquals(0, qt.size());
		Assert.assertEquals(0, qt.sizeSlow());
	}
	
	private List<MyShape> lReturnListOfInterSectingShapes(MyShape s1, List<MyShape> shapes)
	{
		List<MyShape> returnList = new ArrayList<MyShape>();
		for (MyShape s2: shapes)
		{
			if (s2.getBoundingBox().intersects(s1.getBoundingBox()))
			{
				returnList.add(s2);
			}
		}
		return returnList;
	}
	
	@Test
	public void testIntersection() throws Exception 
	{
		int maxshapes = 1000;
		final int widthheightWorld = 300;
		QuadTree qt = new QuadTree(5, new Rectangle(0, 0, widthheightWorld, widthheightWorld));
		List<MyShape> l = new ArrayList<MyShape>(maxshapes);
		for (int i = 0; i < maxshapes; i++)
		{
			MyShape s = new MyShape(widthheightWorld, widthheightWorld);
			qt.insert(s);
			l.add(s);
		}
		Assert.assertEquals(maxshapes, l.size());
		Assert.assertEquals(maxshapes, qt.size());
		
		for (int i = 0; i < maxshapes; i++)
		{
			MyShape s = new MyShape(widthheightWorld, widthheightWorld);
			List<MyShape> intersectionsRef = lReturnListOfInterSectingShapes(s, l);
			List<IShape> intersections = qt.getIntersectingShapes(s);
			//System.out.println(intersectionsRef.size());
			Assert.assertEquals(intersectionsRef.size(), intersections.size());
			for (MyShape s1: intersectionsRef)
			{
				Assert.assertNotNull(intersections.contains(s1));
			}
		}
	}
	
	@Test
	public void testIterator() throws Exception
	{
		int maxshapes = 10000;
		final int widthheightWorld = 300;
		QuadTree qt = new QuadTree(5, new Rectangle(0, 0, widthheightWorld, widthheightWorld));
		
		// reference list to check QuadTree
		List<IShape> l = new ArrayList<IShape>(maxshapes);
		
		for (int i = 0; i < maxshapes; i++)
		{
			IShape s = new MyShape(widthheightWorld, widthheightWorld);
			qt.insert(s);
			l.add(s);
		}
		Assert.assertEquals(maxshapes, l.size());
		Assert.assertEquals(maxshapes, qt.size());
		Iterator<IShape> it = qt.iterator();
		int cnt = 0;

		while(it.hasNext())
		{
			MyShape s = (MyShape)it.next();
			//System.out.println(s);
			Assert.assertFalse(s.visited);
			Assert.assertNotNull(l.contains(s));
			s.visited = true;
			cnt++;
		}
		Assert.assertEquals(maxshapes, cnt);
		
		it = qt.iterator();
		cnt = 0;

		while(it.hasNext())
		{
			it.hasNext(); // just check if double call works
			MyShape s = (MyShape)it.next();
			//System.out.println(s);
			Assert.assertFalse(!s.visited);
			Assert.assertNotNull(l.contains(s));
			s.visited = false;
			cnt++;
		}
		Assert.assertEquals(maxshapes, cnt);
		
		it = qt.iterator();
		cnt = 0;
		try
		{
			while(true)
			{
				MyShape s = (MyShape)it.next();
				//System.out.println(s);
				Assert.assertFalse(s.visited);
				Assert.assertNotNull(l.contains(s));
				s.visited = true;
				cnt++;
			}
		}
		catch (Exception e)
		{
			
		}
		Assert.assertEquals(maxshapes, cnt);
	}
}
