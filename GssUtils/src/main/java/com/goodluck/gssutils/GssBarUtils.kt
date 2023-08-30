package com.goodluck.gssutils

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Build.VERSION_CODES
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.IntDef
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import kotlin.math.abs

object GssBarUtils {
    private const val STATUSBAR_TYPE_DEFAULT = 0
    private const val STATUSBAR_TYPE_MIUI = 1
    private const val STATUSBAR_TYPE_FLYME = 2
    private const val STATUSBAR_TYPE_ANDROID6 = 3 // Android 6.0

    @GssBarUtils.StatusBarType
    private var mStatusBarType = STATUSBAR_TYPE_DEFAULT

    fun showHideStatusBar(activity: Activity, show: Boolean) {
        val insetsController = ViewCompat.getWindowInsetsController(activity.window.decorView)
        if (insetsController != null) {
            if (show) {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    fun showHideNavigationBar(activity: Activity, show: Boolean) {
        val insetsController = ViewCompat.getWindowInsetsController(activity.window.decorView)
        if (insetsController != null) {
            if (show) {
                insetsController.show(WindowInsetsCompat.Type.navigationBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    /**
     * true表示浅色模式，状态栏字休呈黑色，反之呈白色
     * @param light：是否把状态栏字体及图标颜色设置为深色
     */
    fun setStatusBarLight(activity: Activity, light: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11
            val controller = ViewCompat.getWindowInsetsController(activity.window.decorView)
            if (controller != null) {
                controller.isAppearanceLightStatusBars = light
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (light) {
                setStatusBarLightMode(activity)
            } else {
                setStatusBarDarkMode(activity)
            }
        } else {
            setStatusBarColor(activity, Color.parseColor("#33000000"))
        }
    }

    /**
     * 设置状态栏黑色字体图标，
     * 支持 4.4 以上版本 MIUI 和 Flyme，以及 6.0 以上版本的其他 Android
     *
     * @param activity 需要被处理的 Activity
     */
    fun setStatusBarLightMode(activity: Activity?): Boolean {
        if (activity == null) {
            return false
        }
        // 无语系列：ZTK C2016只能时间和电池图标变色。。。。
        if (DeviceUtils.isZTKC2016()) {
            return false
        }
        if (mStatusBarType != STATUSBAR_TYPE_DEFAULT) {
            return setStatusBarLightMode(activity, mStatusBarType)
        }
        if (Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            if (isMIUICustomStatusBarLightModeImpl() && MIUISetStatusBarLightMode(activity.window, true)) {
                mStatusBarType = STATUSBAR_TYPE_MIUI
                return true
            } else if (FlymeSetStatusBarLightMode(activity.window, true)) {
                mStatusBarType = STATUSBAR_TYPE_FLYME
                return true
            } else if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                Android6SetStatusBarLightMode(activity.window, true)
                mStatusBarType = STATUSBAR_TYPE_ANDROID6
                return true
            }
        }
        return false
    }

    /**
     * 已知系统类型时，设置状态栏黑色字体图标。
     * 支持 4.4 以上版本 MIUI 和 Flyme，以及 6.0 以上版本的其他 Android
     *
     * @param activity 需要被处理的 Activity
     * @param type     StatusBar 类型，对应不同的系统
     */
    private fun setStatusBarLightMode(activity: Activity, @StatusBarType type: Int): Boolean {
        if (type == STATUSBAR_TYPE_MIUI) {
            return MIUISetStatusBarLightMode(activity.window, true)
        } else if (type == STATUSBAR_TYPE_FLYME) {
            return FlymeSetStatusBarLightMode(activity.window, true)
        } else if (type == STATUSBAR_TYPE_ANDROID6) {
            return Android6SetStatusBarLightMode(activity.window, true)
        }
        return false
    }

    /**
     * 设置状态栏白色字体图标
     * 支持 4.4 以上版本 MIUI 和 Flyme，以及 6.0 以上版本的其他 Android
     */
    fun setStatusBarDarkMode(activity: Activity?): Boolean {
        if (activity == null) {
            return false
        }
        if (mStatusBarType == STATUSBAR_TYPE_DEFAULT) {
            // 默认状态，不需要处理
            return true
        }
        if (mStatusBarType == STATUSBAR_TYPE_MIUI) {
            return MIUISetStatusBarLightMode(activity.window, false)
        } else if (mStatusBarType == STATUSBAR_TYPE_FLYME) {
            return FlymeSetStatusBarLightMode(activity.window, false)
        } else if (mStatusBarType == STATUSBAR_TYPE_ANDROID6) {
            return Android6SetStatusBarLightMode(activity.window, false)
        }
        return true
    }

    /**
     * 设置状态栏字体图标为深色，Android 6
     *
     * @param window 需要设置的窗口
     * @param light  是否把状态栏字体及图标颜色设置为深色
     * @return boolean 成功执行返回true
     */
    @TargetApi(VERSION_CODES.M)
    private fun Android6SetStatusBarLightMode(window: Window, light: Boolean): Boolean {
        val decorView = window.decorView
        var systemUi = if (light) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        systemUi = changeStatusBarModeRetainFlag(window, systemUi)
        decorView.systemUiVisibility = systemUi
        if (DeviceUtils.isMIUIV9()) {
            // MIUI 9 低于 6.0 版本依旧只能回退到以前的方案
            // https://github.com/Tencent/QMUI_Android/issues/160
            MIUISetStatusBarLightMode(window, light)
        }
        return true
    }

    /**
     * 设置状态栏字体图标为深色，需要 MIUIV6 以上
     *
     * @param window 需要设置的窗口
     * @param light  是否把状态栏字体及图标颜色设置为深色
     * @return boolean 成功执行返回 true
     */
    fun MIUISetStatusBarLightMode(window: Window?, light: Boolean): Boolean {
        var result = false
        if (window != null) {
            val clazz: Class<*> = window.javaClass
            try {
                val darkModeFlag: Int
                val layoutParams = Class.forName("android.view.MiuiWindowManager\$LayoutParams")
                val field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE")
                darkModeFlag = field.getInt(layoutParams)
                val extraFlagField = clazz.getMethod("setExtraFlags", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                if (light) {
                    extraFlagField.invoke(window, darkModeFlag, darkModeFlag) //状态栏透明且黑色字体
                } else {
                    extraFlagField.invoke(window, 0, darkModeFlag) //清除黑色字体
                }
                result = true
            } catch (ignored: java.lang.Exception) {
            }
        }
        return result
    }

    /**
     * 更改状态栏图标、文字颜色的方案是否是MIUI自家的， MIUI9 && Android 6 之后用回Android原生实现
     * 见小米开发文档说明：https://dev.mi.com/console/doc/detail?pId=1159
     */
    private fun isMIUICustomStatusBarLightModeImpl(): Boolean {
        return if (DeviceUtils.isMIUIV9() && Build.VERSION.SDK_INT < VERSION_CODES.M) {
            true
        } else DeviceUtils.isMIUIV5() || DeviceUtils.isMIUIV6() ||
                DeviceUtils.isMIUIV7() || DeviceUtils.isMIUIV8()
    }

    /**
     * 设置状态栏图标为深色和魅族特定的文字风格
     * 可以用来判断是否为 Flyme 用户
     *
     * @param window 需要设置的窗口
     * @param light  是否把状态栏字体及图标颜色设置为深色
     * @return boolean 成功执行返回true
     */
    fun FlymeSetStatusBarLightMode(window: Window?, light: Boolean): Boolean {
        var result = false
        if (window != null) {
            // flyme 在 6.2.0.0A 支持了 Android 官方的实现方案，旧的方案失效
            Android6SetStatusBarLightMode(window, light)
            try {
                val lp = window.attributes
                val darkFlag = WindowManager.LayoutParams::class.java
                    .getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON")
                val meizuFlags = WindowManager.LayoutParams::class.java
                    .getDeclaredField("meizuFlags")
                darkFlag.isAccessible = true
                meizuFlags.isAccessible = true
                val bit = darkFlag.getInt(null)
                var value = meizuFlags.getInt(lp)
                value = if (light) {
                    value or bit
                } else {
                    value and bit.inv()
                }
                meizuFlags.setInt(lp, value)
                window.attributes = lp
                result = true
            } catch (ignored: java.lang.Exception) {
            }
        }
        return result
    }

    @TargetApi(VERSION_CODES.M)
    private fun changeStatusBarModeRetainFlag(window: Window, out: Int): Int {
        var out = out
        out = retainSystemUiFlag(window, out, View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        out = retainSystemUiFlag(window, out, View.SYSTEM_UI_FLAG_FULLSCREEN)
        out = retainSystemUiFlag(window, out, View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        out = retainSystemUiFlag(window, out, View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        out = retainSystemUiFlag(window, out, View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        out = retainSystemUiFlag(window, out, View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        return out
    }

    fun retainSystemUiFlag(window: Window, out: Int, type: Int): Int {
        var out = out
        val now = window.decorView.systemUiVisibility
        if (now and type == type) {
            out = out or type
        }
        return out
    }

    /**
     * 状态栏背景颜色
     *
     * @param color
     */
    fun setStatusBarColor(activity: Activity?, color: Int) {
        if (activity == null) {
            return
        }
        val window = activity.window
        window.statusBarColor = color
    }


    /**
     * 适配 Android P 异形屏
     */
    fun adaptiveSpecialScreen(activity: Activity, fullScreen: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val window: Window = activity.window
                if (!setNavigationInsetConsumed(activity)) {
                    //使内容出现在status bar后边，如果要使用全屏的话再加上View.SYSTEM_UI_FLAG_FULLSCREEN
                    window.decorView.systemUiVisibility = (window.decorView.systemUiVisibility
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                }
                //设置页面全屏显示
                val lp = window.attributes
                lp.layoutInDisplayCutoutMode =
                    if (fullScreen) WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES else WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                //设置页面延伸到刘海区显示
                window.attributes = lp
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取底部导航栏的高度
     */
    fun getNavigationBarHeight(callback: HeightValueCallback) {
//        val topActivity = ActivityUtils.getTopActivity()
//        if (topActivity != null) {
//            val view = topActivity.findViewById<View>(R.id.content)
//            getNavigationBarHeight(view, callback)
//        }
    }

    fun getNavigationBarHeight(view: View?, callback: HeightValueCallback) {
//        if (view == null) {
//            callback.onHeight(BarUtils.getNavBarHeight())
//            return
//        }
//        val attachedToWindow = view.isAttachedToWindow
//        if (attachedToWindow) {
//            val windowInsets = ViewCompat.getRootWindowInsets(view)!!
//            val top = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).top
//            val bottom = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
//            val height = Math.abs(bottom - top)
//            if (height > 0) {
//                callback.onHeight(height)
//            } else {
//                callback.onHeight(BarUtils.getNavBarHeight())
//            }
//        } else {
//            view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
//                override fun onViewAttachedToWindow(v: View) {
//                    val windowInsets = ViewCompat.getRootWindowInsets(v)!!
//                    val top = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).top
//                    val bottom = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
//                    val height = Math.abs(bottom - top)
//                    if (height > 0) {
//                        callback.onHeight(height)
//                    } else {
//                        callback.onHeight(BarUtils.getNavBarHeight())
//                    }
//                }
//
//                override fun onViewDetachedFromWindow(v: View) {}
//            })
//        }
    }

    interface HeightValueCallback {
        fun onHeight(height: Int)
    }

    /**
     * 适配Android 10以上小白条导航栏
     *
     * @param activity
     * @return 最终是否占用小白条区域
     */
    fun setNavigationInsetConsumed(activity: Activity): Boolean {
        return setNavigationInsetConsumed(activity, false)
    }

    fun setNavigationInsetConsumed(activity: Activity, forced: Boolean): Boolean {
        if (needNavigationInsetConsumed(activity) || forced && isFullScreenGesture(activity)
        ) {
            val window = activity.window
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            window.navigationBarColor = Color.TRANSPARENT
            window.decorView.setOnApplyWindowInsetsListener { v, insets -> insets.consumeSystemWindowInsets() }
            return true
        }
        return false
    }

    /**
     * 自动沉浸小白条
     *
     * @param activity
     * @return
     */
    fun needNavigationInsetConsumed(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isEdgeToEdgeEnabled(activity) == 2) {
            return true
        }
        return false
    }

    fun isFullScreenGesture(activity: Activity): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isEdgeToEdgeEnabled(activity) == 2
    }

    /**
     * onWindowFocusChanged
     * 当delta>0，就认为开启了手势提示线
     * 否则认为没有开启手势提示线
     */
    fun isShowSmallWhiteBar(activity: Activity): Boolean {
        val decorView = activity.window.decorView
        val outRect = Rect()
        decorView.getWindowVisibleDisplayFrame(outRect)
        val delta = abs(decorView.bottom - outRect.bottom)
        return delta > 0
    }

    /**
     * 0 : Navigation is displaying with 3 buttons
     * 1 : Navigation is displaying with 2 button(Android P navigation mode)
     * 2 : Full screen gesture(Gesture on android Q)
     */
    fun isEdgeToEdgeEnabled(context: Context): Int {
        var mode = 0
        try {
            val resources = context.resources
            val resourceId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
            if (resourceId > 0) {
                mode = resources.getInteger(resourceId)
            }
        } catch (ignore: java.lang.Exception) {
        }
        return mode
    }

    /**
     * 导航栏按钮色
     */
    fun setNavigationBarBtnColor(mDecorView: View, black: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var vis = mDecorView.systemUiVisibility
            vis = if (black) {
                // 黑色
                vis or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                // 白色
                vis and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
            mDecorView.systemUiVisibility = vis
        }
    }

    @IntDef(STATUSBAR_TYPE_DEFAULT, STATUSBAR_TYPE_MIUI, STATUSBAR_TYPE_FLYME, STATUSBAR_TYPE_ANDROID6)
    @Retention(RetentionPolicy.SOURCE)
    private annotation class StatusBarType
}