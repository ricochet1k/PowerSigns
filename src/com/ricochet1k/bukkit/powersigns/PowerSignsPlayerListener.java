package com.ricochet1k.bukkit.powersigns;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class PowerSignsPlayerListener implements Listener {
	
	private PowerSigns plugin;
	
	public PowerSignsPlayerListener(PowerSigns plugin)
	{
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && plugin.getDebugRightClick(event.getPlayer())
				&& PowerSigns.hasPermission(event.getPlayer(), "powersigns.debug.rightclick")
				&& PowerSigns.materialsMatch(event.getClickedBlock().getType(), PowerSigns.signMaterials))
		{
			boolean result = plugin.doPowerSign(event.getClickedBlock(), true);
			if (!result)
			{
				event.getPlayer().sendMessage(ChatColor.RED+ "Sign failed with message: "+ plugin.failMsg);
			}
			event.setCancelled(true);
		}
	}
}
