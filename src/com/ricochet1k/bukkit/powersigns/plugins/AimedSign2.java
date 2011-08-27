package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.ricochet1k.bukkit.powersigns.PowerSigns;


// all the Activate* plugins use this instead of AimedSign
public abstract class AimedSign2 extends AimedSign
{
	public AimedSign2()
	{
		this("");
	}
	
	public AimedSign2(String argsRegex)
	{
		super(argsRegex);
	}

	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, Matcher argsm, Boolean isOn)
	{
		Matcher argsm2 = argsPattern.matcher(argsm.group(4));
		if (!argsm2.matches()) return plugin.debugFail("syntax2");
		
		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		BlockFace forward = PowerSigns.getForward(signDir, argsm.group(1));
		String skipDirStr = argsm.group(2);
		BlockFace skipDir = skipDirStr != null ? PowerSigns.strToDirection(skipDirStr, forward) : forward;
		Block startBlock;
		if (skipDir != signDir && signBlock.getType() == Material.WALL_SIGN)
		{
			String skipStr = argsm.group(3);
			int skip = skipStr != null && skipStr.length() > 0? Integer.parseInt(skipStr) : 0;
			startBlock = signBlock.getRelative(skipDir, skip + 1);
		}
		else
			startBlock = PowerSigns.getStartBlock(signBlock, signDir, forward, argsm.group(2), argsm.group(3));
		
		return doPowerSign(plugin, signBlock, action, argsm2, isOn, signDir, forward, startBlock);
	}
}
