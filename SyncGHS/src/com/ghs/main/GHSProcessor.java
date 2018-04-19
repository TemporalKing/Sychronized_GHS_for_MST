package com.ghs.main;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ghs.main.GHSUtil.MessageType;

/*
 * Thread to handle incoming connections to node socket
 * Till termination, it listens to socket and puts all the messages
 * to thread safe collection
 */
public class GHSProcessor implements Runnable{

	/*
	 *Object of type of Node on which this clientManager thread runs 
	 */
	Node thisNode;

	public GHSProcessor(Node t) {
		super();
		this.thisNode = t;
	}

	@Override
	public void run() {

		while(!thisNode.isStopClientMgr())
		{
			if(thisNode.isStartMWOESearchFlag())
			{
				thisNode.setStartMWOESearchFlag(false);
				sendMWOESearch(thisNode.getUID());
			}

			processMessages();
			processMWOECandidateMsg();
		}

		System.out.println("Stopping client Manager");
		runCleanUp();
	}

	public void processMessages() {

		CopyOnWriteArrayList<Msg> messageBuffer = thisNode.getMsgBuffer();
		synchronized (messageBuffer) {
			for(Iterator<Msg> iterator = messageBuffer.iterator(); iterator.hasNext();)
			{
				Msg message = iterator.next();

				/*
				 * get current phase unprocessed message out of buffer
				 * process it, once processed, remove message from the buffer
				 */
				if(message.getPhaseNumber() == thisNode.getPhaseNumber())
				{
					switch (message.getMessageType()) {
					case MWOESEARCH:
						processMWOESearch(message);
						messageBuffer.remove(message);
						break;

					default:
						break;
					}
				}
			}

		}
	}

	public void processMWOECandidateMsg() {
		int count = 0;
		int requiredCount = thisNode.getLeaderInd()? thisNode.getGraphEdges().size(): thisNode.getGraphEdges().size()-1;
		CopyOnWriteArrayList<Msg> messageBuffer = thisNode.getMwoeCadidateReplyBuffer();
		synchronized (messageBuffer) {
			for(Iterator<Msg> iterator = messageBuffer.iterator(); iterator.hasNext();)
			{
				Msg message = iterator.next();

				if(message.getPhaseNumber()==thisNode.getPhaseNumber())
					count++;
			}
		}

		if(requiredCount == count)
		{
			ArrayList<Edge> tempCandiateList = new ArrayList<>();
			System.out.println("Required number of response for mwoe search message are received: " + requiredCount);
			synchronized (messageBuffer) {
				for(Iterator<Msg> iterator = messageBuffer.iterator(); iterator.hasNext();)
				{
					Msg message = iterator.next();
					if(message.getMessageType() != MessageType.MWOEREJECT)
						tempCandiateList.add(message.getEdge());
					messageBuffer.remove(message);
				}
			}

			Edge minEdge = getMinEdge(tempCandiateList);

			/*
			 * if this node is not leader then either send 
			 * local min edge up to parent or if not found local min
			 * then send mwoe reject to parent
			 */
			if(!thisNode.getLeaderInd()) {
				if(minEdge!=null)
				{
					/*
					 * if min edge found, send to temp bfs parent
					 */

					Msg mwoeCandiate = new Msg(MessageType.MWOECANDIDATE, minEdge, thisNode.getBFSParentUID(), 
							thisNode.getUID(), thisNode.getComponentId(), thisNode.getPhaseNumber());

					sendMessage(mwoeCandiate, mwoeCandiate.getTargetUID());
				}
				else
				{
					/*
					 * send mwoe reject message
					 */

					Msg mwoeCandiate = new Msg(MessageType.MWOEREJECT, minEdge, thisNode.getBFSParentUID(), 
							thisNode.getUID(), thisNode.getComponentId(), thisNode.getPhaseNumber());

					sendMessage(mwoeCandiate, mwoeCandiate.getTargetUID());
				}
			}
			else
			{
				/*
				 * if ur parent and done with finding global min
				 * if global min not null then initiate merge process
				 * else send termination message
				 */
				
			}

		}
	}

	public Edge getMinEdge(ArrayList<Edge> tempCandiateList) {

		if(tempCandiateList==null || tempCandiateList.size()==0)
			return null;

		Collections.sort(tempCandiateList);
		return tempCandiateList.get(0);
	}

	public void processMWOESearch(Msg message) {

		/*
		 * Message coming from the different component
		 * Send a MWOECandidate message
		 */
		if(message.getSenderComponentId()!= thisNode.getComponentId())
		{
			Msg mwoeCandiateMessage = new Msg(MessageType.MWOECANDIDATE, message.getEdge(), 
					message.getSenderUID(), thisNode.getUID(), 
					thisNode.getComponentId(), thisNode.getPhaseNumber());

			sendMessage(mwoeCandiateMessage, message.getSenderUID());
		}
		else
		{
			/*
			 * if MWOE search message came from the same component
			 * if you are not marked, mark , backup sender as temp parent 
			 * to send result back, propagate message to all neighbors
			 */
			if(!thisNode.isMarked())
			{
				thisNode.setMarked(true);
				thisNode.setBFSParentUID(message.getSenderUID());
				sendMWOESearch(message.getSenderUID());
			}
			else
			{
				/*
				 * if already marked, send MWOE rejection
				 */
				Msg mwoeReject = new Msg(MessageType.MWOEREJECT, message.getEdge(), message.senderUID,
						thisNode.getUID(), thisNode.getComponentId(),thisNode.getPhaseNumber());

				sendMessage(mwoeReject, mwoeReject.getTargetUID());
			}

		}

	}

	public void sendMWOESearch(int doNotSendUID)
	{
		for(Edge e: thisNode.getGraphEdges())
		{
			int targetUID = (thisNode.getUID()!=e.getI()? e.getI(): e.getJ());
			if(targetUID!=doNotSendUID)
			{
				Msg message = new Msg(GHSUtil.MessageType.MWOESEARCH, e, targetUID, thisNode.getUID(),
						thisNode.getComponentId(), thisNode.getPhaseNumber());

				sendMessage(message, targetUID);
			}	
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

	public void runCleanUp() {

		System.out.println("Closing server sockets");
		try {
			thisNode.serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
