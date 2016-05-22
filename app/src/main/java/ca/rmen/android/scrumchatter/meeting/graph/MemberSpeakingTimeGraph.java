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
package ca.rmen.android.scrumchatter.meeting.graph;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.format.DateUtils;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper;
import ca.rmen.android.scrumchatter.util.TextUtils;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * The member speaking-time graph has one line per member. A line for a member contains points for
 * each meeting: the x-axis is the date of the meeting, and the y-axis is the time that member spoke
 * during that meeting.
 */
final class MemberSpeakingTimeGraph {
    private MemberSpeakingTimeGraph() {
        // prevent instantiation
    }

    public static void populateMemberSpeakingTimeGraph(Context context, LineChartView chart, ViewGroup legendView, @NonNull Cursor cursor) {
        List<AxisValue> xAxisValues = new ArrayList<>();
        Map<String, List<PointValue>> memberLines = new HashMap<>();

        MeetingMemberCursorWrapper cursorWrapper = new MeetingMemberCursorWrapper(cursor);
        long lastMemberId = -1;
        while (cursorWrapper.moveToNext()) {
            do {
                long currentMemberId = cursorWrapper.getMemberId();
                String memberName = cursorWrapper.getMemberName();
                if (currentMemberId != lastMemberId) {
                    memberLines.put(memberName, new ArrayList<PointValue>());
                }
                List<PointValue> memberPoints = memberLines.get(memberName);
                memberPoints.add(getSpeakingTimePointValue(cursorWrapper));
                xAxisValues.add(getSpeakingTimeXAxisValue(context, cursorWrapper));
                lastMemberId = currentMemberId;
            } while (cursorWrapper.moveToNext());
        }
        cursor.moveToPosition(-1);
        List<Line> lines = new ArrayList<>();
        for (Map.Entry<String, List<PointValue>> memberLine : memberLines.entrySet()) {
            Line line = MeetingsGraph.createLine(context, memberLine.getValue(), lines.size());
            lines.add(line);
            addLegendEntry(context, legendView, memberLine.getKey(), line.getColor(), line.getShape());
        }

        MeetingsGraph.setupChart(context,
                chart,
                xAxisValues,
                context.getString(R.string.chart_speaking_time),
                lines);

    }

    private static PointValue getSpeakingTimePointValue(MeetingMemberCursorWrapper cursorWrapper) {
        PointValue point = new PointValue();
        point.set(cursorWrapper.getMeetingDate(), (float) cursorWrapper.getDuration() / 60);
        String duration = DateUtils.formatElapsedTime(cursorWrapper.getDuration());
        point.setLabel(duration);
        return point;
    }

    private static AxisValue getSpeakingTimeXAxisValue(Context context, MeetingMemberCursorWrapper cursorWrapper) {
        AxisValue xAxisValue = new AxisValue(cursorWrapper.getMeetingDate());
        String dateString = TextUtils.formatDate(context, cursorWrapper.getMeetingDate());
        xAxisValue.setLabel(dateString);
        return xAxisValue;
    }

    private static void addLegendEntry(Context context, ViewGroup legendView, String name, int color, ValueShape shape) {
        TextView memberLegendEntry = new TextView(context);
        memberLegendEntry.setTextColor(color);
        memberLegendEntry.setText(name);
        memberLegendEntry.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int iconResId;
        if (shape == ValueShape.CIRCLE) {
            iconResId = R.drawable.ic_legend_circle;
        } else if (shape == ValueShape.DIAMOND) {
            iconResId = R.drawable.ic_legend_diamond;
        } else {
            iconResId = R.drawable.ic_legend_square;
        }
        Drawable icon = ContextCompat.getDrawable(context, iconResId);
        DrawableCompat.setTint(icon, color);
        memberLegendEntry.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        legendView.addView(memberLegendEntry);

    }

}
