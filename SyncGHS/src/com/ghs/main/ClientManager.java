package com.ghs.main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import com.ghs.main.GHSUtil.MessageType;

/*
 * Thread to handle incoming connections to node socket
 * Till termination, it listens to socket and puts all the messages
 * to thread safe collection
 */
public class ClientManager implements Runnable{

	/*
	 *Object of type of Node on which this clientManager thread runs 
	 */
	Node thisNode;

	public ClientManager(Node t) {
		super();
		this.thisNode = t;
	}

	@Override
	public void run() {

		while(!thisNode.isStopClientMgr())
		{
			ObjectInputStream in = null;

			try {
				Socket s = thisNode.getServerSocket().accept();
				in = new ObjectInputStream(s.getInputStream());
				Msg msg = (Msg)in.readObject();
				System.out.println("Received message:" + msg);
				if(msg.getMessageType() == MessageType.MWOECANDIDATE || msg.getMessageType() == MessageType.MWOECANDIDATE)
					thisNode.getMwoeCadidateReplyBuffer().add(msg);
				else
					thisNode.getMsgBuffer().add(msg);	

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

		}

		System.out.println("Stopping client Manager");
		runCleanUp();
	}

	public void runCleanUp() {

		System.out.println("Closing server sockets");
		try {
			thisNode.serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
