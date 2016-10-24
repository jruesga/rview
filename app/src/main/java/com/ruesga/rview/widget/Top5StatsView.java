/*
 * Copyright (C) 2016 Jorge Ruesga
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ruesga.rview.widget;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.ruesga.rview.R;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.Top5ItemBinding;
import com.ruesga.rview.misc.ValueComparator;
import com.ruesga.rview.model.Stats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Top5StatsView extends LinearLayout {

    private static final int TOP  = 5;

    public interface OnTopStatsItemPressedListener {
        void onTopStatsItemPressed(String item);
    }

    private static class Value implements Comparable<Value> {
        Integer count;
        String item;

        Value(Integer count, String item) {
            this.count = count;
            this.item = item;
        }

        @Override
        public int compareTo(@NonNull Value o) {
            int ret = count.compareTo(o.count);
            if (ret != 0) {
                return ret;
            }
            return item.compareTo(o.item);
        }
    }

    @ProguardIgnored
    public static class Model {
        public String count;
        public String item;
        public String tag;
    }

    @ProguardIgnored
    public static class EventHandlers {
        private Top5StatsView mView;

        public EventHandlers(Top5StatsView view) {
            mView = view;
        }

        public void onItemPressed(View v) {
            String item = (String) v.getTag();
            mView.onItemPressed(item);
        }
    }

    private class AggregateStatsTask extends AsyncTask<Void, Void, Map<String, Value>> {
        private final List<Stats> mStats;

        AggregateStatsTask(List<Stats> stats) {
            mStats = stats;
        }

        @Override
        protected Map<String, Value> doInBackground(Void... params) {
            return aggregateStats(mStats);
        }

        @Override
        protected void onPostExecute(Map<String, Value> aggregateStats) {
            updateView(aggregateStats);
        }

        private Map<String, Value> aggregateStats(List<Stats> stats) {
            Map<String, Value> aggregatedStats = new HashMap<>();
            for (Stats s : stats) {
                if (!aggregatedStats.containsKey(s.mCrossDescription)) {
                    aggregatedStats.put(s.mCrossDescription, new Value(1, s.mCrossItem));
                } else {
                    Value v = aggregatedStats.get(s.mCrossDescription);
                    v.count = v.count + 1;
                    aggregatedStats.put(s.mCrossDescription, v);
                }
            }

            ValueComparator<String, Value> bvc = new ValueComparator<>(aggregatedStats);
            TreeMap<String, Value> map = new TreeMap<>(bvc);
            map.putAll(aggregatedStats);
            return map;
        }
    }


    private Top5ItemBinding[] mBindings = new Top5ItemBinding[TOP];
    private AggregateStatsTask mTask;
    private OnTopStatsItemPressedListener mCallback;

    public Top5StatsView(Context context) {
        this(context, null);
    }

    public Top5StatsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Top5StatsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        EventHandlers handlers = new EventHandlers(this);

        LayoutInflater inflater = LayoutInflater.from(context);
        int count = mBindings.length;
        for (int i = 0; i < count; i++) {
            mBindings[i] = DataBindingUtil.inflate(inflater, R.layout.top5_item, this, false);
            mBindings[i].setEven(i % 2 == 0);
            mBindings[i].setHandlers(handlers);
            addView(mBindings[i].getRoot());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mTask != null) {
            mTask.cancel(true);
        }
    }

    public Top5StatsView listenTo(OnTopStatsItemPressedListener cb) {
        mCallback = cb;
        return this;
    }

    public void update(List<Stats> stats) {
        if (mTask != null) {
            mTask.cancel(true);
        }

        mTask = new AggregateStatsTask(stats);
        mTask.execute();
    }

    private void updateView(Map<String, Value> aggregatedStats) {
        int i = 0;
        for (String key : aggregatedStats.keySet()) {
            int count = aggregatedStats.get(key).count;
            String item = aggregatedStats.get(key).item;

            Model model = new Model();
            model.count = String.valueOf(count);
            model.item = key;
            model.tag = item;
            mBindings[i].setModel(model);
            mBindings[i].getRoot().setVisibility(View.VISIBLE);

            i++;
            if (i >= TOP) {
                break;
            }
        }

        for (; i < TOP; i++) {
            mBindings[i].getRoot().setVisibility(View.GONE);
        }
    }

    private void onItemPressed(String item) {
        if (mCallback != null) {
            mCallback.onTopStatsItemPressed(item);
        }
    }
}
