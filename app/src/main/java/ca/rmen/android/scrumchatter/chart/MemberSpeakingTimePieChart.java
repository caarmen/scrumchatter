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
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ca.rmen.android.scrumchatter.databinding.PieChartContentBinding;
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

    /**
     * The library's SliceValue doesn't have a field for what to display in the legend for a slice.
     * So, we create our own class containing both the SliceValue and the legend label for a given slice.
     */
    private static class PieChartSlice {
        final SliceValue sliceValue;
        final String legendLabel;

        public PieChartSlice(SliceValue sliceValue, String legendLabel) {
            this.sliceValue = sliceValue;
            this.legendLabel = legendLabel;
        }
    }

    public static void populateMemberSpeakingTimeChart(Context context, PieChartContentBinding pieChartAvgBinding, PieChartContentBinding pieChartTotalBinding, @NonNull Cursor cursor) {
        List<PieChartSlice> sliceValuesAvgSpeakingTime = new ArrayList<>();
        List<PieChartSlice> sliceValuesTotalSpeakingTime = new ArrayList<>();
        MemberCursorWrapper cursorWrapper = new MemberCursorWrapper(cursor);
        while (cursorWrapper.moveToNext()) {
            String memberName = cursorWrapper.getName();

            sliceValuesAvgSpeakingTime.add(createPieChartSlice(
                    context,
                    cursorWrapper.getAverageDuration(),
                    cursorWrapper.getId(),
                    memberName));

            sliceValuesTotalSpeakingTime.add(createPieChartSlice(
                    context,
                    cursorWrapper.getSumDuration(),
                    cursorWrapper.getId(),
                    memberName));
        }
        cursor.moveToPosition(-1);

        setupChart(context, pieChartAvgBinding, sliceValuesAvgSpeakingTime);
        setupChart(context, pieChartTotalBinding, sliceValuesTotalSpeakingTime);

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

    private static PieChartSlice createPieChartSlice(Context context, long duration, long memberId, String memberName) {
        SliceValue sliceValue = new SliceValue();
        sliceValue.setValue(duration);
        String durationString = DateUtils.formatElapsedTime(duration);
        sliceValue.setLabel(durationString);
        sliceValue.setColor(ChartUtils.getMemberColor(context, memberId));
        return new PieChartSlice(sliceValue, memberName);
    }

    private static void setupChart(Context context,
                                   PieChartContentBinding pieChartBinding,
                                   List<PieChartSlice> pieChartSlices) {
        PieChartData data = new PieChartData();
        data.setHasLabels(true);
        //data.setHasLabelsOutside(true);

        Collections.sort(pieChartSlices, PIE_CHART_SLICE_COMPARATOR);
        while (pieChartSlices.size() > MAX_VALUES) {
            pieChartSlices.remove(pieChartSlices.size() - 1);
        }

        List<SliceValue> sliceValues = new ArrayList<>();
        pieChartBinding.legend.removeAllViews();
        for (PieChartSlice pieChartSlice : pieChartSlices) {
            sliceValues.add(pieChartSlice.sliceValue);
            ChartUtils.addLegendEntry(context,
                    pieChartBinding.legend,
                    pieChartSlice.legendLabel,
                    pieChartSlice.sliceValue.getColor());
        }

        data.setValues(sliceValues);
        PieChartView pieChartView = pieChartBinding.memberSpeakingTimeChart;
        pieChartView.setPieChartData(data);
        pieChartView.setInteractive(false);
        pieChartView.setZoomEnabled(true);
        pieChartView.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL);
        // https://github.com/lecho/hellocharts-android/issues/268
        //pieChartView.setCircleFillRatio(0.4f);
    }

    private static final Comparator<PieChartSlice> PIE_CHART_SLICE_COMPARATOR = new Comparator<PieChartSlice>() {
        @Override
        public int compare(PieChartSlice lhs, PieChartSlice rhs) {
            return (int) (rhs.sliceValue.getValue() - lhs.sliceValue.getValue());
        }
    };

}
