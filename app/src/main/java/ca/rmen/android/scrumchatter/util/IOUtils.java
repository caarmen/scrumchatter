/**
 * Copyright 2013 Carmen Alvarez
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
package ca.rmen.android.scrumchatter.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ca.rmen.android.scrumchatter.util.Log;
import ca.rmen.android.scrumchatter.Constants;

public class IOUtils {
    private static final String TAG = Constants.TAG + "/" + IOUtils.class.getSimpleName();

    public static boolean copy(File from, File to) {
        try {
            InputStream is = new FileInputStream(from);
            OutputStream os = new FileOutputStream(to);
            return copy(is, os);
        } catch (FileNotFoundException e) {
            Log.v(TAG, "Could not copy file: " + e.getMessage(), e);
            return false;
        }
    }

    public static boolean copy(InputStream is, OutputStream os) {
        try {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
            is.close();
            os.close();
            return true;
        } catch (IOException e) {
            Log.v(TAG, "Could not copy stream: " + e.getMessage(), e);
            return false;
        }
    }
}
