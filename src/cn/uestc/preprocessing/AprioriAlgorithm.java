package cn.uestc.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class AprioriAlgorithm {

    private Map<Integer, Set<String>> txDatabase; // 事务数据库
    private Float minSup; // 最小支持度
    private Float minConf; // 最小置信度
    private Integer txDatabaseCount; // 事务总数
    private Map<Integer, Map<Set<String>, Float>> freqItemSets; // 存储所有频繁项集 (k -> {ItemSet -> Support})
    private Map<Set<String>, Set<Map<Set<String>, Float>>> associationRules; // 关联规则

    public AprioriAlgorithm(Map<Integer, Set<String>> txDatabase, Float minSup, Float minConf) {
        this.txDatabase = txDatabase;
        this.minSup = minSup;
        this.minConf = minConf;
        this.txDatabaseCount = this.txDatabase.size();
        this.freqItemSets = new TreeMap<>();
        this.associationRules = new HashMap<>();
    }

    //寻找频繁项集的主方法
    public void findAllFreqItemSet() {
        // 1. 寻找频繁 1-项集
        Map<Set<String>, Float> freqOneItemSet = find_Frequent_One_Itemsets();
        if (freqOneItemSet != null && !freqOneItemSet.isEmpty()) {
            freqItemSets.put(1, freqOneItemSet);
            System.out.println("频繁 1-项集: " + freqOneItemSet);
        }

        // 2. 循环寻找 k-项集 (k > 1)
        int k = 2;
        while (freqItemSets.containsKey(k - 1)) {
            // 根据 k-1 项集生成 k 项候选集
            Set<Set<String>> candFreqItemSets = apriori_Gen(k, freqItemSets.get(k - 1).keySet());

            // 计算支持度并筛选
            Map<Set<String>, Float> freqKItemSetMap = getFreqKItemSet(k, candFreqItemSets);

            if (!freqKItemSetMap.isEmpty()) {
                freqItemSets.put(k, freqKItemSetMap);
                System.out.println("频繁 " + k + "-项集: " + freqKItemSetMap);
                k++;
            } else {
                break;
            }
        }
    }

    //寻找频繁 1-项集
    public Map<Set<String>, Float> find_Frequent_One_Itemsets() {
        Map<Set<String>, Float> L1 = new HashMap<>();
        Map<Set<String>, Integer> item1SetMap = new HashMap<>();

        // 扫描数据库计数
        for (Set<String> itemSet : txDatabase.values()) {
            for (String item : itemSet) {
                Set<String> key = new HashSet<>();
                key.add(item.trim());
                item1SetMap.put(key, item1SetMap.getOrDefault(key, 0) + 1);
            }
        }

        // 过滤支持度
        for (Map.Entry<Set<String>, Integer> entry : item1SetMap.entrySet()) {
            float support = (float) entry.getValue() / txDatabaseCount;
            if (support >= minSup) {
                L1.put(entry.getKey(), support);
            }
        }
        return L1;
    }

    //连接步与剪枝步：生成候选 k 项集
    public Set<Set<String>> apriori_Gen(int k, Set<Set<String>> freqK_1_ItemSet) {
        Set<Set<String>> candFreqKItemSet = new HashSet<>();
        List<Set<String>> prevItemSets = new ArrayList<>(freqK_1_ItemSet);

        // 自连接
        for (int i = 0; i < prevItemSets.size(); i++) {
            for (int j = i + 1; j < prevItemSets.size(); j++) {
                Set<String> itemSet1 = prevItemSets.get(i);
                Set<String> itemSet2 = prevItemSets.get(j);

                // 连接逻辑：取并集
                Set<String> unionSet = new HashSet<>(itemSet1);
                unionSet.addAll(itemSet2);

                if (unionSet.size() == k) {
                    // 剪枝步：检查所有 k-1 子集是否都在频繁 k-1 项集中
                    if (!has_infrequent_subset(unionSet, freqK_1_ItemSet)) {
                        candFreqKItemSet.add(unionSet);
                    }
                }
            }
        }
        return candFreqKItemSet;
    }

    //剪枝检查
    private boolean has_infrequent_subset(Set<String> candidateSet, Set<Set<String>> freqK_1_ItemSet) {
        // 生成该候选集的所有 k-1 子集
        List<String> items = new ArrayList<>(candidateSet);
        // 简单的生成子集方法：每次去掉一个元素
        for (String itemToRemove : items) {
            Set<String> subset = new HashSet<>(candidateSet);
            subset.remove(itemToRemove);
            if (!freqK_1_ItemSet.contains(subset)) {
                return true; // 发现有一个子集不在频繁项集中，则该候选集需剪枝
            }
        }
        return false;
    }

    //扫描数据库，计算支持度，生成频繁 k 项集
    public Map<Set<String>, Float> getFreqKItemSet(int k, Set<Set<String>> candFreqKItemSet) {
        Map<Set<String>, Integer> counts = new HashMap<>();

        // 初始化计数
        for (Set<String> cand : candFreqKItemSet) {
            counts.put(cand, 0);
        }

        // 扫描数据库
        for (Set<String> tx : txDatabase.values()) {
            for (Set<String> cand : candFreqKItemSet) {
                if (tx.containsAll(cand)) {
                    counts.put(cand, counts.get(cand) + 1);
                }
            }
        }

        // 计算支持度并过滤
        Map<Set<String>, Float> retMap = new HashMap<>();
        for (Map.Entry<Set<String>, Integer> entry : counts.entrySet()) {
            float support = (float) entry.getValue() / txDatabaseCount;
            if (support >= minSup) {
                retMap.put(entry.getKey(), support);
            }
        }
        return retMap;
    }

    //生成关联规则
    public void findAssociationRules() {
        System.out.println("\n--- 生成关联规则 (MinConf: " + minConf + ") ---");
        // 遍历所有频繁项集 (从 k=2 开始)
        for (int k : freqItemSets.keySet()) {
            if (k < 2) continue;
            Map<Set<String>, Float> currentKItemSets = freqItemSets.get(k);

            for (Set<String> itemSet : currentKItemSets.keySet()) {
                float itemSetSup = currentKItemSets.get(itemSet);

                // 生成该项集的所有非空真子集
                List<Set<String>> subsets = getAllProperSubsets(itemSet);

                for (Set<String> condition : subsets) {
                    Set<String> conclusion = new HashSet<>(itemSet);
                    conclusion.removeAll(condition);

                    // 获取条件集的支持度 (必须存在，因为来自频繁项集)
                    float conditionSup = getSubsetSupport(condition);

                    if (conditionSup > 0) {
                        float conf = itemSetSup / conditionSup;
                        if (conf >= minConf) {
                            System.out.println(condition + " -> " + conclusion + " : " + conf);
                            // 存储规则逻辑略，直接打印
                        }
                    }
                }
            }
        }
    }

    // 辅助方法：获取任意项集的支持度 (需要在已挖掘的频繁项集中查找)
    private float getSubsetSupport(Set<String> subset) {
        int k = subset.size();
        if (freqItemSets.containsKey(k)) {
            return freqItemSets.get(k).getOrDefault(subset, 0.0f);
        }
        return 0.0f;
    }

    // 辅助方法：获取一个集合的所有真子集 (用于生成规则)
    private List<Set<String>> getAllProperSubsets(Set<String> set) {
        List<Set<String>> results = new ArrayList<>();
        List<String> list = new ArrayList<>(set);
        int n = list.size();
        // 二进制法生成子集，排除全集和空集
        for (int i = 1; i < (1 << n) - 1; i++) {
            Set<String> subset = new HashSet<>();
            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) != 0) {
                    subset.add(list.get(j));
                }
            }
            results.add(subset);
        }
        return results;
    }

    public static void main(String[] args) {
        // 模拟数据 (代替文件读取，方便直接运行)
        Map<Integer, Set<String>> db = new HashMap<>();

        // 构造测试数据
        // T1: 1, 3, 4
        db.put(1, new HashSet<>(Arrays.asList("1", "3", "4")));
        // T2: 2, 3, 5
        db.put(2, new HashSet<>(Arrays.asList("2", "3", "5")));
        // T3: 1, 2, 3, 5
        db.put(3, new HashSet<>(Arrays.asList("1", "2", "3", "5")));
        // T4: 2, 5
        db.put(4, new HashSet<>(Arrays.asList("2", "5")));

        System.out.println("数据库事务总数: " + db.size());

        // 参数设置：最小支持度 0.5，最小置信度 0.6
        float minSup = 0.5f;
        float minConf = 0.6f;

        AprioriAlgorithm apr = new AprioriAlgorithm(db, minSup, minConf);
        apr.findAllFreqItemSet();
        apr.findAssociationRules();
    }

}
