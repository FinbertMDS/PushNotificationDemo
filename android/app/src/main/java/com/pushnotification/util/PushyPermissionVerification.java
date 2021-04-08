package com.pushnotification.util;

import android.content.Context;
import com.pushnotification.config.PushyPermissions;
import com.pushnotification.config.PushyPreferenceKeys;
import com.pushnotification.util.exceptions.PushyException;
import com.pushnotification.util.exceptions.PushyPermissionException;

public class PushyPermissionVerification {
  public static void verifyManifestPermissions(Context context) throws PushyException {
    boolean permissionEnforcement = PushyPreferences.getBoolean(PushyPreferenceKeys.PERMISSION_ENFORCEMENT, true, context);
    if (!permissionEnforcement)
      PushyLogger.d("Warning: AndroidManifest permission verification disabled by developer"); 
    for (String permission : PushyPermissions.REQUIRED_MANIFEST_PERMISSIONS) {
      if (context.checkCallingOrSelfPermission(permission) != 0) {
        String message = "Error: " + permission + " is missing from your AndroidManifest.xml.";
        if (permissionEnforcement)
          throw new PushyPermissionException(message); 
        PushyLogger.e(message);
      } 
    } 
  }
}
