package com.ricochet1k.bukkit.powersigns.utils;

import java.util.Iterator;

public class FilteredIterator<E> implements Iterator<E>
{
	private Iterator<E> iterator;
	private Predicate<E> predicate;
	
	private E next;
	
	public FilteredIterator(Iterator<E> iter, Predicate<E> pred)
	{
		iterator = iter;
		predicate = pred;
		
		findNext();
	}
	
	private void findNext()
	{
		while (iterator.hasNext())
		{
			next = iterator.next();
			if (predicate.apply(next))
				return;
		}
		next = null;
	}
	
	@Override
	public boolean hasNext()
	{
		return next != null;
	}

	@Override
	public E next()
	{
		E oldNext = next;
		findNext();
		return oldNext;
	}

	@Override
	public void remove()
	{
		// Can't remove.
	}

}
