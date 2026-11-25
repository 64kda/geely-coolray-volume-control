package rubberbigpepper.VolumeControl.redesign;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.audiofx.Equalizer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import rubberbigpepper.CommonCtrl.ZoomView;

/** Layout and drawing for the existing effect controls and listeners. */
public final class EffectsPanel {
    private static final int LEFT = 0x7f070032, RIGHT = 0x7f070033, TOP = 0x7f070034;
    private static final int BOTTOM = 0x7f070066, TITLE = 0x7f070067;
    private static final int SLIDER = 0x7f070068, VALUE = 0x7f070069;
    private static final int INK = 0xfff5f5f5, MUTED = 0xffc4c4c4, GREEN = 0xff1ed760;
    private static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);

    private EffectsPanel() {}

    public static void mixer(LinearLayout panel) {
        style(panel, true, -1);
    }

    public static void equalizer(LinearLayout panel, Object effect) {
        int zero = -1;
        if (effect instanceof Equalizer) {
            try {
                short[] range = ((Equalizer) effect).getBandLevelRange();
                if (range[0] <= 0 && range[1] >= 0) zero = -range[0];
            } catch (RuntimeException unavailable) {
                // Keep showing the effect controls when the range cannot be read.
            }
        }
        style(panel, false, zero);
    }

    private static void style(LinearLayout panel, boolean mixer, int zero) {
        if (!(panel.getParent() instanceof ViewGroup)) return;
        ViewGroup root = (ViewGroup) panel.getParent();
        root.setBackgroundDrawable(surface(root, 0xff1f1f1f, 16));
        int inset = dp(root, 10);
        root.setPadding(inset, inset, inset, inset);

        styleTab(root, 0x7f07002d, !mixer);
        styleTab(root, 0x7f07002e, mixer);
        styleTab(root, 0x7f07002f, false);
        TextView close = (TextView) root.findViewById(0x7f07002f);
        if (close != null) close.setContentDescription("Закрыть");

        View options = root.findViewById(0x7f070030);
        if (options != null) {
            options.setBackgroundColor(0xff1f1f1f);
            hideEdges(options);
            TextView toggle = (TextView) options.findViewById(0x7f070031);
            if (toggle != null) toggle.setTextColor(INK);
        }
        panel.setOrientation(mixer ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        for (int i = 0; i < panel.getChildCount(); i++) {
            View group = panel.getChildAt(i);
            View control = group.findViewById(SLIDER);
            if (!(control instanceof ZoomView) || !(control.getParent() instanceof LinearLayout)) continue;
            ZoomView slider = (ZoomView) control;
            LinearLayout row = (LinearLayout) slider.getParent();
            TextView title = (TextView) row.findViewById(TITLE);
            TextView value = (TextView) row.findViewById(VALUE);
            if (title == null || value == null) continue;

            group.setBackgroundDrawable(surface(group, 0xff282828, 8));
            hideEdges(group);
            LinearLayout.LayoutParams item = new LinearLayout.LayoutParams(
                    mixer ? -1 : 0, mixer ? 0 : -1, 1f);
            item.setMargins(0, dp(group, 4), mixer ? 0 : dp(group, 3), 0);
            group.setLayoutParams(item);
            row.setOrientation(mixer ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(row, 5), 0, dp(row, 5), 0);
            title.setTextColor(INK);
            value.setTextColor(MUTED);
            title.setTextSize(mixer ? 14 : 11);
            value.setTextSize(13);
            title.setMaxLines(2);
            title.setLayoutParams(new LinearLayout.LayoutParams(mixer ? 0 : -1,
                    mixer ? -1 : dp(row, 40), mixer ? 3f : 0f));
            if (mixer) title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            slider.setLayoutParams(new LinearLayout.LayoutParams(mixer ? 0 : -1,
                    mixer ? -1 : 0, mixer ? 4f : 1f));
            value.setLayoutParams(new LinearLayout.LayoutParams(mixer ? dp(row, 40) : -1,
                    mixer ? -1 : dp(row, 30)));
            slider.setContentDescription(title.getText());
            Object type = slider.getTag();
            slider.setTag(SLIDER, new Track(mixer, !mixer && type instanceof Integer ? zero : -1));
        }
    }

    private static void hideEdges(View view) {
        for (int id : new int[]{LEFT, RIGHT, TOP, BOTTOM}) {
            View edge = view.findViewById(id);
            if (edge != null) edge.setVisibility(View.INVISIBLE);
        }
    }

    private static void styleTab(View root, int id, boolean selected) {
        TextView tab = (TextView) root.findViewById(id);
        if (tab == null) return;
        tab.setSelected(selected);
        tab.setTypeface(null, Typeface.BOLD);
        int color = selected ? GREEN : 0xff303030;
        if (tab.getBackground() instanceof ColorDrawable) {
            int configured = ((ColorDrawable) tab.getBackground()).getColor();
            int alpha = Color.alpha(configured);
            color = Color.rgb(
                    blend(Color.red(configured), alpha), blend(Color.green(configured), alpha),
                    blend(Color.blue(configured), alpha));
        }
        double luminance = 0.2126 * linear(Color.red(color)) + 0.7152 * linear(Color.green(color))
                + 0.0722 * linear(Color.blue(color));
        tab.setTextColor(luminance > 0.179 ? Color.BLACK : Color.WHITE);
        tab.setBackgroundDrawable(surface(tab, color, 8));
    }

    private static int blend(int component, int alpha) {
        return (component * alpha + 31 * (255 - alpha) + 127) / 255;
    }

    private static double linear(int component) {
        double value = component / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static GradientDrawable surface(View view, int color, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(view, radius));
        return background;
    }

    public static boolean draw(Canvas canvas, ZoomView view) {
        Object style = view.getTag(SLIDER);
        if (!(style instanceof Track)) return false;
        Track track = (Track) style;
        float w = view.getWidth(), h = view.getHeight();
        int max = Math.max(1, view.getMax());
        float progress = Math.max(0, Math.min(1, view.getProgress() / (float) max));
        float radius = dp(view, 5);
        float x = w / 2f, y = h / 2f;
        PAINT.setStrokeCap(Paint.Cap.ROUND);
        PAINT.setStrokeWidth(dp(view, 4));
        PAINT.setColor(0xff626262);
        if (track.horizontal) {
            float position = Math.max(radius, Math.min(w - radius, progress * w));
            canvas.drawLine(radius, y, w - radius, y, PAINT);
            PAINT.setColor(GREEN);
            canvas.drawLine(radius, y, position, y, PAINT);
            x = position;
        } else {
            float position = Math.max(radius, Math.min(h - radius, (1f - progress) * h));
            canvas.drawLine(x, radius, x, h - radius, PAINT);
            float baseline = h - radius;
            if (track.zero >= 0) {
                baseline = Math.max(radius, Math.min(h - radius, (1f - track.zero / (float) max) * h));
                PAINT.setStrokeWidth(dp(view, 1));
                PAINT.setColor(MUTED);
                canvas.drawLine(0, baseline, w, baseline, PAINT);
                PAINT.setStrokeWidth(dp(view, 4));
            }
            PAINT.setColor(GREEN);
            if (position != baseline) canvas.drawLine(x, baseline, x, position, PAINT);
            y = position;
        }
        PAINT.setColor(INK);
        canvas.drawCircle(x, y, radius, PAINT);
        return true;
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    private static final class Track {
        final boolean horizontal;
        final int zero;
        Track(boolean horizontal, int zero) {
            this.horizontal = horizontal;
            this.zero = zero;
        }
    }
}
