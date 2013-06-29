/**
 * Copyright 2013 Carmen Alvarez
 *
 * This file is part of Scrum Chatter.
 *
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.util.Log;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.provider.ScrumChatterDatabase;

/**
 * Export the raw database file.
 */
public class DBExport extends FileExport {
    private static final String TAG = Constants.TAG + "/" + DBExport.class.getSimpleName();
    private static final String MIME_TYPE = "application/octet-stream";

    public DBExport(Context context) {
        super(context, MIME_TYPE);
    }

    /**
     * Copy the internal database file to the SD card and return the SD card file.
     * 
     * @see ca.rmen.android.scrumchatter.export.FileExport#createFile()
     */
    @Override
    protected File createFile() {
        File internalDBFile = mContext.getDatabasePath(ScrumChatterDatabase.DATABASE_NAME);
        File externalDBFile = new File(mContext.getExternalFilesDir(null), ScrumChatterDatabase.DATABASE_NAME);
        try {
            InputStream is = new FileInputStream(internalDBFile);
            OutputStream os = new FileOutputStream(externalDBFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
            is.close();
            os.close();
            return externalDBFile;
        } catch (IOException e) {
            Log.v(TAG, "Could not copy DB file: " + e.getMessage(), e);
            return null;
        }
    }

}
