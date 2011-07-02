package com.ricochet1k.bukkit.powersigns.utils;

import java.security.InvalidParameterException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import net.minecraft.server.TileEntity;
import net.minecraft.server.World;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftChunk;

import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class BlockLine implements Iterator<Block>, Iterable<Block>
{
	//public Block prevBlock;
	//public Block nextBlock;
	public Block nextBlock;
	private BlockFace forward;
	private BlockFace backward;
	private boolean failed = false;
	
	public BlockLine(Block start, BlockFace dir)
	{
		assert start != null;
		assert dir != null;
		
		forward = dir;
		backward = dir.getOppositeFace();
		setNextBlock(start);
	}
	
	protected boolean blockMatches(Block block)
	{
		return true; // to be overridden
	}

	@Override
    public final boolean hasNext()
    {
	    return !failed;
    }
	
	public Block getPrevBlock()
	{
		return nextBlock.getFace(backward);
	}
	
	public Block getNextBlock()
	{
		return nextBlock;
	}
	
	public final Block prev()
	{
		if (failed) return null;

	    setNextBlock(getPrevBlock());
	    
	    return nextBlock;
	}
	
	@Override
    public final Block next()
    {
	    if (failed) return null;
	    
	    setNextBlock(nextBlock.getFace(forward));
	    
	    return getPrevBlock();
    }
	
	private void setNextBlock(Block block)
	{
		if (block == null) new Exception("block == null").printStackTrace();
		nextBlock = block;
	    
	    if (!blockMatches(nextBlock))
	    	failed = true;
	}

	@Override
    public final void remove()
    {
		// not removing blocks
		throw new NoSuchElementException(); // better exception?
    }
	
	@Override
	public final Iterator<Block> iterator()
	{
	    return this;
	}
	
	public BlockLine copy()
	{
		BlockLine bl = new BlockLine(getNextBlock(), forward);
		return bl;
	}
	
	public BlockLine flip()
	{
		BlockLine bl = new BlockLine(getPrevBlock(), backward);
		return bl;
	}
	
	public BlockLine matches(Material[] mats)
	{
		final Material[] materials = mats;
		return new BlockLine(getNextBlock(), forward)
			{
				@Override
				public boolean blockMatches(Block block)
				{
					return PowerSigns.materialsMatch(block.getType(), materials);
				}
			};
	}
	
	public BlockLine empties()
	{
		return new BlockLine(getNextBlock(), forward)
			{
				@Override
				public boolean blockMatches(Block block)
				{
					return PowerSigns.isEmpty(block);
				}
			};
	}
	
	
	public int count()
	{
		return count(PowerSigns.maxDistance);
	}
	public int count(int max)
	{
		int found = 0;
		while(hasNext())
		{
			next();
			
			found += 1;
			if (found >= max)
				return -1; // Prevent searching off into oblivion
		}

		return found;
	}
	
	public int skipMatching(Material[] materials)
	{
		return skipMatching(materials, PowerSigns.maxDistance);
	}
	public int skipMatching(Material[] materials, int max)
	{
		int found = 0;
		while(hasNext() && PowerSigns.materialsMatch(getNextBlock().getType(), materials))
		{
			next();
			found += 1;
			if (found >= max)
				return -1; // Prevent searching off into oblivion
		}

		return found;
	}
	
	public int skipEmpty()
	{
		return skipEmpty(PowerSigns.maxDistance);
	}
	public int skipEmpty(int max)
	{
		int found = 0;
		while(hasNext() && PowerSigns.isEmpty(getNextBlock()))
		{
			next();
			found += 1;
			if (found >= max)
				return -1; // Prevent searching off into oblivion
		}

		return found;
	}
	
	public void moveTo(BlockLine toLine, int howMany)
	{
		if (this == toLine || this.getNextBlock() == toLine.getNextBlock())
			throw new InvalidParameterException("this == toLine");
		
		if (howMany <= 0)
			return;

		int i = 0;

		//moveBlock(fromLine.currentBlock, toLine.currentBlock);

		for (; i < howMany; i++)
		{
			moveBlock(this.next(), toLine.next());
		}
	}
	
	public static void moveBlock(Block from, Block to)
	{
		if (from == to)
			throw new InvalidParameterException("from == to");
		
		World world = ((CraftChunk) from.getChunk()).getHandle().world;

		to.setTypeId(from.getTypeId());
		to.setData(from.getData());

		TileEntity tileEntity = world.getTileEntity(from.getX(), from.getY(), from.getZ());

		if (tileEntity != null)
		{
			((CraftChunk) from.getChunk()).getHandle().e(from.getX(), from.getY(), from.getZ()); // delete old tile entity
			world.setTileEntity(to.getX(), to.getY(), to.getZ(), tileEntity);
		}

		from.setTypeId(0); // clear from
	}
}
