/**
 * Copyright 2013-2016 Carmen Alvarez
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
package ca.rmen.android.scrumchatter.widget;

import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.v7.widget.RecyclerView;

public abstract class ScrumChatterCursorAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T> {

    private Cursor mCursor;

    protected ScrumChatterCursorAdapter() {
        setHasStableIds(true);
    }
    public void changeCursor(Cursor cursor) {
        if (mCursor == cursor) return;
        if (mCursor != null) mCursor.close();
        mCursor = cursor;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        if (mCursor == null || !mCursor.moveToPosition(position)) {
            return RecyclerView.NO_ID;
        }
        return mCursor.getLong(mCursor.getColumnIndex(BaseColumns._ID));
    }

    @Override
    public int getItemCount() {
        if (mCursor == null) return 0;
        else return mCursor.getCount();
    }

    protected Cursor getCursor() {
        return mCursor;
    }
}
