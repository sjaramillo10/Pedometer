package dev.sjaramillo.nemi.ui;

import static dev.sjaramillo.nemi.ui.AppComponents.*;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import dev.sjaramillo.nemi.R;
import dev.sjaramillo.nemi.data.DailySteps;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

final class StatsSummary {
    private StatsSummary() {}

    static View create(Context context, DailySteps record, Long lifetimeSteps, Palette colors) {
        var locale = context.getResources().getConfiguration().getLocales().get(0);
        var numbers = NumberFormat.getIntegerInstance(locale);
        String recordDate = record == null ? context.getString(R.string.no_recorded_steps)
                : DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
                        .format(LocalDate.ofEpochDay(record.day()));
        var card = card(context, R.string.your_journey, colors);
        item(card, metricPair(context, colors,
                stepMetric(context, R.string.best_day, record == null ? "—" : numbers.format(record.steps()),
                        recordDate, colors),
                stepMetric(context, R.string.lifetime_steps,
                        lifetimeSteps == null ? "—" : numbers.format(lifetimeSteps),
                        context.getString(R.string.steps_unit), colors)));
        item(card, divider(context, colors), 16, dp(context, 1));
        item(card, label(context, context.getString(R.string.journey_slogan), colors.secondary(), 13, 400, true), 12);
        return card;
    }

    static View stepMetric(Context context, int title, String value, String detail, Palette colors) {
        var metric = column(context);
        metric.setScreenReaderFocusable(true);
        item(metric, label(context, context.getString(title), colors.secondary(), 14, 400, true));
        item(metric, label(context, value, colors.text(), 22, 600, true), 4);
        item(metric, label(context, detail, colors.secondary(), 13, 400, true), 4);
        return metric;
    }

    static View metricPair(Context context, Palette colors, View first, View second) {
        var config = context.getResources().getConfiguration();
        if (config.screenWidthDp < 352 || config.fontScale >= 1.5f) {
            var pair = column(context);
            item(pair, first);
            item(pair, second, 12);
            return pair;
        }
        var pair = row(context);
        pair.setGravity(Gravity.CENTER_VERTICAL);
        weighted(pair, first);
        var params = new LinearLayout.LayoutParams(dp(context, 1), dp(context, 56));
        params.setMarginStart(dp(context, 12));
        params.setMarginEnd(dp(context, 12));
        pair.addView(divider(context, colors), params);
        weighted(pair, second);
        return pair;
    }
}
