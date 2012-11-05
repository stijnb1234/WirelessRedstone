package net.licks92.WirelessRedstone.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import net.licks92.WirelessRedstone.WirelessRedstone;
import net.licks92.WirelessRedstone.Channel.IWirelessPoint;
import net.licks92.WirelessRedstone.Channel.WirelessChannel;
import net.licks92.WirelessRedstone.Channel.WirelessReceiver;
import net.licks92.WirelessRedstone.Channel.WirelessScreen;
import net.licks92.WirelessRedstone.Channel.WirelessTransmitter;

public class YamlStorage implements IWirelessStorageConfiguration
{
	private File channelFolder;
	private WirelessRedstone plugin;
	
	public YamlStorage(File channelFolder, WirelessRedstone r_plugin)
	{
		plugin = r_plugin;
		this.channelFolder = channelFolder;
	}
	
	@Override
	public boolean init()
	{
		//Initialize the serialization
		ConfigurationSerialization.registerClass(WirelessReceiver.class, "WirelessReceiver");
		ConfigurationSerialization.registerClass(WirelessTransmitter.class, "WirelessTransmitter");
		ConfigurationSerialization.registerClass(WirelessChannel.class, "WirelessChannel");
		ConfigurationSerialization.registerClass(WirelessScreen.class, "WirelessScreen");
		
		return true;
	}

	@Override
	public boolean close()
	{
		return true;
	}

	@Override
	public WirelessChannel getWirelessChannel(String channelName)
	{
		FileConfiguration channelConfig = new YamlConfiguration();
		try
		{
			File channelFile = new File(channelFolder, channelName + ".yml");
			channelConfig.load(channelFile);
		}
		catch (FileNotFoundException e)
		{
			//WirelessRedstone.getStackableLogger().debug("File " + channelName + ".yml wans't found in the channels folder, returning null.");
			return null;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (InvalidConfigurationException e)
		{
			e.printStackTrace();
		}
		
		Object channel = channelConfig.get(channelName);
		if(channel == null)
			return null; // channel not found
		else if(!(channel instanceof WirelessChannel))
		{
			WirelessRedstone.getStackableLogger().warning("Channel "+channelName+" does not seem to be of type WirelessChannel.");
			return null;
		}
		else
			return (WirelessChannel)channel;
	}
	
	public void setWirelessChannel(String channelName, WirelessChannel channel)
	{
		FileConfiguration channelConfig = new YamlConfiguration();
		try
		{
			File channelFile = new File(channelFolder, channelName + ".yml");
			if(channel != null)
				channelFile.createNewFile();
			channelConfig.load(channelFile);
		}
		catch (FileNotFoundException e)
		{
			return;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (InvalidConfigurationException e)
		{
			e.printStackTrace();
		}
		
		channelConfig.set(channelName, channel);
		
		try
		{
			channelConfig.save(new File(channelFolder, channelName + ".yml"));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void createWirelessChannel(String channelName, WirelessChannel channel)
	{
		setWirelessChannel(channelName, channel);
	}

	@Override
	public void removeWirelessChannel(String channelName)
	{
		plugin.WireBox.removeSigns(getWirelessChannel(channelName));
		setWirelessChannel(channelName, null);
		for(File f : channelFolder.listFiles())
		{
			if(f.getName().equals(channelName))
			{
				f.delete();
			}
		}
		plugin.WireBox.UpdateCache();
	}

	@Override
	public void createWirelessPoint(String channelName, IWirelessPoint point)
	{
		WirelessChannel channel = getWirelessChannel(channelName);
		if(point instanceof WirelessReceiver)
			channel.addReceiver((WirelessReceiver) point);
		else if(point instanceof WirelessTransmitter)
			channel.addTransmitter((WirelessTransmitter) point);
		else if(point instanceof WirelessScreen)
			channel.addScreen((WirelessScreen) point);
		setWirelessChannel(channelName, channel);
	}

	@Override
	public boolean removeWirelessReceiver(String channelName, Location loc)
	{
		WirelessChannel channel = getWirelessChannel(channelName);
		if (channel != null)
		{
			channel.removeReceiverAt(loc);
			updateChannel(channelName, channel);
			return true;
		}
		else
			return false;
	}
	
	@Override
	public boolean removeWirelessTransmitter(String channelName, Location loc)
	{
		WirelessChannel channel = getWirelessChannel(channelName);
		if (channel != null)
		{
			channel.removeTransmitterAt(loc);
			updateChannel(channelName, channel);
			return true;
		}
		else
			return false;
	}
	
	@Override
	public boolean removeWirelessScreen(String channelName, Location loc)
	{
		WirelessChannel channel = getWirelessChannel(channelName);
		if (channel != null)
		{
			channel.removeScreenAt(loc);
			updateChannel(channelName, channel);
			return true;
		}
		else
			return false;
	}

	@Override
	public boolean wipeData()
	{
		//Backup the channels folder first.
		backupData();
		
		//Then remove the channels and the files.
		for(File f : channelFolder.listFiles())
		{
			removeWirelessChannel(f.getName());
		}
		return true;
	}
	
	@Override
	public boolean backupData()
	{
		try
		{
			String zipName = "WRBackup "
					+ Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
					+ Calendar.getInstance().get(Calendar.MONTH)
					+ Calendar.getInstance().get(Calendar.YEAR) + "-"
					+ Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
					+ Calendar.getInstance().get(Calendar.MINUTE)
					+ Calendar.getInstance().get(Calendar.SECOND);
			FileOutputStream fos = new FileOutputStream((channelFolder.getCanonicalPath().split(channelFolder.getName())[0]) + zipName + ".zip");
			ZipOutputStream zos = new ZipOutputStream(fos);

			for (File file : channelFolder.listFiles())
			{
				if (!file.isDirectory() && file.getName().contains(".yml"))
				{
					FileInputStream fis = new FileInputStream(file);
					
					ZipEntry zipEntry = new ZipEntry(file.getName());
					zos.putNextEntry(zipEntry);
					
					byte[] bytes = new byte[1024];
					int length;
					
					while ((length = fis.read(bytes)) >= 0)
					{
						zos.write(bytes, 0, length);
					}

					zos.closeEntry();
					fis.close();
				}
			}

			zos.close();
			fos.close();
			
		WirelessRedstone.getStackableLogger().info("Channels saved in archive : " + zipName);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public Collection<WirelessChannel> getAllChannels()
	{	
		List<WirelessChannel> channels = new ArrayList<WirelessChannel>();
		
		for(File f : channelFolder.listFiles())
		{
			FileConfiguration channelConfig = new YamlConfiguration();
			try
			{
				channelConfig.load(f);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (InvalidConfigurationException e)
			{
				e.printStackTrace();
			}
			String channelName = f.getName();
			channelName = channelName.split(".yml")[0];
			Object channel = channelConfig.get(channelName);
			if(channel instanceof WirelessChannel)
			{
				channels.add((WirelessChannel)channel);
				WirelessRedstone.getStackableLogger().debug("Channel added in getAllChannels() list : " + ((WirelessChannel)channel).getName());
			}
			else if(channel == null)
			{
				WirelessRedstone.getStackableLogger().debug("File " + f.getName() + " does not contain a Wireless Channel. Removing it.");
				f.delete();
			}
			else
				WirelessRedstone.getStackableLogger().warning("Channel " + channel + " is not of type WirelessChannel.");
		}
		if(channels.isEmpty())
		{
			return new ArrayList<WirelessChannel>();
		}
		return channels;
	}

	@Override
	public void updateChannel(String channelName, WirelessChannel channel)
	{
		setWirelessChannel(channelName, channel);
	}

}
