package com.ricochet1k.bukkit.powersigns.plugins;

import org.bukkit.block.Block;

import com.ricochet1k.bukkit.powersigns.PowerSigns;


public interface PowerSignsPlugin
{
	boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args);
}
