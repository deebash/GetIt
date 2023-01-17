/*
 * Copyright Deebash D. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.deebash.getit;

import android.app.Activity;
import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

/** Simple and robust library to get runtime permissions necessary for your android app. */
public class GetIt {

    private final Activity context;
    ArrayList<String> permissionsGranted;
    ArrayList<String> permissionsRejected;
    ArrayList<String> permissionsDeniedPermanently;
    Application.ActivityLifecycleCallbacks callbacks;
    private OnPermissionsResult onPermissionsResult;
    private OnPermissionResult onPermissionResult;
    private String[] permissionsRequired;

    /**
     * Entry point constructor receiving activity for context
     *
     * @param activity - Activity
     */
    public GetIt(@NonNull Activity activity) {
        this.context = activity;
        attachCallbacks();
    }

    private void attachCallbacks() {
        callbacks =
                new Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {}

                    @Override
                    public void onActivityStarted(@NonNull Activity activity) {}

                    @Override
                    public void onActivityResumed(@NonNull Activity activity) {
                        checkPermissions();
                        publishResult();
                        removeCallbacks();
                    }

                    @Override
                    public void onActivityPaused(@NonNull Activity activity) {}

                    @Override
                    public void onActivityStopped(@NonNull Activity activity) {
                        removeCallbacks();
                    }

                    @Override
                    public void onActivitySaveInstanceState(
                            @NonNull Activity activity, @NonNull Bundle bundle) {}

                    @Override
                    public void onActivityDestroyed(@NonNull Activity activity) {}
                };
        context.getApplication().registerActivityLifecycleCallbacks(callbacks);
    }

    private void removeCallbacks() {
        context.getApplication().unregisterActivityLifecycleCallbacks(callbacks);
    }

    /**
     * Add the required permissions needed here as String[]
     *
     * @param permissions Permissions in String[]
     */
    public GetIt withPermissions(String[] permissions) {
        permissionsRequired = permissions;
        return this;
    }

    /**
     * Add the required permission needed here as String
     *
     * @param permission Permission in String
     */
    public GetIt withPermission(String permission) {
        permissionsRequired = new String[] {permission};
        return this;
    }

    /**
     * Attach listener {@code OnPermissionResult} to receive permission result.
     *
     * @param receiver {@code OnPermissionResult}
     */
    public GetIt setOnPermissionReceiver(OnPermissionResult receiver) {
        onPermissionResult = receiver;
        return this;
    }

    /**
     * Attach listener {@code OnPermissionsResult} to receive permissions result.
     *
     * @param receiver {@code OnPermissionsResult}
     */
    public GetIt setOnPermissionsReceiver(OnPermissionsResult receiver) {
        onPermissionsResult = receiver;
        return this;
    }

    /** Requests Permission to the user */
    public void get() {
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermissions();
            if (permissionsRequired.length == permissionsGranted.size()) {
                publishResult();
            } else {
                ActivityCompat.requestPermissions(context, permissionsRequired, 255);
            }
        } else {
            if (onPermissionsResult != null) onPermissionsResult.onGrantedAll();
            if (onPermissionResult != null) onPermissionResult.onGranted();
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            permissionsGranted = new ArrayList<>();
            permissionsRejected = new ArrayList<>();
            permissionsDeniedPermanently = new ArrayList<>();
            for (String permission : permissionsRequired) {
                if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted.add(permission);
                } else {
                    permissionsRejected.add(permission);
                    if (context.shouldShowRequestPermissionRationale(permission)) {
                        permissionsDeniedPermanently.add(permission);
                    }
                }
            }
        }
    }

    /** Publishes results to the attached listener */
    private void publishResult() {
        if (permissionsRequired.length == permissionsGranted.size()) {
            if (onPermissionsResult != null) onPermissionsResult.onGrantedAll();
            if (onPermissionResult != null) onPermissionResult.onGranted();
        } else if (permissionsRequired.length > permissionsGranted.size()
                && permissionsDeniedPermanently.size() > 0) {
            if (onPermissionsResult != null)
                onPermissionsResult.onPermissionsDeniedPermanently(
                        getStringArray(permissionsDeniedPermanently));
            if (onPermissionResult != null) onPermissionResult.onPermissionDeniedPermanently();
        } else if (permissionsRequired.length > permissionsGranted.size()
                && permissionsGranted.size() > 0) {
            if (onPermissionsResult != null)
                onPermissionsResult.onGrantedPartial(
                        getStringArray(permissionsGranted), getStringArray(permissionsRejected));
        } else {
            if (onPermissionsResult != null) onPermissionsResult.onRejectedAll();
            if (onPermissionResult != null) onPermissionResult.onRejected();
        }
    }

    private void processResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            int result = grantResults[i];
            if (result == PackageManager.PERMISSION_GRANTED) {
                if (permissionsRejected.contains(permission)) {
                    permissionsRejected.remove(permission);
                    permissionsGranted.add(permission);
                }
            }
        }
        publishResult();
    }

    private String[] getStringArray(ArrayList<String> arrayList) {
        String[] result = new String[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            result[i] = arrayList.get(i);
        }
        return result;
    }

    public interface OnPermissionsResult {
        void onGrantedAll();

        void onGrantedPartial(String[] grantedPermissions, String[] rejectedPermissions);

        void onRejectedAll();

        void onPermissionsDeniedPermanently(String[] deniedPermissions);
    }

    public interface OnPermissionResult {
        void onGranted();

        void onRejected();

        void onPermissionDeniedPermanently();
    }
}

