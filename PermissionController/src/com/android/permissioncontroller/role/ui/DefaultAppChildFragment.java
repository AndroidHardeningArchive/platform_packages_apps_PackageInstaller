/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.permissioncontroller.role.ui;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.modules.utils.build.SdkLevel;
import com.android.permissioncontroller.R;
import com.android.permissioncontroller.permission.utils.Utils;
import com.android.permissioncontroller.role.utils.PackageUtils;
import com.android.permissioncontroller.role.utils.RoleUiBehaviorUtils;
import com.android.permissioncontroller.role.utils.SettingsCompat;
import com.android.role.controller.model.Role;
import com.android.role.controller.model.Roles;

import java.util.List;
import java.util.Objects;

/**
 * Child fragment for a default app.
 * <p>
 * Must be added as a child fragment and its parent fragment must be a
 * {@link PreferenceFragmentCompat} that implements {@link Parent}.
 *
 * @param <PF> type of the parent fragment
 */
public class DefaultAppChildFragment<PF extends PreferenceFragmentCompat
        & DefaultAppChildFragment.Parent> extends Fragment
        implements DefaultAppConfirmationDialogFragment.Listener,
        Preference.OnPreferenceClickListener {

    private static final String PREFERENCE_KEY_NONE = DefaultAppChildFragment.class.getName()
            + ".preference.NONE";
    private static final String PREFERENCE_KEY_DESCRIPTION = DefaultAppChildFragment.class.getName()
            + ".preference.DESCRIPTION";
    private static final String PREFERENCE_KEY_OTHER_NFC_SERVICES =
            DefaultAppChildFragment.class.getName() + ".preference.OTHER_NFC_SERVICES";

    @NonNull
    private String mRoleName;
    @NonNull
    private UserHandle mUser;

    @NonNull
    private Role mRole;

    @NonNull
    private DefaultAppViewModel mViewModel;

    /**
     * Create a new instance of this fragment.
     *
     * @param roleName the name of the role for the default app
     * @param user the user for the default app
     *
     * @return a new instance of this fragment
     */
    @NonNull
    public static DefaultAppChildFragment newInstance(@NonNull String roleName,
            @NonNull UserHandle user) {
        DefaultAppChildFragment fragment = new DefaultAppChildFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Intent.EXTRA_ROLE_NAME, roleName);
        arguments.putParcelable(Intent.EXTRA_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        mRoleName = arguments.getString(Intent.EXTRA_ROLE_NAME);
        mUser = arguments.getParcelable(Intent.EXTRA_USER);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PF preferenceFragment = requirePreferenceFragment();
        Activity activity = requireActivity();
        mRole = Roles.get(activity).get(mRoleName);
        preferenceFragment.setTitle(getString(mRole.getLabelResource()));

        ViewModelProvider.Factory viewModelFactory = new DefaultAppViewModel.Factory(mRole, mUser,
                activity.getApplication());
        mViewModel = new ViewModelProvider(this, viewModelFactory).get(DefaultAppViewModel.class);
        mViewModel.getRoleLiveData().observe(this, this::onRoleChanged);
        mViewModel.getManageRoleHolderStateLiveData().observe(this,
                this::onManageRoleHolderStateChanged);
    }

    private void onRoleChanged(
            @NonNull List<Pair<ApplicationInfo, Boolean>> qualifyingApplications) {
        PF preferenceFragment = requirePreferenceFragment();
        PreferenceManager preferenceManager = preferenceFragment.getPreferenceManager();
        Context context = preferenceManager.getContext();

        PreferenceScreen preferenceScreen = preferenceFragment.getPreferenceScreen();
        Preference oldDescriptionPreference = null;
        ArrayMap<String, Preference> oldPreferences = new ArrayMap<>();
        if (preferenceScreen == null) {
            preferenceScreen = preferenceManager.createPreferenceScreen(context);
            preferenceFragment.setPreferenceScreen(preferenceScreen);
        } else {
            for (int i = preferenceScreen.getPreferenceCount() - 1; i >= 0; --i) {
                Preference preference = preferenceScreen.getPreference(i);

                preferenceScreen.removePreference(preference);
                preference.setOrder(Preference.DEFAULT_ORDER);
                oldPreferences.put(preference.getKey(), preference);
            }
        }

        if (mRole.shouldShowNone()) {
            Drawable icon = AppCompatResources.getDrawable(context, R.drawable.ic_remove_circle);
            String title = getString(R.string.default_app_none);
            boolean noHolderApplication = !hasHolderApplication(qualifyingApplications);
            addPreference(PREFERENCE_KEY_NONE, icon, title, noHolderApplication, null,
                    oldPreferences, preferenceScreen, context);
        }

        int qualifyingApplicationsSize = qualifyingApplications.size();
        for (int i = 0; i < qualifyingApplicationsSize; i++) {
            Pair<ApplicationInfo, Boolean> qualifyingApplication = qualifyingApplications.get(i);
            ApplicationInfo qualifyingApplicationInfo = qualifyingApplication.first;
            boolean isHolderApplication = qualifyingApplication.second;

            String key = qualifyingApplicationInfo.packageName;
            Drawable icon = Utils.getBadgedIcon(context, qualifyingApplicationInfo);
            String title = Utils.getFullAppLabel(qualifyingApplicationInfo, context);
            addPreference(key, icon, title, isHolderApplication, qualifyingApplicationInfo,
                    oldPreferences, preferenceScreen, context);
        }

        addNonPaymentNfcServicesPreference(preferenceScreen, oldPreferences, context);
        addDescriptionPreference(preferenceScreen, oldPreferences);

        preferenceFragment.onPreferenceScreenChanged();
    }

    private static boolean hasHolderApplication(
            @NonNull List<Pair<ApplicationInfo, Boolean>> qualifyingApplications) {
        int qualifyingApplicationsSize = qualifyingApplications.size();
        for (int i = 0; i < qualifyingApplicationsSize; i++) {
            Pair<ApplicationInfo, Boolean> qualifyingApplication = qualifyingApplications.get(i);
            boolean isHolderApplication = qualifyingApplication.second;

            if (isHolderApplication) {
                return true;
            }
        }
        return false;
    }

    private void addPreference(@NonNull String key, @NonNull Drawable icon,
            @NonNull CharSequence title, boolean checked, @Nullable ApplicationInfo applicationInfo,
            @NonNull ArrayMap<String, Preference> oldPreferences,
            @NonNull PreferenceScreen preferenceScreen, @NonNull Context context) {
        RoleApplicationPreference roleApplicationPreference =
                (RoleApplicationPreference) oldPreferences.get(key);
        TwoStatePreference preference;
        if (roleApplicationPreference == null) {
            roleApplicationPreference = requirePreferenceFragment().createApplicationPreference();
            preference = roleApplicationPreference.asTwoStatePreference();
            preference.setKey(key);
            preference.setIcon(icon);
            preference.setTitle(title);
            preference.setPersistent(false);
            preference.setOnPreferenceChangeListener((preference2, newValue) -> false);
            preference.setOnPreferenceClickListener(this);
        } else {
            preference = roleApplicationPreference.asTwoStatePreference();
        }

        preference.setChecked(checked);
        if (applicationInfo != null) {
            roleApplicationPreference.setRestrictionIntent(
                    mRole.getApplicationRestrictionIntentAsUser(applicationInfo, mUser, context));
            RoleUiBehaviorUtils.prepareApplicationPreferenceAsUser(mRole, roleApplicationPreference,
                    applicationInfo, mUser, context);
        }

        preferenceScreen.addPreference(preference);
    }

    private void onManageRoleHolderStateChanged(int state) {
        ManageRoleHolderStateLiveData liveData = mViewModel.getManageRoleHolderStateLiveData();
        switch (state) {
            case ManageRoleHolderStateLiveData.STATE_SUCCESS:
                String packageName = liveData.getLastPackageName();
                if (packageName != null) {
                    mRole.onHolderSelectedAsUser(packageName, liveData.getLastUser(),
                            requireContext());
                }
                liveData.resetState();
                break;
            case ManageRoleHolderStateLiveData.STATE_FAILURE:
                liveData.resetState();
                break;
        }
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        String key = preference.getKey();
        if (Objects.equals(key, PREFERENCE_KEY_NONE)) {
            mViewModel.setNoneDefaultApp();
        } else {
            String packageName = key;
            CharSequence confirmationMessage =
                    RoleUiBehaviorUtils.getConfirmationMessage(mRole, packageName,
                            requireContext());
            if (confirmationMessage != null) {
                DefaultAppConfirmationDialogFragment.show(packageName, confirmationMessage, this);
            } else {
                setDefaultApp(packageName);
            }
        }
        return true;
    }

    @Override
    public void setDefaultApp(@NonNull String packageName) {
        mViewModel.setDefaultApp(packageName);
    }

    private void addNonPaymentNfcServicesPreference(@NonNull PreferenceScreen preferenceScreen,
            @NonNull ArrayMap<String, Preference> oldPreferences, @NonNull Context context) {
        if (!(SdkLevel.isAtLeastV() && Objects.equals(mRoleName, RoleManager.ROLE_WALLET))) {
            return;
        }

        Intent intent = new Intent(SettingsCompat.ACTION_MANAGE_OTHER_NFC_SERVICES_SETTINGS);
        if (!PackageUtils.isIntentResolvedToSettings(intent, context)) {
            return;
        }

        Preference preference = oldPreferences.get(PREFERENCE_KEY_OTHER_NFC_SERVICES);
        if (preference == null) {
            preference = new Preference(context);
            preference.setKey(PREFERENCE_KEY_OTHER_NFC_SERVICES);
            preference.setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_nfc));
            preference.setTitle(context.getString(
                    R.string.default_payment_app_other_nfc_services));
            preference.setPersistent(false);
            preference.setOnPreferenceClickListener(preference2 -> {
                context.startActivity(intent);
                return true;
            });
        }

        preferenceScreen.addPreference(preference);
    }

    private void addDescriptionPreference(@NonNull PreferenceScreen preferenceScreen,
            @NonNull ArrayMap<String, Preference> oldPreferences) {
        Preference preference = oldPreferences.get(PREFERENCE_KEY_DESCRIPTION);
        if (preference == null) {
            preference = requirePreferenceFragment().createFooterPreference();
            preference.setKey(PREFERENCE_KEY_DESCRIPTION);
            preference.setSummary(mRole.getDescriptionResource());
        }

        preferenceScreen.addPreference(preference);
    }

    @NonNull
    private PF requirePreferenceFragment() {
        //noinspection unchecked
        return (PF) requireParentFragment();
    }

    /**
     * Interface that the parent fragment must implement.
     */
    public interface Parent {

        /**
         * Set the title of the current settings page.
         *
         * @param title the title of the current settings page
         */
        void setTitle(@NonNull CharSequence title);

        /**
         * Create a new preference for an application.
         *
         * @return a new preference for an application
         */
        @NonNull
        RoleApplicationPreference createApplicationPreference();

        /**
         * Create a new preference for the footer.
         *
         * @return a new preference for the footer
         */
        @NonNull
        Preference createFooterPreference();

        /**
         * Callback when changes have been made to the {@link PreferenceScreen} of the parent
         * {@link PreferenceFragmentCompat}.
         */
        void onPreferenceScreenChanged();
    }
}
