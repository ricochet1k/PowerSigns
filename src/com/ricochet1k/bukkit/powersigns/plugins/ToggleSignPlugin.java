package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class ToggleSignPlugin implements PowerSignsPlugin
{
	public static void register() {
		PowerSigns.register("toggle", "[u|d][@(0-99)]", new ToggleSignPlugin());
	}
	
	static final Pattern argsPattern = Pattern.compile(
			PowerSigns.join(PowerSigns.verticalPart, PowerSigns.skipPart), Pattern.CASE_INSENSITIVE);
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args)
	{
		Matcher m = argsPattern.matcher(args);
		if (!m.matches()) return false;
		
		//Sign signState = (Sign) signBlock.getState();
		
		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		BlockFace forward = PowerSigns.getForward(signDir, m.group(1));
		final Block startBlock = PowerSigns.getStartBlock(signBlock, signDir, forward, m.group(2));
		
		switch (startBlock.getType())
		{
		case LEVER:
		case DIODE_BLOCK_ON:
		case DIODE_BLOCK_OFF:
		//case REDSTONE_TORCH_ON:
		//case REDSTONE_TORCH_OFF:
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
			{
				@Override
				public void run()
				{
					switch (startBlock.getType())
					{
					case LEVER:
						startBlock.setData((byte) (startBlock.getData() ^ 8), true);
						break;
					case DIODE_BLOCK_ON:
						startBlock.setTypeIdAndData(Material.DIODE_BLOCK_OFF.getId(), startBlock.getData(), true);
						break;
					case DIODE_BLOCK_OFF:
						startBlock.setTypeIdAndData(Material.DIODE_BLOCK_ON.getId(), startBlock.getData(), true);
						break;
					/*case REDSTONE_TORCH_ON:
						startBlock.setTypeIdAndData(Material.REDSTONE_TORCH_OFF.getId(), startBlock.getData(), true);
						break;
					case REDSTONE_TORCH_OFF:
						startBlock.setTypeIdAndData(Material.REDSTONE_TORCH_ON.getId(), startBlock.getData(), true);
						break;*/
					}
				}
			});
			return true;
		}
		
		
		return false;
	}

}
