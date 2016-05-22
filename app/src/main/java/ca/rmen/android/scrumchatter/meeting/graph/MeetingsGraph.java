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
import android.graphics.Color;
import android.support.v4.content.res.ResourcesCompat;

import java.util.List;

import ca.rmen.android.scrumchatter.R;
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
 * Some utility methods common to the meetings duration and member speaking time chart generation.
 */
final class MeetingsGraph {

    private MeetingsGraph() {
        // prevent instantiation
    }

    /**
     * Create a Line with the given values.  The style of the line (color and shape) will be determined by the lineIndex
     * @param values the points of the line
     * @param lineIndex the index of the line in the graph.
     * @return a Line containing the given values, and formatted according to its position.
     */
    static Line createLine(Context context, List<PointValue> values, int lineIndex) {
        String[] lineColors = context.getResources().getStringArray(R.array.chart_colors);
        String lineColorString = lineColors[lineIndex % lineColors.length];
        int lineColor = Color.parseColor(lineColorString);
        ValueShape shape = ValueShape.values()[lineIndex % ValueShape.values().length];
        Line line = new Line(values);
        line.setColor(lineColor);
        line.setShape(shape);
        return line;
    }

    static void setupChart(Context context, LineChartView chart, List<AxisValue> xAxisValues, String yAxisLabel, List<Line> lines) {
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
