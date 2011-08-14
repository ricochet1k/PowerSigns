package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.ricochet1k.bukkit.powersigns.PowerSigns;
import com.ricochet1k.bukkit.powersigns.utils.BlockLine;

public class ActivateSignPlugin implements PowerSignsPlugin
{
	public static void register() {
		PowerSigns.register("activate", "[all] [u|d]", new ActivateSignPlugin());
	}
	
	static final Pattern argsPattern = Pattern.compile(
			PowerSigns.join(PowerSigns.allPart, PowerSigns.verticalPart), Pattern.CASE_INSENSITIVE);
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args)
	{
		Matcher m = argsPattern.matcher(args);
		if (!m.matches()) return false;
		
		boolean all = m.group(1) != null;
		
		//Sign signState = (Sign) signBlock.getState();
		
		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		BlockFace forward = PowerSigns.getForward(signDir, m.group(2));
		//Block startBlock = PowerSigns.getStartBlock(signBlock, signDir, forward).getFace(forward, 1);
		Block startBlock;
		if (forward != signDir && signBlock.getType().equals(Material.WALL_SIGN))
			startBlock = signBlock.getRelative(forward, 1);
		else
			startBlock = PowerSigns.getStartBlock(signBlock, signDir, forward).getRelative(forward, 1);
		
		boolean didSomething = false;
		for (Block block : new BlockLine(startBlock, forward).matches(PowerSigns.signMaterials))
		{
			if (!plugin.tryPowerSign(block))
			{
				plugin.debugFail("sign failed");
				//break;
			}
			
			didSomething = true;
			
			if (!all) break;
		}
		plugin.debugFail("no match");
		return didSomething;
	}

}
