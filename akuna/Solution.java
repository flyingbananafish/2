public static long maxProfit(java.util.List<java.util.List<String>> prices) {
    int n = prices.size();
    String[] stock = new String[n];
    int[] date = new int[n];
    double[] price = new double[n];
    Integer[] order = new Integer[n];
    for (int i = 0; i < n; i++) {
        java.util.List<String> row = prices.get(i);
        stock[i] = row.get(0).trim();
        String[] mdy = row.get(1).trim().split("/");           // MM/DD/YYYY
        date[i] = Integer.parseInt(mdy[2].trim()) * 10000
                + Integer.parseInt(mdy[0].trim()) * 100
                + Integer.parseInt(mdy[1].trim());
        price[i] = Double.parseDouble(row.get(2).trim());
        order[i] = i;
    }
    java.util.Arrays.sort(order, (a, b) -> Integer.compare(date[a], date[b]));

    double cash = 1000.0;                                       // best cash we can hold now
    java.util.Map<String, Double> shares = new java.util.HashMap<>(); // best #shares of each stock
    for (int i = 0; i < n; ) {
        int j = i;
        while (j < n && date[order[j]] == date[order[i]]) j++;  // [i, j) = same day
        double newCash = cash;                                  // 1) sell anything priced today
        for (int k = i; k < j; k++) {
            int t = order[k];
            Double s = shares.get(stock[t]);
            if (s != null) newCash = Math.max(newCash, s * price[t]);
        }
        cash = newCash;
        for (int k = i; k < j; k++) {                           // 2) buy with best cash today
            int t = order[k];
            if (price[t] > 0) shares.merge(stock[t], cash / price[t], Math::max);
        }
        i = j;
    }
    return Math.round(cash - 1000.0);
}
