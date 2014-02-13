// SMSLib for Java v3
// A Java API library for sending and receiving SMS via a GSM modem
// or other supported gateways.
// Web Site: http://www.smslib.org
//
// Copyright (C) 2002-2012, Thanasis Delenikas, Athens/GREECE.
// SMSLib is distributed under the terms of the Apache License version 2.0
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.smslib.test;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import org.smslib.AGateway;
import org.smslib.GatewayException;
import org.smslib.InboundMessage;
import org.smslib.OutboundMessage;
import org.smslib.Service;
import org.smslib.TimeoutException;
import org.smslib.InboundMessage.MessageClasses;
import org.smslib.Message.MessageTypes;
import org.smslib.OutboundMessage.MessageStatuses;
import org.smslib.helper.Logger;
import org.smslib.notify.InboundMessageNotification;

/**
 * TestGateway - virtual gateway to simulate sending and receiving messages to
 * make testing easier.
 */
public class TestGateway extends AGateway
{
	private int refCounter = 0;

	private int counter = 0;

	/**
	 * After how much sent messages next one should fail setting to 2 makes two
	 * messages sent, and then one failed.
	 */
	protected int failCycle;

	/**
	 * Duration between incoming messages in milliseconds
	 */
	protected int receiveCycle;

	Thread incomingMessagesThread;

	public TestGateway(String id)
	{
		super(id);
		setAttributes(GatewayAttributes.SEND);
		setInbound(true);
		setOutbound(true);
		setFailCycle(3000);
		this.receiveCycle = 60000;
	}

	/* (non-Javadoc)
	 * @see org.smslib.AGateway#deleteMessage(org.smslib.InboundMessage)
	 */
	@Override
	public boolean deleteMessage(InboundMessage msg) throws TimeoutException, GatewayException, IOException, InterruptedException
	{
		//NOOP
		return true;
	}

	InboundMessage generateIncomingMessage()
	{
		incInboundMessageCount();
		InboundMessage msg = new InboundMessage(new java.util.Date(), "+1234567890", "Hello World! #" + getInboundMessageCount(), 0, null);
		msg.setGatewayId(this.getGatewayId());
		return msg;
	}

	/* (non-Javadoc)
	 * @see org.smslib.AGateway#startGateway()
	 */
	@Override
	public void startGateway() throws TimeoutException, GatewayException, IOException, InterruptedException
	{
		super.startGateway();
		this.incomingMessagesThread = new Thread(new Runnable()
		{
			// Run thread to fake incoming messages
			public void run()
			{
				while (!TestGateway.this.incomingMessagesThread.isInterrupted())
				{
					synchronized (TestGateway.this.incomingMessagesThread)
					{
						try
						{
							TestGateway.this.incomingMessagesThread.wait(TestGateway.this.receiveCycle);
						}
						catch (InterruptedException e)
						{
							// NOOP
							break;
						}
					}
					if (!TestGateway.this.incomingMessagesThread.isInterrupted())
					{
						Logger.getInstance().logInfo("Detecting incoming message", null, getGatewayId());
						Service.getInstance().getNotifyQueueManager().getNotifyQueue().add(new InboundMessageNotification(getMyself(), MessageTypes.INBOUND, generateIncomingMessage()));
					}
				}
			}
		}, "IncomingMessagesThread");
		this.incomingMessagesThread.start();
	}

	/* (non-Javadoc)
	 * @see org.smslib.AGateway#stopGateway()
	 */
	@Override
	public void stopGateway() throws TimeoutException, GatewayException, IOException, InterruptedException
	{
		super.stopGateway();
		if (this.incomingMessagesThread != null)
		{
			synchronized (this.incomingMessagesThread)
			{
				this.incomingMessagesThread.interrupt();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.smslib.AGateway#readMessage(java.lang.String, int)
	 */
	@Override
	public InboundMessage readMessage(String memLoc, int memIndex) throws TimeoutException, GatewayException, IOException, InterruptedException
	{
		// Return a new generated message 
		this.counter++;
		if ((this.failCycle > 0) && (this.counter >= this.failCycle))
		{
			this.counter = 0;
			throw new GatewayException("*** READ ERROR ***");
		}
		return generateIncomingMessage();
	}

	/* (non-Javadoc)
	 * @see org.smslib.AGateway#readMessages(java.util.List, org.smslib.MessageClasses)
	 */
	@Override
	public void readMessages(Collection<InboundMessage> msgList, MessageClasses msgClass) throws TimeoutException, GatewayException, IOException, InterruptedException
	{
		// Return a new generated message
		msgList.add(generateIncomingMessage());
	}

	@Override
	public boolean sendMessage(OutboundMessage msg) throws TimeoutException, GatewayException, IOException, InterruptedException
	{
		//if (getGatewayId().equalsIgnoreCase("Test3"))  throw new IOException("Dummy Exception!!!");
		// simulate delay
		Logger.getInstance().logInfo("Sending to: " + msg.getRecipient() + " via: " + msg.getGatewayId(), null, getGatewayId());
		this.counter++;
		if ((this.failCycle > 0) && (this.counter >= this.failCycle))
		{
			this.counter = 0;
			if (getGatewayId().equalsIgnoreCase("Test3")) ;//
			else
			{
				throw new IOException("Dummy Exception!!!");
				//msg.setFailureCause(FailureCauses.GATEWAY_FAILURE);
				//return false;
			}
		}
		msg.setDispatchDate(new Date());
		msg.setMessageStatus(MessageStatuses.SENT);
		msg.setRefNo(Integer.toString(++this.refCounter));
		msg.setGatewayId(getGatewayId());
		Logger.getInstance().logInfo("Sent to: " + msg.getRecipient() + " via: " + msg.getGatewayId(), null, getGatewayId());
		incOutboundMessageCount();
		return true;
	}

	public int getFailCycle()
	{
		return this.failCycle;
	}

	/**
	 * Set fail cycle value. This is count of successfully sent messages that is
	 * followed by one failed message.
	 * 
	 * @param myFailCycle
	 *            Set to zero to disable failures.
	 */
	public void setFailCycle(int myFailCycle)
	{
		this.failCycle = myFailCycle;
	}

	@Override
	public int getQueueSchedulingInterval()
	{
		return 200;
	}
}
