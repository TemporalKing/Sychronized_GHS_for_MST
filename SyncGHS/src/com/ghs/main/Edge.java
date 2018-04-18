package com.ghs.main;

public class Edge {

	int i;
	int j;
	int weight;
	
	public Edge(int i, int j, int weight) {
		super();
		this.i = i;
		this.j = j;
		this.weight = weight;
	}
	public int getI() {
		return i;
	}
	public void setI(int i) {
		this.i = i;
	}
	public int getJ() {
		return j;
	}
	public void setJ(int j) {
		this.j = j;
	}
	public int getWeight() {
		return weight;
	}
	public void setWeight(int weight) {
		this.weight = weight;
	}
}
