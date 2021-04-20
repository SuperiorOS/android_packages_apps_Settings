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

public class SpringNestScrollView extends NestedScrollView {
    private static final FloatPropertyCompat<SpringNestScrollView> DAMPED_SCROLL = new FloatPropertyCompat<SpringNestScrollView>("value") {

        public float getValue(SpringNestScrollView springNestScrollView) {
            return springNestScrollView.mDampedScrollShift;
        }

        public void setValue(SpringNestScrollView springNestScrollView, float f) {
            springNestScrollView.setDampedScrollShift(f);
        }
    };
    private SpringEdgeEffect mActiveEdge;
    private EdgeEffect mBottomGlow;
    private float mDampedScrollShift = 0.0f;
    private int mDispatchScrollCounter;
    private float mDistance = 0.0f;
    private SEdgeEffectFactory mEdgeEffectFactory;
    private boolean mGlowingBottom = false;
    private boolean mGlowingTop = false;
    private int mLastTouchY;
    private float mLastX;
    private float mLastY;
    private float mLastYVel;
    private int mMaxFlingVelocity;
    private int[] mNestedOffsets;
    boolean mOverScrollNested = false;
    private int mPullCount = 0;
    float mPullGrowBottom = 0.9f;
    float mPullGrowTop = 0.1f;
    int[] mScrollConsumed;
    private int[] mScrollOffset;
    private int mScrollPointerId;
    private int mScrollState;
    int[] mScrollStepConsumed;
    private SpringAnimation mSpring;
    private EdgeEffect mTopGlow;
    private int mTouchSlop;
    private VelocityTracker mVelocityTracker;

    public int getCanvasClipTopForOverscroll() {
        return 0;
    }

    public void onScrolled(int i, int i2) {
    }

    static float access$316(SpringNestScrollView springNestScrollView, float f) {
        float f2 = springNestScrollView.mDistance + f;
        springNestScrollView.mDistance = f2;
        return f2;
    }

    static int access$508(SpringNestScrollView springNestScrollView) {
        int i = springNestScrollView.mPullCount;
        springNestScrollView.mPullCount = i + 1;
        return i;
    }

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

    private void init() {
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        this.mTouchSlop = viewConfiguration.getScaledTouchSlop();
        this.mMaxFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        this.mScrollStepConsumed = new int[2];
        this.mScrollOffset = new int[2];
        this.mNestedOffsets = new int[2];
        this.mScrollConsumed = new int[2];
        ViewEdgeEffectFactory createViewEdgeEffectFactory = createViewEdgeEffectFactory();
        this.mEdgeEffectFactory = createViewEdgeEffectFactory;
        setEdgeEffectFactory(createViewEdgeEffectFactory);
        SpringAnimation springAnimation = new SpringAnimation(this, DAMPED_SCROLL, 0.0f);
        this.mSpring = springAnimation;
        SpringForce springForce = new SpringForce(0.0f);
        springForce.setStiffness(590.0f);
        springForce.setDampingRatio(0.5f);
        springAnimation.setSpring(springForce);
    }

    public void setEdgeEffectFactory(SEdgeEffectFactory sEdgeEffectFactory) {
        this.mEdgeEffectFactory = sEdgeEffectFactory;
        invalidateGlows();
    }

    public void invalidateGlows() {
        this.mBottomGlow = null;
        this.mTopGlow = null;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        boolean z;
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(motionEvent);
        int actionMasked = motionEvent.getActionMasked();
        int actionIndex = motionEvent.getActionIndex();
        MotionEvent obtain = MotionEvent.obtain(motionEvent);
        if (actionMasked == 0) {
            this.mScrollPointerId = motionEvent.getPointerId(0);
            this.mLastTouchY = (int) (motionEvent.getY() + 0.5f);
            if (this.mScrollState == 2) {
                getParent().requestDisallowInterceptTouchEvent(true);
                setScrollState(1);
            }
            int[] iArr = this.mNestedOffsets;
            iArr[1] = 0;
            iArr[0] = 0;
        } else if (actionMasked == 1) {
            this.mVelocityTracker.addMovement(obtain);
            this.mVelocityTracker.computeCurrentVelocity(1000, (float) this.mMaxFlingVelocity);
            float f = -this.mVelocityTracker.getYVelocity(this.mScrollPointerId);
            if (f == 0.0f) {
                setScrollState(0);
            } else {
                this.mLastYVel = f;
                this.mLastX = motionEvent.getX();
                this.mLastY = motionEvent.getY();
            }
            resetTouch();
            stopNestedScroll();
        } else if (actionMasked == 2) {
            int findPointerIndex = motionEvent.findPointerIndex(this.mScrollPointerId);
            if (findPointerIndex < 0) {
                Log.e("SpringScrollView", "Error processing scroll; pointer index for id " + this.mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                return false;
            }
            motionEvent.getX(findPointerIndex);
            int y = (int) (motionEvent.getY(findPointerIndex) + 0.5f);
            int i = this.mLastTouchY - y;
            if (dispatchNestedPreScroll(0, i, this.mScrollConsumed, this.mScrollOffset)) {
                i -= this.mScrollConsumed[1];
                int[] iArr2 = this.mScrollOffset;
                obtain.offsetLocation((float) iArr2[0], (float) iArr2[1]);
                int[] iArr3 = this.mNestedOffsets;
                int i2 = iArr3[0];
                int[] iArr4 = this.mScrollOffset;
                iArr3[0] = i2 + iArr4[0];
                iArr3[1] = iArr3[1] + iArr4[1];
            }
            if (this.mScrollState != 1) {
                int abs = Math.abs(i);
                int i3 = this.mTouchSlop;
                if (abs > i3) {
                    i = i > 0 ? i - i3 : i + i3;
                    z = true;
                } else {
                    z = false;
                }
                if (z) {
                    setScrollState(1);
                }
            }
            if (this.mScrollState == 1) {
                this.mLastTouchY = y - this.mScrollOffset[1];
                if (scrollByInternal(0, i, obtain)) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
        } else if (actionMasked == 3) {
            cancelTouch();
        } else if (actionMasked == 5) {
            this.mScrollPointerId = motionEvent.getPointerId(actionIndex);
            this.mLastTouchY = (int) (motionEvent.getY(actionIndex) + 0.5f);
        } else if (actionMasked == 6) {
            onPointerUp(motionEvent);
        }
        return super.onInterceptTouchEvent(motionEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean z;
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        MotionEvent obtain = MotionEvent.obtain(motionEvent);
        int actionMasked = motionEvent.getActionMasked();
        int actionIndex = motionEvent.getActionIndex();
        boolean z2 = false;
        if (actionMasked == 0) {
            int[] iArr = this.mNestedOffsets;
            iArr[1] = 0;
            iArr[0] = 0;
        }
        int[] iArr2 = this.mNestedOffsets;
        obtain.offsetLocation((float) iArr2[0], (float) iArr2[1]);
        if (actionMasked == 0) {
            this.mScrollPointerId = motionEvent.getPointerId(0);
            this.mLastTouchY = (int) (motionEvent.getY() + 0.5f);
        } else if (actionMasked == 1) {
            this.mVelocityTracker.addMovement(obtain);
            this.mVelocityTracker.computeCurrentVelocity(1000, (float) this.mMaxFlingVelocity);
            float f = -this.mVelocityTracker.getYVelocity(this.mScrollPointerId);
            if (f == 0.0f) {
                setScrollState(0);
            } else {
                this.mLastYVel = f;
                this.mLastX = motionEvent.getX();
                this.mLastY = motionEvent.getY();
            }
            resetTouch();
            z2 = true;
        } else if (actionMasked == 2) {
            int findPointerIndex = motionEvent.findPointerIndex(this.mScrollPointerId);
            if (findPointerIndex < 0) {
                Log.e("SpringScrollView", "Error processing scroll; pointer index for id " + this.mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                return false;
            }
            motionEvent.getX(findPointerIndex);
            int y = (int) (motionEvent.getY(findPointerIndex) + 0.5f);
            int i = this.mLastTouchY - y;
            if (dispatchNestedPreScroll(0, i, this.mScrollConsumed, this.mScrollOffset)) {
                i -= this.mScrollConsumed[1];
                int[] iArr3 = this.mScrollOffset;
                obtain.offsetLocation((float) iArr3[0], (float) iArr3[1]);
                int[] iArr4 = this.mNestedOffsets;
                int i2 = iArr4[0];
                int[] iArr5 = this.mScrollOffset;
                iArr4[0] = i2 + iArr5[0];
                iArr4[1] = iArr4[1] + iArr5[1];
            }
            if (this.mScrollState != 1) {
                int abs = Math.abs(i);
                int i3 = this.mTouchSlop;
                if (abs > i3) {
                    i = i > 0 ? i - i3 : i + i3;
                    z = true;
                } else {
                    z = false;
                }
                if (z) {
                    setScrollState(1);
                }
            }
            if (this.mScrollState == 1) {
                this.mLastTouchY = y - this.mScrollOffset[1];
                if (scrollByInternal(0, i, obtain)) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
        } else if (actionMasked == 3) {
            cancelTouch();
        } else if (actionMasked == 5) {
            this.mScrollPointerId = motionEvent.getPointerId(actionIndex);
            this.mLastTouchY = (int) (motionEvent.getY(actionIndex) + 0.5f);
        } else if (actionMasked == 6) {
            onPointerUp(motionEvent);
        }
        if (!z2) {
            this.mVelocityTracker.addMovement(obtain);
        }
        obtain.recycle();
        return super.onTouchEvent(motionEvent);
    }

    public void ensureTopGlow() {
        SEdgeEffectFactory sEdgeEffectFactory = this.mEdgeEffectFactory;
        if (sEdgeEffectFactory == null) {
            throw new IllegalStateException("setEdgeEffectFactory first, please!");
        } else if (this.mTopGlow == null) {
            this.mTopGlow = sEdgeEffectFactory.createEdgeEffect(this, 1);
            if (getClipToPadding()) {
                this.mTopGlow.setSize((getMeasuredWidth() - getPaddingLeft()) - getPaddingRight(), (getMeasuredHeight() - getPaddingTop()) - getPaddingBottom());
            } else {
                this.mTopGlow.setSize(getMeasuredWidth(), getMeasuredHeight());
            }
        }
    }

    public void ensureBottomGlow() {
        SEdgeEffectFactory sEdgeEffectFactory = this.mEdgeEffectFactory;
        if (sEdgeEffectFactory == null) {
            throw new IllegalStateException("setEdgeEffectFactory first, please!");
        } else if (this.mBottomGlow == null) {
            this.mBottomGlow = sEdgeEffectFactory.createEdgeEffect(this, 3);
            if (getClipToPadding()) {
                this.mBottomGlow.setSize((getMeasuredWidth() - getPaddingLeft()) - getPaddingRight(), (getMeasuredHeight() - getPaddingTop()) - getPaddingBottom());
            } else {
                this.mBottomGlow.setSize(getMeasuredWidth(), getMeasuredHeight());
            }
        }
    }

    private void pullGlows(float f, float f2, float f3, float f4) {
        if (f3 <= ((float) getHeight()) && f3 >= 0.0f) {
            float height = f3 / ((float) getHeight());
            boolean z = true;
            if (f4 < 0.0f && height < this.mPullGrowBottom && height > this.mPullGrowTop) {
                ensureTopGlow();
                this.mTopGlow.onPull((-f4) / ((float) getHeight()), f / ((float) getWidth()));
                this.mGlowingTop = true;
            } else if (f4 <= 0.0f || height <= this.mPullGrowTop || height >= this.mPullGrowBottom) {
                z = false;
            } else {
                ensureBottomGlow();
                this.mBottomGlow.onPull(f4 / ((float) getHeight()), 1.0f - (f / ((float) getWidth())));
                this.mGlowingBottom = true;
            }
            if (z || f2 != 0.0f || f4 != 0.0f) {
                postInvalidateOnAnimation();
            }
        }
    }

    public void setScrollState(int i) {
        if (i != this.mScrollState) {
            this.mScrollState = i;
        }
    }

    private void resetTouch() {
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker != null) {
            velocityTracker.clear();
        }
        releaseGlows();
    }

    private void releaseGlows() {
        boolean z;
        EdgeEffect edgeEffect = this.mTopGlow;
        if (edgeEffect != null) {
            edgeEffect.onRelease();
            this.mGlowingTop = false;
            z = this.mTopGlow.isFinished() | false;
        } else {
            z = false;
        }
        EdgeEffect edgeEffect2 = this.mBottomGlow;
        if (edgeEffect2 != null) {
            edgeEffect2.onRelease();
            this.mGlowingBottom = false;
            z |= this.mBottomGlow.isFinished();
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
        if (motionEvent.getPointerId(actionIndex) == this.mScrollPointerId) {
            int i = actionIndex == 0 ? 1 : 0;
            this.mScrollPointerId = motionEvent.getPointerId(i);
            this.mLastTouchY = (int) (motionEvent.getY(i) + 0.5f);
        }
    }

    public void dispatchOnScrolled(int i, int i2) {
        this.mDispatchScrollCounter++;
        int scrollX = getScrollX();
        int scrollY = getScrollY();
        onScrollChanged(scrollX, scrollY, scrollX, scrollY);
        onScrolled(i, i2);
        this.mDispatchScrollCounter--;
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
            scrollStep(i, i2, this.mScrollStepConsumed);
            int[] iArr = this.mScrollStepConsumed;
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
        boolean dispatchNestedScroll = dispatchNestedScroll(i5, i6, i4, i3, this.mScrollOffset);
        if (dispatchNestedScroll) {
            int i7 = this.mLastTouchY;
            int[] iArr2 = this.mScrollOffset;
            this.mLastTouchY = i7 - iArr2[1];
            if (motionEvent != null) {
                motionEvent.offsetLocation((float) iArr2[0], (float) iArr2[1]);
            }
            int[] iArr3 = this.mNestedOffsets;
            int i8 = iArr3[0];
            int[] iArr4 = this.mScrollOffset;
            iArr3[0] = i8 + iArr4[0];
            iArr3[1] = iArr3[1] + iArr4[1];
        }
        if ((!dispatchNestedScroll || this.mOverScrollNested) && getOverScrollMode() != 2) {
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
        return (i5 == 0 && i6 == 0) ? false : true;
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
        if (!z) {
            return !canScrollVertically(1);
        }
        return false;
    }

    public void considerReleasingGlowsOnScroll(int i, int i2) {
        EdgeEffect edgeEffect = this.mTopGlow;
        boolean z = false;
        if (edgeEffect != null && !edgeEffect.isFinished() && i2 > 0) {
            this.mTopGlow.onRelease();
            z = false | this.mTopGlow.isFinished();
        }
        EdgeEffect edgeEffect2 = this.mBottomGlow;
        if (edgeEffect2 != null && !edgeEffect2.isFinished() && i2 < 0) {
            this.mBottomGlow.onRelease();
            z |= this.mBottomGlow.isFinished();
        }
        if (z) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    public void onScrollChanged(int i, int i2, int i3, int i4) {
        super.onScrollChanged(i, i2, i3, i4);
        if (this.mGlowingTop && canScrollVertically(-1) && i2 > i4) {
            onRecyclerViewScrolled();
        }
        if (this.mGlowingBottom && canScrollVertically(1) && i2 < i4) {
            onRecyclerViewScrolled();
        }
        if (!this.mGlowingTop && !canScrollVertically(-1) && i2 < i4) {
            pullGlows(this.mLastX, 0.0f, this.mLastY, this.mLastYVel / 20.0f);
            EdgeEffect edgeEffect = this.mTopGlow;
            if (edgeEffect != null) {
                edgeEffect.onAbsorb((int) (this.mLastYVel / 20.0f));
            }
        }
        if (!this.mGlowingBottom && !canScrollVertically(1) && i2 > i4) {
            pullGlows(this.mLastX, 0.0f, this.mLastY, this.mLastYVel / 20.0f);
            EdgeEffect edgeEffect2 = this.mBottomGlow;
            if (edgeEffect2 != null) {
                edgeEffect2.onAbsorb((int) (this.mLastYVel / 20.0f));
            }
       }
    }

    public ViewEdgeEffectFactory createViewEdgeEffectFactory() {
        return new ViewEdgeEffectFactory();
    }

    public class ViewEdgeEffectFactory extends SEdgeEffectFactory {
        private ViewEdgeEffectFactory() {
        }

        @Override
        public EdgeEffect createEdgeEffect(View view, int i) {
            if (i == 0 || i == 1) {
                SpringNestScrollView springNestScrollView = SpringNestScrollView.this;
                return new SpringEdgeEffect(springNestScrollView.getContext(), 0.3f);
            } else if (i != 2 && i != 3) {
                return super.createEdgeEffect(view, i);
            } else {
                SpringNestScrollView springNestScrollView2 = SpringNestScrollView.this;
                return new SpringEdgeEffect(springNestScrollView2.getContext(), -0.3f);
            }
        }
    }

    public class SpringEdgeEffect extends EdgeEffect {
        private final float mVelocityMultiplier;

        public boolean draw(Canvas canvas) {
            return false;
        }

        public SpringEdgeEffect(Context context, float f) {
            super(context);
            this.mVelocityMultiplier = f;
        }

        public void onAbsorb(int i) {
            SpringNestScrollView.this.finishScrollWithVelocity(((float) i) * this.mVelocityMultiplier);
            SpringNestScrollView.this.mDistance = 0.0f;
        }

        public void onPull(float f, float f2) {
            if (SpringNestScrollView.this.mSpring.isRunning()) {
                SpringNestScrollView.this.mSpring.cancel();
            }
            SpringNestScrollView.access$508(SpringNestScrollView.this);
            SpringNestScrollView.this.setActiveEdge(this);
            SpringNestScrollView.access$316(SpringNestScrollView.this, f * (this.mVelocityMultiplier / 3.0f));
            SpringNestScrollView springNestScrollView = SpringNestScrollView.this;
            springNestScrollView.setDampedScrollShift(springNestScrollView.mDistance * ((float) SpringNestScrollView.this.getHeight()));
        }

        public void onRelease() {
            SpringNestScrollView.this.mDistance = 0.0f;
            SpringNestScrollView.this.mPullCount = 0;
            SpringNestScrollView.this.finishScrollWithVelocity(0.0f);
        }
    }

    public static class SEdgeEffectFactory {
        public EdgeEffect createEdgeEffect(View view, int i) {
            return new EdgeEffect(view.getContext());
        }
    }

    public void setDampedScrollShift(float f) {
        if (f != this.mDampedScrollShift) {
            this.mDampedScrollShift = f;
            invalidate();
        }
    }

    private void setActiveEdge(SpringEdgeEffect springEdgeEffect) {
        SpringEdgeEffect springEdgeEffect2 = this.mActiveEdge;
        this.mActiveEdge = springEdgeEffect;
    }

    private void finishScrollWithVelocity(float f) {
        this.mSpring.setStartVelocity(f);
        this.mSpring.setStartValue(this.mDampedScrollShift);
        this.mSpring.start();
    }

    public void onRecyclerViewScrolled() {
        if (this.mPullCount != 1) {
            this.mDistance = 0.0f;
            this.mPullCount = 0;
            finishScrollWithVelocity(0.0f);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.mDampedScrollShift != 0.0f) {
            int save = canvas.save();
            canvas.translate(0.0f, this.mDampedScrollShift);
            super.draw(canvas);
            canvas.restoreToCount(save);
            return;
        }
        super.draw(canvas);
    }
}
