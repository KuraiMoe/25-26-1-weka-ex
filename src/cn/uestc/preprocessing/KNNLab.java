package cn.uestc.preprocessing;

import java.io.*;
import java.util.*;

public class KNNLab {

    // 1. 定义内部类 Data：用于封装原始数据
    static class Data {
        double[] features; // 属性
        String label;      // 类别

        public Data(double[] features, String label) {
            this.features = features;
            this.label = label;
        }
    }

    // 2. 辅助类 Neighbor：用于存储“距离”和“对应的数据”，方便排序
    static class Neighbor implements Comparable<Neighbor> {
        Data data;
        double distance;
        int index; // 记录是第几行数据

        public Neighbor(Data data, double distance, int index) {
            this.data = data;
            this.distance = distance;
            this.index = index;
        }

        @Override
        public int compareTo(Neighbor o) {
            // 按照距离从小到大排序
            return Double.compare(this.distance, o.distance);
        }
    }

    public static void main(String[] args) {
        // --- 步骤 0：准备数据 ---
        // 请确保文件名正确
        List<Data> trainData = loadData("C:/Users/zewux/Documents/数据挖掘/实验三/iris.2D.train.arff");

        if (trainData.isEmpty()) {
            System.err.println("错误：没有读取到训练数据，请检查文件路径！");
            return;
        }

        // 定义一个测试样本
        // 例如：花萼长度5.1, 花萼宽度3.5, 花瓣长度1.4, 花瓣宽度0.2
        double[] newSample = {5.1, 3.5, 1.4, 0.2};

        System.out.println("\n>>> 开始 KNN 分类预测 <<<");
        System.out.println("测试样本特征: " + Arrays.toString(newSample));

        int k = 5; // 设定 K 值，通常取奇数
        String result = classify(trainData, newSample, k);

        System.out.println("\n>>> 最终预测结果 <<<");
        System.out.println("预测类别为: " + result);
    }

    // --- 核心过程：读取文件 ---
    public static List<Data> loadData(String filename) {
        List<Data> dataList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("%") || line.startsWith("@")) continue;
                String[] parts = line.split(",");
                int featureCount = parts.length - 1;
                double[] features = new double[featureCount];
                for (int i = 0; i < featureCount; i++) {
                    features[i] = Double.parseDouble(parts[i]);
                }
                String label = parts[featureCount];
                dataList.add(new Data(features, label));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("成功加载数据: " + dataList.size() + " 条");
        return dataList;
    }

    // --- 核心过程：KNN算法实现 ---
    public static String classify(List<Data> trainData, double[] testFeature, int k) {

        // (1) 算距离
        System.out.println("\n=== 步骤 (1): 计算距离 (展示前5个) ===");
        List<Neighbor> allNeighbors = new ArrayList<>();

        for (int i = 0; i < trainData.size(); i++) {
            Data train = trainData.get(i);
            double dist = getDis(train.features, testFeature);
            allNeighbors.add(new Neighbor(train, dist, i));

            // 为了避免刷屏，只打印前5个计算过程作为示例
            if (i < 5) {
                System.out.printf("  样本[%d] (%s) -> 距离: %.4f%n", i, train.label, dist);
            }
        }
        System.out.println("  ... (其余 " + (trainData.size()-5) + " 个样本距离计算完毕)");

        // (2) 找邻居 (排序并取前K个)
        System.out.println("\n=== 步骤 (2): 寻找最近的 " + k + " 个邻居 ===");
        Collections.sort(allNeighbors); // 排序

        List<String> kLabels = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            Neighbor n = allNeighbors.get(i);
            kLabels.add(n.data.label);
            System.out.printf("  第 %d 近邻: 距离=%.4f, 类别=%s%n", (i+1), n.distance, n.data.label);
        }

        // (3) 投票分类
        System.out.println("\n=== 步骤 (3): 投票决策 ===");
        return vote(kLabels);
    }

    // 计算欧氏距离
    public static double getDis(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) { // 注意：只要计算特征部分
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }

    // 投票逻辑
    public static String vote(List<String> neighbors) {
        Map<String, Integer> votes = new HashMap<>();
        for (String label : neighbors) {
            votes.put(label, votes.getOrDefault(label, 0) + 1);
        }

        // 打印投票详情
        System.out.println("  投票统计: " + votes);

        // 选出票数最多的
        String winner = Collections.max(votes.entrySet(), Map.Entry.comparingByValue()).getKey();
        System.out.println("  得票最多的是: " + winner);

        return winner;
    }
}