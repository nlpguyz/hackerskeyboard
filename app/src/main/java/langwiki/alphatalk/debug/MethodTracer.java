package org.langwiki.alphatalk.debug;

public class MethodTracer {
    protected StatsLine.StandardColumn<Float> mStatColumn;

    private long mEnterTime;
    private long mLeaveTime;

    private static final float NANO_TO_MILLIS = 1000000.0f;

    public MethodTracer(String name) {
        mStatColumn = new StatsLine.StandardColumn<Float>(name);
        mEnterTime = -1;
    }

    public void enter() {
        mEnterTime = getTime();
    }

    public void leave() {
        mLeaveTime = getTime();
        long timeDiff = mEnterTime != -1 ? mLeaveTime - mEnterTime : 0;
        mStatColumn.addValue(timeDiff / NANO_TO_MILLIS);
    }

    protected long getTime() {
        return System.nanoTime();
    }

    public StatsLine.StandardColumn<Float> getStatColumn() {
        return mStatColumn;
    }
}
