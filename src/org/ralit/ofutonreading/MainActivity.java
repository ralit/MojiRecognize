package org.ralit.ofutonreading;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;

public class MainActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MojiRecognize moji = new MojiRecognize(Environment.getExternalStorageDirectory().getAbsolutePath() + "/imagemove/imagemove.jpg", this);
		moji.start();
	}
}
