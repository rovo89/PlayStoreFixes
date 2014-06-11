package de.robv.android.xposed.mods.playstorefix;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import android.util.DisplayMetrics;
import android.view.Display;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
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
		if  (!lpparam.packageName.equals(GOOGLE_PLAYSTORE)
		  && !lpparam.packageName.equals(GOOGLE_SERVICES_FRAMEWORK)
		  && !lpparam.packageName.equals(GOOGLE_PLAY_SERVICES))
			return;

		XSharedPreferences prefs = new XSharedPreferences(PACKAGE_NAME);
		final int density = tryParseInt(prefs.getString("density", "240"));
		final boolean noRestrictionsPatch = prefs.getBoolean("no_restrictions", false);
		final boolean enableDebugMenu = prefs.getBoolean("debug", false);

		if (density > 0) {
			findAndHookMethod(Display.class, "getMetrics", DisplayMetrics.class, new XC_MethodHook(XCallback.PRIORITY_LOWEST) {
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					DisplayMetrics metrics = (DisplayMetrics) param.args[0];
					metrics.densityDpi = density;
				}
			});
		}

		if (lpparam.packageName.equals(GOOGLE_PLAYSTORE)) {
			if (noRestrictionsPatch) {
				// http://forum.xda-developers.com/showpost.php?p=29466370&postcount=344
				findAndHookMethod("com.google.android.finsky.utils.LibraryUtils", lpparam.classLoader,
						"isAvailable",
						"com.google.android.finsky.api.model.Document",
						"com.google.android.finsky.api.model.DfeToc",
						"com.google.android.finsky.library.Library",
						XC_MethodReplacement.returnConstant(true));
			}

			if (enableDebugMenu) {
				// see also: https://plus.google.com/117221066931981967754/posts/SnU689qHLaV
				findAndHookMethod("com.google.android.finsky.config.GservicesValue", lpparam.classLoader,
						"value", String.class, Boolean.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if (param.args[0].equals("finsky.debug_options_enabled"))
							setObjectField(param.getResult(), "mOverride", Boolean.TRUE);
					}
				});
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
}
