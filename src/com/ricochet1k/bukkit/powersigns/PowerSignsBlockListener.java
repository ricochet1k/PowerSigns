package com.ricochet1k.bukkit.powersigns;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRedstoneEvent;



public class PowerSignsBlockListener extends BlockListener
{
	public static final BlockFace[] adjacentFaces = new BlockFace[] { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
        BlockFace.UP/*, BlockFace.DOWN */}; // Redstone can't be placed on a sign.
	
	private PowerSigns plugin;

	public PowerSignsBlockListener(PowerSigns plugin)
	{
		this.plugin = plugin;
	}

	@Override
	public void onBlockRedstoneChange(BlockRedstoneEvent event)
	{
		if (event.getNewCurrent() == 0 || event.getOldCurrent() != 0) return;
		
		Block block = event.getBlock();
		for (BlockFace face : adjacentFaces)
		{
			Block temp = block.getFace(face);
			if (temp.getType().equals(Material.WALL_SIGN) || temp.getType().equals(Material.SIGN_POST)) {
				plugin.tryPowerSign(temp);
			}
		}
	}


}
