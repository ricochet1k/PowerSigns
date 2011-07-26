package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.ricochet1k.bukkit.powersigns.PowerSigns;
import com.ricochet1k.bukkit.powersigns.utils.BlockLine;

public class ActivateLongSignPlugin implements PowerSignsPlugin
{
	public static void register() {
		PowerSigns.register("activatelong", "[u|d]", new ActivateLongSignPlugin());
	}
	
	static final Pattern argsPattern = Pattern.compile(
			PowerSigns.join(PowerSigns.verticalPart), Pattern.CASE_INSENSITIVE);
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args)
	{
		Matcher m = argsPattern.matcher(args);
		if (!m.matches()) return false;
		
		//Sign signState = (Sign) signBlock.getState();
		
		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		BlockFace forward = PowerSigns.getForward(signDir, m.group(1));
		Block startBlock = PowerSigns.getStartBlock(signBlock, signDir, forward).getRelative(forward, 1);
		
		BlockLine line = new BlockLine(startBlock, forward);
		line.skipEmpty();
		if (PowerSigns.materialsMatch(line.getNextBlock().getType(), PowerSigns.signMaterials))
		{
			if (plugin.tryPowerSign(line.getNextBlock()))
				return true;
			else
				return plugin.debugFail("sign failed");
		}
		return plugin.debugFail("no match: "+line.getNextBlock().getType().toString());
	}

}
