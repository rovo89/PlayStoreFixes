package de.robv.android.xposed.mods.playstorefix;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class PlayStoreFix implements IXposedHookLoadPackage {
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals("com.android.vending")) {
			// http://forum.xda-developers.com/showthread.php?t=1580827
			findAndHookMethod("com.google.android.finsky.remoting.protos.DeviceConfigurationProto", lpparam.classLoader,
					"setScreenDensity", int.class, new XC_MethodHook() {
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					param.args[0] = 240;					
				}
			});
			
			// http://forum.xda-developers.com/showpost.php?p=29466370&postcount=344
			findAndHookMethod("com.google.android.finsky.utils.LibraryUtils", lpparam.classLoader,
					"isAvailable",
					"com.google.android.finsky.api.model.Document",
					"com.google.android.finsky.api.model.DfeToc",
					"com.google.android.finsky.library.Library",
					XC_MethodReplacement.returnConstant(true));

		} else if (lpparam.packageName.equals("com.google.android.gsf")) {
			// http://forum.xda-developers.com/showpost.php?p=25720539&postcount=92
			findAndHookMethod("com.google.common.io.protocol.ProtoBuf", lpparam.classLoader,
					"setInt", int.class, int.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if ((Integer)param.args[0] == 7)
						param.args[1] = 240;
				}
			});
		}
	}
}
