package com.ghs.main;

import java.io.Serializable;

public class Edge implements Comparable<Edge>, Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1462118527829731810L;
	int firstUID;
	int secondUID;
	int weight;

	public Edge(int firstUID, int secondUID, int weight) {
		super();
		this.firstUID = Math.min(firstUID, secondUID);
		this.secondUID = Math.max(firstUID, secondUID);
		this.weight = weight;
	}
	public int getI() {
		return firstUID;
	}
	public void setI(int i) {
		this.firstUID = i;
	}
	public int getJ() {
		return secondUID;
	}
	public void setJ(int j) {
		this.secondUID = j;
	}
	public int getWeight() {
		return weight;
	}
	public void setWeight(int weight) {
		this.weight = weight;
	}

	@Override
	public String toString() {
		return ("Edge--> " + this.firstUID + "," + this.secondUID + "," + this.weight);
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + firstUID;
		result = prime * result + secondUID;
		result = prime * result + weight;
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		Edge e = (Edge) obj;
		if(this.getWeight()==e.getWeight() && this.getI()==e.getI() && this.getJ()==e.getJ())
			return true;

		return false;
	}

	@Override
	public int compareTo(Edge e) {
		if(this.getWeight() < e.getWeight())
			return -1;
		if(this.getWeight() > e.getWeight())
			return 1;
		if(this.getWeight() == e.getWeight())
		{
			if(this.firstUID < e.firstUID)
				return -1;
			if(this.firstUID > e.firstUID)
				return 1;
			if(this.firstUID == e.firstUID)
			{
				if(this.secondUID < e.secondUID)
					return -1;
				if(this.secondUID > e.secondUID)
					return 1;
			}
		}

		return 0;
	}

	public boolean isContainsUID(int UID)
	{
		if(this.firstUID==UID || this.secondUID==UID)
			return true;
		return false;	
	}
}
