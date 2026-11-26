package dev.sjaramillo.nemi.ui;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static dev.sjaramillo.nemi.ui.AppComponents.*;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;

final class ScreenLayout {
    final LinearLayout content;
    final ScrollView scroll;
    final FrameLayout root;

    ScreenLayout(Context context, Palette colors, int title, int icon,
            int actionLabel, boolean actionAtStart, Runnable onAction) {
        content = column(context);
        pad(content, 16);
        scroll = new HeaderScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.addView(content, new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        var toolbar = new BackdropHeader(context, scroll, content, colors);
        toolbar.setMinimumHeight(dp(context, 56));
        var heading = label(context, context.getString(title), colors.text(), 24, 600, true);
        heading.setAccessibilityHeading(true);
        var headingParams = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.CENTER);
        headingParams.setMarginStart(dp(context, 48));
        headingParams.setMarginEnd(dp(context, 48));
        toolbar.addView(heading, headingParams);

        var button = new ImageButton(context);
        button.setImageResource(icon);
        button.setImageTintList(ColorStateList.valueOf(colors.accent()));
        button.setContentDescription(context.getString(actionLabel));
        button.setBackground(ripple(context, colors.accent(), 24, null));
        pad(button, 12);
        button.setOnClickListener(view -> onAction.run());
        toolbar.addView(button, new FrameLayout.LayoutParams(dp(context, 48), dp(context, 48),
                Gravity.CENTER_VERTICAL | (actionAtStart ? Gravity.START : Gravity.END)));

        root = new FrameLayout(context) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                measureChildWithMargins(toolbar, widthSpec, 0, heightSpec, 0);
                if (scroll.getPaddingTop() != toolbar.getMeasuredHeight()) {
                    scroll.setPadding(scroll.getPaddingLeft(), toolbar.getMeasuredHeight(),
                            scroll.getPaddingRight(), scroll.getPaddingBottom());
                }
                super.onMeasure(widthSpec, heightSpec);
            }

            @Override public void onDescendantInvalidated(View child, View target) {
                super.onDescendantInvalidated(child, target);
                if (child == scroll) {
                    toolbar.invalidate();
                }
            }
        };
        root.setBackgroundColor(colors.background());
        root.addView(scroll, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        root.addView(toolbar, new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        scroll.setOnScrollChangeListener((view, x, y, oldX, oldY) -> toolbar.invalidate());
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            var safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            int gutter = dp(context, 16);
            toolbar.setMinimumHeight(safe.top + dp(context, 56));
            toolbar.setPadding(safe.left + gutter, safe.top, safe.right + gutter, 0);
            scroll.setPadding(safe.left, scroll.getPaddingTop(), safe.right, safe.bottom);
            return insets;
        });
    }

    void restoreScroll(int position) {
        if (position > 0) {
            scroll.post(() -> scroll.scrollTo(0, position));
        }
    }

    static int scrollDeltaForPinnedHeader(int targetTop, int targetBottom, int viewportTop,
            int viewportBottom) {
        boolean oversized = targetBottom - targetTop > viewportBottom - viewportTop;
        if (targetBottom > viewportBottom && targetTop > viewportTop) {
            return oversized ? targetTop - viewportTop : targetBottom - viewportBottom;
        }
        if (targetTop < viewportTop && targetBottom < viewportBottom) {
            return oversized ? targetBottom - viewportBottom : targetTop - viewportTop;
        }
        return 0;
    }

    private static final class HeaderScrollView extends ScrollView {
        HeaderScrollView(Context context) {
            super(context);
        }

        @Override protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
            return scrollDeltaForPinnedHeader(rect.top, rect.bottom, getScrollY() + getPaddingTop(),
                    getScrollY() + getHeight() - getPaddingBottom());
        }
    }

    private static final class BackdropHeader extends FrameLayout {
        private final ScrollView scroll;
        private final View content;
        private final Palette colors;
        private final int overscan;
        private final int tint;
        private final RenderNode backdrop = new RenderNode("Header backdrop");

        BackdropHeader(Context context, ScrollView scroll, View content, Palette colors) {
            super(context);
            this.scroll = scroll;
            this.content = content;
            this.colors = colors;
            overscan = dp(context, 42);
            tint = withAlpha(colors.background(), 0.50f);
            float radius = dp(context, 14);
            backdrop.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
            setWillNotDraw(false);
        }

        @Override protected void onDraw(Canvas canvas) {
            if (canvas.isHardwareAccelerated() && getWidth() > 0 && getHeight() > 0) {
                // Include the area below the toolbar in the blur.
                backdrop.setPosition(0, 0, getWidth(), getHeight() + overscan);
                Canvas recording = backdrop.beginRecording();
                try {
                    recording.drawColor(colors.background());
                    recording.translate(content.getLeft(), content.getTop() - scroll.getScrollY());
                    content.draw(recording);
                } finally {
                    backdrop.endRecording();
                }
                int checkpoint = canvas.save();
                canvas.clipRect(0, 0, getWidth(), getHeight());
                canvas.drawRenderNode(backdrop);
                canvas.restoreToCount(checkpoint);
            }
            canvas.drawColor(tint);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            return true;
        }

        @Override protected void onDetachedFromWindow() {
            backdrop.discardDisplayList();
            super.onDetachedFromWindow();
        }
    }
}
