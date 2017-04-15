/*
 * Copyright 2013-2017 Carmen Alvarez
 *
 * This file is part of Scrum Chatter.
 *
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.scrumchatter.export;

import java.io.File;

import android.content.Context;

import ca.rmen.android.scrumchatter.util.Log;
import ca.rmen.android.scrumchatter.Constants;

/**
 * Base class for sharing a file using an intent chooser. The base classes must provide a mime-type (used to determine which apps can share the file) and must
 * override {@link #createFile()} to provide a file to share.
 */
public abstract class FileExport {
    private static final String TAG = Constants.TAG + "/" + FileExport.class.getSimpleName();
    final Context mContext;
    private final String mMimeType;

    /**
     * @param mimeType will be used to show a list of applications which can share the file created by {@link #createFile()}.
     */
    FileExport(Context context, String mimeType) {
        Log.v(TAG, "Constructor: mimeType=" + mimeType);
        mContext = context;
        mMimeType = mimeType;
    }

    /**
     * Subclasses must implement this and return an existing file which will be shared. The file should be on the SD card.
     * 
     * @return the file we wish to share.
     */
    protected abstract File createFile();

    /**
     * @return true if we were able to export the file.
     */
    public boolean export() {
        Log.v(TAG, "export");
        File file = createFile();
        Log.v(TAG, "export: created file " + file);
        if (file == null || !file.exists()) return false;
        Export.share(mContext, file, mMimeType);
        return true;
    }

}
