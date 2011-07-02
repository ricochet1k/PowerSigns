package com.ricochet1k.bukkit.powersigns;

import java.util.regex.Matcher;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.ItemStack;



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
	public void onSignChange(org.bukkit.event.block.SignChangeEvent event) {
		Matcher m = PowerSigns.actionPattern.matcher(event.getLine(0));
		if (m.matches())
		{
			if (!PowerSigns.hasPermission(event.getPlayer(), "powersigns.create."+m.group(1).toLowerCase()))
			{
				event.getBlock().setType(Material.AIR);
				event.getPlayer().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.SIGN, 1));
				event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to create a " + m.group(1) + " sign.");
			}
		}
	}

	@Override
	public void onBlockRedstoneChange(BlockRedstoneEvent event)
	{
		if (plugin.isDisabled() || event.getNewCurrent() == 0 || event.getOldCurrent() != 0) return;
		
		Block block = event.getBlock();
		for (BlockFace face : adjacentFaces)
		{
			Block temp = block.getFace(face);
			Material tempType = temp.getType();
			if (tempType.equals(Material.WALL_SIGN) || tempType.equals(Material.SIGN_POST)) {
				plugin.tryPowerSign(temp);
			}
		}
	}


}
