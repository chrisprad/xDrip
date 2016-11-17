package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.PathDashPathEffect;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;

/**
 * Created by stephenblack on 11/15/14.
 */
public class BgGraphBuilder {
    private int timespan;
    public double end_time;
    public double start_time;
    public double fuzzyTimeDenom = (1000 * 60 * 1);
    public Context context;
    public double highMark;
    public double lowMark;
    public List<BgWatchData> bgDataList = new ArrayList<BgWatchData>();

    public int pointSize;
    public int highColor;
    public int lowColor;
    public int midColor;
    public boolean singleLine = false;

    private double endHour;
    private List<PointValue> inRangeValues = new ArrayList<PointValue>();
    private List<PointValue> highValues = new ArrayList<PointValue>();
    private List<PointValue> lowValues = new ArrayList<PointValue>();
    public Viewport viewport;

    public BgGraphBuilder(Context context, List<BgWatchData> aBgList, int aPointSize, int aMidColor, int timespan) {
        end_time = new Date().getTime() + (1000 * 60 * 6 * timespan); //Now plus 30 minutes padding (for 5 hours. Less if less.)
        start_time = new Date().getTime()  - (1000 * 60 * 60 * timespan); //timespan hours ago
        this.bgDataList = aBgList;
        this.context = context;
        this.highMark = aBgList.get(aBgList.size() - 1).high;
        this.lowMark = aBgList.get(aBgList.size() - 1).low;
        this.pointSize = aPointSize;
        this.singleLine = true;
        this.midColor = aMidColor;
        this.lowColor = aMidColor;
        this.highColor = aMidColor;
        this.timespan = timespan;
    }

    public BgGraphBuilder(Context context, List<BgWatchData> aBgList, int aPointSize, int aHighColor, int aLowColor, int aMidColor, int timespan) {
        end_time = new Date().getTime() + (1000 * 60 * 6 * timespan); //Now plus 30 minutes padding (for 5 hours. Less if less.)
        start_time = new Date().getTime()  - (1000 * 60 * 60 * timespan); //timespan hours ago
        this.bgDataList = aBgList;
        this.context = context;
        this.highMark = aBgList.get(aBgList.size() - 1).high;
        this.lowMark = aBgList.get(aBgList.size() - 1).low;
        this.pointSize = aPointSize;
        this.highColor = aHighColor;
        this.lowColor = aLowColor;
        this.midColor = aMidColor;
        this.timespan = timespan;
    }

    public LineChartData lineData() {
        LineChartData lineData = new LineChartData(defaultLines());
        lineData.setAxisYLeft(yAxis());
        lineData.setAxisXBottom(xAxis());
        return lineData;
    }

    public List<Line> defaultLines() {
        addBgReadingValues();
        List<Line> lines = new ArrayList<Line>();
        lines.add(highLine());
        lines.add(lowLine());
        lines.add(inRangeValuesLine());
        lines.add(lowValuesLine());
        lines.add(highValuesLine());
        lines.add(basalValuesLine());

        //TODO: foreach basal deviation: add:

        float basal = (float) (lowMark + 0.25*(highMark - lowMark));
        float temp = (float) (lowMark + 0.15*(highMark - lowMark));
        float begin = (float) (start_time + 0.12*(end_time-start_time));
        float end = (float) (start_time + 0.2*(end_time-start_time));
        lines.add(addBasalDeviation(basal,basal, temp, begin, end));

        temp = (float) (lowMark + 0.0*(highMark - lowMark));
        begin = (float) (start_time + 0.2*(end_time-start_time));
        end = (float) (start_time + 0.3*(end_time-start_time));
        lines.add(addBasalDeviation(basal,basal, temp, begin, end));

        basal = (float) (lowMark + 0.3*(highMark - lowMark));
        temp = (float) (lowMark + 0.75*(highMark - lowMark));
        begin = (float) (start_time + 0.7*(end_time-start_time));
        end = (float) (start_time + 0.85*(end_time-start_time));
        lines.add(addBasalDeviation(basal,basal, temp, begin, end));

        return lines;
    }

    public Line highValuesLine() {
        Line highValuesLine = new Line(highValues);
        highValuesLine.setColor(highColor);
        highValuesLine.setHasLines(false);
        highValuesLine.setPointRadius(pointSize);
        highValuesLine.setHasPoints(true);
        return highValuesLine;
    }

    public Line lowValuesLine() {
        Line lowValuesLine = new Line(lowValues);
        lowValuesLine.setColor(lowColor);
        lowValuesLine.setHasLines(false);
        lowValuesLine.setPointRadius(pointSize);
        lowValuesLine.setHasPoints(true);
        return lowValuesLine;
    }

    public Line basalValuesLine() {
        List<PointValue> lowLineValues = new ArrayList<PointValue>();
        lowLineValues.add(new PointValue(fuzz(start_time), (float) (lowMark + 0.25*(highMark - lowMark))));
        lowLineValues.add(new PointValue(fuzz((start_time+end_time)/2), (float) (lowMark + 0.25*(highMark - lowMark))));
        lowLineValues.add(new PointValue(fuzz((start_time+end_time)/2), (float) (lowMark + 0.3*(highMark - lowMark))));
        lowLineValues.add(new PointValue(fuzz(end_time), (float) (lowMark + 0.3*(highMark - lowMark))));
        Line lowLine = new Line(lowLineValues);
        lowLine.setHasPoints(false);
        lowLine.setColor(Color.parseColor("#00BFFF"));
        lowLine.setPathEffect(new DashPathEffect(new float[]{4f, 3f}, 4f));
        lowLine.setStrokeWidth(1);
        return lowLine;
    }

    public Line addBasalDeviation(float basalStart, float basalEnd, float absoluteTemp, float from, float to){
        List<PointValue> valueList = new ArrayList<PointValue>();
        valueList.add(new PointValue(fuzz(from), basalStart ));
        valueList.add(new PointValue(fuzz(from), absoluteTemp ));
        valueList.add(new PointValue(fuzz(to), absoluteTemp ));
        valueList.add(new PointValue(fuzz(to), basalEnd ));
        Line valueLine = new Line(valueList);
        valueLine.setHasPoints(false);
        valueLine.setColor(Color.BLUE);
        valueLine.setStrokeWidth(1);
        return valueLine;
    }

    public Line inRangeValuesLine() {
        Line inRangeValuesLine = new Line(inRangeValues);
        inRangeValuesLine.setColor(midColor);
        if(singleLine) {
            inRangeValuesLine.setHasLines(true);
            inRangeValuesLine.setHasPoints(false);
            inRangeValuesLine.setStrokeWidth(pointSize);
        } else {
            inRangeValuesLine.setPointRadius(pointSize);
            inRangeValuesLine.setHasPoints(true);
            inRangeValuesLine.setHasLines(false);
        }
        return inRangeValuesLine;
    }

    private void addBgReadingValues() {
        if(singleLine) {
            for (BgWatchData bgReading : bgDataList) {
                if(bgReading.timestamp > start_time) {
                    if (bgReading.sgv >= 400) {
                        inRangeValues.add(new PointValue(fuzz(bgReading.timestamp), (float) 400));
                    } else if (bgReading.sgv >= highMark) {
                        inRangeValues.add(new PointValue(fuzz(bgReading.timestamp), (float) bgReading.sgv));
                    } else if (bgReading.sgv >= lowMark) {
                        inRangeValues.add(new PointValue(fuzz(bgReading.timestamp), (float) bgReading.sgv));
                    } else if (bgReading.sgv >= 40) {
                        inRangeValues.add(new PointValue(fuzz(bgReading.timestamp), (float) bgReading.sgv));
                    } else if (bgReading.sgv >= 11) {
                        inRangeValues.add(new PointValue(fuzz(bgReading.timestamp), (float) 40));
                    }
                }
            }
        } else {
            for (BgWatchData bgReading : bgDataList) {
                if (bgReading.timestamp > start_time) {
                    if (bgReading.sgv >= 400) {
                        highValues.add(new PointValue(fuzz(bgReading.timestamp), (float) 400));
                    } else if (bgReading.sgv >= highMark) {
                        highValues.add(new PointValue(fuzz(bgReading.timestamp), (float) bgReading.sgv));
                    } else if (bgReading.sgv >= lowMark) {
                        inRangeValues.add(new PointValue(fuzz(bgReading.timestamp), (float) bgReading.sgv));
                    } else if (bgReading.sgv >= 40) {
                        lowValues.add(new PointValue(fuzz(bgReading.timestamp), (float) bgReading.sgv));
                    } else if (bgReading.sgv >= 11) {
                        lowValues.add(new PointValue(fuzz(bgReading.timestamp), (float) 40));
                    }
                }
            }
        }
    }

    public Line highLine() {
        List<PointValue> highLineValues = new ArrayList<PointValue>();
        highLineValues.add(new PointValue(fuzz(start_time), (float) highMark));
        highLineValues.add(new PointValue(fuzz(end_time), (float) highMark));
        Line highLine = new Line(highLineValues);
        highLine.setHasPoints(false);
        highLine.setStrokeWidth(1);
        highLine.setColor(highColor);
        return highLine;
    }

    public Line lowLine() {
        List<PointValue> lowLineValues = new ArrayList<PointValue>();
        lowLineValues.add(new PointValue(fuzz(start_time), (float) lowMark));
        lowLineValues.add(new PointValue(fuzz(end_time), (float) lowMark));
        Line lowLine = new Line(lowLineValues);
        lowLine.setHasPoints(false);
        lowLine.setColor(lowColor);
        lowLine.setStrokeWidth(1);
        return lowLine;
    }

    /////////AXIS RELATED//////////////
    public Axis yAxis() {
        Axis yAxis = new Axis();
        yAxis.setAutoGenerated(true);
        List<AxisValue> axisValues = new ArrayList<AxisValue>();
        yAxis.setValues(axisValues);
        yAxis.setHasLines(false);
        return yAxis;
    }

    public Axis xAxis() {
        final boolean is24 = DateFormat.is24HourFormat(context);
        Axis xAxis = new Axis();
        xAxis.setAutoGenerated(false);
        List<AxisValue> xAxisValues = new ArrayList<AxisValue>();
        GregorianCalendar now = new GregorianCalendar();
        GregorianCalendar today = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        SimpleDateFormat timeFormat = new SimpleDateFormat(is24? "HH" : "h a");
        timeFormat.setTimeZone(TimeZone.getDefault());
        double start_hour = today.getTime().getTime();
        double timeNow = new Date().getTime();
        for (int l = 0; l <= 24; l++) {
            if ((start_hour + (60000 * 60 * (l))) < timeNow) {
                if ((start_hour + (60000 * 60 * (l + 1))) >= timeNow) {
                    endHour = start_hour + (60000 * 60 * (l));
                    l = 25;
                }
            }
        }
        //Display current time on the graph
        SimpleDateFormat longTimeFormat = new SimpleDateFormat(is24? "HH:mm" : "h:mm a");
        xAxisValues.add(new AxisValue(fuzz(timeNow), (longTimeFormat.format(timeNow)).toCharArray()));

        //Add whole hours to the axis (as long as they are more than 15 mins away from the current time)
        for (int l = 0; l <= 24; l++) {
            double timestamp = endHour - (60000 * 60 * l);
            if((timestamp - timeNow < 0) && (timestamp > start_time)) {
                if(Math.abs(timestamp - timeNow) > (1000 * 60 * 8 * timespan)){
                    xAxisValues.add(new AxisValue(fuzz(timestamp), (timeFormat.format(timestamp)).toCharArray()));
                }else {
                    xAxisValues.add(new AxisValue(fuzz(timestamp), "".toCharArray()));
                }
            }
        }
        xAxis.setValues(xAxisValues);
        xAxis.setTextSize(10);
        xAxis.setHasLines(true);
        return xAxis;
    }

    public float fuzz(double value) {
        return (float) Math.round(value / fuzzyTimeDenom);
    }
}
