/**
 * ConnectTimer.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter;

import android.os.CountDownTimer;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Polling of connecting if signals are lost. When code is done correctly I
 * should be able to remove this class.
 */
public class ConnectTimer extends CountDownTimer {
	
	public interface Listener {
		public void timerTick(int secondsLeft);
	}
	
	private final Collection<Listener> mListeners = new ArrayList<Listener>();

	/**
	 * 
	 */
	public ConnectTimer(Listener listener) {
		super(10000, 1000);
		if (listener != null) {
			mListeners.add(listener);
		}
	}
	
	public void addListener (Listener listener) {
		mListeners.add(listener);
	}
	
	public void removeListener (Listener listener) {
		mListeners.remove(listener);
	}

	/* (non-Javadoc)
	 * @see android.os.CountDownTimer#onTick(long)
	 */
	@Override
	public void onTick(long millisUntilFinished) {
		for (Listener listener : mListeners) {
			listener.timerTick((int)(millisUntilFinished / 1000));
		}
	}

	/* (non-Javadoc)
	 * @see android.os.CountDownTimer#onFinish()
	 */
	@Override
	public void onFinish() {
		for (Listener listener : mListeners) {
			listener.timerTick(0);
		}
	}
}
