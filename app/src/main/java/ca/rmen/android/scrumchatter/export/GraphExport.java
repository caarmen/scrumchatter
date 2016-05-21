/**
 * Copyright 2016 Carmen Alvarez
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
import android.graphics.Canvas;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.util.Log;

/**
 * Export data for all meetings to an Excel file.
 */
public class GraphExport extends FileExport {
    private static final String TAG = Constants.TAG + "/" + GraphExport.class.getSimpleName();

    private static final String FILE = "scrumchatter.png";
    private static final String MIME_TYPE = "image/png";
    private final View mView;

    public GraphExport(Context context, View view) {
        super(context, MIME_TYPE);
        mView = view;
    }

    /**
     * Create and return a bitmap of our view.
     *
     * @see FileExport#createFile()
     */
    @Override
    protected File createFile() {
        Log.v(TAG, "export");

        File file = new File(mContext.getExternalFilesDir(null), FILE);
        // Draw everything to a bitmap.
        Bitmap bitmap = Bitmap.createBitmap(mView.getWidth(), mView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        mView.draw(canvas);
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
        } catch (FileNotFoundException e) {
            Log.v(TAG, "Error writing bitmap file", e);
        } finally {
            if (os != null)  {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.v(TAG, "Error closing bitmap file", e);
                }
            }
        }

        return file;
    }

}