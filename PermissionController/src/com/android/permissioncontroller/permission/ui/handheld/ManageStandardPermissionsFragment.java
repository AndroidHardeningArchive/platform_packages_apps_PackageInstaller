/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.permissioncontroller.permission.ui.handheld;

import static androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory;

import static com.android.permissioncontroller.Constants.EXTRA_SESSION_ID;
import static com.android.permissioncontroller.Constants.INVALID_SESSION_ID;
import static com.android.permissioncontroller.permission.ui.handheld.UtilsKt.pressBack;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.modules.utils.build.SdkLevel;
import com.android.permissioncontroller.R;
import com.android.permissioncontroller.permission.ui.UnusedAppsFragment;
import com.android.permissioncontroller.permission.ui.model.ManageStandardPermissionsViewModel;
import com.android.permissioncontroller.permission.utils.StringUtils;
import com.android.permissioncontroller.permission.utils.Utils;

/**
 * Fragment that allows the user to manage standard permissions.
 */
public final class ManageStandardPermissionsFragment extends ManagePermissionsFragment {
    private static final String EXTRA_PREFS_KEY = "extra_prefs_key";
    private static final String AUTO_REVOKE_KEY = "auto_revoke_key";
    private static final String LOG_TAG = ManageStandardPermissionsFragment.class.getSimpleName();
    private ManageStandardPermissionsViewModel mViewModel;

    /**
     * Create a bundle with the arguments needed by this fragment
     *
     * @param sessionId The current session ID
     * @return A bundle with all of the args placed
     */
    public static Bundle createArgs(long sessionId) {
        Bundle arguments = new Bundle();
        arguments.putLong(EXTRA_SESSION_ID, sessionId);
        return arguments;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Application application = getActivity().getApplication();
        mViewModel = new ViewModelProvider(this, AndroidViewModelFactory.getInstance(application))
                .get(ManageStandardPermissionsViewModel.class);
        mPermissionGroups = mViewModel.getUiDataLiveData().getValue();

        mViewModel.getUiDataLiveData().observe(this, permissionGroups -> {
            // Once we have loaded data for the first time, further loads should be staggered,
            // for performance reasons.
            mViewModel.getUiDataLiveData().setLoadStaggered(true);
            if (permissionGroups != null) {
                mPermissionGroups = permissionGroups;
                updatePermissionsUi();
            } else {
                Log.e(LOG_TAG, "ViewModel returned null data, exiting");
                getActivity().finishAfterTransition();
            }

            // If we've loaded all LiveDatas, no need to prioritize loading any particular one
            if (!mViewModel.getUiDataLiveData().isStale()) {
                mViewModel.getUiDataLiveData().setFirstLoadGroup(null);
            }
        });

        mViewModel.getNumCustomPermGroups().observe(this, permNames -> updatePermissionsUi());
        mViewModel.getNumAutoRevoked().observe(this, show -> updatePermissionsUi());
    }

    @Override
    public void onStart() {
        super.onStart();

        getActivity().setTitle(com.android.permissioncontroller.R.string.app_permission_manager);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                pressBack(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected PreferenceScreen updatePermissionsUi() {
        PreferenceScreen screen = super.updatePermissionsUi();
        if (screen == null) {
            return null;
        }

        // Check if we need an additional permissions preference
        int numExtraPermissions = 0;
        if (mViewModel.getNumCustomPermGroups().getValue() != null) {
            numExtraPermissions = mViewModel.getNumCustomPermGroups().getValue();
        }

        Preference additionalPermissionsPreference = screen.findPreference(EXTRA_PREFS_KEY);
        if (numExtraPermissions == 0) {
            if (additionalPermissionsPreference != null) {
                screen.removePreference(additionalPermissionsPreference);
            }
        } else {
            if (additionalPermissionsPreference == null) {
                additionalPermissionsPreference = new FixedSizeIconPreference(
                        getPreferenceManager().getContext());
                additionalPermissionsPreference.setKey(EXTRA_PREFS_KEY);
                additionalPermissionsPreference.setIcon(Utils.applyTint(getActivity(),
                        R.drawable.ic_more_items,
                        android.R.attr.colorControlNormal));
                additionalPermissionsPreference.setTitle(R.string.additional_permissions);
                additionalPermissionsPreference.setOnPreferenceClickListener(preference -> {
                    mViewModel.showCustomPermissions(this,
                            ManageCustomPermissionsFragment.createArgs(
                                    getArguments().getLong(EXTRA_SESSION_ID)));
                    return true;
                });

                screen.addPreference(additionalPermissionsPreference);
            }

            additionalPermissionsPreference.setSummary(StringUtils.getIcuPluralsString(getContext(),
                    R.string.additional_permissions_more, numExtraPermissions));
        }

        Integer numAutoRevoked = mViewModel.getNumAutoRevoked().getValue();

        Preference autoRevokePreference = screen.findPreference(AUTO_REVOKE_KEY);
        if (numAutoRevoked != null && numAutoRevoked != 0) {
            if (autoRevokePreference == null) {
                if (SdkLevel.isAtLeastS()) {
                    autoRevokePreference = createAutoRevokeFooterPreferenceForSPlus();
                } else {
                    autoRevokePreference = createAutoRevokeFooterPreferenceForR();
                }
                autoRevokePreference.setKey(AUTO_REVOKE_KEY);
                screen.addPreference(autoRevokePreference);
            }
        } else if (numAutoRevoked != null && autoRevokePreference != null) {
            screen.removePreference(autoRevokePreference);
        }

        return screen;
    }

    private PermissionFooterPreference createAutoRevokeFooterPreferenceForSPlus() {
        PermissionFooterPreference autoRevokePreference =
                new PermissionFooterPreference(getContext());
        autoRevokePreference.setSummary(R.string.auto_revoked_apps_page_summary);
        autoRevokePreference.setLearnMoreAction(view -> {
            mViewModel.showAutoRevoke(this, UnusedAppsFragment.createArgs(
                            getArguments().getLong(EXTRA_SESSION_ID, INVALID_SESSION_ID)));
            });
        return autoRevokePreference;
    }

    private Preference createAutoRevokeFooterPreferenceForR() {
        Preference autoRevokePreference = new FixedSizeIconPreference(
                getPreferenceManager().getContext(), true, true);
        autoRevokePreference.setOrder(-1);
        autoRevokePreference.setSingleLineTitle(false);
        autoRevokePreference.setIcon(R.drawable.ic_info_outline_accent);
        autoRevokePreference.setTitle(R.string.auto_revoke_permission_notification_title);
        autoRevokePreference.setSummary(R.string.auto_revoke_setting_subtitle);
        autoRevokePreference.setOnPreferenceClickListener(preference -> {
            mViewModel.showAutoRevoke(this, UnusedAppsFragment.createArgs(
                    getArguments().getLong(EXTRA_SESSION_ID, INVALID_SESSION_ID)));
            return true;
        });
        return autoRevokePreference;
    }

    @Override
    public void showPermissionApps(String permissionGroupName) {
        // If we return to this page within a reasonable time, prioritize loading data from the
        // permission group whose page we are going to, as that is group most likely to have changed
        mViewModel.getUiDataLiveData().setFirstLoadGroup(permissionGroupName);
        mViewModel.showPermissionApps(this, PermissionAppsFragment.createArgs(
                permissionGroupName, getArguments().getLong(EXTRA_SESSION_ID)));
    }
}
