package de.robv.android.xposed.mods.playstorefix;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.callbacks.XCallback;

public class PlayStoreFix implements IXposedHookLoadPackage {
    private static final String PACKAGE_NAME = PlayStoreFix.class.getPackage().getName();

    private static final String GOOGLE_PLAYSTORE = "com.android.vending";
    private static final String GOOGLE_SERVICES_FRAMEWORK = "com.google.android.gsf";
    private static final String GOOGLE_PLAY_SERVICES = "com.google.android.gms";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(GOOGLE_PLAYSTORE)
                && !lpparam.packageName.equals(GOOGLE_SERVICES_FRAMEWORK)
                && !lpparam.packageName.equals(GOOGLE_PLAY_SERVICES))
            return;

        XSharedPreferences prefs = new XSharedPreferences(PACKAGE_NAME);
        final int density = tryParseInt(prefs.getString("density", "240"));
        final boolean enableDebugMenu = prefs.getBoolean("debug", false);
        final boolean calculateLayout = prefs.getBoolean("calculateLayout", false);
        final int screenLayout = tryParseInt(prefs.getString("screenLayout", "0"));

        if (density > 0) {
            findAndHookMethod(Display.class, "getMetrics", DisplayMetrics.class, new XC_MethodHook(XCallback.PRIORITY_LOWEST) {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    DisplayMetrics metrics = (DisplayMetrics) param.args[0];
                    metrics.densityDpi = density;
                    calculateOriginalScreenLayout(metrics.xdpi, metrics.ydpi);
                }
            });
        }

        if (screenLayout > 0) {
            findAndHookMethod(Resources.class, "getConfiguration", new XC_MethodHook(XCallback.PRIORITY_LOWEST) {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Configuration c = (Configuration) param.getResult();
                    c.screenLayout = (calculateLayout) ? getOriginalScreenLayout() : getScreenLayout(screenLayout);
                    c.screenLayout &= Configuration.SCREENLAYOUT_SIZE_MASK;
                }
            });
        }

        if (lpparam.packageName.equals(GOOGLE_PLAYSTORE)) {
            if (enableDebugMenu) {
                // see also: https://plus.google.com/117221066931981967754/posts/SnU689qHLaV
                findAndHookMethod("com.google.android.finsky.config.GservicesValue", lpparam.classLoader,
                        "value", String.class, Boolean.class, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                if (param.args[0].equals("finsky.debug_options_enabled"))
                                    setObjectField(param.getResult(), "mOverride", Boolean.TRUE);
                            }
                        }
                );
            }
        }
    }

    private static int tryParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    private static int originalScreenLayout = 0;

    private static int getOriginalScreenLayout(){
        return originalScreenLayout;
    }

    private static int getScreenLayout(int layout){
        switch (layout) {
            case 1:
                return Configuration.SCREENLAYOUT_SIZE_SMALL;
            case 2:
                return Configuration.SCREENLAYOUT_SIZE_NORMAL;
            case 3:
                return Configuration.SCREENLAYOUT_SIZE_LARGE;
            case 4:
                return Configuration.SCREENLAYOUT_SIZE_XLARGE;
            default:
                return Configuration.SCREENLAYOUT_SIZE_UNDEFINED;
        }
    }

    private static void calculateOriginalScreenLayout(float xdpi, float ydpi) {
        /**
         * xlarge screens are at least 960dp x 720dp
         * large screens are at least 640dp x 480dp
         * normal screens are at least 470dp x 320dp
         * small screens are at least 426dp x 320dp
         * 442 - 439
         */
        if ((xdpi <= 426 || xdpi <= 320) && (ydpi <= 426 || ydpi <= 320)){
            originalScreenLayout = Configuration.SCREENLAYOUT_SIZE_SMALL; // SMALL
        }else if ((xdpi <= 470 || xdpi <= 320) && (ydpi <= 470 || ydpi <= 320)){
            originalScreenLayout =  Configuration.SCREENLAYOUT_SIZE_NORMAL; // NORMAL
        }else if ((xdpi <= 640 || xdpi <= 480) && (ydpi <= 640 || ydpi <= 480)){
            originalScreenLayout =  Configuration.SCREENLAYOUT_SIZE_LARGE; // LARGE
        }else if ((xdpi <= 960 || xdpi <= 720) && (ydpi <= 960 || ydpi <= 720)){
            originalScreenLayout = Configuration.SCREENLAYOUT_SIZE_XLARGE; // XLARGE
        }else{
            originalScreenLayout = Configuration.SCREENLAYOUT_SIZE_UNDEFINED; // UNDEFINED
        }
    }
}
