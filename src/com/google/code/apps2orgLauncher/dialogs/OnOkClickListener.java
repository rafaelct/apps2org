package com.google.code.apps2orgLauncher.dialogs;

import java.io.Serializable;

import android.content.DialogInterface;

public interface OnOkClickListener extends Serializable {

	void onClick(CharSequence charSequence, DialogInterface dialog, int which);
}
