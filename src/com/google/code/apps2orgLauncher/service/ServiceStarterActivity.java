/**
 * 
 */
package com.google.code.apps2orgLauncher.service;

import android.app.Activity;
import android.os.Bundle;

/**
 * @author fabio
 * 
 */
public class ServiceStarterActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		StartupListener.startService(this);
	}
}
