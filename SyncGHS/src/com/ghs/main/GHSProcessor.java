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

			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			processMessages();
			processMWOECandidateMsg();

			//should i sleep for some time before processing new leader message ?
			// i guess yes, to wait for all merge messages to reach me if any
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			processInformNewLeaderMessage();
			processNewLeaderMessage();
			detectAndProcessTermination();

		}

		System.out.println("Stopping GHS Processor");
		//runCleanUp();
	}

	public void detectAndProcessTermination() {

		// do we need to check message buffers for pending messages

		CopyOnWriteArrayList<Msg> terminateMsgBuffer = thisNode.getTerminateMsgBuffer();

		synchronized (terminateMsgBuffer) {

			for(Iterator<Msg> itr = terminateMsgBuffer.iterator(); itr.hasNext();)
			{
				Msg msg = itr.next();

				if(msg.getMessageType()==MessageType.TERMINATE && msg.getPhaseNumber()==thisNode.getPhaseNumber())
				{
					/*
					 * do not process termination immediately, wait for 5 rounds to make sure
					 * any delayed message is received and processed
					 */
					if(thisNode.getTerminationDelaycnt() >= 5)
					{
						Msg terminateMsg = new Msg(MessageType.TERMINATE, null, 1, thisNode.getUID(),
								thisNode.getComponentId(), thisNode.getPhaseNumber());

						sendMessageOnTreeEdges(thisNode, terminateMsg, MessageType.TERMINATE);
						thisNode.setStopClientMgr(true);
					}
					else
					{
						thisNode.setTerminationDelaycnt((thisNode.getTerminationDelaycnt()+1));
					}
				}
			}
		}

	}


	public void processNewLeaderMessage() {
		CopyOnWriteArrayList<Msg> messageBuffer = thisNode.getMsgBuffer();
		synchronized (messageBuffer) {
			for(Iterator<Msg> iterator = messageBuffer.iterator(); iterator.hasNext();)
			{
				Msg message = iterator.next();

				/*
				 * get current phase unprocessed message out of buffer
				 * process it, once processed, remove message from the buffer
				 */
				if(message.getPhaseNumber() == thisNode.getPhaseNumber() && message.getMessageType() == MessageType.NEWLEADER)
				{
					System.out.println("Okay, well lets process new leader message now ->" + message);

					/*
					 * update my leader id, phase number, reset few flags
					 * get ready for next phase
					 */
					//Msg newLeaderMSg = new Msg(MessageType.NEWLEADER, message.getEdge(), targetUID, senderUID, senderComponentId, phaseNumber)
					sendMessageOnTreeEdges(thisNode, message, MessageType.NEWLEADER);
					moveToNextPhase(thisNode, message.getEdge().getI());
					printAllTreeEdges();

				}
			}
		}

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

					case MERGE:
						processMergeMsg(message);
						messageBuffer.remove(message);
						break;

					default:
						break;
					}
				}
			}

		}
	}

	public void processMergeMsg(Msg message) {


		if(!isNodePartOfEdge(thisNode, message.getEdge())) {
			/*
			 * if this node is not on the min edge in the merge message,
			 * just relay that message to all my tree edges except sender
			 */
			System.out.println("Well, I am not a part of edge from merge msg, relaying it");
			sendMessageOnTreeEdges(thisNode,message,MessageType.MERGE);
		}
		else
		{
			/*
			 * if this node is on the edge from the merge message, need to start
			 * merge process, this will also have logic to detect the core edge
			 * and detect new leader
			 */
			if(!edgeExistInTreeEdgeList(message.getEdge(), thisNode))
			{
				/*
				 * Edge does not exist in my tree list
				 * add it and pass on merge message to other end of 
				 * the edge, that other end should detect new leader
				 */
				System.out.println("Great, Edge does not exist, adding it to my tree edge list ->" + message.getEdge());
				thisNode.getTreeEdges().add(message.getEdge());
				printAllTreeEdges();
				int targeUID = thisNode.getUID()!=message.getEdge().getI()? message.getEdge().getI():message.getEdge().getJ();

				if(message.getSenderComponentId()== thisNode.getComponentId())
				{
					Msg mergeMsg = new Msg(MessageType.MERGE, message.getEdge(), targeUID, 
							thisNode.getUID(), thisNode.getComponentId(), thisNode.getPhaseNumber());

					sendMessage(mergeMsg, targeUID);
				}
			}
			else
			{
				/*
				 * Edge from merge message already exist in my tree list
				 * damn, I found core edge, OK then, find max UID on this edge
				 * and send it you are the new leade message
				 */

				int newLeaderUID = Math.max(message.getEdge().getI(), message.getEdge().getJ());
				System.out.println("Whola, New Leader found-->" + newLeaderUID);

				Msg newLeaderInfoMsg = new Msg(MessageType.NEWLEADERINFO, new Edge(newLeaderUID, newLeaderUID, -1), newLeaderUID,
						thisNode.getUID(), thisNode.getComponentId(), thisNode.getPhaseNumber());
				sendMessage(newLeaderInfoMsg, newLeaderUID);

			}

		}
	}

	public void processInformNewLeaderMessage()
	{

		CopyOnWriteArrayList<Msg> messageBuffer = thisNode.getMsgBuffer();
		synchronized (messageBuffer) {
			for(Iterator<Msg> iterator = messageBuffer.iterator(); iterator.hasNext();)
			{
				Msg message = iterator.next();

				/*
				 * get current phase unprocessed message out of buffer
				 * process it, once processed, remove message from the buffer
				 */
				if(message.getPhaseNumber() == thisNode.getPhaseNumber() && message.getMessageType()==MessageType.NEWLEADERINFO)
				{
					/*
					 * 3n rounds dummy message transfer
					 */

					int dummyTarget = thisNode.getUID()!=thisNode.getGraphEdges().get(0).getI()?thisNode.getGraphEdges().get(0).getI():thisNode.getGraphEdges().get(0).getJ();

					for(int i=0;i< (1*thisNode.getNumberOfNodes());i++)
					{
						Msg dummyMsg = new Msg(MessageType.DUMMY, null, dummyTarget, thisNode.getUID(), -1, thisNode.getPhaseNumber());
						sendMessage(dummyMsg, dummyTarget);
					}

					if(thisNode.getNumberOfDummyReplies() >= (1*thisNode.getNumberOfNodes()))
					{
						int newLeaderUID = message.getEdge().getI();
						messageBuffer.remove(message);

						Edge leaderEdge = new Edge(newLeaderUID, newLeaderUID, 0);
						Msg leaderMsgDataHolder = new Msg(MessageType.NEWLEADER, leaderEdge, -1, -1, -1, -1);
						sendMessageOnTreeEdges(thisNode, leaderMsgDataHolder, MessageType.NEWLEADER);
						moveToNextPhase(thisNode,newLeaderUID);
					}
					else {
						System.out.println("Waiting for 3N synch to complete-- received till now " + thisNode.getNumberOfDummyReplies());
					}
				}
			}
		}

	}

	public void moveToNextPhase(Node thisNode2, int newLeaderUID) {
		/*
		 * set unmarked, phase number+1, new Leader, startMWOESearchFlag=true, bfsparentid=-1
		 */
		System.out.println("Woh woh, changing my phase, leader and other crap");

		thisNode2.setComponentId(newLeaderUID);
		if(thisNode2.getUID()!=newLeaderUID)
			thisNode2.setLeaderInd(false);
		thisNode2.setPhaseNumber((thisNode2.getPhaseNumber()+1));
		thisNode2.setMarked(false);
		if(thisNode2.getUID()==newLeaderUID)
			thisNode2.setStartMWOESearchFlag(true);
		thisNode2.setBFSParentUID(-1);

		thisNode.setSendRejectMsgEnable(true);
		printAllTreeEdges();

		//should we clear the MwoeCadidateReplyBuffer as well ???
	}

	public void sendMessageOnTreeEdges(Node thisNode2, Msg message, MessageType merge) {

		/*
		 * send merge message to all tree edges of this node except one which sent the message.
		 */
		System.out.println("Sending message on all tree edges except sender-" + merge);
		printAllTreeEdges();

		CopyOnWriteArrayList<Edge> treeEdgesList = thisNode2.getTreeEdges();
		synchronized (treeEdgesList) {
			for(Iterator<Edge> itr = treeEdgesList.iterator();itr.hasNext();)
			{
				Edge e = itr.next();
				int targetId = getTargetUID(thisNode2,e);

				if(targetId!=message.getSenderUID())
				{
					Msg msg = new Msg(merge, message.getEdge(), targetId, 
							thisNode2.getUID(), thisNode2.getComponentId(), thisNode2.getPhaseNumber());

					sendMessage(msg, targetId);
				}
			}
		}
	}

	public int getTargetUID(Node thisNode2, Edge e) {

		return (thisNode2.getUID()!=e.getI()?e.getI():e.getJ());
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
			System.out.println("Min Edge found--> " + (minEdge!=null? minEdge:"null"));

			/*
			 * if this node is not leader then either send 
			 * local min edge up to parent or if not found local min
			 * then send mwoe reject to parent
			 */
			if(!thisNode.getLeaderInd()) {
				System.out.println("Sending min edge to updward to the parent.");
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

					if(thisNode.isSendRejectMsgEnable())
					{
						thisNode.setSendRejectMsgEnable(false);
						Msg mwoeCandiate = new Msg(MessageType.MWOEREJECT, minEdge, thisNode.getBFSParentUID(), 
								thisNode.getUID(), thisNode.getComponentId(), thisNode.getPhaseNumber());

						sendMessage(mwoeCandiate, mwoeCandiate.getTargetUID());
					}

				}
			}
			else
			{
				/*
				 * if ur parent and done with finding global min
				 * if global min not null then initiate merge process
				 * else send termination message
				 */
				if(minEdge==null)
				{
					/*
					 * send termination signal along tree edges
					 *
					 */
					System.out.println("MWOE search failed, termination detected");
					Msg terminateMsg = new Msg(MessageType.TERMINATE, null, -1, thisNode.getUID(),
							thisNode.getComponentId(), thisNode.getPhaseNumber());

					sendMessageOnTreeEdges(thisNode, terminateMsg, MessageType.TERMINATE);
					thisNode.setStopClientMgr(true);
				}
				else
				{
					//System.out.println("Sending merge message on tree edges");
					/*
					 * this node is leader and found global min edge which is not
					 * connected to you,
					 * send merge message to all its tree edges
					 */
					if(thisNode.getTreeEdges().size()!=0 && !isNodePartOfEdge(thisNode, minEdge))
						sendMergeMsgOnTreeEdges(minEdge);
					else
					{
						if(!edgeExistInTreeEdgeList(minEdge, thisNode))
						{
							int targetUID = thisNode.getUID()!=minEdge.getI()?minEdge.getI():minEdge.getJ();

							System.out.println("Addint min edge in my edge list--> " + minEdge);
							thisNode.getTreeEdges().add(minEdge);

							Msg msg = new Msg(MessageType.MERGE, minEdge, targetUID, thisNode.getUID(), 
									thisNode.getComponentId(), thisNode.getPhaseNumber());
							sendMessage(msg, targetUID);
						}
						else
						{
							System.out.println("Whola, Leader found in MWOE candiate processing");
							int newLeaderUID = Math.max(minEdge.getI(), minEdge.getJ());
							Msg newLeaderInfoMsg = new Msg(MessageType.NEWLEADERINFO, new Edge(newLeaderUID, newLeaderUID, -1), newLeaderUID,
									thisNode.getUID(), thisNode.getComponentId(), thisNode.getPhaseNumber());
							sendMessage(newLeaderInfoMsg, newLeaderUID);
						}

					}
				}
			}

		}
		else
		{
			System.out.println("waiting for all responses to mwoe search messages, requiredCount= " + requiredCount 
					+ ", actual count: " + count + ", messageBuffer size: " + messageBuffer.size());
		}
	}

	public void sendMergeMsgOnTreeEdges(Edge minEdge) {
		CopyOnWriteArrayList<Edge> nodeTreeEdgeList = thisNode.getTreeEdges();
		synchronized (nodeTreeEdgeList) {
			for(Iterator<Edge> itr= nodeTreeEdgeList.iterator(); itr.hasNext();)
			{
				Edge e = (Edge) itr.next();
				int targetUID = thisNode.getUID()!=e.getI()?e.getI():e.getJ();

				Msg mergeMessage = new Msg(MessageType.MERGE, minEdge, targetUID, thisNode.getUID(), 
						thisNode.getComponentId(), thisNode.getPhaseNumber());

				sendMessage(mergeMessage, targetUID);
			}
		}

	}

	public boolean existEdgeInTreeEdgeList(Edge minEdge) {

		CopyOnWriteArrayList<Edge> nodeTreeEdgeList = thisNode.getTreeEdges();
		synchronized (nodeTreeEdgeList) {
			for(Iterator<Edge> itr= nodeTreeEdgeList.iterator(); itr.hasNext();)
			{
				Edge e = (Edge) itr.next();

				if(e.equals(minEdge))
					return true;
			}
		}
		return false;
	}

	public boolean isNodePartOfEdge(Node thisNode2, Edge minEdge) {
		if(thisNode2.getUID()==minEdge.getI() || thisNode2.getUID()==minEdge.getJ())
			return true;
		return false;
	}

	private boolean edgeExistInTreeEdgeList(Edge minEdge, Node thisNode2) {
		CopyOnWriteArrayList<Edge> nodeTreeEdgeList = thisNode2.getTreeEdges();
		synchronized (nodeTreeEdgeList) {
			for(Iterator<Edge> itr= nodeTreeEdgeList.iterator(); itr.hasNext();)
			{
				Edge e = (Edge) itr.next();
				if(e.equals(minEdge))
					return true;
			}
		}

		return false;
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
			//e.printStackTrace();
			System.out.println("Error in sending message on socket");
		}

	}

	public void printAllTreeEdges()
	{
		System.out.println("My leader is: " + thisNode.getComponentId());
		System.out.println("All my tree edges are");
		CopyOnWriteArrayList<Edge> nodeTreeEdgeList = thisNode.getTreeEdges();
		synchronized (nodeTreeEdgeList) {
			for(Iterator<Edge> itr= nodeTreeEdgeList.iterator(); itr.hasNext();)
			{
				Edge e = (Edge) itr.next();
				System.out.println(e);
			}
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
