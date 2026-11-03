public static String play(java.util.List<String> grid) {
    final int N = 15, CELLS = N * N;
    final int[] DR = {-1, 1, 0, 0, 0}, DC = {0, 0, -1, 1, 0};   // U D L R S
    final String DIRS = "UDLRS";
    final boolean[] wall = new boolean[CELLS];
    // state: [0]=me, [1]=rival, [2]=food left, [3]=food I ate, [4 + cell]=1 if food on cell
    int[] st = new int[4 + CELLS];
    for (int r = 0; r < N; r++)
        for (int c = 0; c < N; c++) {
            char ch = grid.get(r).charAt(c);
            int x = r * N + c;
            if (ch == '#') wall[x] = true;
            else if (ch == 'P') st[0] = x;
            else if (ch == 'O') st[1] = x;
            else if (ch == 'F') { st[4 + x] = 1; st[2]++; }
        }

    // neighbour of cell in direction d (d = 4 means stay), or -1 if off-grid / wall
    java.util.function.IntBinaryOperator nxt = (cell, d) -> {
        int r = cell / N + DR[d], c = cell % N + DC[d];
        return (r < 0 || r >= N || c < 0 || c >= N || wall[r * N + c]) ? -1 : r * N + c;
    };
    // multi-source BFS avoiding walls and the `blocked` cell; -1 = unreachable
    java.util.function.BiFunction<int[], Integer, int[]> bfs = (src, blocked) -> {
        int[] dist = new int[CELLS], q = new int[CELLS];
        java.util.Arrays.fill(dist, -1);
        int h = 0, t = 0;
        for (int s : src) if (dist[s] < 0) { dist[s] = 0; q[t++] = s; }
        while (h < t) {
            int u = q[h++];
            for (int d = 0; d < 4; d++) {
                int v = nxt.applyAsInt(u, d);
                if (v >= 0 && v != blocked && dist[v] < 0) { dist[v] = dist[u] + 1; q[t++] = v; }
            }
        }
        return dist;
    };
    // first step (U>D>L>R priority) on a shortest path from `from` to the nearest cell in `targets`,
    // treating `blocked` as a wall; returns 4 (stay) if no target is reachable
    java.util.function.BiFunction<int[], java.util.List<Integer>, Integer> stepToward = (fb, targets) -> {
        int from = fb[0], blocked = fb[1];
        int[] d0 = bfs.apply(new int[]{from}, blocked);
        int best = Integer.MAX_VALUE;
        for (int f : targets) if (d0[f] > 0) best = Math.min(best, d0[f]);
        if (best == Integer.MAX_VALUE) return 4;
        java.util.List<Integer> near = new java.util.ArrayList<>();
        for (int f : targets) if (d0[f] == best) near.add(f);
        int[] dn = bfs.apply(near.stream().mapToInt(Integer::intValue).toArray(), blocked);
        for (int d = 0; d < 4; d++) {
            int v = nxt.applyAsInt(from, d);
            if (v >= 0 && v != blocked && dn[v] == best - 1) return d;
        }
        return 4;
    };
    java.util.function.Function<int[], java.util.List<Integer>> foods = s -> {
        java.util.List<Integer> fs = new java.util.ArrayList<>();
        for (int x = 0; x < CELLS; x++) if (s[4 + x] == 1) fs.add(x);
        return fs;
    };
    // one full round: my move d, then the rival's deterministic move (if food remains)
    java.util.function.ObjIntConsumer<int[]> round = (s, d) -> {
        if (d < 4) s[0] = nxt.applyAsInt(s[0], d);
        if (s[4 + s[0]] == 1) { s[4 + s[0]] = 0; s[2]--; s[3]++; }
        if (s[2] == 0) return;
        int rd = stepToward.apply(new int[]{s[1], s[0]}, foods.apply(s));
        if (rd < 4) s[1] = nxt.applyAsInt(s[1], rd);
        if (s[4 + s[1]] == 1) { s[4 + s[1]] = 0; s[2]--; }
    };
    // base policy: go for the nearest food I reach no later than the rival; else nearest food;
    // if nothing is reachable (rival blocks the way), step aside to break the deadlock
    java.util.function.ToIntFunction<int[]> greedy = s -> {
        java.util.List<Integer> all = foods.apply(s), mine = new java.util.ArrayList<>();
        int[] dm = bfs.apply(new int[]{s[0]}, s[1]), dr = bfs.apply(new int[]{s[1]}, s[0]);
        for (int f : all) if (dm[f] >= 0 && (dr[f] < 0 || dm[f] <= dr[f])) mine.add(f);
        int d = stepToward.apply(new int[]{s[0], s[1]}, mine.isEmpty() ? all : mine);
        if (d == 4)
            for (int k = 0; k < 4; k++) {
                int v = nxt.applyAsInt(s[0], k);
                if (v >= 0 && v != s[1]) return k;
            }
        return d;
    };

    // rollout-based one-step lookahead: try every legal move, finish the game with the greedy
    // policy (the rival is deterministic, so this is an exact simulation), keep the best move
    final int CAP = 400;
    final long deadline = System.currentTimeMillis() + 8000;
    StringBuilder out = new StringBuilder();
    while (st[2] > 0 && out.length() < 100000) {
        int bestD = greedy.applyAsInt(st);
        if (System.currentTimeMillis() < deadline) {
            double bestScore = Double.NEGATIVE_INFINITY;
            for (int d = 0; d <= 4; d++) {
                if (d < 4) {
                    int v = nxt.applyAsInt(st[0], d);
                    if (v < 0 || v == st[1]) continue;
                }
                int[] s = st.clone();
                round.accept(s, d);
                int k = 0;
                while (s[2] > 0 && k < CAP) { round.accept(s, greedy.applyAsInt(s)); k++; }
                double score = s[3] - (s[2] > 0 ? 1000 : 0) - k * 1e-4;   // more food, must finish, then faster
                if (score > bestScore) { bestScore = score; bestD = d; }
            }
        }
        out.append(DIRS.charAt(bestD));
        round.accept(st, bestD);
    }
    return out.toString();
}
