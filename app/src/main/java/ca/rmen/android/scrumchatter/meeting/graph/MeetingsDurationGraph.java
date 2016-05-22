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
import android.graphics.Color;
import android.support.annotation.NonNull;
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
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.AbstractChartView;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * The meetings duration graph has one single line.  This line plots the meeting duration in minutes
 * on the y-axis versus the meeting dates on the x-axis.
 */
final class MeetingsDurationGraph {
    // prevent instantiation
    public static void populateMeetingDurationGraph(Context context, LineChartView chart, @NonNull Cursor cursor) {
        List<PointValue> points = new ArrayList<>();
        List<AxisValue> xAxisValues = new ArrayList<>();

        MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(cursor);
        while (cursorWrapper.moveToNext()) {
            Meeting meeting = Meeting.read(context, cursorWrapper);
            points.add(getMeetingDurationPointValue(meeting));
            xAxisValues.add(getMeetingDurationXAxisValue(context, meeting));
        }
        cursor.moveToPosition(-1);

        Line line = createLine(context, points, 0);
        List<Line> lines = new ArrayList<>();
        lines.add(line);

        LineChartData lineChartData = new LineChartData();
        lineChartData.setLines(lines);
        setupChart(context,
                chart,
                xAxisValues,
                context.getString(R.string.chart_duration),
                lines);
    }

    private static PointValue getMeetingDurationPointValue(Meeting meeting) {
        String duration = DateUtils.formatElapsedTime(meeting.getDuration());
        PointValue point = new PointValue();
        point.set(meeting.getStartDate(), (float) meeting.getDuration() / (60));
        point.setLabel(duration);
        return point;
    }

    private static AxisValue getMeetingDurationXAxisValue(Context context, Meeting meeting) {
        AxisValue xAxisValue = new AxisValue(meeting.getStartDate());
        String dateString = TextUtils.formatDate(context, meeting.getStartDate());
        xAxisValue.setLabel(dateString);
        return xAxisValue;
    }

    /**
     * Create a Line with the given values.  The style of the line (color and shape) will be determined by the lineIndex
     *
     * @param values    the points of the line
     * @param lineIndex the index of the line in the graph.
     * @return a Line containing the given values, and formatted according to its position.
     */
    private static Line createLine(Context context, List<PointValue> values, int lineIndex) {
        String[] lineColors = context.getResources().getStringArray(R.array.chart_colors);
        String lineColorString = lineColors[lineIndex % lineColors.length];
        int lineColor = Color.parseColor(lineColorString);
        ValueShape shape = ValueShape.values()[lineIndex % ValueShape.values().length];
        Line line = new Line(values);
        line.setColor(lineColor);
        line.setShape(shape);
        return line;
    }

    private static void setupChart(Context context, LineChartView chart, List<AxisValue> xAxisValues, String yAxisLabel, List<Line> lines) {
        Axis xAxis = new Axis(xAxisValues);
        MeetingsGraph.setupXAxis(context, xAxis);
        Axis yAxis = new Axis();
        MeetingsGraph.setupYAxis(context, yAxisLabel, yAxis);
        LineChartData lineChartData = new LineChartData();
        lineChartData.setAxisXBottom(xAxis);
        lineChartData.setAxisYLeft(yAxis);
        lineChartData.setLines(lines);
        chart.setZoomEnabled(true);
        chart.setZoomType(ZoomType.HORIZONTAL);
        chart.setLineChartData(lineChartData);
        resetViewport(chart);
    }

    private static void resetViewport(AbstractChartView chart) {
        Viewport viewport = chart.getMaximumViewport();
        viewport.set(viewport.left, viewport.top, viewport.right, 0);
        chart.setMaximumViewport(viewport);
        viewport = chart.getCurrentViewport();
        viewport.set(viewport.left, viewport.top, viewport.right, 0);
        chart.setCurrentViewport(viewport);
    }


}
