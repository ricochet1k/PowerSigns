package com.ricochet1k.bukkit.powersigns.plugins;
/*
import java.util.regex.Matcher;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import com.iConomy.iConomy;
import com.iConomy.system.Holdings;
import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class MoneySignPlugin extends AimedSign
{
	public MoneySignPlugin()
	{
		super("\\s+(take|give)");
	}

	public iConomy iconomy = null;

	public static void register(final PowerSigns plugin)
	{
		final MoneySignPlugin msp = new MoneySignPlugin();
		PowerSigns.register("money", "take|give  /  amount", msp);

		Listener serverListener = new Listener()
			{
				@EventHandler(priority=EventPriority.MONITOR)
				public void onPluginEnable(PluginEnableEvent event)
				{
					if (msp.iconomy == null)
					{
						Plugin iConomy = plugin.getServer().getPluginManager().getPlugin("iConomy");

						if (iConomy != null)
						{
							if (iConomy.isEnabled() && iConomy.getClass().getName().equals("com.iConomy.iConomy"))
							{
								msp.iconomy = (iConomy) iConomy;
								System.out.println("[PowerSigns-money] Found iConomy.");
							}
						}
					}
				}

				@EventHandler(priority=EventPriority.MONITOR)
				public void onPluginDisable(PluginDisableEvent event)
				{
					if (msp.iconomy != null)
					{
						if (event.getPlugin().getDescription().getName().equals("iConomy"))
						{
							msp.iconomy = null;
							System.out.println("[PowerSigns-money] Lost iConomy.");
						}
					}

				}
			};

		plugin.getServer().getPluginManager().registerEvents(serverListener, plugin);
	}

	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, Matcher argsm, Boolean isOn,
			BlockFace signDir, BlockFace forward, Block startBlock)
	{
		if (iconomy == null) return plugin.debugFail("No iConomy");

		Sign signState = (Sign) signBlock.getState();

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
			if (entity instanceof Player) player = ((Player) entity);
		}

		if (player == null) return plugin.debugFail("No player");

		Holdings holdings = iConomy.getAccount(player.getName()).getHoldings();
		String otherAccount = signState.getLine(2);
		Holdings other = otherAccount.length() > 0 ? iConomy.getAccount(otherAccount).getHoldings() : null;
		if (other == null && otherAccount.length() > 0) return plugin.debugFail("no account: " + otherAccount);

		String maction = argsm.group(1).toLowerCase();

		if (maction.equals("give"))
		{
			if (other != null && !other.hasEnough(amount)) return plugin.debugFail("Not enough money");
			if (other != null) other.subtract(amount);
			holdings.add(amount);
		}
		else
		// if (action.equals("take"))
		{
			if (!holdings.hasEnough(amount)) return plugin.debugFail("Not enough money");
			holdings.subtract(amount);
			if (other != null) other.add(amount);
		}

		return true;
	}

}
*/