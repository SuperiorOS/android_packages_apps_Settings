package com.android.settings.edgeeffect;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.EdgeEffect;

import androidx.core.widget.NestedScrollView;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

public class SpringNestScrollView extends NestedScrollView {
    private static final FloatPropertyCompat<SpringNestScrollView> DAMPED_SCROLL = new FloatPropertyCompat<SpringNestScrollView>("value") {

        public float getValue(SpringNestScrollView springNestScrollView) {
            return springNestScrollView.mDampedScrollShift;
        }

        public void setValue(SpringNestScrollView springNestScrollView, float f) {
            springNestScrollView.setDampedScrollShift(f);
        }
    };
    private static final float DAMPING_RATIO = 0.5f;
    private static final float STIFFNESS = 590.0f;
    private static final float VELOCITY_MULTIPLIER = 0.3f;
    boolean mOverScrollNested = false;
    float mPullGrowBottom = 0.9f;
    float mPullGrowTop = 0.1f;
    int[] mScrollConsumed;
    int[] mScrollStepConsumed;
    private SpringEdgeEffect mActiveEdge;
    private EdgeEffect mBottomGlow;
    private float mDampedScrollShift = 0.0f;
    private float mDamping = 0.5f;
    private int mDisableEdgeEffect = 0;
    private boolean mDisableEffectBottom = false;
    private boolean mDisableEffectTop = false;
    private int mDispatchScrollCounter;
    private float mDistance = 0.0f;
    private SEdgeEffectFactory mEdgeEffectFactory;
    private boolean mGlowingBottom = false;
    private boolean mGlowingTop = false;
    private int mInitialTouchY;
    private boolean mIsEmpty = false;
    private int mLastTouchX;
    private int mLastTouchY;
    private float mLastX;
    private float mLastY;
    private float mLastYVel;
    private int mMaxFlingVelocity;
    private int[] mNestedOffsets;
    private int mPullCount = 0;
    private boolean mRecycleScrolled = false;
    private int[] mScrollOffset;
    private int mScrollPointerId;
    private int mScrollState;
    private SpringAnimation mSpring;
    private float mStif = STIFFNESS;
    private EdgeEffect mTopGlow;
    private int mTouchSlop;
    private VelocityTracker mVelocityTracker;
    private float mVelocity_multiplier = 0.3f;

    public SpringNestScrollView(Context context) {
        super(context);
        init();
    }

    public SpringNestScrollView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public SpringNestScrollView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init();
    }

    static int pullCount(SpringNestScrollView springNestScrollView) {
        int i = springNestScrollView.mPullCount;
        springNestScrollView.mPullCount = i + 1;
        return i;
    }

    public int getCanvasClipTopForOverscroll() {
        return 0;
    }

    public void onScrolled(int i, int i2) {
    }

    private void init() {
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        mMaxFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        mScrollStepConsumed = new int[2];
        mScrollOffset = new int[2];
        mNestedOffsets = new int[2];
        mScrollConsumed = new int[2];
        ViewEdgeEffectFactory createViewEdgeEffectFactory = createViewEdgeEffectFactory();
        mEdgeEffectFactory = createViewEdgeEffectFactory;
        setEdgeEffectFactory(createViewEdgeEffectFactory);
        SpringAnimation springAnimation = new SpringAnimation(this, DAMPED_SCROLL, 0.0f);
        mSpring = springAnimation;
        springAnimation.setSpring(new SpringForce(0.0f).setStiffness(STIFFNESS).setDampingRatio(0.5f));
    }

    private Object getSuperField(Object obj, String str) {
        Object obj2;
        try {
            Field declaredField = obj.getClass().getSuperclass().getDeclaredField(str);
            declaredField.setAccessible(true);
            obj2 = declaredField.get(obj);
        } catch (Exception e) {
            e.printStackTrace();
            obj2 = null;
        }
        if (obj2 != null) {
            return obj2;
        }
        try {
            Field declaredField2 = obj.getClass().getSuperclass().getSuperclass().getDeclaredField(str);
            declaredField2.setAccessible(true);
            return declaredField2.get(obj);
        } catch (Exception e2) {
            e2.printStackTrace();
            return obj2;
        }
    }

    public void setEdgeEffectFactory(SEdgeEffectFactory sEdgeEffectFactory) {
        mEdgeEffectFactory = sEdgeEffectFactory;
        invalidateGlows();
    }

    public void invalidateGlows() {
        mBottomGlow = null;
        mTopGlow = null;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        boolean z;
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(motionEvent);
        int actionMasked = motionEvent.getActionMasked();
        int actionIndex = motionEvent.getActionIndex();
        MotionEvent obtain = MotionEvent.obtain(motionEvent);
        if (actionMasked == 0) {
            mScrollPointerId = motionEvent.getPointerId(0);
            int y = (int) (motionEvent.getY() + 0.5f);
            mLastTouchY = y;
            mInitialTouchY = y;
            if (mScrollState == 2) {
                getParent().requestDisallowInterceptTouchEvent(true);
                setScrollState(1);
            }
            int[] iArr = mNestedOffsets;
            iArr[1] = 0;
            iArr[0] = 0;
            mRecycleScrolled = false;
        } else if (actionMasked == 1) {
            mVelocityTracker.addMovement(obtain);
            mVelocityTracker.computeCurrentVelocity(1000, (float) mMaxFlingVelocity);
            float f = -mVelocityTracker.getYVelocity(mScrollPointerId);
            if (f == 0.0f) {
                setScrollState(0);
            } else {
                mLastYVel = f;
                mLastX = motionEvent.getX();
                mLastY = motionEvent.getY();
            }
            resetTouch();
            stopNestedScroll();
        } else if (actionMasked == 2) {
            int findPointerIndex = motionEvent.findPointerIndex(mScrollPointerId);
            if (findPointerIndex < 0) {
                Log.e("SpringScrollView", "Error processing scroll; pointer index for id " + mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                obtain.recycle();
                return false;
            }
            motionEvent.getX(findPointerIndex);
            int y2 = (int) (motionEvent.getY(findPointerIndex) + 0.5f);
            int i = mLastTouchY - y2;
            if (mScrollState != 1) {
                int abs = Math.abs(i);
                int i2 = mTouchSlop;
                if (abs > i2) {
                    i = i > 0 ? i - i2 : i + i2;
                    z = true;
                } else {
                    z = false;
                }
                if (z) {
                    setScrollState(1);
                }
            }
            if (mScrollState == 1) {
                mLastTouchY = y2 - mScrollOffset[1];
                if (scrollByInternal(0, i, obtain)) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
        } else if (actionMasked == 3) {
            cancelTouch();
        } else if (actionMasked == 5) {
            mScrollPointerId = motionEvent.getPointerId(actionIndex);
            int y3 = (int) (motionEvent.getY(actionIndex) + 0.5f);
            mLastTouchY = y3;
            mInitialTouchY = y3;
        } else if (actionMasked == 6) {
            onPointerUp(motionEvent);
        }
        obtain.recycle();
        mLastX = motionEvent.getX();
        mLastY = motionEvent.getY();
        return super.onInterceptTouchEvent(motionEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean z;
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        MotionEvent obtain = MotionEvent.obtain(motionEvent);
        int actionMasked = motionEvent.getActionMasked();
        int actionIndex = motionEvent.getActionIndex();
        if (actionMasked == 0) {
            int[] iArr = mNestedOffsets;
            iArr[1] = 0;
            iArr[0] = 0;
        }
        int[] iArr2 = mNestedOffsets;
        obtain.offsetLocation((float) iArr2[0], (float) iArr2[1]);
        if (actionMasked == 0) {
            mScrollPointerId = motionEvent.getPointerId(0);
            int y = (int) (motionEvent.getY() + 0.5f);
            mLastTouchY = y;
            mInitialTouchY = y;
            mRecycleScrolled = false;
        } else if (actionMasked == 1) {
            mVelocityTracker.addMovement(obtain);
            mVelocityTracker.computeCurrentVelocity(1000, (float) mMaxFlingVelocity);
            float f = -mVelocityTracker.getYVelocity(mScrollPointerId);
            if (f == 0.0f) {
                setScrollState(0);
            } else {
                mLastYVel = f;
            }
            resetTouch();
            obtain.recycle();
            mLastX = motionEvent.getX();
            mLastY = motionEvent.getY();
            return super.onTouchEvent(motionEvent);
        } else if (actionMasked == 2) {
            int findPointerIndex = motionEvent.findPointerIndex(mScrollPointerId);
            if (findPointerIndex < 0) {
                Log.e("SpringScrollView", "Error processing scroll; pointer index for id " + mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                obtain.recycle();
                return false;
            }
            motionEvent.getX(findPointerIndex);
            int y2 = (int) (motionEvent.getY(findPointerIndex) + 0.5f);
            int i = mLastTouchY - y2;
            if (mScrollState != 1) {
                int abs = Math.abs(i);
                int i2 = mTouchSlop;
                if (abs > i2) {
                    i = i > 0 ? i - i2 : i + i2;
                    z = true;
                } else {
                    z = false;
                }
                if (z) {
                    setScrollState(1);
                }
            }
            if (mScrollState == 1) {
                mLastTouchY = y2 - mScrollOffset[1];
                if (scrollByInternal(0, i, obtain)) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
        } else if (actionMasked == 3) {
            cancelTouch();
        } else if (actionMasked == 5) {
            mScrollPointerId = motionEvent.getPointerId(actionIndex);
            int y3 = (int) (motionEvent.getY(actionIndex) + 0.5f);
            mLastTouchY = y3;
            mInitialTouchY = y3;
        } else if (actionMasked == 6) {
            onPointerUp(motionEvent);
        }
        obtain.recycle();
        mLastX = motionEvent.getX();
        mLastY = motionEvent.getY();
        return super.onTouchEvent(motionEvent);
    }

    public void ensureTopGlow() {
        SEdgeEffectFactory sEdgeEffectFactory = mEdgeEffectFactory;
        if (sEdgeEffectFactory == null) {
            Log.e("SpringNestScrollView", "setEdgeEffectFactory first, please!");
        } else if (mTopGlow == null) {
            mTopGlow = sEdgeEffectFactory.createEdgeEffect(this, 1);
            if (getClipToPadding()) {
                mTopGlow.setSize((getMeasuredWidth() - getPaddingLeft()) - getPaddingRight(), (getMeasuredHeight() - getPaddingTop()) - getPaddingBottom());
            } else {
                mTopGlow.setSize(getMeasuredWidth(), getMeasuredHeight());
            }
        }
    }

    public void ensureBottomGlow() {
        SEdgeEffectFactory sEdgeEffectFactory = mEdgeEffectFactory;
        if (sEdgeEffectFactory == null) {
            Log.e("SpringNestScrollView", "setEdgeEffectFactory first, please!");
        } else if (mBottomGlow == null) {
            mBottomGlow = sEdgeEffectFactory.createEdgeEffect(this, 3);
            if (getClipToPadding()) {
                mBottomGlow.setSize((getMeasuredWidth() - getPaddingLeft()) - getPaddingRight(), (getMeasuredHeight() - getPaddingTop()) - getPaddingBottom());
            } else {
                mBottomGlow.setSize(getMeasuredWidth(), getMeasuredHeight());
            }
        }
    }

    private void pullGlows(float f, float f2, float f3, float f4) {
        if (f3 <= ((float) getHeight()) && f3 >= 0.0f) {
            float height = f3 / ((float) getHeight());
            if (f4 < 0.0f && height < mPullGrowBottom && height > mPullGrowTop) {
                ensureTopGlow();
                EdgeEffect edgeEffect = mTopGlow;
                if (edgeEffect != null) {
                    edgeEffect.onPull((-f4) / ((float) getHeight()), f / ((float) getWidth()));
                    mGlowingTop = true;
                    postInvalidateOnAnimation();
                }
            } else if (f4 > 0.0f && height > mPullGrowTop && height < mPullGrowBottom) {
                ensureBottomGlow();
                EdgeEffect edgeEffect2 = mBottomGlow;
                if (edgeEffect2 != null) {
                    edgeEffect2.onPull(f4 / ((float) getHeight()), 1.0f - (f / ((float) getWidth())));
                    mGlowingBottom = true;
                    if (f2 != 0.0f || f4 != 0.0f) {
                        postInvalidateOnAnimation();
                    }
                    return;
                }
            }
            postInvalidateOnAnimation();
        }
    }

    public void setScrollState(int i) {
        if (i != mScrollState) {
            mScrollState = i;
        }
    }

    private void resetTouch() {
        VelocityTracker velocityTracker = mVelocityTracker;
        if (velocityTracker != null) {
            velocityTracker.clear();
        }
        releaseGlows();
    }

    private void releaseGlows() {
        boolean z;
        EdgeEffect edgeEffect = mTopGlow;
        if (edgeEffect != null) {
            edgeEffect.onRelease();
            mGlowingTop = false;
            z = mTopGlow.isFinished();
        } else {
            z = false;
        }
        EdgeEffect edgeEffect2 = mBottomGlow;
        if (edgeEffect2 != null) {
            edgeEffect2.onRelease();
            mGlowingBottom = false;
            z |= mBottomGlow.isFinished();
        }
        if (z) {
            postInvalidateOnAnimation();
        }
    }

    private void cancelTouch() {
        resetTouch();
        setScrollState(0);
    }

    private void onPointerUp(MotionEvent motionEvent) {
        int actionIndex = motionEvent.getActionIndex();
        if (motionEvent.getPointerId(actionIndex) == mScrollPointerId) {
            int i = actionIndex == 0 ? 1 : 0;
            mScrollPointerId = motionEvent.getPointerId(i);
            int y = (int) (motionEvent.getY(i) + 0.5f);
            mLastTouchY = y;
            mInitialTouchY = y;
        }
    }

    public void dispatchOnScrolled(int i, int i2) {
        mDispatchScrollCounter++;
        int scrollX = getScrollX();
        int scrollY = getScrollY();
        onScrollChanged(scrollX, scrollY, scrollX, scrollY);
        onScrolled(i, i2);
        mDispatchScrollCounter--;
    }

    public boolean scrollByInternal(int i, int i2, MotionEvent motionEvent) {
        int i3;
        int i4;
        int i5;
        int i6;
        if (!isReadyToOverScroll(i2 < 0)) {
            return false;
        }
        if (getChildCount() >= 0) {
            scrollStep(i, i2, mScrollStepConsumed);
            int[] iArr = mScrollStepConsumed;
            i5 = iArr[0];
            i6 = iArr[1];
            i4 = i - i5;
            i3 = i2 - i6;
        } else {
            i6 = 0;
            i5 = 0;
            i4 = 0;
            i3 = 0;
        }
        invalidate();
        if (getOverScrollMode() != 2) {
            if (motionEvent != null && !motionEvent.isFromSource(8194)) {
                pullGlows(motionEvent.getX(), (float) i4, motionEvent.getY(), (float) i3);
            }
            considerReleasingGlowsOnScroll(i, i2);
        }
        if (!(i5 == 0 && i6 == 0)) {
            dispatchOnScrolled(i5, i6);
        }
        if (!awakenScrollBars()) {
            invalidate();
        }
        return i5 != 0 || i6 != 0;
    }

    @Override
    public void fling(int i) {
        if (i <= 10000 || !mIsEmpty) {
            super.fling(i);
        } else {
            super.fling(1200);
        }
    }

    public void setIsEmpty(boolean z) {
        mIsEmpty = z;
    }

    public void scrollStep(int i, int i2, int[] iArr) {
        if (iArr != null) {
            iArr[1] = 0;
        }
    }

    private boolean isReadyToOverScroll(boolean z) {
        if (getChildCount() <= 0) {
            return false;
        }
        if (z) {
            return !canScrollVertically(-1);
        }
        return !canScrollVertically(1);
    }

    public void considerReleasingGlowsOnScroll(int i, int i2) {
        EdgeEffect edgeEffect = mTopGlow;
        boolean z = false;
        if (edgeEffect != null && !edgeEffect.isFinished() && i2 > 0) {
            mTopGlow.onRelease();
            z = mTopGlow.isFinished();
        }
        EdgeEffect edgeEffect2 = mBottomGlow;
        if (edgeEffect2 != null && !edgeEffect2.isFinished() && i2 < 0) {
            mBottomGlow.onRelease();
            z = mBottomGlow.isFinished();
        }
        if (z) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    public void onScrollChanged(int i, int i2, int i3, int i4) {
        if (mGlowingTop && canScrollVertically(-1) && i2 > i4) {
            onRecyclerViewScrolled();
            mRecycleScrolled = true;
        }
        if (mGlowingBottom && canScrollVertically(1) && i2 < i4) {
            onRecyclerViewScrolled();
            mRecycleScrolled = true;
        }
        if (!mGlowingTop && !canScrollVertically(-1) && i2 < i4) {
            float f = mLastYVel;
            if (f >= 0.0f) {
                f = computeVelocity();
            }
            float f2 = f / 20.0f;
            pullGlows(mLastX, 0.0f, mLastY, f2);
            EdgeEffect edgeEffect = mTopGlow;
            if (edgeEffect != null) {
                edgeEffect.onAbsorb((int) f2);
            }
        }
        if (!mGlowingBottom && !canScrollVertically(1) && i2 > i4) {
            float f3 = mLastYVel;
            if (f3 <= 0.0f) {
                f3 = computeVelocity();
            }
            float f4 = f3 / 20.0f;
            pullGlows(mLastX, 0.0f, mLastY, f4);
            EdgeEffect edgeEffect2 = mBottomGlow;
            if (edgeEffect2 != null) {
                edgeEffect2.onAbsorb((int) f4);
            }
        }
        super.onScrollChanged(i, i2, i3, i4);
    }

    public ViewEdgeEffectFactory createViewEdgeEffectFactory() {
        return new ViewEdgeEffectFactory();
    }

    public void setDampedScrollShift(float f) {
        if (f != mDampedScrollShift) {
            mDampedScrollShift = f;
            invalidate();
        }
    }

    private void setActiveEdge(SpringEdgeEffect springEdgeEffect) {
        SpringEdgeEffect springEdgeEffect2 = mActiveEdge;
        mActiveEdge = springEdgeEffect;
    }

    private void finishScrollWithVelocity(float f) {
        float f2 = mDampedScrollShift;
        if (f2 > Float.MAX_VALUE || f2 < -3.4028235E38f) {
            Log.e("SpringNestScrollView", "animation parameter out of range!");
        } else if (f > 0.0f && mDisableEffectTop) {
        } else {
            if (f >= 0.0f || !mDisableEffectBottom) {
                mSpring.setStartVelocity(f);
                mSpring.setStartValue(mDampedScrollShift);
                mSpring.start();
            }
        }
    }

    public void onRecyclerViewScrolled() {
        if (mPullCount != 1 && !mSpring.isRunning()) {
            mDistance = 0.0f;
            mPullCount = 0;
            finishScrollWithVelocity(0.0f);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (mDampedScrollShift != 0.0f) {
            int save = canvas.save();
            canvas.translate(0.0f, mDampedScrollShift);
            super.draw(canvas);
            canvas.restoreToCount(save);
            return;
        }
        super.draw(canvas);
    }

    public float computeVelocity() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.computeCurrentVelocity(1000, (float) mMaxFlingVelocity);
        return -mVelocityTracker.getYVelocity(mScrollPointerId);
    }

    public void setVelocityMultiplier(float f) {
        mVelocity_multiplier = f;
    }

    public void setStiffness(float f) {
        mStif = (1500.0f * f) + ((1.0f - f) * 200.0f);
        mSpring.getSpring().setStiffness(mStif);
    }

    public void setBouncy(float f) {
        mDamping = f;
        mSpring.getSpring().setDampingRatio(mDamping);
    }

    public void setEdgeEffectDisable(int i) {
        mDisableEdgeEffect = i;
        if ((i & 1) != 0) {
            mDisableEffectTop = true;
        }
        if ((i & 2) != 0) {
            mDisableEffectBottom = true;
        }
    }

    public static class SEdgeEffectFactory {
        public static final int DIRECTION_BOTTOM = 3;
        public static final int DIRECTION_LEFT = 0;
        public static final int DIRECTION_RIGHT = 2;
        public static final int DIRECTION_TOP = 1;

        public EdgeEffect createEdgeEffect(View view, int i) {
            return new EdgeEffect(view.getContext());
        }

        @Retention(RetentionPolicy.SOURCE)
        public @interface EdgeDirection {
        }
    }

    public class ViewEdgeEffectFactory extends SEdgeEffectFactory {
        private ViewEdgeEffectFactory() {
        }

        @Override
        public EdgeEffect createEdgeEffect(View view, int i) {
            if (i == 0 || i == 1) {
                SpringNestScrollView springNestScrollView = SpringNestScrollView.this;
                return new SpringEdgeEffect(springNestScrollView.getContext(), SpringNestScrollView.this.mVelocity_multiplier);
            } else if (i != 2 && i != 3) {
                return super.createEdgeEffect(view, i);
            } else {
                SpringNestScrollView springNestScrollView2 = SpringNestScrollView.this;
                return new SpringEdgeEffect(springNestScrollView2.getContext(), -SpringNestScrollView.this.mVelocity_multiplier);
            }
        }
    }
    
    public class SpringEdgeEffect extends EdgeEffect {
        private final float mVelocityMultiplier;
        private boolean mReleased = true;

        public SpringEdgeEffect(Context context, float f) {
            super(context);
            mVelocityMultiplier = f;
        }

        public boolean draw(Canvas canvas) {
            return false;
        }

        public void onAbsorb(int i) {
            SpringNestScrollView.this.finishScrollWithVelocity(((float) i) * mVelocityMultiplier);
            SpringNestScrollView.this.mDistance = 0.0f;
        }

        public void onPull(float f, float f2) {
            if (SpringNestScrollView.this.mSpring.isRunning()) {
                SpringNestScrollView.this.mSpring.cancel();
            }
            SpringNestScrollView.pullCount(SpringNestScrollView.this);
            SpringNestScrollView.this.setActiveEdge(this);
            SpringNestScrollView.this.mDistance += f * (mVelocityMultiplier / 3.0f);
            if (SpringNestScrollView.this.mDistance > 0.0f && SpringNestScrollView.this.mDisableEffectTop) {
                SpringNestScrollView.this.mDistance = 0.0f;
            } else if (SpringNestScrollView.this.mDistance < 0.0f && SpringNestScrollView.this.mDisableEffectBottom) {
                SpringNestScrollView.this.mDistance = 0.0f;
            }
            SpringNestScrollView springNestScrollView = SpringNestScrollView.this;
            springNestScrollView.setDampedScrollShift(springNestScrollView.mDistance * ((float) SpringNestScrollView.this.getHeight()));
            mReleased = false;
        }

        public void onRelease() {
            if (!mReleased) {
                SpringNestScrollView.this.mDistance = 0.0f;
                SpringNestScrollView.this.mPullCount = 0;
                SpringNestScrollView.this.finishScrollWithVelocity(0.0f);
                mReleased = true;
            }
        }
    }
}
