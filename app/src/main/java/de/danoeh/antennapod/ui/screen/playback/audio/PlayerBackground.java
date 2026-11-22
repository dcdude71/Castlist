package de.danoeh.antennapod.ui.screen.playback.audio;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.palette.graphics.Palette;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.appbar.MaterialToolbar;

import java.lang.ref.WeakReference;
import java.util.Objects;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.episodes.ImageResourceUtils;

import org.greenrobot.eventbus.EventBus;

/**
 * Castlist: modern player background and control colours taken from the episode artwork.
 * Styles: theme colour, gradient from cover, solid cover colour, blurred artwork (Android 12+,
 * gradient on older versions). The tint switch colours the play button and progress bar.
 */
final class PlayerBackground {
    static final String PREF_PLAYER_BACKGROUND = "prefPlayerBackground";
    static final String PREF_PLAYER_TINT = "prefPlayerTint";
    private static final String STYLE_THEME = "theme";
    private static final String STYLE_GRADIENT = "gradient";
    private static final String STYLE_SOLID = "solid";
    private static final String STYLE_BLUR = "blur";
    private static final int FADE_MS = 450;
    private static final int BITMAP_SIZE = 256;
    private static final double MIN_ACCENT_CONTRAST = 2.6;
    private static final double MIN_TEXT_CONTRAST = 4.5;
    @ColorInt
    private static final int DARK_TEXT = 0xFF1C1B22;

    private static WeakReference<PlayerBackground> current = new WeakReference<>(null);

    /** Colours applied to the player; a null field means "use the theme". */
    static final class Colors {
        @Nullable final Integer foreground;
        @ColorInt final int secondary;
        @Nullable final Integer accent;
        @ColorInt final int onAccent;
        @Nullable final Boolean lightStatusBar;

        Colors(@Nullable Integer foreground, int secondary, @Nullable Integer accent, int onAccent,
                @Nullable Boolean lightStatusBar) {
            this.foreground = foreground;
            this.secondary = secondary;
            this.accent = accent;
            this.onAccent = onAccent;
            this.lightStatusBar = lightStatusBar;
        }
    }

    private final AudioPlayerFragment fragment;
    private final View root;
    private final View backgroundView;
    private final ImageView backgroundImage;
    @Nullable
    private String loadedKey;
    @Nullable
    private Colors colors;
    @Nullable
    private CustomTarget<Bitmap> target;
    @Nullable
    private Boolean originalLightStatusBar;
    private float slideOffset;

    private PlayerBackground(@NonNull AudioPlayerFragment fragment, @NonNull View root) {
        this.fragment = fragment;
        this.root = root;
        backgroundView = root.findViewById(R.id.playerBackground);
        backgroundImage = root.findViewById(R.id.playerBackgroundImage);
    }

    static void setup(@NonNull AudioPlayerFragment fragment, @NonNull View root) {
        if (root.findViewById(R.id.playerBackground) == null) {
            return;
        }
        PlayerBackground background = new PlayerBackground(fragment, root);
        root.setTag(R.id.playerBackgroundController, background);
        current = new WeakReference<>(background);
    }

    @Nullable
    private static PlayerBackground get(@Nullable View root) {
        Object tag = root == null ? null : root.getTag(R.id.playerBackgroundController);
        return tag instanceof PlayerBackground ? (PlayerBackground) tag : null;
    }

    @Nullable
    static Colors currentColors() {
        PlayerBackground background = current.get();
        return background == null ? null : background.colors;
    }

    /** Re-colours views created after the colours were computed (cover page, playlist line). */
    static void reapplyCurrent() {
        PlayerBackground background = current.get();
        if (background != null && background.fragment.isAdded()) {
            background.applyColors();
        }
    }

    static void update(@Nullable View root, @Nullable FeedMedia media) {
        PlayerBackground background = get(root);
        if (background != null && media != null) {
            background.load(media);
        }
    }

    static void onSlide(@Nullable View root, float slideOffset) {
        PlayerBackground background = get(root);
        if (background != null) {
            background.slideOffset = slideOffset;
            background.updateStatusBar();
        }
    }

    static void onStop(@Nullable View root) {
        PlayerBackground background = get(root);
        if (background != null) {
            background.restoreStatusBar();
        }
    }

    private static String getStyle(Context context) {
        String style = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_PLAYER_BACKGROUND, STYLE_GRADIENT);
        if (STYLE_BLUR.equals(style) && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return STYLE_GRADIENT;
        }
        return style;
    }

    private static boolean isTintEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_PLAYER_TINT, true);
    }

    private void load(@NonNull FeedMedia media) {
        Context context = fragment.requireContext();
        String style = getStyle(context);
        boolean tint = isTintEnabled(context);
        String key = media.getImageLocation() + "|" + style + "|" + tint;
        if (key.equals(loadedKey)) {
            updateStatusBar();
            return;
        }
        loadedKey = key;
        if (STYLE_THEME.equals(style) && !tint) {
            show(style, null, null);
            return;
        }
        if (target != null) {
            Glide.with(fragment).clear(target);
        }
        target = new CustomTarget<Bitmap>(BITMAP_SIZE, BITMAP_SIZE) {
            @Override
            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                Palette.from(bitmap).generate(palette -> {
                    if (fragment.isAdded() && key.equals(loadedKey)) {
                        show(style, palette, bitmap);
                    }
                });
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                if (fragment.isAdded() && key.equals(loadedKey)) {
                    show(style, null, null);
                }
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        };
        Glide.with(fragment)
                .asBitmap()
                .load(media.getImageLocation())
                .error(Glide.with(fragment).asBitmap().load(ImageResourceUtils.getFallbackImageLocation(media)))
                .centerCrop()
                .into(target);
    }

    private void show(String style, @Nullable Palette palette, @Nullable Bitmap bitmap) {
        Context context = fragment.requireContext();
        boolean tint = isTintEnabled(context);
        @ColorInt int surface = ThemeUtils.getColorFromAttr(context, android.R.attr.colorBackground);
        @ColorInt int themeText = ThemeUtils.getColorFromAttr(context, android.R.attr.textColorPrimary);
        boolean dark = ColorUtils.calculateLuminance(surface) < 0.5;
        if (palette == null) {
            style = STYLE_THEME;
        }

        Drawable drawable = null;
        Integer foreground = null;
        Boolean lightStatusBar = null;
        @ColorInt int behindControls = surface;
        @ColorInt int dominant = palette == null ? surface : palette.getDominantColor(surface);
        @ColorInt int muted = palette == null ? surface : palette.getMutedColor(dominant);

        if (STYLE_GRADIENT.equals(style)) {
            @ColorInt int top = dark ? ColorUtils.blendARGB(muted, Color.BLACK, 0.25f)
                    : palette.getLightMutedColor(ColorUtils.blendARGB(dominant, Color.WHITE, 0.7f));
            top = ensureContrast(top, themeText, surface);
            drawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{
                    top, ColorUtils.blendARGB(top, surface, 0.45f), surface, surface});
            lightStatusBar = ColorUtils.calculateLuminance(top) > 0.5;
        } else if (STYLE_SOLID.equals(style)) {
            @ColorInt int solid = dark
                    ? palette.getDarkMutedColor(ColorUtils.blendARGB(dominant, Color.BLACK, 0.5f))
                    : palette.getLightMutedColor(ColorUtils.blendARGB(dominant, Color.WHITE, 0.7f));
            int text = ColorUtils.calculateLuminance(solid) > 0.18 ? DARK_TEXT : Color.WHITE;
            solid = ensureContrast(solid, text, text == Color.WHITE ? Color.BLACK : Color.WHITE);
            drawable = new ColorDrawable(solid);
            foreground = text;
            behindControls = solid;
            lightStatusBar = text == DARK_TEXT;
        } else if (STYLE_BLUR.equals(style) && bitmap != null) {
            ColorDrawable scrim = new ColorDrawable(ColorUtils.setAlphaComponent(surface, dark ? 140 : 158));
            drawable = scrim;
            behindControls = ColorUtils.blendARGB(surface, muted, 0.35f);
        }

        Integer accent = null;
        if (palette != null && tint) {
            @ColorInt int vibrant = dark
                    ? palette.getLightVibrantColor(palette.getVibrantColor(palette.getLightMutedColor(muted)))
                    : palette.getVibrantColor(palette.getDarkVibrantColor(muted));
            // Keep the cover's hue, but lighten (dark theme) or darken it until it stands out.
            @ColorInt int towards = ColorUtils.calculateLuminance(behindControls) < 0.5 ? Color.WHITE : Color.BLACK;
            for (int i = 0; i < 6 && ColorUtils.calculateContrast(vibrant, behindControls) < MIN_ACCENT_CONTRAST; i++) {
                vibrant = ColorUtils.blendARGB(vibrant, towards, 0.15f);
            }
            if (ColorUtils.calculateContrast(vibrant, behindControls) >= MIN_ACCENT_CONTRAST) {
                accent = vibrant;
            }
        }
        if (accent == null && foreground != null) {
            accent = foreground;
        }
        int onAccent = accent == null ? ThemeUtils.getColorFromAttr(context, R.attr.colorOnPrimary)
                : ColorUtils.calculateContrast(Color.WHITE, accent) >= MIN_ACCENT_CONTRAST ? Color.WHITE : DARK_TEXT;
        int secondary = foreground == null ? ThemeUtils.getColorFromAttr(context, android.R.attr.textColorSecondary)
                : ColorUtils.setAlphaComponent(foreground, 190);

        colors = foreground == null && accent == null ? null
                : new Colors(foreground, secondary, accent, onAccent, lightStatusBar);
        if (colors == null && lightStatusBar != null) {
            colors = new Colors(null, secondary, null, onAccent, lightStatusBar);
        }
        setBackground(drawable);
        setImage(STYLE_BLUR.equals(style) ? bitmap : null);
        applyColors();
        updateStatusBar();
    }

    /** Moves the background towards the other colour until text on it is readable. */
    @ColorInt
    private static int ensureContrast(@ColorInt int background, @ColorInt int text, @ColorInt int towards) {
        int result = background;
        for (int i = 0; i < 10 && ColorUtils.calculateContrast(text, result) < MIN_TEXT_CONTRAST; i++) {
            result = ColorUtils.blendARGB(result, towards, 0.15f);
        }
        return result;
    }

    private void setBackground(@Nullable Drawable drawable) {
        Drawable from = backgroundView.getBackground();
        if (from instanceof TransitionDrawable) {
            TransitionDrawable transition = (TransitionDrawable) from;
            from = transition.getDrawable(transition.getNumberOfLayers() - 1);
        }
        Drawable to = drawable != null ? drawable : new ColorDrawable(Color.TRANSPARENT);
        if (from == null) {
            backgroundView.setBackground(to);
            return;
        }
        TransitionDrawable transition = new TransitionDrawable(new Drawable[]{from, to});
        transition.setCrossFadeEnabled(true);
        backgroundView.setBackground(transition);
        transition.startTransition(FADE_MS);
    }

    private void setImage(@Nullable Bitmap bitmap) {
        if (bitmap == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            backgroundImage.setVisibility(View.GONE);
            backgroundImage.setImageDrawable(null);
            return;
        }
        float radius = 40 * root.getResources().getDisplayMetrics().density;
        backgroundImage.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
        backgroundImage.setScaleX(1.3f);
        backgroundImage.setScaleY(1.3f);
        Drawable to = new BitmapDrawable(root.getResources(), bitmap);
        Drawable from = backgroundImage.getVisibility() == View.VISIBLE ? backgroundImage.getDrawable() : null;
        if (from instanceof TransitionDrawable) {
            TransitionDrawable transition = (TransitionDrawable) from;
            from = transition.getDrawable(transition.getNumberOfLayers() - 1);
        }
        backgroundImage.setVisibility(View.VISIBLE);
        if (from == null) {
            backgroundImage.setAlpha(0f);
            backgroundImage.setImageDrawable(to);
            backgroundImage.animate().alpha(1f).setDuration(FADE_MS).start();
        } else {
            TransitionDrawable transition = new TransitionDrawable(new Drawable[]{from, to});
            transition.setCrossFadeEnabled(true);
            backgroundImage.setImageDrawable(transition);
            transition.startTransition(FADE_MS);
        }
    }

    private void applyColors() {
        Context context = root.getContext();
        // Look up inside the full player only: the mini player reuses some ids (butPlay).
        View content = root.findViewById(R.id.playerContent);
        Colors c = colors;
        Integer foreground = c == null ? null : c.foreground;
        recolorTree(content.findViewById(R.id.toolbar), foreground);
        recolorTree(content.findViewById(R.id.playtime_layout), foreground);
        recolorTree(content.findViewById(R.id.cover_fragment), foreground);
        recolorToolbar(content.findViewById(R.id.toolbar), foreground);

        TextView playlistLine = content.findViewById(R.id.playlistLine);
        if (playlistLine != null) {
            playlistLine.setTextColor(c != null && c.accent != null ? c.accent
                    : ThemeUtils.getColorFromAttr(context, androidx.appcompat.R.attr.colorPrimary));
        }

        ChapterSeekBar seekBar = content.findViewById(R.id.sbPosition);
        if (seekBar != null) {
            Integer accent = c == null ? null : c.accent;
            seekBar.setTrackColors(foreground == null ? null : ColorUtils.setAlphaComponent(foreground, 70), accent);
            seekBar.setThumbTintList(accent == null ? null : ColorStateList.valueOf(accent));
        }

        View play = content.findViewById(R.id.butPlay);
        if (play instanceof ImageView) {
            if (c == null || c.accent == null) {
                play.setBackgroundResource(R.drawable.bg_player_play_button);
            } else {
                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);
                circle.setColor(c.accent);
                play.setBackground(new RippleDrawable(ColorStateList.valueOf(ThemeUtils.getColorFromAttr(context,
                        androidx.appcompat.R.attr.colorControlHighlight)), circle, null));
            }
            ImageViewCompat.setImageTintList((ImageView) play, ColorStateList.valueOf(c == null || c.accent == null
                    ? ThemeUtils.getColorFromAttr(context, R.attr.colorOnPrimary) : c.onAccent));
        }

        SleepTimerUpdatedEvent sleep = EventBus.getDefault().getStickyEvent(SleepTimerUpdatedEvent.class);
        if (sleep != null) {
            PlayerStyle.onSleepTimerUpdate(root, sleep);
        }
    }

    private static void recolorTree(@Nullable View view, @Nullable Integer foreground) {
        if (view == null || view.getId() == R.id.imgvCover || view.getId() == R.id.butPlay
                || view.getId() == R.id.playlistLine || view instanceof ProgressBar) {
            return;
        }
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            Object original = text.getTag(R.id.playerOriginalColor);
            if (foreground == null) {
                if (original instanceof ColorStateList) {
                    text.setTextColor((ColorStateList) original);
                    text.setTag(R.id.playerOriginalColor, null);
                }
            } else {
                if (!(original instanceof ColorStateList)) {
                    original = text.getTextColors();
                    text.setTag(R.id.playerOriginalColor, original);
                }
                int alpha = Color.alpha(((ColorStateList) original).getDefaultColor());
                text.setTextColor(ColorUtils.setAlphaComponent(foreground, Math.max(alpha, 170)));
            }
        } else if (view instanceof ImageView) {
            ImageView image = (ImageView) view;
            Object original = image.getTag(R.id.playerOriginalColor);
            if (foreground == null) {
                if (original instanceof OriginalFilter) {
                    image.setColorFilter(((OriginalFilter) original).filter);
                    image.setTag(R.id.playerOriginalColor, null);
                }
            } else {
                if (!(original instanceof OriginalFilter)) {
                    image.setTag(R.id.playerOriginalColor, new OriginalFilter(image.getColorFilter()));
                }
                image.setColorFilter(foreground, PorterDuff.Mode.SRC_IN);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                recolorTree(group.getChildAt(i), foreground);
            }
        }
    }

    private static void recolorToolbar(@Nullable MaterialToolbar toolbar, @Nullable Integer foreground) {
        if (toolbar == null) {
            return;
        }
        int color = foreground != null ? foreground
                : ThemeUtils.getColorFromAttr(toolbar.getContext(), androidx.appcompat.R.attr.colorControlNormal);
        toolbar.setNavigationIconTint(color);
        Drawable overflow = toolbar.getOverflowIcon();
        if (overflow != null) {
            overflow.setTint(color);
        }
        ColorStateList tint = foreground == null ? null : ColorStateList.valueOf(foreground);
        Menu menu = toolbar.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            MenuItemCompat.setIconTintList(item, tint);
        }
    }

    private void updateStatusBar() {
        Window window = fragment.getActivity() == null ? null : fragment.getActivity().getWindow();
        if (window == null) {
            return;
        }
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        Boolean wanted = colors == null || slideOffset < 0.9f ? null : colors.lightStatusBar;
        if (wanted == null) {
            restoreStatusBar();
            return;
        }
        if (originalLightStatusBar == null) {
            originalLightStatusBar = controller.isAppearanceLightStatusBars();
        }
        if (!Objects.equals(controller.isAppearanceLightStatusBars(), wanted)) {
            controller.setAppearanceLightStatusBars(wanted);
        }
    }

    private void restoreStatusBar() {
        if (originalLightStatusBar == null || fragment.getActivity() == null) {
            return;
        }
        Window window = fragment.getActivity().getWindow();
        WindowCompat.getInsetsController(window, window.getDecorView())
                .setAppearanceLightStatusBars(originalLightStatusBar);
        originalLightStatusBar = null;
    }

    private static final class OriginalFilter {
        @Nullable
        final ColorFilter filter;

        OriginalFilter(@Nullable ColorFilter filter) {
            this.filter = filter;
        }
    }
}
