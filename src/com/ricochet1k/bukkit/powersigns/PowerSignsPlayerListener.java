package com.ricochet1k.bukkit.powersigns;

import org.bukkit.ChatColor;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

public class PowerSignsPlayerListener extends PlayerListener {
	
	private PowerSigns plugin;
	
	public PowerSignsPlayerListener(PowerSigns plugin)
	{
		this.plugin = plugin;
	}
	
	@Override
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && PowerSigns.debugRightClick
				&& PowerSigns.hasPermission(event.getPlayer(), "powersigns.debug.rightclick")
				&& PowerSigns.matchesMaterials(event.getClickedBlock(), PowerSigns.signMaterials))
		{
			boolean result = plugin.tryPowerSign(event.getClickedBlock());
			if (!result)
			{
				event.getPlayer().sendMessage(ChatColor.RED+ "Sign failed with message: "+ plugin.failMsg);
			}
			event.setCancelled(true);
		}
	}
}