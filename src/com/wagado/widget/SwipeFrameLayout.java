package com.wagado.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;

public class SwipeFrameLayout extends FrameLayout {
	public static final String TAG = "widget.SwipeFrameLayout";


	public static enum SwipeState {
		COMMON, LEFT, RIGHT;
	}

	public static interface ISwipeListener {
		public void swipeComplete(SwipeFrameLayout view, SwipeState state);
	}

	public static final int MINIMAL_OFFSET = 0;
	public static final long ANIMATION_DURATION = 200;

	private SwipeState mState;
	private SwipeDismissTouchListener mListener;
	private ISwipeListener mSwipeListener;
	private int mOffsetLeft;
	private int mOffsetRight;

	private View mBack;
	private View mFront;

	public SwipeFrameLayout(Context context) {
		super(context);

		initialize(context);
	}

	public SwipeFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		initialize(context);
	}

	public SwipeFrameLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		initialize(context);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		mBack = getChildAt(0);
		mFront = getChildAt(1);
		mFront.setOnTouchListener(getSwipeListener());
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		final float dx = ViewHelper.getTranslationX(mFront);
		if (dx < 0 && event.getX() > mFront.getMeasuredWidth() + dx) {
			mBack.dispatchTouchEvent(event);

			return true;
		} else if (dx > 0 && event.getX() < dx) {
			mBack.dispatchTouchEvent(event);

			return true;
		}

		return super.dispatchTouchEvent(event);
	}




	public void setOffsets(int offsetLeft, int offsetRight) {
		mOffsetLeft = offsetLeft < MINIMAL_OFFSET ? MINIMAL_OFFSET : offsetLeft;
		mOffsetRight = offsetRight < MINIMAL_OFFSET ? MINIMAL_OFFSET : offsetRight;
	}

	public void resetItemWithAnimation() {
		if (mFront == null || ViewHelper.getTranslationX(mFront) == MINIMAL_OFFSET) {
			return;
		}

		ViewPropertyAnimator.animate(mFront).translationX(MINIMAL_OFFSET).setDuration(ANIMATION_DURATION).setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				mState = SwipeState.COMMON;
				onSwipeComplete(mState);
			}
		});
	}

	public void resetItem() {
		if (mFront == null || ViewHelper.getTranslationX(mFront) == MINIMAL_OFFSET) {
			return;
		}

		ViewHelper.setTranslationX(mFront, MINIMAL_OFFSET);
		mState = SwipeState.COMMON;
		onSwipeComplete(mState);
	}

	public SwipeDismissTouchListener getSwipeListener() {
		return mListener;
	}

	public void setSwipeListener(ISwipeListener listener) {
		mSwipeListener = listener;
	}

	protected void initialize(Context context) {
		mListener = new SwipeDismissTouchListener(context);
	}

	protected void onSwipeComplete(SwipeState state) {
		if (mSwipeListener != null) {
			mSwipeListener.swipeComplete(this, state);
		}
	}




	public class SwipeDismissTouchListener implements View.OnTouchListener {
		private final int mSlop;
		private final int mMinFlingVelocity;
		private final int mMaxFlingVelocity;
		private final long mAnimationTime;

		private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero
		private float mDownX;
		private boolean mSwiping;
		private float mTranslationX;
		private VelocityTracker mVelocityTracker;

		public SwipeDismissTouchListener(Context context) {
			final ViewConfiguration vc = ViewConfiguration.get(context);
			mSlop = vc.getScaledTouchSlop();
			mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
			mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();

			mAnimationTime = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
			mState = SwipeState.COMMON;
		}

		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			// offset because the view is translated during swipe
			motionEvent.offsetLocation(mTranslationX, 0);

			if (mViewWidth < 2) {
				mViewWidth = mFront.getWidth();
			}

			switch (motionEvent.getActionMasked()) {
				case MotionEvent.ACTION_DOWN: {
					// TODO: ensure this is a finger, and set a flag

					mDownX = motionEvent.getRawX() - ViewHelper.getTranslationX(mFront);

					if (mVelocityTracker == null) {
						mVelocityTracker = VelocityTracker.obtain();
					}
					mVelocityTracker.addMovement(motionEvent);

					mFront.onTouchEvent(motionEvent);

					return true;
				}

				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP: {
					if (mVelocityTracker == null) {
						break;
					}

					float deltaX = motionEvent.getRawX() - mDownX;
					mVelocityTracker.addMovement(motionEvent);
					mVelocityTracker.computeCurrentVelocity(1000);
					float velocityX = Math.abs(mVelocityTracker.getXVelocity());
					float velocityY = Math.abs(mVelocityTracker.getYVelocity());
					boolean dismiss = false;
					boolean dismissRight = false;

					if (Math.abs(deltaX) > mViewWidth / 2) {
						dismiss = true;
						dismissRight = deltaX > 0;
					} else if (mMinFlingVelocity <= velocityX && velocityX <= mMaxFlingVelocity && velocityY < velocityX) {
						dismiss = true;
						dismissRight = mVelocityTracker.getXVelocity() > 0;
					}

					if (dismiss) {
						final int translationX;
						switch (mState) {
							case LEFT:
								translationX = dismissRight ? mOffsetLeft : 0;
								break;

							case RIGHT:
								translationX = dismissRight ? 0 : -mOffsetRight;
								break;

							case COMMON:
							default:
								translationX = 0;
								break;
						}

						ViewPropertyAnimator
						.animate(mFront)
						.translationX(translationX)
						.setDuration(mAnimationTime)
						.setListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								mTranslationX = ViewHelper.getTranslationX(mFront);

								if (mTranslationX == mOffsetLeft) {
									mState = SwipeState.LEFT;
								} else if (mTranslationX == -mOffsetRight) {
									mState = SwipeState.RIGHT;
								} else {
									mState = SwipeState.COMMON;
								}

								onSwipeComplete(mState);
							}
						});
					} else {
						// cancel
						ViewPropertyAnimator
						.animate(mFront)
						.translationX(0)
						.alpha(1)
						.setDuration(mAnimationTime)
						.setListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								mTranslationX = 0;
								mDownX = 0;
								mSwiping = false;

								mState = SwipeState.COMMON;
								onSwipeComplete(mState);
							}
						});
					}

					mVelocityTracker.recycle();
					mVelocityTracker = null;

					break;
				}

				case MotionEvent.ACTION_MOVE: {
					if (mVelocityTracker == null) {
						break;
					}

					mVelocityTracker.addMovement(motionEvent);
					float deltaX = motionEvent.getRawX() - mDownX;
					if (Math.abs(deltaX) > mSlop) {
						mSwiping = true;
						mFront.getParent().requestDisallowInterceptTouchEvent(true);

						// Cancel listview's touch
						final MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
						cancelEvent.setAction(MotionEvent.ACTION_CANCEL | (motionEvent.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
						mFront.onTouchEvent(cancelEvent);

						cancelEvent.recycle();
					}

					if (mSwiping) {
						if (deltaX > 0) {
							mTranslationX = Math.min(deltaX, mOffsetLeft);

							if (mTranslationX == mOffsetLeft) {
								mState = SwipeState.LEFT;
								onSwipeComplete(mState);
							}
						} else {
							mTranslationX = Math.max(deltaX, - mOffsetRight);

							if (mTranslationX == - mOffsetRight) {
								mState = SwipeState.RIGHT;
								onSwipeComplete(mState);
							}
						}

						ViewHelper.setTranslationX(mFront, mTranslationX);

						return true;
					}
					break;
				}
			}

			return false;
		}
	}
}