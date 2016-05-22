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
package ca.rmen.android.scrumchatter.member.graph;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MemberCursorWrapper;
import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.view.PieChartView;

/**
 */
final class MemberSpeakingTimePieChart {
    // prevent instantiation
    public static void populateMemberSpeakingTimeChart(Context context, PieChartView avgSpeakingTimeChart, PieChartView totalSpeakingTimeChart, @NonNull Cursor cursor) {
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

        setupChart(context, avgSpeakingTimeChart, sliceValuesAvgSpeakingTime);
        setupChart(context, totalSpeakingTimeChart, sliceValuesTotalSpeakingTime);

    }

    private static SliceValue createSliceValue(int duration, String memberName) {
        SliceValue sliceValue = new SliceValue();
        sliceValue.setValue(duration);
        String durationString = DateUtils.formatElapsedTime(duration);
        String label = String.format("%s (%s)", memberName, durationString);
        sliceValue.setLabel(label);
        return sliceValue;
    }

    private static void setupChart(Context context, PieChartView pieChartView, List<SliceValue> sliceValues) {
        PieChartData data = new PieChartData();
        data.setHasLabels(true);
        data.setHasLabelsOutside(true);
        data.setHasCenterCircle(false);

        Collections.sort(sliceValues, SLICE_VALUE_COMPARATOR);

        String[] lineColors = context.getResources().getStringArray(R.array.chart_colors);
        for (int i=0; i < sliceValues.size(); i++) {
            String colorString = lineColors[i % lineColors.length];
            int color = Color.parseColor(colorString);
            sliceValues.get(i).setColor(color);
        }

        data.setValues(sliceValues);
        pieChartView.setPieChartData(data);
        pieChartView.setInteractive(false);
        // https://github.com/lecho/hellocharts-android/issues/268
        pieChartView.setCircleFillRatio(0.7f);
    }

    private static final Comparator<SliceValue>  SLICE_VALUE_COMPARATOR = new Comparator<SliceValue>() {
        @Override
        public int compare(SliceValue lhs, SliceValue rhs) {
            return (int) (rhs.getValue() - lhs.getValue());
        }
    };

}
