package org.ralit.ofutonreading;

import android.util.Log;

class Word {
	private int left;
	private int top;
	private int right;
	private int bottom;
	private String text;
	private int score;
	
	public void setPoint(int left, int top, int right, int bottom) {
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
	}
	
	public int getLeft() {
		return left;
	}
	public int getTop() {
		return top;
	}
	public int getRight() {
		return right;
	}
	public int getBottom() {
		return bottom;
	}
	public int getArea() {
		return (right-left)*(bottom-top);
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public String getText() {
		return this.text;
	}
	
	public void setScore(int score) {
		this.score = score;
	}
	
	public int getScore() {
		return this.score;
	}
	
	private void log(String log) {
		Log.i("ralit", log);
	}
}
