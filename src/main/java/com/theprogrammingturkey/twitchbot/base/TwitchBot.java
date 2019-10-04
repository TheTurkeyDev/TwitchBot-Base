package com.theprogrammingturkey.twitchbot.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jibble.pircbot.PircBot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.theprogrammingturkey.volatiliaweb.WebRequestBuilder;

public abstract class TwitchBot extends PircBot
{
	public static final String VERSION = "0.1";
	public static final JsonParser PARSER = new JsonParser();

	private String clientID;
	private String botName;
	private String oAuth;

	private boolean connected = false;
	private StreamCheckThread streamcheck;
	private List<String> connectChannels = new ArrayList<String>();
	private List<String> watchedChannels = new ArrayList<String>();
	private Map<String, String> idToChannelName = new HashMap<String, String>();
	private Map<String, String> channelNameToID = new HashMap<String, String>();

	public TwitchBot(String clientID, String name, String oAuth)
	{
		this.clientID = clientID;
		this.botName = name;
		this.oAuth = oAuth;
	}

	public void onMessage(String channel, String sender, String login, String hostname, String message)
	{
		CommandManager.handleMessage(channel, sender, message);
	}

	public void onJoin(String channel, String sender, String login, String hostname)
	{
	}

	public void onPart(String channel, String sender, String login, String hostname)
	{
	}

	public void reconnectBot()
	{
		disconnect();
		this.connected = false;
		this.streamcheck.stopThread();
		for(int i = 0; i < this.connectChannels.size(); i++)
		{
			String c = (String) this.connectChannels.get(i);
			disconnectFromChannel(c);
		}
		this.connectChannels.clear();
		connectToTwitch();
	}

	private boolean connectToTwitch()
	{
		setName(this.botName);
		try
		{
			connect("irc.twitch.tv", 6667, this.oAuth);
		} catch(Exception e)
		{
			if(!e.getMessage().equalsIgnoreCase("The PircBot is already connected to an IRC server.  Disconnect first."))
			{
				this.connected = false;
				logError("Could not connect to Twitch! \n" + e.getMessage());
				return false;
			}
		}
		this.connected = true;
		this.streamcheck = new StreamCheckThread(10, this);
		this.streamcheck.initCheckThread();
		connectToChannel("32907202");
		return true;
	}

	public void connectToChannel(String channel)
	{
		if(!this.connected)
			return;

		if(channel.startsWith("#"))
			channel = this.getChannelID(channel.substring(1));

		if(channel.equals("") || this.connectChannels.contains(channel))
			return;
		
		joinChannel(this.getChannelNameFromID(channel));
		this.connectChannels.add(channel);
	}

	public void disconnectFromChannel(String channel)
	{
		if(channel.startsWith("#"))
			channel = getChannelID(channel.substring(1));

		partChannel(this.getChannelNameFromID(channel));
		this.connectChannels.remove(channel);
	}

	public String capitalizeName(String name)
	{
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	public boolean didConnect()
	{
		return this.connected;
	}

	public void addWatchedChannel(String channel)
	{
		if(channel.startsWith("#"))
			channel = getChannelID(channel.substring(1));

		this.watchedChannels.add(channel);
	}

	public void removeWatchedChannel(String channel)
	{
		if(channel.startsWith("#"))
			channel = getChannelID(channel.substring(1));
		this.watchedChannels.remove(channel);
	}

	public List<String> getConnectChannels()
	{
		return this.connectChannels;
	}

	public List<String> getWatchedChannels()
	{
		return this.watchedChannels;
	}

	public boolean isConnectedToChannel(String channel)
	{
		if(channel.startsWith("#"))
			channel = getChannelID(channel.substring(1));
		return this.connectChannels.contains(channel);
	}

	public List<String> getNonConnectedChannels()
	{
		List<String> toReturn = new ArrayList<String>();
		toReturn.addAll(this.watchedChannels);
		toReturn.removeAll(this.connectChannels);
		return toReturn;
	}

	public String getClientID()
	{
		return this.clientID;
	}

	public String getChannelNameFromID(String id)
	{
		return this.idToChannelName.getOrDefault(id, "");
	}

	public String getChannelID(String channel)
	{
		if(this.channelNameToID.containsKey(channel))
			return this.channelNameToID.get(channel);

		try
		{
			WebRequestBuilder request = new WebRequestBuilder("https://api.twitch.tv/kraken/users");
			request.addURLProp("login", channel.substring(1));
			request.addURLProp("client_id", this.clientID);
			request.addURLProp("api_version", "5");
			String result = request.executeRequest();
			JsonObject jsonresp = PARSER.parse(result).getAsJsonObject();
			String id = jsonresp.getAsJsonArray("users").get(0).getAsJsonObject().get("_id").getAsString();
			this.channelNameToID.put(channel, id);
			this.idToChannelName.put(id, channel);
			return id;
		} catch(Exception e)
		{
			logError("Failed to get channel id for stream: " + channel);
			e.printStackTrace();
		}
		return "";
	}

	public abstract void log(String message);

	public abstract void logError(String error);
}
