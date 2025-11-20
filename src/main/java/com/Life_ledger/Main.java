import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Main {

    public static void main(String[] args) {

        String apiKey = "AIzaSyBkrrUvK5O8drmXq45nTGfQ4qUZX8VPkmA";
        String modelName = "gemini-2.5-flash";

        try {
            // Read JSON file
            String jsonData = new String(Files.readAllBytes(Paths.get("lifeledger.json")));

            System.out.println("\n====== STEP 1: Categorization Using Gemini ======\n");

            // ---- CATEGORIZATION LOGIC ----
            String categoryOutput = categorize(apiKey, modelName, jsonData);
            System.out.println(categoryOutput);


            System.out.println("\n====== STEP 2: Recurring Detection Logic ======\n");

            // ---- EXTRACT TRANSACTIONS ----
            List<Transaction> txList = extractTransactions(jsonData);

            if (txList.isEmpty()) {
                System.out.println("[]  // No transactions extracted.");
                return;
            }


            // Group by merchant
            Map<String, List<Transaction>> grouped = groupByMerchant(txList);

            // Recurring detection
            List<Map<String, Object>> recurring = getRecurringGroups(grouped);

            // Print recurring result
            System.out.println(toJson(recurring));

            System.out.println("\n====== STEP 3: Anomaly Detection ======\n");

            List<Map<String, Object>> anomalies = detectAnomalies(txList);

            if (anomalies.isEmpty()) {
                System.out.println("[]  // No anomalies detected.");
            } else {
                System.out.println(toJsonAnomalies(anomalies));
            }
            System.out.println("\n====== STEP 4: Insight Generation ======\n");

            Map<String, Double> categoryTotals = getCategoryTotals(txList);
            Map<String, Double> monthlyTotals = getMonthlyTotals(txList);

            List<String> insights = generateInsights(categoryTotals, monthlyTotals);

            System.out.println(insightSummary(insights));


        } catch (IOException e) {
            System.err.println("Error reading lifeledger.json: " + e.getMessage());
        }
    }

    // ===================================================================
    //  CATEGORIZATION USING SIMPLE RULES
    // ===================================================================
    public static String categorize(String apiKey, String modelName, String data) {
        List<Transaction> transactions = extractTransactions(data);
        StringBuilder result = new StringBuilder("[\n");
        
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            String category = getCategoryForMerchant(t.merchant);
            
            if (i > 0) result.append(",\n");
            result.append(String.format(
                "  {\"id\": \"%s\", \"merchant\": \"%s\", \"category\": \"%s\"}",
                t.id, t.merchant, category
            ));
        }
        
        result.append("\n]");
        return result.toString();
    }
    
    private static String getCategoryForMerchant(String merchant) {
        merchant = merchant.toLowerCase();
        if (merchant.contains("zomato") || merchant.contains("swiggy") || merchant.contains("domino") ||
            merchant.contains("starbucks") || merchant.contains("subway") || merchant.contains("mcdonald") ||
            merchant.contains("cafe coffee day")) return "Food";
        if (merchant.contains("uber") || merchant.contains("ola")) return "Transport";
        if (merchant.contains("amazon") || merchant.contains("flipkart") || merchant.contains("d-mart") ||
            merchant.contains("blinkit") || merchant.contains("reliance trends")) return "Shopping";
        if (merchant.contains("jiofiber")) return "Utilities";
        if (merchant.contains("netflix") || merchant.contains("spotify") || merchant.contains("pvr") ||
            merchant.contains("bookmyshow")) return "Entertainment";
        if (merchant.contains("medical")) return "Health";
        if (merchant.contains("gym")) return "Other";
        return "Other";
    }

    // ===================================================================
    //  RECURRING DETECTION LOGIC
    // ===================================================================

    private static List<Transaction> extractTransactions(String data) {

        Pattern p = Pattern.compile(
                "\\{[^}]*\"id\":\\s*\"([^\"]*)\"[^}]*" +
                        "\"merchant\":\\s*\"([^\"]*)\"[^}]*" +
                        "\"amount\":\\s*([0-9.]+)[^}]*" +
                        "\"transactionDate\":\\s*\"([^\"]*)\"[^}]*" +
                        "\"transactionType\":\\s*\"([^\"]*)\"[^}]*\\}"
        );

        Matcher m = p.matcher(data);
        List<Transaction> list = new ArrayList<>();

        while (m.find()) {
            list.add(new Transaction(
                    m.group(1),
                    m.group(2),
                    Double.parseDouble(m.group(3)),
                    m.group(4),
                    m.group(5)
            ));
        }
        return list;
    }

    private static Map<String, List<Transaction>> groupByMerchant(List<Transaction> txs) {
        Map<String, List<Transaction>> map = new HashMap<>();
        for (Transaction t : txs) {
            map.computeIfAbsent(t.merchant, k -> new ArrayList<>()).add(t);
        }
        return map;
    }

    private static List<Map<String, Object>> getRecurringGroups(Map<String, List<Transaction>> grouped) {

        List<Map<String, Object>> recurringList = new ArrayList<>();

        for (String merchant : grouped.keySet()) {
            List<Transaction> txns = grouped.get(merchant);

            if (txns.size() < 3) continue;

            txns.sort(Comparator.comparing(t -> LocalDate.parse(t.date)));

            double base = txns.get(0).amount;

            boolean amountOK = txns.stream()
                    .allMatch(t -> Math.abs(t.amount - base) <= base * 0.05);

            if (!amountOK) continue;

            boolean valid = true;

            for (int i = 1; i < txns.size(); i++) {
                long gap = ChronoUnit.DAYS.between(
                        LocalDate.parse(txns.get(i - 1).date),
                        LocalDate.parse(txns.get(i).date)
                );

                if (gap < 28 || gap > 32) {
                    valid = false;
                    break;
                }
            }

            if (!valid) continue;

            Map<String, Object> obj = new LinkedHashMap<>();
            obj.put("merchant", merchant);
            obj.put("amount", base);

            List<String> timestamps = new ArrayList<>();
            for (Transaction t : txns) timestamps.add(t.date);

            obj.put("timestamps", timestamps);
            recurringList.add(obj);
        }
        return recurringList;
    }
    private static List<Map<String, Object>> detectAnomalies(List<Transaction> txList) {

        List<Map<String, Object>> anomalies = new ArrayList<>();

        if (txList.isEmpty()) return anomalies;

        Map<String, Double> avgMerchantSpend = new HashMap<>();
        Map<String, Integer> merchantCount = new HashMap<>();
        Map<Integer, Integer> dayFrequency = new HashMap<>();

        // Build stats
        for (Transaction t : txList) {
            avgMerchantSpend.merge(t.merchant, t.amount, Double::sum);
            merchantCount.merge(t.merchant, 1, Integer::sum);
            int dow = LocalDate.parse(t.date).getDayOfWeek().getValue();
            dayFrequency.merge(dow, 1, Integer::sum);
        }

        // Final average
        for (String m : avgMerchantSpend.keySet()) {
            avgMerchantSpend.put(m, avgMerchantSpend.get(m) / merchantCount.get(m));
        }

        // Detect anomalies
        for (Transaction t : txList) {

            double avgForMerchant = avgMerchantSpend.get(t.merchant);
            int dow = LocalDate.parse(t.date).getDayOfWeek().getValue();

            boolean highSpike = t.amount > avgForMerchant * 5 || t.amount > 25000;
            boolean newMerchantSpike = merchantCount.get(t.merchant) == 1 && t.amount > 5000;
            boolean rareDay = dayFrequency.getOrDefault(dow, 0) < txList.size() * 0.10;
            boolean oddMerchantSpend = Math.abs(t.amount - avgForMerchant) > avgForMerchant * 0.7;

            boolean isAnomaly = highSpike || newMerchantSpike || (rareDay && highSpike) || oddMerchantSpend;

            if (isAnomaly) {
                Map<String, Object> obj = new LinkedHashMap<>();
                obj.put("id", t.id);
                obj.put("merchant", t.merchant);
                obj.put("amount", t.amount);
                obj.put("date", t.date);
                obj.put("reason", buildReason(highSpike, newMerchantSpike, rareDay, oddMerchantSpend));
                anomalies.add(obj);
            }
        }

        return anomalies;
    }

    private static String buildReason(boolean highSpike, boolean newMerchant, boolean rareDay, boolean oddSpend) {
        StringBuilder r = new StringBuilder();
        if (highSpike) r.append("High-value spike. ");
        if (newMerchant) r.append("New merchant + high amount. ");
        if (rareDay) r.append("Unusual day-of-week. ");
        if (oddSpend) r.append("Irregular spending vs usual. ");
        return r.toString().trim();
    }
    private static Map<String, Double> getCategoryTotals(List<Transaction> txList) {
        Map<String, Double> totals = new HashMap<>();

        for (Transaction t : txList) {
            String category = Main.getCategoryForMerchant(t.merchant); // reuse your categorizer
            totals.merge(category, t.amount, Double::sum);
        }
        return totals;
    }

    private static Map<String, Double> getMonthlyTotals(List<Transaction> txList) {
        Map<String, Double> monthTotals = new HashMap<>();

        for (Transaction t : txList) {
            String month = t.date.substring(0, 7); // "2025-11"
            monthTotals.merge(month, t.amount, Double::sum);
        }

        return monthTotals;
    }
    private static List<String> generateInsights(
            Map<String, Double> categoryTotals,
            Map<String, Double> monthlyTotals
    ) {
        List<String> insights = new ArrayList<>();

        // ========== CATEGORY INSIGHTS ==========

        // Highest spent category
        String topCategory = categoryTotals.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");

        double topValue = categoryTotals.getOrDefault(topCategory, 0.0);

        insights.add("Your highest spending this month was on "
                + topCategory + " totaling ₹" + topValue);

        // Identify overspending categories ( > ₹5000 or > 40% of total )
        double totalSpent = categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum();

        for (String cat : categoryTotals.keySet()) {
            double amt = categoryTotals.get(cat);
            if (amt > 5000 || (amt / totalSpent) > 0.40) {
                insights.add("You spent a large amount on " + cat + " this month: ₹" + amt);
            }
        }

        // ========== MONTHLY COMPARISON ==========

        if (monthlyTotals.size() >= 2) {
            List<String> keys = new ArrayList<>(monthlyTotals.keySet());
            Collections.sort(keys);

            String lastMonth = keys.get(keys.size() - 2);
            String thisMonth = keys.get(keys.size() - 1);

            double lastVal = monthlyTotals.get(lastMonth);
            double thisVal = monthlyTotals.get(thisMonth);

            double diff = thisVal - lastVal;
            double percent = (diff / lastVal) * 100;

            if (diff > 0) {
                insights.add("Your spending increased by " + String.format("%.1f", percent)
                        + "% compared to last month.");
            } else if (diff < 0) {
                insights.add("Your spending decreased by " + String.format("%.1f", Math.abs(percent))
                        + "% compared to last month.");
            }
        }

        return insights;
    }
    private static String insightSummary(List<String> insights) {
        StringBuilder sb = new StringBuilder("\n====== INSIGHTS ======\n\n");

        for (String i : insights) {
            sb.append("- ").append(i).append("\n");
        }

        return sb.toString();
    }




    private static String toJson(List<Map<String, Object>> list) {

        StringBuilder sb = new StringBuilder("[\n");

        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> obj = list.get(i);

            sb.append("  {\n");
            sb.append("    \"merchant\": \"").append(obj.get("merchant")).append("\",\n");
            sb.append("    \"amount\": ").append(obj.get("amount")).append(",\n");

            @SuppressWarnings("unchecked")
            List<String> timestamps = (List<String>) obj.get("timestamps");

            sb.append("    \"timestamps\": [");
            for (int j = 0; j < timestamps.size(); j++) {
                sb.append("\"").append(timestamps.get(j)).append("\"");
                if (j < timestamps.size() - 1) sb.append(", ");
            }
            sb.append("]\n");

            sb.append("  }");

            if (i < list.size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("]");
        return sb.toString();
    }
    private static String toJsonAnomalies(List<Map<String, Object>> list) {

        StringBuilder sb = new StringBuilder("[\n");

        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> obj = list.get(i);

            sb.append("  {\n");
            sb.append("    \"id\": \"").append(obj.get("id")).append("\",\n");
            sb.append("    \"merchant\": \"").append(obj.get("merchant")).append("\",\n");
            sb.append("    \"amount\": ").append(obj.get("amount")).append(",\n");
            sb.append("    \"date\": \"").append(obj.get("date")).append("\",\n");
            sb.append("    \"reason\": \"").append(obj.get("reason")).append("\"\n");
            sb.append("  }");

            if (i < list.size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("]");
        return sb.toString();
    }


    
    //  TRANSACTION CLASS

    static class Transaction {
        String id;
        String merchant;
        double amount;
        String date;
        String type;

        public Transaction(String id, String merchant, double amount, String date, String type) {
            this.id = id;
            this.merchant = merchant;
            this.amount = amount;
            this.date = date;
            this.type = type;
        }
    }
}
