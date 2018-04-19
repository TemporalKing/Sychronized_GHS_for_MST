package com.ghs.main;

import java.io.Serializable;

import com.ghs.main.GHSUtil.MessageType;

public class Msg implements Serializable {

	private static final long serialVersionUID = 1L;
	MessageType messageType;
	Edge edge;
	int targetUID;
	int senderUID;
	int senderComponentId;
	int phaseNumber;

	public Msg(MessageType messageType, Edge edge, int targetUID, int senderUID, 
			int senderComponentId, int phaseNumber) {
		super();
		this.messageType = messageType;
		this.edge = edge;
		this.targetUID = targetUID;
		this.senderUID = senderUID;
		this.senderComponentId = senderComponentId;
		this.phaseNumber = phaseNumber;
	}

	/**
	 * @return the messageType
	 */
	public MessageType getMessageType() {
		return messageType;
	}

	/**
	 * @param messageType the messageType to set
	 */
	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	/**
	 * @return the edge
	 */
	public Edge getEdge() {
		return edge;
	}

	/**
	 * @param edge the edge to set
	 */
	public void setEdge(Edge edge) {
		this.edge = edge;
	}

	/**
	 * @return the targetUID
	 */
	public int getTargetUID() {
		return targetUID;
	}

	/**
	 * @param targetUID the targetUID to set
	 */
	public void setTargetUID(int targetUID) {
		this.targetUID = targetUID;
	}

	/**
	 * @return the senderUID
	 */
	public int getSenderUID() {
		return senderUID;
	}

	/**
	 * @param senderUID the senderUID to set
	 */
	public void setSenderUID(int senderUID) {
		this.senderUID = senderUID;
	}

	/**
	 * @return the senderComponentId
	 */
	public int getSenderComponentId() {
		return senderComponentId;
	}

	/**
	 * @param senderComponentId the senderComponentId to set
	 */
	public void setSenderComponentId(int senderComponentId) {
		this.senderComponentId = senderComponentId;
	}

	/**
	 * @return the phaseNumber
	 */
	public int getPhaseNumber() {
		return phaseNumber;
	}

	/**
	 * @param phaseNumber the phaseNumber to set
	 */
	public void setPhaseNumber(int phaseNumber) {
		this.phaseNumber = phaseNumber;
	}

	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String msg = "Message: Type:" + this.messageType + " ,Phase: " + this.phaseNumber + ", Source:"
				+ this.senderUID + ", Source Component:" +  this.senderComponentId + ", Edge: " + this.edge
				+ ", Target UID: " + this.getTargetUID();
		return msg;
	}



}
