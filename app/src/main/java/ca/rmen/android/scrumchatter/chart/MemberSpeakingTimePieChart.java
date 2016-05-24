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
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MemberCursorWrapper;
import ca.rmen.android.scrumchatter.util.TextUtils;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.view.PieChartView;

/**
 */
final class MemberSpeakingTimePieChart {
    private static final int MAX_VALUES = 10;

    private MemberSpeakingTimePieChart() {
        // prevent instantiation
    }

    public static void populateMemberSpeakingTimeChart(Context context, PieChartView pieChartAvg, PieChartView pieChartTotal, @NonNull Cursor cursor) {
        List<SliceValue> sliceValuesAvgSpeakingTime = new ArrayList<>();
        List<SliceValue> sliceValuesTotalSpeakingTime = new ArrayList<>();
        MemberCursorWrapper cursorWrapper = new MemberCursorWrapper(cursor);
        while (cursorWrapper.moveToNext()) {
            String memberName = cursorWrapper.getName();

            sliceValuesAvgSpeakingTime.add(createSliceValue(
                    cursorWrapper.getAverageDuration(),
                    memberName));

            sliceValuesTotalSpeakingTime.add(createSliceValue(
                    cursorWrapper.getSumDuration(),
                    memberName));
        }
        cursor.moveToPosition(-1);

        setupChart(context, pieChartAvg, sliceValuesAvgSpeakingTime);
        setupChart(context, pieChartTotal, sliceValuesTotalSpeakingTime);

    }

    static void updateMeetingDateRanges(Context context,
                                        TextView tvPieChartAvgSubtitle,
                                        TextView tvPieChartTotalSubtitle,
                                        Cursor cursor) {
        if (cursor.moveToFirst()) {
            long minDate = cursor.getLong(0);
            long maxDate = cursor.getLong(1);
            String minDateStr = TextUtils.formatDate(context, minDate);
            String maxDateStr = TextUtils.formatDate(context, maxDate);
            String dateRange = String.format("%s - %s", minDateStr, maxDateStr);
            tvPieChartAvgSubtitle.setText(dateRange);
            tvPieChartTotalSubtitle.setText(dateRange);
        }
    }

    private static SliceValue createSliceValue(long duration, String memberName) {
        SliceValue sliceValue = new SliceValue();
        sliceValue.setValue(duration);
        String durationString = DateUtils.formatElapsedTime(duration);
        String label = String.format("%s###%s", memberName, durationString);
        sliceValue.setLabel(label);
        return sliceValue;
    }

    private static void setupChart(Context context, PieChartView pieChartView, List<SliceValue> sliceValues) {
        PieChartData data = new PieChartData();
        data.setHasLabels(true);
        //data.setHasLabelsOutside(true);

        Collections.sort(sliceValues, SLICE_VALUE_COMPARATOR);
        while (sliceValues.size() > MAX_VALUES) {
            sliceValues.remove(sliceValues.size() - 1);
        }

        String[] lineColors = context.getResources().getStringArray(R.array.chart_colors);
        ViewGroup legendView = (ViewGroup) ((ViewGroup) pieChartView.getParent()).findViewById(R.id.legend);
        legendView.removeAllViews();
        for (int i = 0; i < sliceValues.size(); i++) {
            String colorString = lineColors[i % lineColors.length];
            int color = Color.parseColor(colorString);
            SliceValue sliceValue = sliceValues.get(i);
            String label = new String(sliceValue.getLabelAsChars());
            String memberName = label.substring(0, label.indexOf("###"));
            String duration = label.substring(label.indexOf("###") + 3);
            sliceValue.setColor(color);

            ChartUtils.addLegendEntry(context, legendView, memberName, color);
            sliceValue.setLabel(duration);
        }

        data.setValues(sliceValues);
        pieChartView.setPieChartData(data);
        pieChartView.setInteractive(false);
        pieChartView.setZoomEnabled(true);
        pieChartView.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL);
        // https://github.com/lecho/hellocharts-android/issues/268
        //pieChartView.setCircleFillRatio(0.4f);
    }

    private static final Comparator<SliceValue> SLICE_VALUE_COMPARATOR = new Comparator<SliceValue>() {
        @Override
        public int compare(SliceValue lhs, SliceValue rhs) {
            return (int) (rhs.getValue() - lhs.getValue());
        }
    };

}
