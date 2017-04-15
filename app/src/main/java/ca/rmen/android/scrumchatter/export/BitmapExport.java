/*
 * Copyright 2016-2017 Carmen Alvarez
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
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.util.Log;

/**
 * Export a bitmap as a png file.
 */
public class BitmapExport {
    private static final String TAG = Constants.TAG + "/" + BitmapExport.class.getSimpleName();

    private static final String FILE = "scrumchatter.png";
    private static final String MIME_TYPE = "image/png";
    private final Context mContext;
    private final Bitmap mBitmap;

    public BitmapExport(Context context, Bitmap bitmap) {
        mBitmap = bitmap;
        mContext = context;
    }

    /**
     * Create and return a bitmap of our view.
     *
     * @see FileExport#createFile()
     */
    private File createFile() {
        Log.v(TAG, "export");

        File file = Export.getExportFile(mContext, FILE);
        if (file == null) return null;
        // Draw everything to a bitmap.
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
        } catch (FileNotFoundException e) {
            Log.v(TAG, "Error writing bitmap file", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.v(TAG, "Error closing bitmap file", e);
                }
            }
        }

        return file;
    }

    /**
     * Export the file.
     */
    public void export() {
        Log.v(TAG, "export");
        File file = createFile();
        Log.v(TAG, "export: created file " + file);
        if (file != null && file.exists()) Export.share(mContext, file, MIME_TYPE);
    }

}
