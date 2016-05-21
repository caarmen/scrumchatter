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
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.text.format.DateUtils;

import java.util.ArrayList;
import java.util.List;

import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.meeting.detail.Meeting;
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper;
import ca.rmen.android.scrumchatter.util.TextUtils;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;


/**
 * Populates a line graph with meetings.
 */
final class MeetingsGraph {

    private MeetingsGraph() {
        // prevent instantiation
    }

    public static void populateMeetingsGraph(Context context, LineChartView chart, @NonNull Cursor cursor) {
        List<PointValue> points = new ArrayList<>();
        List<AxisValue> axisValues = new ArrayList<>();


        while (cursor.moveToNext()) {
            MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(cursor);
            Meeting meeting = Meeting.read(context, cursorWrapper);
            String duration = DateUtils.formatElapsedTime(meeting.getDuration());
            PointValue point = new PointValue();
            point.set(meeting.getStartDate(), (float) meeting.getDuration() / (60));
            point.setLabel(duration);
            points.add(point);
            AxisValue axisValue = new AxisValue(meeting.getStartDate());
            String dateString = TextUtils.formatDate(context, meeting.getStartDate());
            axisValue.setLabel(dateString);
            axisValues.add(axisValue);
        }
        cursor.moveToPosition(-1);

        Line line = new Line(points);
        line.setColor(ResourcesCompat.getColor(context.getResources(), R.color.scrum_chatter_primary_color, null));
        List<Line> lines = new ArrayList<>();
        lines.add(line);

        Axis xAxis = new Axis(axisValues);
        xAxis.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.primary_text_color, null));
        xAxis.setHasTiltedLabels(true);
        xAxis.setName(context.getString(R.string.chart_date));
        xAxis.setMaxLabelChars(10);
        Axis yAxis = new Axis();
        yAxis.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.primary_text_color, null));

        yAxis.setName(context.getString(R.string.chart_duration));
        LineChartData lineChartData = new LineChartData();
        lineChartData.setAxisXBottom(xAxis);
        lineChartData.setAxisYLeft(yAxis);
        lineChartData.setLines(lines);

        chart.setZoomEnabled(true);
        chart.setZoomType(ZoomType.HORIZONTAL);
        chart.setLineChartData(lineChartData);

        Viewport viewport = chart.getMaximumViewport();
        viewport.set(viewport.left, viewport.top, viewport.right, 0);
        chart.setMaximumViewport(viewport);
        viewport = chart.getCurrentViewport();
        viewport.set(viewport.left, viewport.top, viewport.right, 0);
        chart.setCurrentViewport(viewport);
    }
}
