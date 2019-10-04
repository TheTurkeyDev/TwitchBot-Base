package com.theprogrammingturkey.twitchbot.base;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.theprogrammingturkey.volatiliaweb.WebRequestBuilder;

public class StreamCheckThread implements Runnable
{
	private int delay;
	private boolean run = false;
	private Thread thread;
	private TwitchBot bot;

	public StreamCheckThread(int delay, TwitchBot bot)
	{
		this.delay = delay;
		this.bot = bot;
	}

	public void initCheckThread()
	{
		this.run = true;
		if((this.thread == null) || (!this.thread.isAlive()))
		{
			this.thread = new Thread(this);
			this.thread.start();
		}
	}

	public void run()
	{
		while(this.run)
		{
			checkStreams();
			try
			{
				synchronized(this)
				{
					wait(60000 * this.delay);
				}
			} catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		try
		{
			this.thread.interrupt();
			this.thread.join();
		} catch(InterruptedException localInterruptedException1)
		{
		}
	}

	private void checkStreams()
	{
		for(String stream : bot.getWatchedChannels())
		{
			String result = "";
			try
			{
				WebRequestBuilder request = new WebRequestBuilder("https://api.twitch.tv/kraken/streams/" + stream);
				request.addURLProp("client_id", bot.getClientID());
				request.addURLProp("api_version", "5");
				result = request.executeRequest();
			} catch(Exception e)
			{
				System.out.println("Failed to get api info for stream: " + stream);
			}
			if(!result.equalsIgnoreCase(""))
			{
				JsonElement jsonresp = TwitchBot.PARSER.parse(result);
				if(!(jsonresp.getAsJsonObject().get("stream") instanceof JsonNull))
				{
					if(!bot.isConnectedToChannel(stream))
						bot.connectToChannel(stream);
				}
				else if((bot.isConnectedToChannel(stream)) && (!stream.equalsIgnoreCase("#turkey2349")))
				{
					bot.disconnectFromChannel(stream);
				}
			}
		}
	}

	public void stopThread()
	{
		this.run = false;
	}

	public boolean isRunning()
	{
		return this.run;
	}
}
