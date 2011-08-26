package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.ricochet1k.bukkit.powersigns.PowerSigns;
import com.ricochet1k.bukkit.powersigns.utils.BlockLine;

public class ActivateSignPlugin extends AimedSign2
{
	public ActivateSignPlugin()
	{
		super("(?:\\s+(all))?");
	}
	
	public static void register() {
		PowerSigns.register("activate", AimedSign.syntax + " [all]", new ActivateSignPlugin());
	}
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, Matcher argsm, BlockFace signDir,
			BlockFace forward, Block startBlock)
	{
		boolean all = argsm.group(1) != null;
		
		
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
