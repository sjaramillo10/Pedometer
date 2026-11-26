package dev.sjaramillo.nemi.ui;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import dev.sjaramillo.nemi.R;

final class AppComponents {
    private AppComponents() {}

    static int withAlpha(int color, float alpha) {
        return (color & 0xFFFFFF) | (Math.round(255 * alpha) << 24);
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable rounded(Context context, int color, int radius) {
        var shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(context, radius));
        return shape;
    }

    static void pad(View view, int inset) {
        pad(view, inset, inset);
    }

    static void pad(View view, int horizontal, int vertical) {
        int x = dp(view.getContext(), horizontal);
        int y = dp(view.getContext(), vertical);
        view.setPadding(x, y, x, y);
    }

    static LinearLayout column(Context context) {
        var view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    static LinearLayout row(Context context) {
        var view = new LinearLayout(context);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    static void item(LinearLayout parent, View view) {
        item(parent, view, 0);
    }

    static void item(LinearLayout parent, View view, int top) {
        item(parent, view, top, WRAP_CONTENT);
    }

    static void item(LinearLayout parent, View view, int top, int height) {
        var params = new LinearLayout.LayoutParams(MATCH_PARENT, height);
        params.topMargin = dp(parent.getContext(), top);
        parent.addView(view, params);
    }

    static void weighted(LinearLayout parent, View view) {
        parent.addView(view, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));
    }

    static TextView label(Context context, CharSequence value, int color) {
        return label(context, value, color, 16);
    }

    static TextView label(Context context, CharSequence value, int color, int size) {
        return label(context, value, color, size, 400, false);
    }

    static TextView label(Context context, CharSequence value, int color, int size, int weight,
            boolean centered) {
        var view = new TextView(context);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setTypeface(Typeface.create(Typeface.DEFAULT, weight, false));
        view.setIncludeFontPadding(false);
        if (centered) {
            view.setGravity(Gravity.CENTER);
        }
        return view;
    }

    static RippleDrawable ripple(Context context, int color, int radius, Integer fill) {
        return new RippleDrawable(ColorStateList.valueOf(withAlpha(color, 0.12f)),
                fill == null ? null : rounded(context, fill, radius),
                rounded(context, Color.WHITE, radius));
    }

    static Button action(Context context, String title, Palette colors, Runnable onClick) {
        var button = new Button(context);
        button.setText(title);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTypeface(Typeface.create(Typeface.DEFAULT, 600, false));
        button.setIncludeFontPadding(false);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        int minSize = dp(context, 48);
        button.setMinWidth(minSize);
        button.setMinHeight(minSize);
        button.setMinimumWidth(minSize);
        button.setMinimumHeight(minSize);
        pad(button, 8, 4);
        button.setStateListAnimator(null);
        button.setBackground(ripple(context, colors.accent(), 8, null));
        button.setTextColor(new ColorStateList(
                new int[][] {{-android.R.attr.state_enabled}, {}},
                new int[] {colors.secondary(), colors.accent()}));
        button.setOnClickListener(view -> onClick.run());
        return button;
    }

    static LinearLayout card(Context context, int title, Palette colors) {
        var card = column(context);
        card.setBackground(rounded(context, colors.surface(), 24));
        pad(card, 20);
        var heading = label(context, context.getString(title), colors.text(), 18, 600, true);
        heading.setAccessibilityHeading(true);
        item(card, heading);
        item(card, new View(context), 0, dp(context, 12));
        return card;
    }

    static View divider(Context context, Palette colors) {
        var view = new View(context);
        view.setBackgroundColor(colors.divider());
        return view;
    }

    static Dialog showDialog(Context context, Palette colors, int title, String message,
            Runnable onDismiss) {
        var dialog = new Dialog(context);
        var content = column(context);
        content.setBackground(rounded(context, colors.surface(), 20));
        pad(content, 20);
        var heading = label(context, context.getString(title), colors.text(), 24, 600, false);
        heading.setAccessibilityHeading(true);
        item(content, heading);
        item(content, label(context, message, colors.text()), 8);
        item(content, action(context, context.getString(R.string.close), colors, dialog::dismiss), 8);
        var scroll = new ScrollView(context);
        scroll.addView(content);
        dialog.setContentView(scroll);
        dialog.setOnDismissListener(ignored -> onDismiss.run());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
        return dialog;
    }
}

record Palette(int background, int surface, int text, int accent, int track, int bar) {
    static final Palette LIGHT =
            new Palette(0xFFF8F7F2, Color.WHITE, 0xFF202722, 0xFF3D5948, 0xFFECF2ED, 0xFF49728C);
    static final Palette DARK =
            new Palette(0xFF111914, 0xFF1C2920, 0xFFE6F0E7, 0xFF9BDAAA, 0xFF35483A, 0xFF8EB4CD);

    int secondary() {
        return AppComponents.withAlpha(text, 0.72f);
    }

    int divider() {
        return AppComponents.withAlpha(text, 0.10f);
    }
}
