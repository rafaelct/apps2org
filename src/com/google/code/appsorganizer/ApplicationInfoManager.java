/*
 * Copyright (C) 2009 Apps Organizer
 *
 * This file is part of Apps Organizer
 *
 * Apps Organizer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Apps Organizer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Apps Organizer.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.google.code.appsorganizer;

import java.io.ByteArrayOutputStream;
import java.util.List;

import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;

import com.google.code.appsorganizer.db.AppCacheDao;
import com.google.code.appsorganizer.db.DatabaseHelper;
import com.google.code.appsorganizer.maps.AppCacheMap;
import com.google.code.appsorganizer.model.AppCache;

public class ApplicationInfoManager {

	private ApplicationInfoManager() {
	}

	public static void reloadAll(PackageManager pm, DatabaseHelper dbHelper, Handler handler, boolean discardCache, String packageToReload) {
		AppCacheDao appCacheDao = dbHelper.appCacheDao;
		synchronized (ApplicationInfoManager.class) {
			appCacheDao.fixDuplicateApps();
			AppCacheMap nameCache = appCacheDao.queryForCacheMap(false);
			boolean[] installedApps = new boolean[nameCache.size()];
			List<ResolveInfo> installedApplications = getAllResolveInfo(pm);

			if (handler != null) {
				sendSizeMessage(handler, installedApplications.size());
			}

			for (ResolveInfo resolveInfo : installedApplications) {
				ComponentInfo a = resolveInfo.activityInfo;
				if (a.enabled) {
					String name = a.packageName + AppCacheMap.SEPARATOR + a.name;
					int appCachePosition = nameCache.getPosition(name);
					AppCache appCache = nameCache.getAt(appCachePosition);
					if (appCache != null) {
						installedApps[appCachePosition] = true;
					}
					String label = loadAppLabel(pm, a, discardCache || a.packageName.equals(packageToReload), appCacheDao, appCache);
					if (handler != null) {
						Message message = new Message();
						message.obj = label;
						handler.sendMessage(message);
					}
				}
			}

			appCacheDao.removeUninstalledApps(installedApps, nameCache);
		}
	}

	private static void sendSizeMessage(Handler handler, int size) {
		Message message = new Message();
		message.arg1 = size;
		handler.sendMessage(message);
	}

	private static String loadAppLabel(PackageManager pm, ComponentInfo a, boolean discardCache, AppCacheDao appCacheDao, AppCache loadedObj) {
		boolean changed = false;
		String label = null;
		byte[] image = null;
		if (loadedObj != null) {
			label = loadedObj.label;
			image = loadedObj.image;
			if (loadedObj.disabled) {
				changed = true;
			}
		}
		if (label == null || discardCache) {
			CharSequence l = a.loadLabel(pm);
			if (l != null) {
				label = l.toString();
				changed = true;
			}
		}
		if (image == null || discardCache) {
			Drawable drawable = a.loadIcon(pm);
			if (drawable instanceof BitmapDrawable) {
				Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				boolean compressed = bitmap.compress(CompressFormat.PNG, 100, os);
				if (!compressed) {
					bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
					os = new ByteArrayOutputStream();
					compressed = bitmap.compress(CompressFormat.PNG, 100, os);
				}
				image = os.toByteArray();
				changed = true;
			}
		}
		if (changed) {
			// if label is not in cache table
			if (loadedObj == null) {
				// retrieve and store label
				AppCache obj = new AppCache(a.packageName, a.name, label);
				obj.image = image;
				appCacheDao.insert(obj);
			} else {
				appCacheDao.updateLabel(a.packageName, a.name, label, image, false);
			}
		}
		return label;
	}

	private static List<ResolveInfo> getAllResolveInfo(PackageManager pm) {
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		return pm.queryIntentActivities(mainIntent, 0);
	}

}
