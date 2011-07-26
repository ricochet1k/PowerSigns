package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;

import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class DetectSignPlugin implements PowerSignsPlugin
{
	public static void register() {
		PowerSigns.register("detect", new DetectSignPlugin());
	}
	
	static final Pattern argsPattern = Pattern.compile(
			PowerSigns.join(PowerSigns.verticalPart),
			Pattern.CASE_INSENSITIVE);
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args)
	{
		Matcher m = argsPattern.matcher(args);
		if (!m.matches()) return false;
		
		Sign signState = (Sign) signBlock.getState();
		
		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		BlockFace forward = PowerSigns.getForward(signDir, m.group(1));
		Block startBlock = PowerSigns.getStartBlock(signBlock, signDir, forward).getRelative(forward, 1);
		
		signState.setLine(1, startBlock.getType().toString().toLowerCase());
		signState.setLine(2, Integer.toString(startBlock.getType().getId()));
		plugin.updateSignState(signState);
		return true;
	}

}
