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
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.format.DateUtils;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.meeting.detail.Meeting;
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper;
import ca.rmen.android.scrumchatter.util.TextUtils;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;


/**
 * Populates a line graph with meetings.
 */
final class MeetingsGraph {

    private MeetingsGraph() {
        // prevent instantiation
    }

    public static void populateMeetingDurationGraph(Context context, LineChartView chart, @NonNull Cursor cursor) {
        List<PointValue> points = new ArrayList<>();
        List<AxisValue> axisValues = new ArrayList<>();

        MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(cursor);
        while (cursorWrapper.moveToNext()) {
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

        setupChart(context,
                chart,
                axisValues,
                context.getString(R.string.chart_duration),
                lines);
    }

    public static void populateMemberSpeakingTimeGraph(Context context, LineChartView chart, ViewGroup legendView, @NonNull Cursor cursor) {
        List<AxisValue> axisValues = new ArrayList<>();
        Map<String, List<PointValue>> memberLines = new HashMap<>();

        MeetingMemberCursorWrapper cursorWrapper = new MeetingMemberCursorWrapper(cursor);
        while (cursorWrapper.moveToNext()) {
            long currentMeetingId = cursorWrapper.getMeetingId();
            do {
                long meetingId = cursorWrapper.getMeetingId();
                if (meetingId != currentMeetingId) {
                    cursorWrapper.move(-1);
                    break;
                }
                String memberName = cursorWrapper.getMemberName();
                List<PointValue> memberPoints = memberLines.get(memberName);
                if (memberPoints == null) {
                    memberPoints = new ArrayList<>();
                    memberLines.put(memberName, memberPoints);
                }
                PointValue point = new PointValue();
                point.set(cursorWrapper.getMeetingDate(), (float) cursorWrapper.getDuration() / 60);
                String duration = DateUtils.formatElapsedTime(cursorWrapper.getDuration());
                point.setLabel(duration);
                memberPoints.add(point);
                AxisValue axisValue = new AxisValue(cursorWrapper.getMeetingDate());
                String dateString = TextUtils.formatDate(context, cursorWrapper.getMeetingDate());
                axisValue.setLabel(dateString);
                axisValues.add(axisValue);
            } while (cursorWrapper.moveToNext());
        }
        cursor.moveToPosition(-1);
        List<Line> lines = new ArrayList<>();
        int i = 0;
        String[] lineColors = context.getResources().getStringArray(R.array.chart_colors);
        for (Map.Entry<String, List<PointValue>> memberLine : memberLines.entrySet()) {
            Line line = new Line(memberLine.getValue());
            lines.add(line);
            String lineColorString = lineColors[i % lineColors.length];
            int lineColor = Color.parseColor(lineColorString);
            ValueShape shape = ValueShape.values()[i % ValueShape.values().length];
            line.setColor(lineColor);
            line.setShape(shape);
            addLegendEntry(context, legendView, memberLine.getKey(), lineColor, shape);
            i++;
        }
        legendView.getParent().requestLayout();
        setupChart(context,
                chart,
                axisValues,
                context.getString(R.string.chart_speaking_time),
                lines);

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

    private static void setupChart(Context context, LineChartView chart, List<AxisValue> xAxisValues, String yAxisLabel, List<Line> lines) {
        Axis xAxis = new Axis(xAxisValues);
        setupXAxis(context, xAxis);
        Axis yAxis = new Axis();
        setupYAxis(context, yAxisLabel, yAxis);
        LineChartData lineChartData = new LineChartData();
        lineChartData.setAxisXBottom(xAxis);
        lineChartData.setAxisYLeft(yAxis);
        lineChartData.setLines(lines);

        chart.setZoomEnabled(true);
        chart.setZoomType(ZoomType.HORIZONTAL);
        chart.setLineChartData(lineChartData);
        resetViewport(chart);
    }


    private static void setupXAxis(Context context, Axis xAxis) {
        xAxis.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.primary_text_color, null));
        xAxis.setHasTiltedLabels(true);
        xAxis.setName(context.getString(R.string.chart_date));
        xAxis.setMaxLabelChars(10);
    }

    private static void setupYAxis(Context context, String yAxisLabel, Axis yAxis) {
        yAxis.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.primary_text_color, null));
        yAxis.setName(yAxisLabel);
    }

    private static void resetViewport(LineChartView chart) {
        Viewport viewport = chart.getMaximumViewport();
        viewport.set(viewport.left, viewport.top, viewport.right, 0);
        chart.setMaximumViewport(viewport);
        viewport = chart.getCurrentViewport();
        viewport.set(viewport.left, viewport.top, viewport.right, 0);
        chart.setCurrentViewport(viewport);
    }

}
