package net.typeblog.netlock.mod;

import android.os.Message;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static net.typeblog.netlock.BuildConfig.DEBUG;

public class ModNetLock implements IXposedHookZygoteInit
{

	private static final String TAG = ModNetLock.class.getSimpleName() + ":";

	public static final String PREF_XML = "network";
	public static final String PREF_NAME = "lock";

	private XSharedPreferences mPref;

	@Override
	public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
		// Create preference first
		mPref = new XSharedPreferences("net.typeblog.netlock", PREF_XML);
		mPref.makeWorldReadable();

		Class<?> RIL = XposedHelpers.findClass("com.android.internal.telephony.RIL", null);
		if (RIL == null) {
			if (DEBUG) XposedBridge.log(TAG + "cannot get RIL class");
			return;
		}

		XposedBridge.hookAllConstructors(RIL, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(XC_MethodHook.MethodHookParam mhparams) throws Throwable {
				if (DEBUG) XposedBridge.log(TAG + "RIL Instance created");
				mPref.reload();
				mhparams.args[1] = mPref.getInt(PREF_NAME, 0);
			}
		});

		XposedHelpers.findAndHookMethod(RIL, "setPreferredNetworkType", int.class, Message.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(XC_MethodHook.MethodHookParam mhparams) throws Throwable {

				if (DEBUG) XposedBridge.log(TAG + "setPreferredNetworkType called.");

				mPref.reload();
				mhparams.args[0] = mPref.getInt(PREF_NAME, 0);
			}
		});
	}
}
