package de.danoeh.antennapod.ui.screen.playback.audio;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.CoverFragmentBinding;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.storage.preferences.SleepTimerType;
import de.danoeh.antennapod.ui.common.Converter;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.screen.playback.SleepTimerDialog;

public final class PlayerStyle {
    public static final String PREF_PLAYER_STYLE = "prefPlayerStyle";
    private static final String STYLE_CLASSIC = "classic";
    private static final int MAX_COVER_SIZE_DP = 260;
    private static final int COVER_CORNER_RADIUS_DP = 16;
    private static final float TRACK_SCALE = 2f;

    private PlayerStyle() {
    }

    public static boolean isModern(@NonNull Context context) {
        return !STYLE_CLASSIC.equals(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_PLAYER_STYLE, "modern"));
    }

    @LayoutRes
    static int getPlayerLayout(boolean modern) {
        return modern ? R.layout.audioplayer_fragment_modern : R.layout.audioplayer_fragment;
    }

    static void setupPlayer(@NonNull AudioPlayerFragment fragment, @NonNull View root) {
        View sleepButton = root.findViewById(R.id.butSleepTimer);
        if (sleepButton == null) {
            return;
        }
        ChapterSeekBar seekBar = root.findViewById(R.id.sbPosition);
        seekBar.setTrackScale(TRACK_SCALE);
        sleepButton.setOnClickListener(v ->
                new SleepTimerDialog().show(fragment.getChildFragmentManager(), "SleepTimerDialog"));
        root.findViewById(R.id.butShownotes).setOnClickListener(v ->
                fragment.scrollToPage(AudioPlayerFragment.POS_DESCRIPTION, true));
        hideToolbarSleepTimer(root.findViewById(R.id.toolbar));
    }

    static void onSleepTimerUpdate(@Nullable View root, @NonNull SleepTimerUpdatedEvent event) {
        if (root == null) {
            return;
        }
        ImageButton sleepButton = root.findViewById(R.id.butSleepTimer);
        TextView label = root.findViewById(R.id.txtvSleepTimer);
        if (sleepButton == null || label == null) {
            return;
        }
        hideToolbarSleepTimer(root.findViewById(R.id.toolbar));
        Context context = root.getContext();
        boolean active = !event.isCancelled() && !event.isOver();
        PlayerBackground.Colors colors = PlayerBackground.currentColors();
        int accent = colors != null && colors.accent != null ? colors.accent
                : ThemeUtils.getColorFromAttr(context, androidx.appcompat.R.attr.colorPrimary);
        int color = active ? accent : colors != null && colors.foreground != null ? colors.secondary
                : ThemeUtils.getColorFromAttr(context, android.R.attr.textColorSecondary);
        label.setTextColor(color);
        int idle = colors != null && colors.foreground != null ? colors.foreground
                : ThemeUtils.getColorFromAttr(context, androidx.appcompat.R.attr.colorControlNormal);
        sleepButton.setColorFilter(active ? color : idle);
        if (!active) {
            label.setText(R.string.player_sleep_label);
        } else if (SleepTimerPreferences.getSleepTimerType() == SleepTimerType.EPISODES) {
            int episodes = (int) event.getDisplayTimeLeft();
            label.setText(context.getResources().getQuantityString(R.plurals.num_episodes, episodes, episodes));
        } else {
            String time = Converter.getDurationStringLong((int) event.getDisplayTimeLeft());
            label.setText(time.startsWith("00:") ? time.substring(3) : time);
        }
    }

    private static void hideToolbarSleepTimer(@Nullable MaterialToolbar toolbar) {
        if (toolbar == null) {
            return;
        }
        toolbar.getMenu().findItem(R.id.set_sleeptimer_item).setVisible(false);
        toolbar.getMenu().findItem(R.id.disable_sleeptimer_item).setVisible(false);
    }

    static void setupLayout(@NonNull AudioPlayerFragment fragment, @NonNull View root) {
        if (root.findViewById(R.id.descriptionPane) == null) {
            return;
        }
        PlayerLayout layout = new PlayerLayout(fragment, root);
        root.setTag(R.id.playerBody, layout);
        layout.apply();
    }

    static void updateLayout(@NonNull AudioPlayerFragment fragment) {
        PlayerLayout layout = getLayout(fragment);
        if (layout != null) {
            layout.apply();
        }
    }

    static void onStart(@NonNull AudioPlayerFragment fragment) {
        PlayerLayout layout = getLayout(fragment);
        if (layout != null) {
            layout.start();
        }
    }

    static void onStop(@NonNull AudioPlayerFragment fragment) {
        PlayerLayout layout = getLayout(fragment);
        if (layout != null) {
            layout.stop();
        }
    }

    @Nullable
    private static PlayerLayout getLayout(@NonNull AudioPlayerFragment fragment) {
        View root = fragment.getView();
        if (root == null) {
            return null;
        }
        Object layout = root.getTag(R.id.playerBody);
        return layout instanceof PlayerLayout ? (PlayerLayout) layout : null;
    }

    static void styleCover(@NonNull CoverFragmentBinding binding) {
        Context context = binding.getRoot().getContext();
        ConstraintLayout.LayoutParams coverParams =
                (ConstraintLayout.LayoutParams) binding.imgvCover.getLayoutParams();
        coverParams.matchConstraintMaxWidth = dp(context, MAX_COVER_SIZE_DP);
        coverParams.matchConstraintMaxHeight = dp(context, MAX_COVER_SIZE_DP);
        binding.imgvCover.setLayoutParams(coverParams);
        final float radius = dp(context, COVER_CORNER_RADIUS_DP);
        binding.imgvCover.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        binding.imgvCover.setClipToOutline(true);

        ViewGroup textContainer = binding.coverFragmentTextContainer;
        int padding = dp(context, 16);
        textContainer.setPadding(padding, 0, padding, 0);
        textContainer.removeView(binding.txtvEpisodeTitle);
        textContainer.addView(binding.txtvEpisodeTitle, 0);

        binding.txtvEpisodeTitle.setGravity(Gravity.CENTER_HORIZONTAL);
        binding.txtvEpisodeTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        binding.txtvEpisodeTitle.setTypeface(binding.txtvEpisodeTitle.getTypeface(), Typeface.BOLD);
        ((LinearLayout.LayoutParams) binding.txtvEpisodeTitle.getLayoutParams()).bottomMargin = 0;

        binding.txtvPodcastTitle.setGravity(Gravity.CENTER_HORIZONTAL);
        binding.txtvPodcastTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        ((LinearLayout.LayoutParams) binding.txtvPodcastTitle.getLayoutParams()).bottomMargin = dp(context, 8);

        binding.openDescription.setVisibility(View.GONE);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
