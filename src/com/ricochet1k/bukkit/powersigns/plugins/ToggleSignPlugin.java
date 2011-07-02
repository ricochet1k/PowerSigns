package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class ToggleSignPlugin implements PowerSignsPlugin
{
	public static void register() {
		PowerSigns.register("toggle", "[u|d]", new ToggleSignPlugin());
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
		Block startBlock = PowerSigns.getStartBlock(signBlock, signDir, forward).getFace(forward, 1);
		
		if (startBlock.getType() == Material.LEVER)
		{
			startBlock.setData((byte) (startBlock.getData() ^ 8), true);
			return true;
		}
		return false;
	}

}
