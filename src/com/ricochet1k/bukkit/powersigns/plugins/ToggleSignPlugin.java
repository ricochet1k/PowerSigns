package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class ToggleSignPlugin extends AimedSign
{
	public static void register()
	{
		PowerSigns.register("toggle", AimedSign.syntax, new ToggleSignPlugin());
	}

	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, Matcher argsm, BlockFace signDir,
			BlockFace forward, final Block startBlock)
	{
		Runnable r;

		switch (startBlock.getType())
		{
		case LEVER:
			r = new Runnable()
				{
					@Override
					public void run()
					{
						startBlock.setData((byte) (startBlock.getData() ^ 8), true);
					}
				};
			break;
		case DIODE_BLOCK_ON:
			r = new Runnable()
				{
					@Override
					public void run()
					{
						startBlock.setTypeIdAndData(Material.DIODE_BLOCK_OFF.getId(), startBlock.getData(), true);
					}
				};
			break;
		case DIODE_BLOCK_OFF:
			r = new Runnable()
				{
					@Override
					public void run()
					{
						startBlock.setTypeIdAndData(Material.DIODE_BLOCK_ON.getId(), startBlock.getData(), true);
					}
				};
			break;
		default:
			return false;
		}

		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, r);

		return true;
	}

}
