package org.langwiki.alphatalk.debug;

import java.util.List;

public class Stats {
    public static class DescriptiveResult {
        public int n;
        public double mean;
        public double stdev;
    }

    public static <T extends Number> DescriptiveResult computeDescriptive(List<T> data) {
        DescriptiveResult desc = new DescriptiveResult();

        desc.n = data.size();
        if (desc.n == 0)
            return desc;

        for (T val : data) {
            desc.mean += val.doubleValue();
        }

        desc.mean /= desc.n;

        for (T val : data) {
            desc.stdev += (val.doubleValue() - desc.mean) * (val.doubleValue() - desc.mean);
        }

        desc.stdev /= (desc.n - 1);
        desc.stdev = Math.sqrt(desc.stdev);

        return desc;
    }
}
