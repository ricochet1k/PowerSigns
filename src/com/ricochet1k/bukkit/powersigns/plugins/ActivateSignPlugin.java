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
		PowerSigns.register("activate", "[u|d][@(0-99)] [all]", new ActivateSignPlugin());
	}
	
	static final Pattern argsPattern = Pattern.compile(
			PowerSigns.join(PowerSigns.verticalPart, PowerSigns.skipPart, PowerSigns.allPart), Pattern.CASE_INSENSITIVE);
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args)
	{
		Matcher argsm = argsPattern.matcher(args);
		if (!argsm.matches()) return plugin.debugFail("syntax");
		
		
		//Sign signState = (Sign) signBlock.getState();
		
		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		BlockFace forward = PowerSigns.getForward(signDir, argsm.group(1));
		Block startBlock;
		if (forward != signDir && signBlock.getType().equals(Material.WALL_SIGN))
		{
			String skipStr = argsm.group(2);
			int skip = skipStr != null && skipStr.length() > 0? Integer.parseInt(skipStr) : 0;
			startBlock = signBlock.getRelative(forward, skip + 1);
		}
		else
			startBlock = PowerSigns.getStartBlock(signBlock, signDir, forward, argsm.group(2));
		
		boolean all = argsm.group(3) != null;
		
		
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
