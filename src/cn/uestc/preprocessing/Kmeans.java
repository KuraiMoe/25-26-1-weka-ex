package cn.uestc.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class Kmeans {

    // 计算欧氏距离
    public static double getDist(double[] point1, double[] point2) {
        double sum = 0;
        for (int i = 0; i < point1.length; i++) {
            sum += Math.pow(point1[i] - point2[i], 2);
        }
        return Math.sqrt(sum);
    }

    public static void main(String[] args) {
        // ---------------------------------------------------------
        // 1. 读入数据
        // ---------------------------------------------------------
        ArrayList<double[]> dataSet = new ArrayList<>();

        File file = new File("C:/Users/zewux/Documents/数据挖掘/实验四/data.txt");

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                // 去除首尾空格，并按空格或制表符分割
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    double[] point = new double[2];
                    point[0] = Double.parseDouble(parts[0]); // x
                    point[1] = Double.parseDouble(parts[1]); // y
                    dataSet.add(point);
                }
            }
            System.out.println("数据读取成功，共读取到 " + dataSet.size() + " 个点。");
        } catch (IOException e) {
            System.err.println("读取文件失败，请检查路径: " + file.getAbsolutePath());
            e.printStackTrace();
            return;
        }

        // ---------------------------------------------------------
        // 2. 初始化参数
        // ---------------------------------------------------------
        int clusterNum = 2; // 设置聚类数量 K
        int maxIterations = 50; // 最大迭代次数，防止死循环
        double[][] clusterMeans = new double[clusterNum][2]; // 聚类中心

        // 随机选取 K 个点作为初始中心
        Random random = new Random();
        ArrayList<Integer> usedIndices = new ArrayList<>();
        for (int i = 0; i < clusterNum; i++) {
            int index;
            do {
                index = random.nextInt(dataSet.size());
            } while (usedIndices.contains(index)); // 避免重复选取
            usedIndices.add(index);

            // 复制该点坐标作为初始中心
            clusterMeans[i][0] = dataSet.get(index)[0];
            clusterMeans[i][1] = dataSet.get(index)[1];
        }

        System.out.println("初始聚类中心已生成。开始迭代...");

        // ---------------------------------------------------------
        // 3. 迭代聚类 (Assignment & Update)
        // ---------------------------------------------------------
        ArrayList<ArrayList<double[]>> clusters = null;

        for (int iter = 0; iter < maxIterations; iter++) {
            // 每次迭代前清空簇列表
            clusters = new ArrayList<ArrayList<double[]>>();
            for (int n = 0; n < clusterNum; n++) {
                clusters.add(new ArrayList<double[]>());
            }

            // --- 步骤 A: 分配点到最近的簇 (Assignment) ---
            for (int n = 0; n < dataSet.size(); n++) {
                double minDis = Double.MAX_VALUE;
                int whoCluster = -1;
                for (int m = 0; m < clusterNum; m++) {
                    double distance = getDist(clusterMeans[m], dataSet.get(n));
                    if (distance < minDis) {
                        whoCluster = m;
                        minDis = distance;
                    }
                }
                clusters.get(whoCluster).add(dataSet.get(n));
            }

            // --- 步骤 B: 更新聚类中心 (Update) ---
            boolean isConverged = true; // 假设收敛标志

            for (int m = 0; m < clusterNum; m++) {
                ArrayList<double[]> currentClusterPoints = clusters.get(m);
                if (currentClusterPoints.size() == 0) continue; // 防止空簇

                double sumX = 0;
                double sumY = 0;
                for (double[] point : currentClusterPoints) {
                    sumX += point[0];
                    sumY += point[1];
                }

                double newMeanX = sumX / currentClusterPoints.size();
                double newMeanY = sumY / currentClusterPoints.size();

                // 检查中心点是否发生了移动
                if (newMeanX != clusterMeans[m][0] || newMeanY != clusterMeans[m][1]) {
                    isConverged = false; // 只要有一个中心动了，就没收敛
                }

                clusterMeans[m][0] = newMeanX;
                clusterMeans[m][1] = newMeanY;
            }

            // 如果中心不再变化，提前结束循环
            if (isConverged) {
                System.out.println("迭代在第 " + (iter + 1) + " 次收敛。");
                break;
            }
        }

        // ---------------------------------------------------------
        // 4. 准备结果并可视化
        // ---------------------------------------------------------
        // 将 ArrayList 转换为 PicUtility 需要的 double[][][] 数组
        double[][][] datas = new double[clusterNum][][];
        for (int n = 0; n < clusterNum; n++) {
            double[][] cluster = new double[clusters.get(n).size()][2];
            for (int m = 0; m < cluster.length; m++) {
                cluster[m] = clusters.get(n).get(m);
            }
            datas[n] = cluster;
        }

        // 打印最终的聚类中心
        System.out.println("最终聚类中心 (Cluster Means):");
        for (int n = 0; n < clusterMeans.length; n++) {
            System.out.print("Cluster " + n + ": [");
            for (double x : clusterMeans[n]) {
                System.out.print(String.format("%.2f", x) + " ");
            }
            System.out.println("]");
        }

        // 调用工具类显示
        try {
            PicUtility.show(datas, clusterNum, "kmean");
        } catch (Exception e) {
            System.err.println("可视化出错: 请确保 PicUtility.java 存在且已导入 jfreechart 包。");
        }
    }
}