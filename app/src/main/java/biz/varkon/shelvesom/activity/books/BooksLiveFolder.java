/*
 * Copyright (C) 2009 The Android Open Source Project
 * Copyright (C) 2010 The Android Open Source Project
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

package biz.varkon.shelvesom.activity.books;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.LiveFolders;

import biz.varkon.shelvesom.R;
import biz.varkon.shelvesom.provider.books.BooksStore;

public class BooksLiveFolder extends Activity {
	public static final Uri CONTENT_URI = Uri
			.parse("content://shelves/live_folders/books");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		final String action = intent.getAction();

		if (LiveFolders.ACTION_CREATE_LIVE_FOLDER.equals(action)) {
			setResult(
					RESULT_OK,
					createLiveFolder(this, CONTENT_URI,
							getString(R.string.live_folder_books),
							R.drawable.ic_livefolder_book_icon));
		} else {
			setResult(RESULT_CANCELED);
		}

		finish();
	}

	private static Intent createLiveFolder(Context context, Uri uri,
			String name, int icon) {

		final Intent intent = new Intent();

		intent.setData(uri);

		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_NAME, name);
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_ICON,
				Intent.ShortcutIconResource.fromContext(context, icon));
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE,
				LiveFolders.DISPLAY_MODE_LIST);
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_BASE_INTENT, new Intent(
				Intent.ACTION_VIEW, BooksStore.Book.CONTENT_URI));

		return intent;
	}
}
