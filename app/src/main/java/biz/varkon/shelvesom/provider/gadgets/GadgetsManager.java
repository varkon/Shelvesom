/*
 * Copyright (C) 2010 Garen J. Torikian
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package biz.varkon.shelvesom.provider.gadgets;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;

import biz.varkon.shelvesom.base.BaseItem;
import biz.varkon.shelvesom.provider.gadgets.GadgetsStore.Gadget;
import biz.varkon.shelvesom.util.IOUtilities;
import biz.varkon.shelvesom.util.ImageUtilities;
import biz.varkon.shelvesom.util.ImportUtilities;
import biz.varkon.shelvesom.util.Preferences;
import biz.varkon.shelvesom.util.loan.Calendars;

public class GadgetsManager {
	static int GADGET_COVER_WIDTH;
	static int GADGET_COVER_HEIGHT;

	private static String sIdSelection;
	private static String sSelection;

	private static String[] sArguments1 = new String[1];
	private static String[] sArguments4 = new String[4];

	private static final String[] PROJECTION_ID_IID = new String[] {
			BaseItem._ID, BaseItem.INTERNAL_ID };

	private static final String[] PROJECTION_ID = new String[] { BaseItem._ID };

	static {
		StringBuilder selection = new StringBuilder();
		selection.append(BaseItem.INTERNAL_ID);
		selection.append(" LIKE ?");
		sIdSelection = selection.toString();

		selection = new StringBuilder();
		selection.append(sIdSelection);
		selection.append(" OR ");
		selection.append(BaseItem.EAN);
		selection.append(" LIKE ? OR ");
		selection.append(BaseItem.UPC);
		selection.append(" LIKE ? OR ");
		selection.append(BaseItem.ISBN);
		selection.append(" LIKE ?");
		sSelection = selection.toString();
	}

	private GadgetsManager() {
	}

	public static String findGadgetId(ContentResolver contentResolver,
			String id, String sortOrder) {
		String internalId = null;
		Cursor c = null;

		try {
			final String[] arguments4 = sArguments4;
			arguments4[0] = arguments4[1] = arguments4[2] = arguments4[3] = id;
			c = contentResolver.query(GadgetsStore.Gadget.CONTENT_URI,
					PROJECTION_ID_IID, sSelection, arguments4, sortOrder);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					internalId = c.getString(c
							.getColumnIndexOrThrow(BaseItem.INTERNAL_ID));
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return internalId;
	}

	public static boolean gadgetExists(ContentResolver contentResolver,
			String id, String sortOrder, IOUtilities.inputTypes type) {
		boolean exists;
		Cursor c = null;

		try {
			final String[] arguments4 = sArguments4;
			arguments4[0] = arguments4[1] = arguments4[2] = arguments4[3] = id
					.replaceFirst("^0+(?!$)", "");

			c = contentResolver.query(GadgetsStore.Gadget.CONTENT_URI,
					PROJECTION_ID, sSelection, arguments4, sortOrder);
			exists = c.getCount() > 0;
		} finally {
			if (c != null)
				c.close();
		}

		return exists;
	}

	public static GadgetsStore.Gadget loadAndAddGadget(
			ContentResolver resolver, String id, GadgetsStore gadgetsStore,
			IOUtilities.inputTypes mSavedImportType, Context context) {

		final GadgetsStore.Gadget gadget = gadgetsStore.findGadget(id,
				mSavedImportType, context);
		if (gadget != null) {
			Bitmap bitmap = null;

			bitmap = Preferences.getBitmapForManager(gadget);
			GADGET_COVER_WIDTH = Preferences.getWidthForManager();
			GADGET_COVER_HEIGHT = Preferences.getHeightForManager();

			if (bitmap != null) {
				bitmap = ImageUtilities.createCover(bitmap, GADGET_COVER_WIDTH,
						GADGET_COVER_HEIGHT);
				ImportUtilities.addCoverToCache(gadget.getInternalId(), bitmap);
				bitmap.recycle();
			}

			// Should kill duplicate item entry bug...
			Cursor c = null;
			sArguments1[0] = gadget.getInternalId();
			c = resolver.query(GadgetsStore.Gadget.CONTENT_URI, null,
					BaseItem.INTERNAL_ID + "='" + gadget.getInternalId() + "'",
					null, null);
			if (c.moveToFirst()) {
				if (c.getCount() < 1) {
					final Uri uri = resolver.insert(
							GadgetsStore.Gadget.CONTENT_URI,
							gadget.getContentValues());
					if (uri != null) {
						if (c != null) {
							c.close();
						}
						return gadget;
					}
				}
			} else {
				if (c != null) {
					c.close();
				}
				final Uri uri = resolver.insert(
						GadgetsStore.Gadget.CONTENT_URI,
						gadget.getContentValues());
				return gadget;
			}
		}

		return null;
	}

	public static boolean deleteGadget(ContentResolver contentResolver,
			String gadgetId) {
		Gadget gadget = GadgetsManager.findGadget(contentResolver, gadgetId,
				null);
		int eventId = 0;

		if (gadget != null) {
			eventId = gadget.getEventId();
		}

		final String[] arguments1 = sArguments1;
		arguments1[0] = gadgetId;
		int count = contentResolver.delete(GadgetsStore.Gadget.CONTENT_URI,
				sIdSelection, arguments1);
		ImageUtilities.deleteCachedCover(gadgetId);

		if (eventId > 0) {
			Calendars.deleteCalendar(contentResolver, gadget);
		}

		return count > 0;
	}

	public static GadgetsStore.Gadget findGadget(
			ContentResolver contentResolver, String id, String sortOrder) {
		GadgetsStore.Gadget gadget = null;
		Cursor c = null;

		try {
			sArguments1[0] = id;
			c = contentResolver.query(GadgetsStore.Gadget.CONTENT_URI, null,
					sIdSelection, sArguments1, sortOrder);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					gadget = GadgetsStore.Gadget.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return gadget;
	}

	public static GadgetsStore.Gadget findGadget(
			ContentResolver contentResolver, Uri data, String sortOrder) {
		GadgetsStore.Gadget gadget = null;
		Cursor c = null;

		try {
			c = contentResolver.query(data, null, null, null, null);
			if (c != null && c.getCount() > 0) {
				if (c.moveToFirst()) {
					gadget = GadgetsStore.Gadget.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return gadget;
	}

	public static GadgetsStore.Gadget findGadgetById(
			ContentResolver contentResolver, String id, String sortOrder) {
		GadgetsStore.Gadget gadget = null;
		Cursor c = null;

		try {
			final String[] arguments4 = sArguments4;
			arguments4[0] = arguments4[1] = arguments4[2] = arguments4[3] = id;

			c = contentResolver.query(GadgetsStore.Gadget.CONTENT_URI, null,
					sSelection, sArguments4, sortOrder);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					gadget = GadgetsStore.Gadget.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return gadget;
	}
}
