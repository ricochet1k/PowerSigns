package com.ricochet1k.bukkit.powersigns;

import java.util.Iterator;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class BlockLineIterator implements Iterator<Block>, Iterable<Block>
{
	public Block prevBlock;
	public Block nextBlock;
	public Block currentBlock;
	private BlockFace direction;
	private boolean failed = false;
	
	public BlockLineIterator(Block start, BlockFace dir)
	{
		assert start != null;
		assert dir == null;
		
		direction = dir;
		setNextBlock(start);
	}
	
	public boolean blockMatches(Block block)
	{
		return true; // to be overridden
	}

	@Override
    public final boolean hasNext()
    {
	    return !failed;
    }

	@Override
    public final Block next()
    {
	    if (failed) return null;
	    
	    setNextBlock(nextBlock.getFace(direction));
	    
	    return currentBlock;
    }
	
	private void setNextBlock(Block block)
	{
		if (block == null) new Exception("block == null").printStackTrace();
		prevBlock = currentBlock;
		currentBlock = nextBlock;
	    nextBlock = block;
	    
	    if (!blockMatches(nextBlock))
	    	failed = true;
	}

	@Override
    public final void remove()
    {
		// not removing blocks
    }
	
	@Override
	public final Iterator<Block> iterator()
	{
	    return this;
	}
}
