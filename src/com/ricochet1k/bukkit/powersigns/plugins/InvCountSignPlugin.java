package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ContainerBlock;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.Vector;

import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class InvCountSignPlugin extends ArgsSign
{
	public InvCountSignPlugin()
	{
		super(PowerSigns.vectorPart);
	}
	
	public static void register() {
		InvCountSignPlugin iosp = new InvCountSignPlugin();
		PowerSigns.register("invcount", "(s|(fblrud)+)", iosp);
	}
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, Matcher argsm)
	{
		Sign signState = (Sign) signBlock.getState();
		
		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		
		Vector dir = PowerSigns.strToVector(argsm.group(1), signDir);
		
		Block invBlock = signBlock.getRelative(dir.getBlockX(), dir.getBlockY(), dir.getBlockZ());
		
		Inventory inventory;
		BlockState state = invBlock.getState();
		if (state instanceof ContainerBlock)
			inventory = ((ContainerBlock)state).getInventory();
		else
			return plugin.debugFail("bad inv:" + invBlock.getType().toString() + " " + dir.toString());
		
		
		Material[] materials = PowerSigns.getMaterials(signState.getLine(1));
		
		
		int count = 0;
		
		for (Material material : materials)
			count += PowerSigns.inventoryCount(inventory, material);
		
		signState.setLine(2, Integer.toString(count));
		
		return false;
	}

}
