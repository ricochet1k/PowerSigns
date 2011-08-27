package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.ricochet1k.bukkit.powersigns.PowerSigns;

public abstract class AimedSign extends ArgsSign
{
	public static String syntax = "[u|d][@[udlrfb](0-99)]";
	protected final Pattern argsPattern;

	public AimedSign()
	{
		this("");
	}
	
	public AimedSign(String argsRegex)
	{
		super("(?:\\s+([ud]))?" + "(?:\\s*@([udlrfb])?([0-9]+))?" + "(.*)$");
		
		argsPattern = Pattern.compile(argsRegex, Pattern.CASE_INSENSITIVE);
	}

	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, Matcher argsm)
	{
		Matcher argsm2 = argsPattern.matcher(argsm.group(4));
		if (!argsm2.matches()) return plugin.debugFail("syntax1");
		
		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		BlockFace forward = PowerSigns.getForward(signDir, argsm.group(1));
		Block startBlock = PowerSigns.getStartBlock(signBlock, signDir, forward, argsm.group(2), argsm.group(3));
		
		return doPowerSign(plugin, signBlock, action, argsm2, signDir, forward, startBlock);
	}
	
	public abstract boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, Matcher argsm,
										BlockFace signDir, BlockFace forward, Block startBlock);
}
