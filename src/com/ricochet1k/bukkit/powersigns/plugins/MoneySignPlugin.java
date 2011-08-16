package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.Plugin;

import com.iConomy.iConomy;
import com.iConomy.system.Holdings;
import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class MoneySignPlugin implements PowerSignsPlugin
{
	public iConomy iconomy = null;

	public static void register(final PowerSigns plugin) {
		final MoneySignPlugin msp = new MoneySignPlugin();
		PowerSigns.register("money", "take|give  /  amount", msp);
		
		ServerListener server = new ServerListener() {
			@Override public void onPluginEnable(PluginEnableEvent event)
			{
				if (msp.iconomy == null) {
		            Plugin iConomy = plugin.getServer().getPluginManager().getPlugin("iConomy");

		            if (iConomy != null) {
		                if (iConomy.isEnabled() && iConomy.getClass().getName().equals("com.iConomy.iConomy")) {
		                    msp.iconomy = (iConomy)iConomy;
		                    System.out.println("[PowerSigns-money] Found iConomy.");
		                }
		            }
		        }
			}
			
			@Override public void onPluginDisable(PluginDisableEvent event)
			{
				if (msp.iconomy != null) {
		            if (event.getPlugin().getDescription().getName().equals("iConomy")) {
		                msp.iconomy = null;
		                System.out.println("[PowerSigns-money] Lost iConomy.");
		            }
		        }

			}
		};
		
		plugin.getServer().getPluginManager().registerEvent(Type.PLUGIN_ENABLE, server, Priority.Monitor, plugin);
        plugin.getServer().getPluginManager().registerEvent(Type.PLUGIN_DISABLE, server, Priority.Monitor, plugin);
	}

	static final Pattern argsPattern = Pattern.compile(
			PowerSigns.join(PowerSigns.verticalPart, PowerSigns.skipPart, "\\s+(take|give)"),
			Pattern.CASE_INSENSITIVE);
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args)
	{
		if (iconomy == null) return plugin.debugFail("No iConomy");
		
		Matcher m = argsPattern.matcher(args);
		if (!m.matches()) return plugin.debugFail("syntax");
		
		Sign signState = (Sign) signBlock.getState();
		
		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		BlockFace forward = PowerSigns.getForward(signDir, m.group(1));
		Block startBlock = PowerSigns.getStartBlock(signBlock, signDir, forward, m.group(2));
		
		double amount;
		try
		{
			amount = Double.parseDouble(signState.getLine(1));
		}
		catch (NumberFormatException e)
		{
			return plugin.debugFail("Bad amount");
		}
		
		Player player = null;
		
		for (Entity entity : PowerSigns.entitiesNearBlock(startBlock, 1))
		{
			if (entity instanceof Player)
				player = ((Player)entity);
		}
		
		if (player == null)
			return plugin.debugFail("No player");
		
		Holdings holdings = iConomy.getAccount(player.getName()).getHoldings();
		String otherAccount = signState.getLine(2);
		Holdings other = otherAccount.length() > 0? iConomy.getAccount(otherAccount).getHoldings() : null;
		if (other == null && otherAccount.length() > 0)
			return plugin.debugFail("no account: "+otherAccount);
		
		String maction = m.group(3).toLowerCase();
		
		if (maction.equals("give"))
		{
			if (other != null && !other.hasEnough(amount))
				return plugin.debugFail("Not enough money");
			if (other != null) other.subtract(amount);
			holdings.add(amount);
		}
		else //if (action.equals("take"))
		{
			if (!holdings.hasEnough(amount))
				return plugin.debugFail("Not enough money");
			holdings.subtract(amount);
			if (other != null) other.add(amount);
		}
		
		return true;
	}

}
