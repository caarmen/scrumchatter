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
import android.database.Cursor;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper;
import ca.rmen.android.scrumchatter.util.TextUtils;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.view.ColumnChartView;

/**
 * The member speaking-time chart has one stacked column per meeting, with each column having stacked
 * boxes for each member (the time the member spoke during that meeting).
 */
final class MemberSpeakingTimeColumnChart {
    private MemberSpeakingTimeColumnChart() {
        // prevent instantiation
    }

    public static void populateMemberSpeakingTimeChart(Context context, ColumnChartView chart, ViewGroup legendView, @NonNull Cursor cursor) {
        List<AxisValue> xAxisValues = new ArrayList<>();
        List<Column> columns = new ArrayList<>();

        MeetingMemberCursorWrapper cursorWrapper = new MeetingMemberCursorWrapper(cursor);
        Map<String, Integer> memberColors = buildMemberColorMap(context, cursorWrapper);
        long lastMeetingId = -1;
        List<SubcolumnValue> subcolumnValues = new ArrayList<>();
        while (cursorWrapper.moveToNext()) {
            do {
                long currentMeetingId = cursorWrapper.getMeetingId();
                if (currentMeetingId != lastMeetingId) {
                    if (!subcolumnValues.isEmpty()) {
                        Column column = new Column(subcolumnValues);
                        column.setHasLabelsOnlyForSelected(true);
                        columns.add(column);
                        AxisValue xAxisValue = new AxisValue(xAxisValues.size());
                        String dateString = TextUtils.formatDate(context, cursorWrapper.getMeetingDate());
                        xAxisValue.setLabel(dateString);
                        xAxisValues.add(xAxisValue);
                    }
                    subcolumnValues = new ArrayList<>();
                }
                String memberName = cursorWrapper.getMemberName();
                SubcolumnValue subcolumnValue = new SubcolumnValue();
                String durationString = DateUtils.formatElapsedTime(cursorWrapper.getDuration());
                subcolumnValue.setLabel(String.format("%s (%s)", memberName, durationString));
                subcolumnValue.setColor(memberColors.get(memberName));
                subcolumnValue.setValue((float) cursorWrapper.getDuration() / 60);
                subcolumnValues.add(subcolumnValue);
                lastMeetingId = currentMeetingId;
            } while (cursorWrapper.moveToNext());
        }
        cursor.moveToPosition(-1);

        legendView.removeAllViews();
        for (String memberName : memberColors.keySet()) {
            ChartUtils.addLegendEntry(context, legendView, memberName, memberColors.get(memberName));
        }

        setupChart(context,
                chart,
                xAxisValues,
                context.getString(R.string.chart_speaking_time),
                columns);

    }

    private static Map<String, Integer> buildMemberColorMap(Context context, MeetingMemberCursorWrapper cursorWrapper) {
        Set<String> memberNames = new TreeSet<>();
        while (cursorWrapper.moveToNext()) {
            memberNames.add(cursorWrapper.getMemberName());
        }
        cursorWrapper.moveToPosition(-1);
        LinkedHashMap<String, Integer> memberColors = new LinkedHashMap<>();
        int index = 0;
        for (String memberName : memberNames) {
            memberColors.put(memberName, getColor(context, index++));
        }
        return memberColors;
    }

    private static int getColor(Context context, int index) {
        String[] colors = context.getResources().getStringArray(R.array.chart_colors);
        String colorString = colors[index % colors.length];
        return Color.parseColor(colorString);
    }

    private static void setupChart(Context context, ColumnChartView chart, List<AxisValue> xAxisValues, String yAxisLabel, List<Column> columns) {
        Axis xAxis = new Axis(xAxisValues);
        ChartUtils.setupXAxis(context, xAxis);
        Axis yAxis = new Axis();
        ChartUtils.setupYAxis(context, yAxisLabel, yAxis);
        ColumnChartData data = new ColumnChartData();
        data.setAxisXBottom(xAxis);
        data.setAxisYLeft(yAxis);
        data.setColumns(columns);
        data.setStacked(true);
        chart.setInteractive(true);
        chart.setColumnChartData(data);
        chart.setValueSelectionEnabled(true);
        chart.setZoomEnabled(true);
        chart.setZoomType(ZoomType.HORIZONTAL);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        float numberVisibleColumns = displayMetrics.widthPixels / context.getResources().getDimension(R.dimen.column_chart_column_width);
        float zoomLevel = columns.size() / numberVisibleColumns;
        if (zoomLevel > 0) {
            chart.setZoomLevel(chart.getMaximumViewport().right - 1, 0.0f, zoomLevel);
        }
    }

}
