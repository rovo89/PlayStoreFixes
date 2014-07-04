package de.robv.android.xposed.mods.playstorefix;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import android.util.DisplayMetrics;
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
		if  (!lpparam.packageName.equals(GOOGLE_PLAYSTORE)
		  && !lpparam.packageName.equals(GOOGLE_SERVICES_FRAMEWORK)
		  && !lpparam.packageName.equals(GOOGLE_PLAY_SERVICES))
			return;

		XSharedPreferences prefs = new XSharedPreferences(PACKAGE_NAME);
		final int density = tryParseInt(prefs.getString("density", "240"));
		final boolean enableDebugMenu = prefs.getBoolean("debug", false);

		if (density > 0) {
			// SECTION TO DETECT LAYOUT AND SEND A NORMAL ONE, OR ORIGINAL ONE.
			findAndHookMethod(Resources.class, "getConfiguration", new XC_MethodHook(XCallback.PRIORITY_LOWEST) {
                        	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            		Configuration c = (Configuration) param.getResult();
                            		c.screenLayout = Configuration.SCREENLAYOUT_SIZE_NORMAL&Configuration.SCREENLAYOUT_SIZE_MASK;
                            		// I use NORMAL because was my previous SIZE in 480 dpi. Has to choose the original one.
                        	}
                    	});
			
			findAndHookMethod(Display.class, "getMetrics", DisplayMetrics.class, new XC_MethodHook(XCallback.PRIORITY_LOWEST) {
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					DisplayMetrics metrics = (DisplayMetrics) param.args[0];
					metrics.densityDpi = density;
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
