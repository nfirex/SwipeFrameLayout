package com.example.swipeframelayout;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wagado.widget.SwipeFrameLayout;
import com.wagado.widget.SwipeFrameLayout.ISwipeListener;
import com.wagado.widget.SwipeFrameLayout.SwipeState;

public class MainActivity extends ListActivity implements ISwipeListener {
	private static final int ITEM_COUNT = 100;

	private static final String ITEM_PATTERN = "Item - %s";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final String[] items = new String[ITEM_COUNT];
		for (int i = 0; i < items.length; i ++) {
			items[i] = String.format(ITEM_PATTERN, i);
		}

		final ListAdapter adapter = new CustomAdapter(getApplicationContext(), this, items);
		setListAdapter(adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Toast.makeText(getApplicationContext(), l.getItemAtPosition(position).toString(), Toast.LENGTH_LONG).show();
	}

	@Override
	public void swipeComplete(SwipeFrameLayout view, View child, SwipeState state) {

	}




	private static class CustomAdapter extends BaseAdapter {
		private final Context mContext;
		private final LayoutInflater mInflater;
		private final ISwipeListener mListener;
		private final String[] mItems;

		public CustomAdapter(Context context, ISwipeListener listener, String[] items) {
			mContext = context;
			mListener = listener;
			mItems = items;

			mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return mItems.length;
		}

		@Override
		public String getItem(int position) {
			return mItems[position];
		}

		@Override
		public long getItemId(int position) {
			return -1;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			final SwipeFrameLayout frame;
			final Holder holder;
			if (convertView == null) {
				frame = (SwipeFrameLayout) mInflater.inflate(R.layout.item, parent, false);
				frame.setSwipeListener(mListener);
				convertView = frame;

				holder = new Holder();
				holder.text = (TextView) convertView.findViewById(R.id.item_front);

				holder.redListener = new BackClickListener(mContext, frame, BackClickListener.COLOR_RED);
				convertView.findViewById(R.id.item_back_red).setOnClickListener(holder.redListener);

				holder.greenListener = new BackClickListener(mContext, frame, BackClickListener.COLOR_GREEN);
				convertView.findViewById(R.id.item_back_green).setOnClickListener(holder.greenListener);

				holder.blueListener = new BackClickListener(mContext, frame, BackClickListener.COLOR_BLUE);
				convertView.findViewById(R.id.item_back_blue).setOnClickListener(holder.blueListener);

				convertView.setTag(holder);
			} else {
				frame = (SwipeFrameLayout) convertView;
				holder = (Holder) convertView.getTag();
			}

			final String item = getItem(position);
			holder.text.setText(item);
			holder.redListener.setItem(item);
			holder.greenListener.setItem(item);
			holder.blueListener.setItem(item);
			frame.resetItems();

			return convertView;
		}




		private static class Holder {
			public TextView text;
			public BackClickListener redListener;
			public BackClickListener greenListener;
			public BackClickListener blueListener;
		}

		private class BackClickListener implements OnClickListener {
			private static final String TOAST_PATTERN = "(%s) %s";
			private static final String COLOR_RED = "red";
			private static final String COLOR_GREEN = "green";
			private static final String COLOR_BLUE = "blue";

			private final Context mContext;
			private final SwipeFrameLayout mFrame;
			private final String mColorName;

			private String mItem;

			public BackClickListener(Context context, SwipeFrameLayout frame, String colorName) {
				mContext = context;
				mFrame = frame;
				mColorName = colorName;
			}

			@Override
			public void onClick(View v) {
				Toast.makeText(mContext, String.format(TOAST_PATTERN, mColorName, mItem), Toast.LENGTH_SHORT).show();
				mFrame.resetItemsWithAnimation();
			}




			public void setItem(String item) {
				mItem = item;
			}
		}
	}
}