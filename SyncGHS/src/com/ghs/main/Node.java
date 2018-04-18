package com.ghs.main;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 * A class to store node specific data
 */
public class Node {

	int UID;
	int leaderUID;
	ArrayList<Edge> graphEdges;
	ArrayList<Edge> treeEdges;
	Boolean leaderInd;

	String host;
	int port;
	ServerSocket serverSocket;

	boolean stopClientMgr;
	public synchronized boolean isStopClientMgr() {
		return stopClientMgr;
	}

	public void setStopClientMgr(boolean stopClientMgr) {
		this.stopClientMgr = stopClientMgr;
	}

	//map of configuration file with UID as a key
	static HashMap<Integer, Node> configMap;

	/*
	 * Thread safe buffer of messages received till now
	 */
	CopyOnWriteArrayList<Msg> msgBuffer;

	public int getUID() {
		return UID;
	}

	public void setUID(int uID) {
		UID = uID;
	}

	public int getLeaderUID() {
		return leaderUID;
	}

	public void setLeaderUID(int leaderUID) {
		this.leaderUID = leaderUID;
	}

	public ArrayList<Edge> getGraphEdges() {
		return graphEdges;
	}

	public void setGraphEdges(ArrayList<Edge> graphEdges) {
		this.graphEdges = graphEdges;
	}

	public ArrayList<Edge> getTreeEdges() {
		return treeEdges;
	}

	public void setTreeEdges(ArrayList<Edge> treeEdges) {
		this.treeEdges = treeEdges;
	}

	public Boolean getLeaderInd() {
		return leaderInd;
	}

	public void setLeaderInd(Boolean leaderInd) {
		this.leaderInd = leaderInd;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public ServerSocket getServerSocket() {
		return serverSocket;
	}

	public void setServerSocket(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	public static HashMap<Integer, Node> getConfigMap() {
		return configMap;
	}

	public static void setConfigMap(HashMap<Integer, Node> configMap) {
		Node.configMap = configMap;
	}

	public CopyOnWriteArrayList<Msg> getMsgBuffer() {
		return msgBuffer;
	}

	public void setMsgBuffer(CopyOnWriteArrayList<Msg> msgBuffer) {
		this.msgBuffer = msgBuffer;
	}

	Node(int id)
	{
		this.UID = id;
		this.msgBuffer = new CopyOnWriteArrayList<>();
	}

	public static void main(String[] args) throws IOException {

		int UID = Integer.parseInt(args[0]);

		System.out.println("Starting execution for the UID:" + UID);

		String configFile = args[1];
		int numberOfNode = 0;
		Scanner sc = new Scanner(new File(configFile));
		HashMap<Integer, Node> confMap = new HashMap<>();

		boolean nodeParsingDone = false;
		int count=0;
		ArrayList<Edge> edgeList = new ArrayList<>();
		while(sc.hasNextLine())
		{
			String line = sc.nextLine().trim();
			if(line.startsWith("# Number of nodes"))
			{
				numberOfNode = Integer.parseInt(sc.nextLine());
			}

			if(numberOfNode > 0 && !nodeParsingDone && !line.startsWith("#"))
			{
				String[] nodeData = line.split("\\s+");

				int tempUID = Integer.parseInt(nodeData[0]);	
				Node tempNode = new Node(tempUID);
				tempNode.setHost(nodeData[1]);
				tempNode.setPort(Integer.parseInt(nodeData[2]));
				confMap.put(tempUID, tempNode);
				count++;

				if(numberOfNode==count)
				{
					line = sc.nextLine().trim();
					nodeParsingDone=true;
				}
						
			}
			
			if(nodeParsingDone && !line.startsWith("#"))
			{
				String[] nodeData = line.split("\\s+");
				String[] edge = nodeData[0].substring(1, nodeData[0].length()-1).split(",");
				
				Edge edg = new Edge(Integer.parseInt(edge[0]), 
						Integer.parseInt(edge[1]), Integer.parseInt(nodeData[1]));
				
				edgeList.add(edg);	
			}
		}

		Node.setConfigMap(confMap);
		Node thisNode = Node.getConfigMap().get(UID);
		thisNode.setTreeEdges(edgeList);

		ServerSocket socket = new ServerSocket(thisNode.getPort(), numberOfNode);

		thisNode.setServerSocket(socket);

		System.out.println("Config file read, Socket created");

		/*
		 * Start client manager process which will keep accepting incoming connections to socket
		 * read messages and store them into thread safe buffer
		 */
		ClientManager ct = new ClientManager(thisNode);
		Thread t = new Thread(ct);
		t.start();

		
		sc.close();
	}

}
