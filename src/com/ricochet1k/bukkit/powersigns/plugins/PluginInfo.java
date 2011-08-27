package com.ricochet1k.bukkit.powersigns.plugins;


public class PluginInfo {
	public String action;
	public String syntax;
	public IPowerSignsPlugin plugin;
	public boolean handlesPowerOff;
	
	public PluginInfo(String a, String sy, IPowerSignsPlugin pl, boolean hpo)
	{
		action = a;
		syntax = sy;
		plugin = pl;
		handlesPowerOff = hpo;
	}
}
