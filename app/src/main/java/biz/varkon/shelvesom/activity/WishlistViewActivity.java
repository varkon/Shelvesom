package biz.varkon.shelvesom.activity;

import java.util.ArrayList;
import java.util.HashSet;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.ads.AdRequest;
import com.google.ads.AdView;
import biz.varkon.shelvesom.R;
import biz.varkon.shelvesom.ShelvesomApplication;
import biz.varkon.shelvesom.activity.apparel.ApparelDetailsActivity;
import biz.varkon.shelvesom.activity.boardgames.BoardGameDetailsActivity;
import biz.varkon.shelvesom.activity.books.BookDetailsActivity;
import biz.varkon.shelvesom.activity.gadgets.GadgetDetailsActivity;
import biz.varkon.shelvesom.activity.movies.MovieDetailsActivity;
import biz.varkon.shelvesom.activity.music.MusicDetailsActivity;
import biz.varkon.shelvesom.activity.software.SoftwareDetailsActivity;
import biz.varkon.shelvesom.activity.tools.ToolDetailsActivity;
import biz.varkon.shelvesom.activity.toys.ToyDetailsActivity;
import biz.varkon.shelvesom.activity.videogames.VideoGameDetailsActivity;
import biz.varkon.shelvesom.base.BaseItem;
import biz.varkon.shelvesom.drawable.FastBitmapDrawable;
import biz.varkon.shelvesom.provider.apparel.ApparelManager;
import biz.varkon.shelvesom.provider.apparel.ApparelStore;
import biz.varkon.shelvesom.provider.apparel.ApparelStore.Apparel;
import biz.varkon.shelvesom.provider.boardgames.BoardGamesManager;
import biz.varkon.shelvesom.provider.boardgames.BoardGamesStore;
import biz.varkon.shelvesom.provider.boardgames.BoardGamesStore.BoardGame;
import biz.varkon.shelvesom.provider.books.BooksManager;
import biz.varkon.shelvesom.provider.books.BooksStore;
import biz.varkon.shelvesom.provider.books.BooksStore.Book;
import biz.varkon.shelvesom.provider.gadgets.GadgetsManager;
import biz.varkon.shelvesom.provider.gadgets.GadgetsStore;
import biz.varkon.shelvesom.provider.gadgets.GadgetsStore.Gadget;
import biz.varkon.shelvesom.provider.movies.MoviesManager;
import biz.varkon.shelvesom.provider.movies.MoviesStore;
import biz.varkon.shelvesom.provider.movies.MoviesStore.Movie;
import biz.varkon.shelvesom.provider.music.MusicManager;
import biz.varkon.shelvesom.provider.music.MusicStore;
import biz.varkon.shelvesom.provider.music.MusicStore.Music;
import biz.varkon.shelvesom.provider.software.SoftwareManager;
import biz.varkon.shelvesom.provider.software.SoftwareStore;
import biz.varkon.shelvesom.provider.software.SoftwareStore.Software;
import biz.varkon.shelvesom.provider.tools.ToolsManager;
import biz.varkon.shelvesom.provider.tools.ToolsStore;
import biz.varkon.shelvesom.provider.tools.ToolsStore.Tool;
import biz.varkon.shelvesom.provider.toys.ToysManager;
import biz.varkon.shelvesom.provider.toys.ToysStore;
import biz.varkon.shelvesom.provider.toys.ToysStore.Toy;
import biz.varkon.shelvesom.provider.videogames.VideoGamesManager;
import biz.varkon.shelvesom.provider.videogames.VideoGamesStore;
import biz.varkon.shelvesom.provider.videogames.VideoGamesStore.VideoGame;
import biz.varkon.shelvesom.util.AnalyticsUtils;
import biz.varkon.shelvesom.util.ImageUtilities;
import biz.varkon.shelvesom.util.UIUtilities;

public class WishlistViewActivity extends ListActivity {

	private final String LOG_TAG = "WishlistViewActivity";

	private ArrayList<Wishlist> m_wishlist = null;
	private WishlistAdapter m_adapter;
	private final int WISHLIST_VIEW_WAITING_DIALOG = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AnalyticsUtils.getInstance(this).trackPageView("/" + LOG_TAG);
		setContentView(R.layout.wishlist_item_view);
		setupViews();
	}

	@Override
	protected void onResume() {
		super.onResume();

		setupViews();

		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(final AdapterView<?> parent,
					final View view, final int position, long time) {
				Wishlist w = m_adapter.items.get(position);
				viewWishlistItem(w.wishlistItemType, w.wishlistItemId);

			}
		});
	}

	void setupViews() {
		AdView adView = (AdView) findViewById(R.id.adview);
		if (!UIUtilities.isPaid(getContentResolver(), this)) {
			adView.setVisibility(View.VISIBLE);
			adView.loadAd(new AdRequest());
		} else {
			adView.setVisibility(View.GONE);
		}
		new retrieveWishlistsTask().execute();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		final ProgressDialog progressDialog = new ProgressDialog(
				WishlistViewActivity.this);
		switch (id) {
		case WISHLIST_VIEW_WAITING_DIALOG:
			progressDialog.setTitle(getString(R.string.progress_dialog_wait));
			progressDialog.setIcon(android.R.drawable.ic_dialog_alert);
			progressDialog
					.setMessage(getString(R.string.wishlist_retrieving_dialog));
		}

		return progressDialog;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		getMenuInflater().inflate(R.menu.wishlist_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		Wishlist w = m_adapter.items.get(info.position);

		switch (item.getItemId()) {
		case R.id.context_menu_item_wishlist_view:
			viewWishlistItem(w.wishlistItemType, w.wishlistItemId);
			return true;
		case R.id.context_menu_item_wishlist_return:
			return removeWishlistItem(w.wishlistItemType, w.wishlistItemId);

		}

		return super.onContextItemSelected(item);
	}

	private class retrieveWishlistsTask extends AsyncTask<Void, Void, Object> {

		private int wishlistCount;

		@Override
		protected void onPreExecute() {
			wishlistCount = 0;

			showDialog(WISHLIST_VIEW_WAITING_DIALOG);
		}

		@Override
		public Object doInBackground(Void... params) {
			ArrayList<Wishlist> wishlists = new ArrayList<Wishlist>();
			getWishlists(wishlists);
			m_adapter = new WishlistAdapter(getBaseContext(),
					R.layout.wishlist_item_rows, wishlists);

			return null;
		}

		private void getWishlists(ArrayList<Wishlist> m_wishlists) {
			Cursor c = null;
			ContentResolver contentResolver = getContentResolver();

			HashSet<Uri> uris = new HashSet<Uri>(
					ShelvesomApplication.TYPES_TO_URI.values());

			try {
				for (Uri uri : uris) {
					c = contentResolver.query(uri, new String[] {
							BaseItem.INTERNAL_ID, BaseItem.TITLE,
							BaseItem.WISHLIST_DATE }, BaseItem.WISHLIST_DATE
							+ " NOT NULL AND " + BaseItem.WISHLIST_DATE
							+ " != ''", null, null);

					if (c.moveToFirst()) {
						do {
							Wishlist w = new Wishlist();
							w.wishlistItemType = uri.toString();
							w.wishlistItemId = c.getString(0);
							w.wishlistName = c.getString(1);
							w.wishlistDate = c.getString(2);
							w.wishlistCover = ImageUtilities.getCachedCover(
									c.getString(0),
									new FastBitmapDrawable(BitmapFactory
											.decodeResource(getResources(),
													R.drawable.unknown_cover)));
							m_wishlists.add(w);
							wishlistCount++;
						} while (c.moveToNext());
					}

					if (c != null) {
						c.close();
					}
				}

			} catch (Exception e) {
				Log.e(LOG_TAG, e.getMessage());
			}
		}

		@Override
		protected void onPostExecute(Object result) {
			try {
				dismissDialog(WISHLIST_VIEW_WAITING_DIALOG);
			} catch (IllegalArgumentException iae) {
				Log.e(LOG_TAG, "Dialog never shown: " + iae);
			}

			setListAdapter(m_adapter);
			registerForContextMenu(getListView());

			TabSelector.changeActionBarTitle(
					getString(R.string.application_name), wishlistCount);
		}
	}

	private class WishlistAdapter extends ArrayAdapter<Wishlist> {
		private ArrayList<Wishlist> items;

		public WishlistAdapter(Context context, int textViewResourceId,
				ArrayList<Wishlist> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater li = getLayoutInflater();
				convertView = li.inflate(R.layout.wishlist_item_rows, null);
			}
			Wishlist o = items.get(position);
			if (o != null) {
				TextView wishlist_title = (TextView) convertView
						.findViewById(R.id.wishlist_title);
				TextView wishlist_date = (TextView) convertView
						.findViewById(R.id.wishlist_date);
				ImageView wishlist_icon = (ImageView) convertView
						.findViewById(R.id.wishlist_icon);

				wishlist_title.setText(o.wishlistName);
				wishlist_date.setText(o.wishlistDate);
				wishlist_icon.setImageDrawable(o.wishlistCover);

			}

			return convertView;
		}

	}

	private void viewWishlistItem(String mWishlistItemType, String mWishlistId) {
		if (mWishlistItemType.contains("apparel"))
			ApparelDetailsActivity.showFromOutside(getBaseContext(),
					mWishlistId);
		else if (mWishlistItemType.contains("boardgames"))
			BoardGameDetailsActivity.showFromOutside(getBaseContext(),
					mWishlistId);
		else if (mWishlistItemType.contains("books"))
			BookDetailsActivity.showFromOutside(getBaseContext(), mWishlistId);
		else if (mWishlistItemType.contains("gadgets"))
			GadgetDetailsActivity
					.showFromOutside(getBaseContext(), mWishlistId);
		else if (mWishlistItemType.contains("movies"))
			MovieDetailsActivity.showFromOutside(getBaseContext(), mWishlistId);
		else if (mWishlistItemType.contains("music"))
			MusicDetailsActivity.showFromOutside(getBaseContext(), mWishlistId);
		else if (mWishlistItemType.contains("software"))
			SoftwareDetailsActivity.showFromOutside(getBaseContext(),
					mWishlistId);
		else if (mWishlistItemType.contains("tools"))
			ToolDetailsActivity.showFromOutside(getBaseContext(), mWishlistId);
		else if (mWishlistItemType.contains("toys"))
			ToyDetailsActivity.showFromOutside(getBaseContext(), mWishlistId);
		else if (mWishlistItemType.contains("videogames"))
			VideoGameDetailsActivity.showFromOutside(getBaseContext(),
					mWishlistId);
	}

	private boolean removeWishlistItem(String mWishlistItemType,
			String mWishlistId) {
		ContentResolver cr = getContentResolver();
		ContentValues wishlistValues = new ContentValues();

		if (mWishlistItemType.contains("apparel")) {
			Apparel apparel = ApparelManager.findApparel(getContentResolver(),
					mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(ApparelStore.Apparel.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });
		} else if (mWishlistItemType.contains("boardgames")) {
			BoardGame boardgame = BoardGamesManager.findBoardGame(
					getContentResolver(), mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(BoardGamesStore.BoardGame.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });

		} else if (mWishlistItemType.contains("books")) {
			Book book = BooksManager.findBook(getContentResolver(),
					mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(BooksStore.Book.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });

		} else if (mWishlistItemType.contains("gadgets")) {
			Gadget gadget = GadgetsManager.findGadget(getContentResolver(),
					mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(GadgetsStore.Gadget.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });
		}

		else if (mWishlistItemType.contains("movies")) {
			Movie movie = MoviesManager.findMovie(getContentResolver(),
					mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(MoviesStore.Movie.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });
		} else if (mWishlistItemType.contains("music")) {
			Music music = MusicManager.findMusic(getContentResolver(),
					mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(MusicStore.Music.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });

		} else if (mWishlistItemType.contains("software")) {
			Software software = SoftwareManager.findSoftware(
					getContentResolver(), mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(SoftwareStore.Software.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });

		} else if (mWishlistItemType.contains("tools")) {
			Tool tool = ToolsManager.findTool(getContentResolver(),
					mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(ToolsStore.Tool.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });

		} else if (mWishlistItemType.contains("toys")) {
			Toy toy = ToysManager.findToy(getContentResolver(), mWishlistId,
					null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(ToysStore.Toy.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });

		} else if (mWishlistItemType.contains("videogames")) {
			VideoGame videogame = VideoGamesManager.findVideoGame(
					getContentResolver(), mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(VideoGamesStore.VideoGame.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });
		}

		setupViews();
		UIUtilities.showToast(getBaseContext(), R.string.wishlist_removed_item);

		return true;
	}

	class Wishlist {
		String wishlistItemId;
		String wishlistName;
		String wishlistDate;
		String wishlistItemType;
		Drawable wishlistCover;
	}
}