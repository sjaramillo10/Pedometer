package dev.sjaramillo.nemi.ui;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static dev.sjaramillo.nemi.ui.AppComponents.*;
import static dev.sjaramillo.nemi.ui.StatsSummary.metricPair;
import static dev.sjaramillo.nemi.ui.StatsSummary.stepMetric;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.icu.number.Notation;
import android.icu.number.NumberFormatter;
import android.icu.number.Precision;
import android.icu.text.ConstrainedFieldPosition;
import android.icu.text.DateFormat;
import android.icu.text.DateIntervalFormat;
import android.icu.util.DateInterval;
import android.icu.util.TimeZone;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import dev.sjaramillo.nemi.R;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.StringJoiner;

final class StepCharts {
    private StepCharts() {}

    static View todayChart(Context context, HomeUiState state, Palette colors) {
        var today = state.today();
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        var numbers = NumberFormat.getIntegerInstance(locale);
        var summary = column(context);
        var count = label(context, today != null ? numbers.format(today.steps()) : "—",
                colors.text(), 38, 700, true);
        autoSize(count, 10, 38);
        item(summary, count);
        if (today == null) {
            Integer message = state.todayEmptyMessage();
            if (message != null) {
                item(summary, label(context, context.getString(message), colors.secondary(), 13, 400, true));
            }
        } else {
            item(summary, label(context, context.getString(R.string.steps_unit), colors.secondary(), 14, 400, true));
            if (today.fromBackup()) {
                item(summary, label(context, context.getString(R.string.from_backup), colors.secondary(), 12, 400, true));
            }
            if (state.reachedGoal(today.steps())) {
                item(summary, label(context, context.getString(R.string.goal_reached), colors.accent(), 12, 400, true));
            }
        }

        var chart = new FrameLayout(context) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                int diameter = Math.min(MeasureSpec.getSize(widthSpec), dp(context, 214));
                summary.getLayoutParams().width = (int) (diameter * 0.72f);
                int square = MeasureSpec.makeMeasureSpec(diameter, MeasureSpec.EXACTLY);
                super.onMeasure(square, square);
            }
        };
        chart.addView(new GoalRing(context, colors, state.progress()),
                new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        chart.addView(summary, new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER));
        var card = card(context, R.string.today, colors);
        var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.topMargin = dp(context, 12);
        params.bottomMargin = dp(context, 20);
        card.addView(chart, params);
        return card;
    }

    static View weekChart(Context context, HomeUiState state, Palette colors,
            Runnable onPreviousWeek, Runnable onNextWeek) {
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        var week = state.week();
        long maximum = week.scaleMaximum(state.goal());
        var numbers = NumberFormat.getIntegerInstance(locale);
        String unit = context.getString(R.string.steps_unit);
        var card = card(context, R.string.weekly_rhythm, colors);
        var navigation = row(context);
        navigation.addView(weekNavigation(context, R.string.previous_week, false, colors, true, onPreviousWeek));
        LocalDate start = week.days().get(0).date();
        LocalDate end = week.days().get(week.days().size() - 1).date();
        var range = label(context, displayWeekRange(start, end, state.date().getYear(), locale),
                colors.secondary(), 14, 400, true);
        range.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        weighted(navigation, range);
        navigation.addView(weekNavigation(context, R.string.next_week, true, colors,
                week.range().canGoNext(), onNextWeek));
        item(card, navigation);

        if (week.isEmpty()) {
            var empty = label(context, context.getString(R.string.no_steps_for_week), colors.text());
            pad(empty, 16, 0);
            item(card, empty, 12);
        }

        var scroller = new HorizontalScrollView(context);
        scroller.setFillViewport(true);
        scroller.setHorizontalScrollBarEnabled(false);
        var bars = row(context);
        bars.setMinimumWidth(dp(context, 294));
        for (WeekDay day : week.days()) {
            weighted(bars, weekBar(context, day, state.date(), locale, maximum, state.goal(), colors));
        }
        scroller.addView(bars, new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        item(card, scroller, 12);
        item(card, divider(context, colors), 12, dp(context, 1));
        item(card, metricPair(context, colors,
                stepMetric(context, R.string.week_total, numbers.format(week.total()), unit, colors),
                stepMetric(context, R.string.week_average, numbers.format(week.average()), unit, colors)), 12);
        return card;
    }

    private static View weekNavigation(Context context, int label, boolean forward, Palette colors,
            boolean enabled, Runnable onClick) {
        var button = action(context, "", colors, onClick);
        button.setContentDescription(context.getString(label));
        button.setEnabled(enabled);
        button.setForeground(new Drawable() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Path path = new Path();

            {
                paint.setColor(enabled ? colors.accent() : withAlpha(colors.text(), 0.38f));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(context, 2));
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
            }

            @Override public void draw(Canvas canvas) {
                float x = getBounds().exactCenterX();
                float y = getBounds().exactCenterY();
                boolean pointsRight = forward == (button.getLayoutDirection() != View.LAYOUT_DIRECTION_RTL);
                float offset = dp(context, 2) * (pointsRight ? 1 : -1);
                path.rewind();
                path.moveTo(x - offset, y - dp(context, 5));
                path.lineTo(x + offset, y);
                path.lineTo(x - offset, y + dp(context, 5));
                canvas.drawPath(path, paint);
            }

            @Override public void setAlpha(int alpha) {
                paint.setAlpha(alpha);
            }

            @Override public void setColorFilter(ColorFilter colorFilter) {
                paint.setColorFilter(colorFilter);
            }

            @Override @SuppressWarnings("deprecation")
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        });
        button.setForegroundGravity(Gravity.FILL);
        return button;
    }

    private static View weekBar(Context context, WeekDay day, LocalDate today, Locale locale,
            long maximum, int goal, Palette colors) {
        Long steps = day.total() == null ? null : day.total().steps();
        boolean reached = steps != null && steps >= goal;
        String date = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                .withLocale(locale).format(day.date());
        String total;
        if (steps != null) {
            total = context.getString(R.string.step_count, NumberFormat.getIntegerInstance(locale).format(steps));
        } else {
            total = context.getString(day.date().isAfter(today) ? R.string.future_day : R.string.no_steps_for_day);
        }
        var description = new StringJoiner(", ").add(date).add(total);
        if (reached) {
            description.add(context.getString(R.string.goal_reached));
        }
        if (day.total() != null && day.total().fromBackup()) {
            description.add(context.getString(R.string.from_backup));
        }
        var bar = column(context);
        bar.setScreenReaderFocusable(true);
        bar.setContentDescription(description.toString());
        pad(bar, 2, 4);
        item(bar, new WeekPlot(context, colors, steps, maximum, reached), 0, dp(context, 104));
        String compact = steps == null ? "—" : NumberFormatter.withLocale(locale)
                .notation(Notation.compactShort()).precision(Precision.maxFraction(1))
                .format(steps).toString();
        String weekday = day.date().getDayOfWeek().getDisplayName(TextStyle.SHORT_STANDALONE, locale);
        boolean current = day.date().equals(today);
        item(bar, chartLabel(context, compact, colors.text(), 400), 4);
        item(bar, chartLabel(context, weekday, current ? colors.accent() : colors.text(),
                current ? 600 : 400), 4);
        return bar;
    }

    private static TextView chartLabel(Context context, String text, int color, int weight) {
        var label = label(context, text, color, 13, weight, true);
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        autoSize(label, 9, 13);
        return label;
    }

    private static void autoSize(TextView view, int min, int max) {
        view.setMaxLines(1);
        view.setAutoSizeTextTypeUniformWithConfiguration(min, max, 1, TypedValue.COMPLEX_UNIT_SP);
    }

    private static String displayWeekRange(LocalDate start, LocalDate end, int currentYear, Locale locale) {
        boolean showYear = start.getYear() != currentYear || end.getYear() != currentYear;
        var format = DateIntervalFormat.getInstance(showYear ? "yyMMMd" : "MMMd", locale);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        var interval = new DateInterval(start.toEpochDay() * 86_400_000L, end.toEpochDay() * 86_400_000L);
        var formatted = format.formatToValue(interval);
        if (!showYear || !"en".equals(locale.getLanguage())) {
            return formatted.toString();
        }
        var result = new StringBuilder(formatted.toString());
        var year = new ConstrainedFieldPosition();
        year.constrainField(DateFormat.Field.YEAR);
        int added = 0;
        while (formatted.nextPosition(year)) {
            int from = year.getStart();
            int to = year.getLimit();
            String shortYear = formatted.subSequence(Math.max(from, to - 2), to).toString();
            result.replace(from + added, to + added, "’" + shortYear);
            added += 1 + shortYear.length() - (to - from);
        }
        return result.toString();
    }

    private static final class GoalRing extends View {
        private final Palette colors;
        private final float progress;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        GoalRing(Context context, Palette colors, float progress) {
            super(context);
            this.colors = colors;
            this.progress = progress;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(context, 20));
            paint.setStrokeCap(Paint.Cap.ROUND);
            setContentDescription(context.getString(R.string.daily_goal_progress));
        }

        @Override protected void onDraw(Canvas canvas) {
            float inset = paint.getStrokeWidth() / 2;
            paint.setColor(colors.track());
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 2f - inset, paint);
            if (progress > 0) {
                paint.setColor(colors.accent());
                canvas.drawArc(inset, inset, getWidth() - inset, getHeight() - inset,
                        -90, 360 * progress, false, paint);
            }
        }

        @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName("android.widget.ProgressBar");
            info.setRangeInfo(new AccessibilityNodeInfo.RangeInfo(
                    AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_PERCENT, 0, 1, progress));
        }
    }

    private static final class WeekPlot extends View {
        private final Palette colors;
        private final Long steps;
        private final long maximum;
        private final boolean reached;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path check = new Path();

        WeekPlot(Context context, Palette colors, Long steps, long maximum, boolean reached) {
            super(context);
            this.colors = colors;
            this.steps = steps;
            this.maximum = maximum;
            this.reached = reached;
        }

        @Override protected void onDraw(Canvas canvas) {
            Context context = getContext();
            float width = getWidth() * 0.55f;
            float left = (getWidth() - width) / 2;
            float right = left + width;
            float bottom = getHeight();
            paint.setColor(reached ? colors.accent() : colors.bar());
            paint.setStyle(Paint.Style.FILL);
            if (steps == null) {
                canvas.drawRect(left, bottom - dp(context, 1), right, bottom, paint);
                return;
            }
            float height = Math.max(dp(context, 2), (float) ((getHeight() - dp(context, 16)) * ((double) steps / maximum)));
            float top = bottom - height;
            float radius = Math.min(dp(context, 5), height / 2);
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint);
            if (reached) {
                float center = getWidth() / 2f;
                float y = top - dp(context, 8);
                check.rewind();
                check.moveTo(center - dp(context, 4), y);
                check.lineTo(center - dp(context, 1), y + dp(context, 3));
                check.lineTo(center + dp(context, 5), y - dp(context, 3));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(context, 2));
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                canvas.drawPath(check, paint);
            }
        }
    }
}
