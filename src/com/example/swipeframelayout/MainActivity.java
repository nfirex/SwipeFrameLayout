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

		getListView().setDrawSelectorOnTop(true);
		getListView().setOnItemClickListener(null);
	}

	@Override
	public void swipeComplete(SwipeFrameLayout view, SwipeState state) {

	}




	private static class CustomAdapter extends BaseAdapter {
		private final Context mContext;
		private final LayoutInflater mInflater;
		private final ISwipeListener mListener;
		private final String[] mItems;
		private final int mOffsetRight;
		private final int mOffsetLeft;

		public CustomAdapter(Context context, ISwipeListener listener, String[] items) {
			mContext = context;
			mListener = listener;
			mItems = items;

			mInflater = LayoutInflater.from(context);
			mOffsetLeft = context.getResources().getDimensionPixelSize(R.dimen.offset_left);
			mOffsetRight = context.getResources().getDimensionPixelSize(R.dimen.offset_right);
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
				frame.setOffsets(mOffsetLeft, mOffsetRight);
				frame.setSwipeListener(mListener);
				convertView = frame;

				holder = new Holder();
				holder.text = (TextView) convertView.findViewById(R.id.item_front);
				holder.listener = new BackClickListener(mContext, frame);
				holder.back = convertView.findViewById(R.id.item_back);
				holder.back.setOnClickListener(holder.listener);
				convertView.setTag(holder);
			} else {
				frame = (SwipeFrameLayout) convertView;
				holder = (Holder) convertView.getTag();
			}

			final String item = getItem(position);
			holder.text.setText(item);
			holder.listener.setItem(item);
			frame.resetItem();

			return convertView;
		}




		private static class Holder {
			public TextView text;
			public View back;
			public BackClickListener listener;
		}

		private class BackClickListener implements OnClickListener {
			private final Context mContext;
			private final SwipeFrameLayout mFrame;

			private String mItem;

			public BackClickListener(Context context, SwipeFrameLayout frame) {
				mContext = context;
				mFrame = frame;
			}

			@Override
			public void onClick(View v) {
				Toast.makeText(mContext, mItem, Toast.LENGTH_SHORT).show();
				mFrame.resetItemWithAnimation();
			}




			public void setItem(String item) {
				mItem = item;
			}
		}
	}
}