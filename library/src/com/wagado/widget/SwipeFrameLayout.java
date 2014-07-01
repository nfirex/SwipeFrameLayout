package com.wagado.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.wagado.R;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;

public class SwipeFrameLayout extends FrameLayout {
	public static final String TAG = "widget.SwipeFrameLayout";


	public static enum SwipeState {
		COMMON, LEFT, RIGHT, MOVE_LEFT, MOVE_RIGHT;
	}

	public static interface ISwipeListener {
		public void swipeComplete(SwipeFrameLayout parent, View child, SwipeState state);
	}

	public static final int MINIMAL_OFFSET = 0;
	public static final long ANIMATION_DURATION = 200;

	private ISwipeListener mSwipeListener;

	private SwipeState mState;
	private int mSlop;
	private int mMinFlingVelocity;
	private int mMaxFlingVelocity;
	private long mMoveAnimationDuration;
	private float mDownX;
	private boolean mSwiping;
	private float mTranslationX;
	private VelocityTracker mVelocityTracker;

	private View[] mViews;
	private View mInterceptedView;

	public SwipeFrameLayout(Context context) {
		super(context);

		initialize(context, null, 0);
	}

	public SwipeFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		initialize(context, attrs, 0);
	}

	public SwipeFrameLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		initialize(context, attrs, defStyle);
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams();
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}

	@Override
	protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new LayoutParams(p);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		if (getChildCount() > 0) {
			mViews = new View[getChildCount()];
			for (int i = 0; i < mViews.length; i ++) {
				mViews[i] = getChildAt(i);
			}
		} else {
			mViews = null;
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		return mInterceptedView != null;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				if (handleDownEvent(event)) {
					return true;
				}
				break;

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				if (handleUpEvent(mInterceptedView, event)) {
					return true;
				}
				break;

			case MotionEvent.ACTION_MOVE:
				if (handleMoveEvent(mInterceptedView, event)) {
					return true;
				}
				break;

			default:
				break;
		}

		return onTouchEvent(event);
	}




	public void resetItems() {
		if (mViews == null) {
			return;
		}

		for (View view: mViews) {
			resetItem(view);
		}
	}

	public void resetItem(int index) {
		resetItem(getChildAt(index));
	}

	public void resetItem(View child) {
		if (child == null) {
			return;
		}

		if (ViewHelper.getTranslationX(child) != MINIMAL_OFFSET) {
			ViewHelper.setTranslationX(child, MINIMAL_OFFSET);
			mState = SwipeState.COMMON;
			onSwipeComplete(child, mState);
		}
	}

	public void resetItemsWithAnimation() {
		if (mViews == null) {
			return;
		}

		for (int i = 0; i < getChildCount(); i ++) {
			resetItemWithAnimation(i);
		}
	}

	public void resetItemWithAnimation(int index) {
		resetItemWithAnimation(getChildAt(index));
	}

	public void resetItemWithAnimation(final View child) {
		if (child == null) {
			return;
		}

		if (ViewHelper.getTranslationX(child) != MINIMAL_OFFSET) {
			ViewPropertyAnimator.animate(child).translationX(MINIMAL_OFFSET).setDuration(ANIMATION_DURATION).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mState = SwipeState.COMMON;
					onSwipeComplete(child, mState);
				}
			});
		}
	}

	public void setSwipeListener(ISwipeListener listener) {
		mSwipeListener = listener;
	}

	public void setMoveAnimationTime(long moveAnimationDuration) {
		mMoveAnimationDuration = moveAnimationDuration;
	}

	protected void initialize(Context context, AttributeSet attrs, int defStyle) {
		mState = SwipeState.COMMON;

		final ViewConfiguration vc = ViewConfiguration.get(context);
		mSlop = vc.getScaledTouchSlop();
		mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
		mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();

		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwipeFrameLayout_Layout, defStyle, 0);
		setMoveAnimationTime(a.getInt(R.styleable.SwipeFrameLayout_Layout_moveAnimationDuration, context.getResources().getInteger(R.integer.moveAnimationDurationDefault)));
		a.recycle();
	}

	protected void onSwipeComplete(View child, SwipeState state) {
		if (mSwipeListener != null) {
			mSwipeListener.swipeComplete(this, child, state);
		}
	}

	protected View getInterceptedView(MotionEvent event) {
		if (mViews != null) {
			for (int i = mViews.length - 2; i >= 0; i--) {
				final View front = mViews[i + 1];
				final float frontOffsetX = ViewHelper.getTranslationX(front);

				if (event.getX() < frontOffsetX || event.getX() > front.getMeasuredWidth() + frontOffsetX) {
					final View back = mViews[i];
					final float backOffsetX = ViewHelper.getTranslationX(back);

					if (event.getX() > backOffsetX && event.getX() < back.getMeasuredWidth() + backOffsetX) {
						return back;
					}
				} else {
					return front;
				}
			}
		}

		return null;
	}

	protected boolean handleDownEvent(MotionEvent event) {
		mInterceptedView = getInterceptedView(event);
		if (mInterceptedView == null) {
			return false;
		}
		event.offsetLocation(mTranslationX, 0);

		mDownX = event.getRawX() - ViewHelper.getTranslationX(mInterceptedView);
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(event);
		mInterceptedView.onTouchEvent(event);

		return true;
	}

	protected boolean handleMoveEvent(final View view, MotionEvent event) {
		if (mVelocityTracker == null || view == null) {
			return false;
		}
		event.offsetLocation(mTranslationX, 0);
		final LayoutParams params = (LayoutParams) view.getLayoutParams();

		mVelocityTracker.addMovement(event);
		float deltaX = event.getRawX() - mDownX;
		if (Math.abs(deltaX) > mSlop) {
			mSwiping = true;
			view.getParent().requestDisallowInterceptTouchEvent(true);

			// Cancel listview's touch
			final MotionEvent cancelEvent = MotionEvent.obtain(event);
			cancelEvent.setAction(MotionEvent.ACTION_CANCEL | (event.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
			view.onTouchEvent(cancelEvent);

			cancelEvent.recycle();
		}

		if (mSwiping) {
			if (deltaX > 0) {
				mTranslationX = Math.min(deltaX, params.offsetOpenLeft);

				if (mTranslationX == params.offsetOpenLeft) {
					mState = SwipeState.LEFT;
					onSwipeComplete(view, mState);
				}
			} else {
				mTranslationX = Math.max(deltaX, - params.offsetOpenRight);

				if (mTranslationX == - params.offsetOpenRight) {
					mState = SwipeState.RIGHT;
					onSwipeComplete(view, mState);
				}
			}

			ViewHelper.setTranslationX(view, mTranslationX);

			return true;
		}

		return false;
	}

	protected boolean handleUpEvent(final View view, MotionEvent event) {
		if (mVelocityTracker == null || view == null) {
			return false;
		}
		event.offsetLocation(mTranslationX, 0);
		final LayoutParams params = (LayoutParams) view.getLayoutParams();

		float deltaX = event.getRawX() - mDownX;
		mVelocityTracker.addMovement(event);
		mVelocityTracker.computeCurrentVelocity(1000);
		float velocityX = Math.abs(mVelocityTracker.getXVelocity());
		float velocityY = Math.abs(mVelocityTracker.getYVelocity());
		boolean dismiss = false;
		boolean dismissRight = false;

		if (Math.abs(deltaX) > view.getWidth() / 2) {
			dismiss = true;
			dismissRight = deltaX > 0;
		} else if (mMinFlingVelocity <= velocityX && velocityX <= mMaxFlingVelocity && velocityY < velocityX) {
			dismiss = true;
			dismissRight = mVelocityTracker.getXVelocity() > 0;
		}

		if (dismiss) {
			final float translationX;
			switch (mState) {
				case LEFT:
					translationX = dismissRight ? params.offsetOpenLeft : 0;
					break;

				case RIGHT:
					translationX = dismissRight ? 0 : -params.offsetOpenRight;
					break;

				case COMMON:
				default:
					translationX = 0;
					break;
			}

			ViewPropertyAnimator
			.animate(view)
			.translationX(translationX)
			.setDuration(mMoveAnimationDuration)
			.setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					final float newTranslationX = ViewHelper.getTranslationX(view);
					if (newTranslationX == params.offsetOpenLeft) {
						mState = SwipeState.LEFT;
					} else if (newTranslationX == -params.offsetOpenRight) {
						mState = SwipeState.RIGHT;
					} else {
						mState = SwipeState.COMMON;
					}

					onSwipeComplete(view, mState);
				}
			});
		} else {
			// cancel
			ViewPropertyAnimator
			.animate(view)
			.translationX(0)
			.alpha(1)
			.setDuration(mMoveAnimationDuration)
			.setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mTranslationX = 0;
					mDownX = 0;
					mSwiping = false;

					mState = SwipeState.COMMON;
					onSwipeComplete(view, mState);
				}
			});
		}

		mVelocityTracker.recycle();
		mVelocityTracker = null;

		view.onTouchEvent(event);

		return false;
	}




	public static class LayoutParams extends FrameLayout.LayoutParams {
		public float offsetOpenLeft;
		public float offsetOpenRight;

		public LayoutParams() {
			this(MATCH_PARENT, WRAP_CONTENT);
		}

		public LayoutParams(int width, int height) {
			super(width, height);
		}

		public LayoutParams(Context c, AttributeSet attrs) {
			super(c, attrs);

			final TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.SwipeFrameLayout_Layout);
			offsetOpenLeft = a.getDimension(R.styleable.SwipeFrameLayout_Layout_offsetOpenLeft, 0);
			offsetOpenRight = a.getDimension(R.styleable.SwipeFrameLayout_Layout_offsetOpenRight, 0);
			a.recycle();
		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}
	}
}