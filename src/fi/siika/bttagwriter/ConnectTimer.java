/**
 * ConnectTimer.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter;

import android.os.CountDownTimer;

/**
 * Polling of connecting if signals are lost. When code is done correctly I
 * should be able to remove this class.
 */
public class ConnectTimer extends CountDownTimer {
	
	public interface Listener {
		public void timerTick(int secondsLeft);
	}
	
	private Listener mListener = null;

	/**
	 * 
	 */
	public ConnectTimer(Listener listener) {
		super(10000, 1000);
		 mListener = listener;
	}

	/* (non-Javadoc)
	 * @see android.os.CountDownTimer#onTick(long)
	 */
	@Override
	public void onTick(long millisUntilFinished) {
		if (mListener != null) {
			mListener.timerTick((int)(millisUntilFinished / 1000));
		}
	}

	/* (non-Javadoc)
	 * @see android.os.CountDownTimer#onFinish()
	 */
	@Override
	public void onFinish() {
		if (mListener != null) {
			mListener.timerTick(0);
		}
	}
}
