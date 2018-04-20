package com.ghs.main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
				System.out.println("Received " + msg.getMessageType() + " message:" + msg);

				if(msg.messageType==MessageType.DUMMY && msg.getSenderUID()!=-1)
				{
					Msg message = new Msg(MessageType.DUMMY, null, msg.getSenderUID(), -1, -1, thisNode.getPhaseNumber());
					sendMessage(message, message.getTargetUID());
				}
				if(msg.getSenderUID()==-1)
					thisNode.setNumberOfDummyReplies((thisNode.getNumberOfDummyReplies()+1));

				if(msg.messageType!=MessageType.DUMMY)
				{
					if(msg.getMessageType() == MessageType.MWOEREJECT || msg.getMessageType() == MessageType.MWOECANDIDATE)
						thisNode.getMwoeCadidateReplyBuffer().add(msg);
					else
						thisNode.getMsgBuffer().add(msg);	

				}
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

	public void sendMessage(Msg message, int targetUID)
	{
		Node targetNode  = Node.getConfigMap().get(targetUID);
		try {
			Socket socket = new Socket(targetNode.getHost(), targetNode.getPort());
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			System.out.println("Sending message: " + message);
			out.writeObject(message);
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
