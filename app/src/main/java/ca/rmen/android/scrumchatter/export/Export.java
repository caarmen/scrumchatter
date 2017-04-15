/*
 * Copyright 2017 Carmen Alvarez
 * <p/>
 * This file is part of Scrum Chatter.
 * <p/>
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.scrumchatter.export;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.util.List;

import ca.rmen.android.scrumchatter.BuildConfig;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.util.Log;

class Export {
    private static final String TAG = Constants.TAG + "/" + Export.class.getSimpleName();
    private static final String EXPORT_FOLDER_PATH = "export";

    /**
     * @return File in the share folder that we can write to before sharing.
     */
    @Nullable
    static File getExportFile(Context context, String filename) {
        File exportFolder = new File(context.getFilesDir(), EXPORT_FOLDER_PATH);
        if (!exportFolder.exists() && !exportFolder.mkdirs()) {
            Log.v(TAG, "Couldn't find or create export folder " + exportFolder);
            return null;
        }
        return new File(exportFolder, filename);
    }

    /**
     * Bring up the chooser to send the file.
     */
    static void share(Context context, File file, String mimeType) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_message_subject));
        sendIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.export_message_body));
        Uri uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setType(mimeType);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(sendIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Log.v(TAG, "grant permission to " + packageName);
            }
        }
        context.startActivity(Intent.createChooser(sendIntent, context.getResources().getText(R.string.action_share)));
    }

}
