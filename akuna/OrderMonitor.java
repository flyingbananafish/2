// 假设的事件格式（原题只有一句描述，事件格式需以考试时为准）：
//   NEW,<orderId>,<symbol>,<BUY|SELL>,<qty>   挂单
//   CANCEL,<orderId>                          撤单（撤掉剩余未成交部分）
//   FILL,<orderId>,<qty>                      成交（可部分成交）
// marking position(symbol) = 已持有股数 − 所有未成交卖单的剩余数量
public static java.util.Map<String, Long> markingPositions(java.util.List<String> events) {
    java.util.Map<String, Long> held = new java.util.HashMap<>();       // 已成交后的持仓
    java.util.Map<String, Long> openSell = new java.util.HashMap<>();   // 未成交卖单剩余量
    java.util.Map<String, String[]> orders = new java.util.HashMap<>(); // id -> {symbol, side}
    java.util.Map<String, Long> remaining = new java.util.HashMap<>();  // id -> 剩余数量
    java.util.Map<String, Long> marking = new java.util.TreeMap<>();
    for (String e : events) {
        String[] p = e.trim().split("\\s*,\\s*");
        String type = p[0].toUpperCase();
        String sym;
        if (type.equals("NEW")) {
            String id = p[1], side = p[3].toUpperCase();
            long qty = Long.parseLong(p[4]);
            sym = p[2];
            orders.put(id, new String[]{sym, side});
            remaining.put(id, qty);
            if (side.equals("SELL")) openSell.merge(sym, qty, Long::sum);
        } else {
            String id = p[1];
            String[] o = orders.get(id);
            if (o == null) continue;                              // 未知订单，忽略
            sym = o[0];
            long rem = remaining.get(id);
            long qty = type.equals("FILL") ? Math.min(rem, Long.parseLong(p[2])) : rem;
            remaining.put(id, rem - qty);
            if (o[1].equals("SELL")) openSell.merge(sym, -qty, Long::sum);
            if (type.equals("FILL")) held.merge(sym, o[1].equals("BUY") ? qty : -qty, Long::sum);
            if (rem - qty == 0) { orders.remove(id); remaining.remove(id); }
        }
        marking.put(sym, held.getOrDefault(sym, 0L) - openSell.getOrDefault(sym, 0L));
    }
    return marking;
}
