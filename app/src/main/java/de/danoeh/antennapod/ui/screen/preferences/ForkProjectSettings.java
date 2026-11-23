package de.danoeh.antennapod.ui.screen.preferences;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.common.IntentUtils;
import de.danoeh.antennapod.ui.preferences.screen.about.ForkInfo;

final class ForkProjectSettings {
    private ForkProjectSettings() {
    }

    static void setup(@NonNull PreferenceFragmentCompat fragment, @NonNull String projectKey,
                      @NonNull String documentationKey, @NonNull String forumKey,
                      @NonNull String contributeKey) {
        PreferenceGroup project = fragment.findPreference(projectKey);
        if (project == null) {
            return;
        }
        Preference castlist = new Preference(fragment.requireContext());
        castlist.setIcon(R.drawable.ic_playlist_play);
        castlist.setTitle(R.string.castlist_settings_title);
        castlist.setSummary(R.string.castlist_settings_summary);
        castlist.setOrder(project.getOrder());
        project.setOrder(project.getOrder() + 1);
        castlist.setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) fragment.requireActivity()).openScreen(R.xml.preferences_castlist);
            return true;
        });
        fragment.getPreferenceScreen().addPreference(castlist);
        Preference notice = new Preference(fragment.requireContext());
        notice.setIcon(R.drawable.ic_info);
        notice.setTitle(R.string.fork_notice_title);
        notice.setSummary(R.string.fork_notice_summary);
        notice.setOrder(-2);
        notice.setOnPreferenceClickListener(preference -> {
            IntentUtils.openInBrowser(fragment.requireContext(), ForkInfo.REPOSITORY_URL);
            return true;
        });
        project.addPreference(notice);

        Preference guide = new Preference(fragment.requireContext());
        guide.setIcon(R.drawable.ic_questionmark);
        guide.setTitle(R.string.fork_guide_title);
        guide.setSummary(R.string.fork_guide_summary);
        guide.setOrder(-1);
        guide.setOnPreferenceClickListener(preference -> {
            IntentUtils.openInBrowser(fragment.requireContext(), ForkInfo.GUIDE_URL);
            return true;
        });
        project.addPreference(guide);

        Preference documentation = fragment.findPreference(documentationKey);
        if (documentation != null) {
            documentation.setTitle(R.string.fork_documentation_title);
            documentation.setSummary(R.string.fork_documentation_summary);
        }

        Preference forum = fragment.findPreference(forumKey);
        if (forum != null) {
            forum.setVisible(false);
        }
        Preference contribute = fragment.findPreference(contributeKey);
        if (contribute != null) {
            contribute.setOnPreferenceClickListener(preference -> {
                IntentUtils.openInBrowser(fragment.requireContext(), ForkInfo.REPOSITORY_URL);
                return true;
            });
        }
    }
}
