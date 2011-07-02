package com.ricochet1k.bukkit.powersigns.plugins;


public class PluginInfo {
	public String action;
	public String syntax;
	//public Pattern pattern;
	public PowerSignsPlugin plugin;
	
	public PluginInfo(String a, String sy, PowerSignsPlugin pl)
	{
		action = a;
		syntax = sy;
		//pattern = pat;
		plugin = pl;
	}
}
