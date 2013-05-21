package com.fsck.k9;

public class NotificationQueue {
	private String mMediaToPlay;
	
	private String mPhraseToSay;
	
	private int mAccountNumber;
	
	public enum State {
		eNone, eMedia, eSpeech
	}
	
	private State mState;
	
	public NotificationQueue() {
		mMediaToPlay = null;
		mPhraseToSay = null;
		mState = State.eNone;
		mAccountNumber = 0;
	}
	
	public NotificationQueue(String inMediaPlayer, String inPhraseToSay, State inState, int inAccountNumber) {
		SetMediaToPlay(inMediaPlayer);
		SetPhraseToSay(inPhraseToSay);
		SetState(inState);
		SetAccountNumber(inAccountNumber);
	}
	
	public void SetAccountNumber(int inAccountNumber) {
		mAccountNumber = inAccountNumber;
	}
	
	public int GetAccountNumber() {
		return mAccountNumber;
	}
	
	public void SetState(State inState) {
		mState = inState;
	}
	
	public void SetMediaToPlay(String inMp) {
		mMediaToPlay = inMp;
	}
	
	public void SetPhraseToSay(String inPhraseToSay) {
		mPhraseToSay = inPhraseToSay;
	}
	
	public String GetMediaToPlay() {
		return mMediaToPlay;
	}
	
	public String GetPhraseToSay() {
		return mPhraseToSay;
	}
	
	public State GetState() {
		return mState;
	}
	
}