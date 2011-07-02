package com.ricochet1k.bukkit.powersigns.utils;

import java.util.Iterator;

public class TransformedIterator<R, E> implements Iterator<R>
{
	private Iterator<E> iterator;
	private Transformer<R, E> transformer;
	
	public TransformedIterator(Iterator<E> iter, Transformer<R, E> trans)
	{
		iterator = iter;
		transformer = trans;
	}
	
	@Override
	public boolean hasNext()
	{
		return iterator.hasNext();
	}

	@Override
	public R next()
	{
		return transformer.transform(iterator.next());
	}

	@Override
	public void remove()
	{
		// Can't remove.
	}

}
