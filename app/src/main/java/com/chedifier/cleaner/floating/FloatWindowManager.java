/*
 * Copyright (C) 2016 Facishare Technology Co., Ltd. All Rights Reserved.
 */
package com.chedifier.cleaner.floating;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import com.chedifier.cleaner.floating.rom.HuaweiUtils;
import com.chedifier.cleaner.floating.rom.MeizuUtils;
import com.chedifier.cleaner.floating.rom.MiuiUtils;
import com.chedifier.cleaner.floating.rom.OppoUtils;
import com.chedifier.cleaner.floating.rom.QikuUtils;
import com.chedifier.cleaner.floating.rom.RomUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Description:
 *
 * @author zhaozp
 * @since 2016-10-17
 */

public class FloatWindowManager {
    private static final String TAG = "FloatWindowManager";

    private static volatile FloatWindowManager instance;

    public boolean isWindowDismiss = true;
    private WindowManager windowManager = null;
    private WindowManager.LayoutParams mParams = null;
    private Dialog dialog;
    private String pkgName = "";

    public static FloatWindowManager getInstance() {
        if (instance == null) {
            synchronized (FloatWindowManager.class) {
                if (instance == null) {
                    instance = new FloatWindowManager();
                }
            }
        }
        return instance;
    }

    public boolean isWindowShow(String pkgName) {
        return this.pkgName.equals(pkgName) && !isWindowDismiss;
    }

    public void applyOrShowFloatWindow(Context context, String pkgName) {
        if (checkPermission(context)) {
            showWindow(context, pkgName);
        } else {
            applyPermission(context);
        }
    }

    public boolean checkPermission(Context context) {
        //6.0 版本之后由于 google 增加了对悬浮窗权限的管理，所以方式就统一了
        if (Build.VERSION.SDK_INT < 23) {
            if (RomUtils.checkIsMiuiRom()) {
                return miuiPermissionCheck(context);
            } else if (RomUtils.checkIsMeizuRom()) {
                return meizuPermissionCheck(context);
            } else if (RomUtils.checkIsHuaweiRom()) {
                return huaweiPermissionCheck(context);
            } else if (RomUtils.checkIs360Rom()) {
                return qikuPermissionCheck(context);
            } else if (RomUtils.checkIsOppoRom()) {
                return oppoROMPermissionCheck(context);
            }
        }
        return commonROMPermissionCheck(context);
    }

    private boolean huaweiPermissionCheck(Context context) {
        return HuaweiUtils.checkFloatWindowPermission(context);
    }

    private boolean miuiPermissionCheck(Context context) {
        return MiuiUtils.checkFloatWindowPermission(context);
    }

    private boolean meizuPermissionCheck(Context context) {
        return MeizuUtils.checkFloatWindowPermission(context);
    }

    private boolean qikuPermissionCheck(Context context) {
        return QikuUtils.checkFloatWindowPermission(context);
    }

    private boolean oppoROMPermissionCheck(Context context) {
        return OppoUtils.checkFloatWindowPermission(context);
    }

    private boolean commonROMPermissionCheck(Context context) {
        //最新发现魅族6.0的系统这种方式不好用，天杀的，只有你是奇葩，没办法，单独适配一下
        if (RomUtils.checkIsMeizuRom()) {
            return meizuPermissionCheck(context);
        } else {
            Boolean result = true;
            if (Build.VERSION.SDK_INT >= 23) {
                try {
                    Class clazz = Settings.class;
                    Method canDrawOverlays = clazz.getDeclaredMethod("canDrawOverlays", Context.class);
                    result = (Boolean) canDrawOverlays.invoke(null, context);
                } catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
            return result;
        }
    }

    public void applyPermission(Context context) {
        if (Build.VERSION.SDK_INT < 23) {
            if (RomUtils.checkIsMiuiRom()) {
                miuiROMPermissionApply(context);
            } else if (RomUtils.checkIsMeizuRom()) {
                meizuROMPermissionApply(context);
            } else if (RomUtils.checkIsHuaweiRom()) {
                huaweiROMPermissionApply(context);
            } else if (RomUtils.checkIs360Rom()) {
                ROM360PermissionApply(context);
            } else if (RomUtils.checkIsOppoRom()) {
                oppoROMPermissionApply(context);
            }
        } else {
            commonROMPermissionApply(context);
        }
    }

    private void ROM360PermissionApply(final Context context) {
        showConfirmDialog(context, new OnConfirmResult() {
            @Override
            public void confirmResult(boolean confirm) {
                if (confirm) {
                    QikuUtils.applyPermission(context);
                } else {
                    Log.e(TAG, "ROM:360, user manually refuse OVERLAY_PERMISSION");
                }
            }
        });
    }

    private void huaweiROMPermissionApply(final Context context) {
        showConfirmDialog(context, new OnConfirmResult() {
            @Override
            public void confirmResult(boolean confirm) {
                if (confirm) {
                    HuaweiUtils.applyPermission(context);
                } else {
                    Log.e(TAG, "ROM:huawei, user manually refuse OVERLAY_PERMISSION");
                }
            }
        });
    }

    private void meizuROMPermissionApply(final Context context) {
        showConfirmDialog(context, new OnConfirmResult() {
            @Override
            public void confirmResult(boolean confirm) {
                if (confirm) {
                    MeizuUtils.applyPermission(context);
                } else {
                    Log.e(TAG, "ROM:meizu, user manually refuse OVERLAY_PERMISSION");
                }
            }
        });
    }

    private void miuiROMPermissionApply(final Context context) {
        showConfirmDialog(context, new OnConfirmResult() {
            @Override
            public void confirmResult(boolean confirm) {
                if (confirm) {
                    MiuiUtils.applyMiuiPermission(context);
                } else {
                    Log.e(TAG, "ROM:miui, user manually refuse OVERLAY_PERMISSION");
                }
            }
        });
    }

    private void oppoROMPermissionApply(final Context context) {
        showConfirmDialog(context, new OnConfirmResult() {
            @Override
            public void confirmResult(boolean confirm) {
                if (confirm) {
                    OppoUtils.applyOppoPermission(context);
                } else {
                    Log.e(TAG, "ROM:miui, user manually refuse OVERLAY_PERMISSION");
                }
            }
        });
    }

    /**
     * 通用 rom 权限申请
     */
    private void commonROMPermissionApply(final Context context) {
        //这里也一样，魅族系统需要单独适配
        if (RomUtils.checkIsMeizuRom()) {
            meizuROMPermissionApply(context);
        } else {
            if (Build.VERSION.SDK_INT >= 23) {
                showConfirmDialog(context, new OnConfirmResult() {
                    @Override
                    public void confirmResult(boolean confirm) {
                        if (confirm) {
                            try {
                                commonROMPermissionApplyInternal(context);
                            } catch (Exception e) {
                                Log.e(TAG, Log.getStackTraceString(e));
                            }
                        } else {
                            Log.d(TAG, "user manually refuse OVERLAY_PERMISSION");
                            //需要做统计效果
                        }
                    }
                });
            }
        }
    }

    public static void commonROMPermissionApplyInternal(Context context) throws NoSuchFieldException, IllegalAccessException {
        Class clazz = Settings.class;
        Field field = clazz.getDeclaredField("ACTION_MANAGE_OVERLAY_PERMISSION");

        Intent intent = new Intent(field.get(null).toString());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }

    private void showConfirmDialog(Context context, OnConfirmResult result) {
        showConfirmDialog(context, "开启系统悬浮窗", result);
    }

    private void showConfirmDialog(Context context, String message, final OnConfirmResult result) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new AlertDialog.Builder(context).setCancelable(true).setTitle("")
                .setMessage(message)
                .setPositiveButton("开启",
                        (dialog, which) -> {
                            result.confirmResult(true);
                            dialog.dismiss();
                        }).setNegativeButton("暂不",
                        (dialog, which) -> {
                            result.confirmResult(false);
                            dialog.dismiss();
                        }).create();
        dialog.show();
    }

    private interface OnConfirmResult {
        void confirmResult(boolean confirm);
    }

    public void showWindow(Context context, String packageName) {
        if (!isWindowDismiss) {
            Log.e(TAG, "view is already added here");
            return;
        }

        isWindowDismiss = false;
        if (windowManager == null) {
            windowManager = (WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        }

        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSPARENT
        );
        mParams.packageName = context.getPackageName();
        int mType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mType = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        }
        mParams.type = mType;
        mParams.gravity = Gravity.CENTER;

//        floatView = new UnlockView(context);
////        floatView.setParams(mParams);
//        this.pkgName = packageName;
//        floatView.setPackageName(packageName);
//        floatView.showUnLockView();
//        windowManager.addView(floatView, mParams);
    }

    public void dismissWindow() {
        if (isWindowDismiss) {
            Log.e(TAG, "window can not be dismiss cause it has not been added");
            return;
        }

        isWindowDismiss = true;
//        if (windowManager != null && floatView != null) {
//            windowManager.removeViewImmediate(floatView);
//        }
    }

    private int dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}
