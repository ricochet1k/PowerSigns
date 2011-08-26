package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.block.Block;

import com.ricochet1k.bukkit.powersigns.PowerSigns;

public abstract class ArgsSign implements IPowerSignsPlugin
{
	private final Pattern argsPattern;
	
	public ArgsSign(String argsRegex)
	{
		argsPattern = Pattern.compile(argsRegex, Pattern.CASE_INSENSITIVE);
	}
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args)
	{
		Matcher argsm = argsPattern.matcher(args);
		if (!argsm.matches()) return plugin.debugFail("syntax");
		
		return doPowerSign(plugin, signBlock, action, argsm);
	}
	
	public abstract boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, Matcher argsm);

}
