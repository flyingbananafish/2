// 暴力：枚举所有交易序列。每笔交易 = 选一只股票的两个报价 (买 d1 < 卖 d2)，
// 下一笔买入日期 >= 上一笔卖出日期（允许同一天卖出再买入）。指数复杂度，仅用于小数据对拍。
public static double bruteForce(java.util.List<java.util.List<String>> prices) {
    int n = prices.size();
    String[] st = new String[n]; int[] d = new int[n]; double[] p = new double[n];
    for (int i = 0; i < n; i++) {
        st[i] = prices.get(i).get(0).trim();
        String[] m = prices.get(i).get(1).trim().split("/");
        d[i] = Integer.parseInt(m[2]) * 10000 + Integer.parseInt(m[0]) * 100 + Integer.parseInt(m[1]);
        p[i] = Double.parseDouble(prices.get(i).get(2).trim());
    }
    double[] best = {1000.0};
    java.util.function.BiConsumer<Integer, Double>[] dfs = new java.util.function.BiConsumer[1];
    dfs[0] = (from, cash) -> {
        best[0] = Math.max(best[0], cash);                       // 选择就此停手
        for (int b = 0; b < n; b++) {
            if (d[b] < from) continue;
            for (int s = 0; s < n; s++) {
                if (st[s].equals(st[b]) && d[s] > d[b]) dfs[0].accept(d[s], cash / p[b] * p[s]);
            }
        }
    };
    dfs[0].accept(Integer.MIN_VALUE, 1000.0);
    return best[0] - 1000.0;
}
