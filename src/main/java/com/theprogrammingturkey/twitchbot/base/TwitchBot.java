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

	protected String clientID;
	protected String botName;
	protected String oAuth;

	private boolean connected = false;
	private StreamCheckThread streamcheck;
	private List<Integer> connectChannels = new ArrayList<Integer>();
	private List<Integer> watchedChannels = new ArrayList<Integer>();
	private Map<Integer, String> idToChannelName = new HashMap<Integer, String>();
	private Map<String, Integer> channelNameToID = new HashMap<String, Integer>();

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
			disconnectFromChannel(this.connectChannels.get(i));
		this.connectChannels.clear();
		connectToTwitch();
	}

	public boolean connectToTwitch()
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
		connectToChannel(32907202);
		return true;
	}

	public void disconnectFromTwitch()
	{
		if(this.isConnected())
		{
			disconnect();
			this.connected = false;
			this.streamcheck.stopThread();
			for(int i = 0; i < this.connectChannels.size(); i++)
				disconnectFromChannel(this.connectChannels.get(i));
			this.connectChannels.clear();
		}
	}

	public void connectToChannel(Integer channelID)
	{
		if(!this.connected)
			return;

		if(this.connectChannels.contains(channelID))
			return;

		joinChannel(this.getChannelNameFromID(channelID));
		this.connectChannels.add(channelID);
	}

	public void disconnectFromChannel(Integer channelID)
	{
		partChannel(this.getChannelNameFromID(channelID));
		this.connectChannels.remove(channelID);
	}

	public String capitalizeName(String name)
	{
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	public boolean didConnect()
	{
		return this.connected;
	}

	public void addWatchedChannel(Integer channelID)
	{
		this.watchedChannels.add(channelID);
	}

	public void removeWatchedChannel(Integer channelID)
	{
		this.watchedChannels.remove(channelID);
	}

	public List<Integer> getConnectChannels()
	{
		return this.connectChannels;
	}

	public List<Integer> getWatchedChannels()
	{
		return this.watchedChannels;
	}

	public boolean isConnectedToChannel(Integer channelID)
	{
		return this.connectChannels.contains(channelID);
	}

	public List<Integer> getNonConnectedChannels()
	{
		List<Integer> toReturn = new ArrayList<Integer>();
		toReturn.addAll(this.watchedChannels);
		toReturn.removeAll(this.connectChannels);
		return toReturn;
	}

	public String getClientID()
	{
		return this.clientID;
	}

	public String getChannelNameFromID(Integer id)
	{
		return "#" + this.idToChannelName.computeIfAbsent(id, (key) -> {
			WebRequestBuilder request = new WebRequestBuilder("https://api.twitch.tv/kraken/users/" + id);
			String response = "NONE";
			try
			{
				request.addURLProp("client_id", this.clientID);
				request.addURLProp("api_version", "5");
				response = request.executeRequest();
				JsonObject jsonresp = PARSER.parse(response).getAsJsonObject();
				String channelName = jsonresp.get("name").getAsString();
				this.channelNameToID.put(channelName, id);
				this.idToChannelName.put(id, channelName);
				return channelName;
			} catch(Exception e)
			{
				logError("Failed to get channel name for streamID: " + id);
				logError("Request: " + request.getURL());
				logError("Response text: " + response);
			}
			return "UNKNOWN";
		});
	}

	public Integer getChannelID(String channel)
	{
		if(channel.startsWith("#"))
			channel = channel.substring(1);

		if(this.channelNameToID.containsKey(channel))
			return this.channelNameToID.get(channel);

		WebRequestBuilder request = new WebRequestBuilder("https://api.twitch.tv/kraken/users");
		String response = "NONE";
		try
		{

			request.addURLProp("login", channel);
			request.addURLProp("client_id", this.clientID);
			request.addURLProp("api_version", "5");
			response = request.executeRequest();
			JsonObject jsonresp = PARSER.parse(response).getAsJsonObject();
			Integer id = Integer.parseInt(jsonresp.getAsJsonArray("users").get(0).getAsJsonObject().get("_id").getAsString());
			this.channelNameToID.put(channel, id);
			this.idToChannelName.put(id, channel);
			return id;
		} catch(Exception e)
		{
			logError("Failed to get channel id for stream: " + channel);
			logError("Request: " + request.getURL());
			logError("Response text: " + response);
			e.printStackTrace();
		}
		return -1;
	}

	public abstract void logInfo(String message);

	public abstract void logError(String error);
}
