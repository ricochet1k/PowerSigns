package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ContainerBlock;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.Vector;

import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class InvCountSignPlugin implements PowerSignsPlugin
{
	public static void register() {
		InvCountSignPlugin iosp = new InvCountSignPlugin();
		PowerSigns.register("invcount", "(s|(fblrud)+)", iosp);
	}
	
	static final Pattern argsPattern = Pattern.compile(
			PowerSigns.join(PowerSigns.vectorPart),
			Pattern.CASE_INSENSITIVE);
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args)
	{
		Matcher m = argsPattern.matcher(args);
		if (!m.matches()) return plugin.debugFail("syntax");
		
		Sign signState = (Sign) signBlock.getState();
		
		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		
		Vector dir = PowerSigns.strToVector(m.group(1), signDir);
		
		Block invBlock = signBlock.getRelative(dir.getBlockX(), dir.getBlockY(), dir.getBlockZ());
		
		Inventory inventory;
		BlockState state = invBlock.getState();
		if (state instanceof ContainerBlock)
			inventory = ((ContainerBlock)state).getInventory();
		else
			return plugin.debugFail("bad inv:"+invBlock.getType().toString()+" "+dir.toString());
		
		
		Material[] materials = PowerSigns.getMaterials(signState.getLine(1));
		
		
		int count = 0;
		
		for (Material material : materials)
			count += PowerSigns.inventoryCount(inventory, material);
		
		signState.setLine(2, Integer.toString(count));
		
		return false;
	}

}
