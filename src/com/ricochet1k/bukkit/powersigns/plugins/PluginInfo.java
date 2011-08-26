package com.ricochet1k.bukkit.powersigns.plugins;


public class PluginInfo {
	public String action;
	public String syntax;
	//public Pattern pattern;
	public IPowerSignsPlugin plugin;
	
	public PluginInfo(String a, String sy, IPowerSignsPlugin pl)
	{
		action = a;
		syntax = sy;
		//pattern = pat;
		plugin = pl;
	}
}
