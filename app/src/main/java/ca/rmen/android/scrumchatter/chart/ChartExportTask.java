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

package ca.rmen.android.scrumchatter.chart;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.view.View;

import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.export.BitmapExport;

class ChartExportTask extends AsyncTask<Void, Void, Void> {

    private final Context mContext;
    private final View mView;
    private Bitmap mBitmap;

    ChartExportTask(Context context, View view) {
        super();
        mContext = context;
        mView = view;
    }

    @Override
    protected void onPreExecute() {
        Snackbar.make(mView, mContext.getString(R.string.chart_exporting_snackbar), Snackbar.LENGTH_LONG).show();
        mBitmap = Bitmap.createBitmap(mView.getWidth(), mView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mBitmap);
        mView.draw(canvas);
    }

    @Override
    protected Void doInBackground(Void... params) {
        BitmapExport export = new BitmapExport(mContext, mBitmap);
        export.export();
        return null;
    }

}
