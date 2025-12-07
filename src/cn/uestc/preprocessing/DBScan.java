package cn.uestc.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DBScan {

    // 参数设置
    static double epsilon = 5.0;  // 邻域半径
    static int minPts = 3;        // 成为核心点所需的最小邻居数

    // 标记状态常量
    static final int UNVISITED = 0;
    static final int VISITED = 1;
    static final int NOISE = -1;

    // 计算欧氏距离
    public static double getDist(double[] point1, double[] point2) {
        double sum = 0;
        for (int i = 0; i < point1.length; i++) {
            sum += Math.pow(point1[i] - point2[i], 2);
        }
        return Math.sqrt(sum);
    }

    // 获取某个点在 epsilon 半径内的所有邻居索引
    public static ArrayList<Integer> getNeighbors(ArrayList<double[]> dataSet, int pointIdx) {
        ArrayList<Integer> neighbors = new ArrayList<>();
        for (int i = 0; i < dataSet.size(); i++) {
            if (getDist(dataSet.get(pointIdx), dataSet.get(i)) <= epsilon) {
                neighbors.add(i);
            }
        }
        return neighbors;
    }

    public static void main(String[] args) {
        // ---------------------------------------------------------
        // 1. 读入数据
        // ---------------------------------------------------------
        ArrayList<double[]> dataSet = new ArrayList<>();
        File file = new File("C:/Users/zewux/Documents/数据挖掘/实验四/data.txt"); // 确保文件在项目根目录

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    double[] point = new double[2];
                    point[0] = Double.parseDouble(parts[0]);
                    point[1] = Double.parseDouble(parts[1]);
                    dataSet.add(point);
                }
            }
            System.out.println("数据读取成功，共 " + dataSet.size() + " 个点。");
        } catch (IOException e) {
            System.err.println("读取文件失败: " + e.getMessage());
            return;
        }

        // ---------------------------------------------------------
        // 2. 初始化 DBSCAN 辅助变量
        // ---------------------------------------------------------
        int size = dataSet.size();
        int[] pointStatus = new int[size]; // 记录访问状态 (UNVISITED/VISITED)
        int[] clusterAssignment = new int[size]; // 记录点属于哪个簇 (0代表未分类/噪声，1,2,3...代表簇)

        // 初始化数组
        for(int i=0; i<size; i++) {
            pointStatus[i] = UNVISITED;
            clusterAssignment[i] = 0; // 0 在这里暂时表示未分配
        }

        int clusterID = 0; // 当前簇的 ID 计数器

        // ---------------------------------------------------------
        // 3. 执行 DBSCAN 算法
        // ---------------------------------------------------------
        System.out.println("开始 DBSCAN 聚类 (Eps=" + epsilon + ", MinPts=" + minPts + ")...");

        for (int i = 0; i < size; i++) {
            if (pointStatus[i] == VISITED) {
                continue;
            }

            pointStatus[i] = VISITED;
            ArrayList<Integer> neighbors = getNeighbors(dataSet, i);

            if (neighbors.size() < minPts) {
                // 邻居太少，标记为噪声 (注意：后续可能会被归入某个簇的边界)
                clusterAssignment[i] = NOISE;
            } else {
                // 满足核心点条件，新建一个簇
                clusterID++;
                clusterAssignment[i] = clusterID; // 将当前点加入新簇

                // 扩展簇 (Expand Cluster)
                // 使用列表模拟队列进行广度优先搜索
                // 这里 neighbors 列表会在循环中动态增加
                int index = 0;
                while(index < neighbors.size()){
                    int neighborIdx = neighbors.get(index);
                    index++;

                    // 处理该邻居点
                    if (pointStatus[neighborIdx] == UNVISITED) {
                        pointStatus[neighborIdx] = VISITED;
                        ArrayList<Integer> neighborNeighbors = getNeighbors(dataSet, neighborIdx);
                        // 如果这个邻居也是核心点，则将其邻居也加入待处理列表
                        if (neighborNeighbors.size() >= minPts) {
                            for(Integer idx : neighborNeighbors){
                                // 避免重复加入队列
                                if(!neighbors.contains(idx)){
                                    neighbors.add(idx);
                                }
                            }
                        }
                    }

                    // 如果该邻居还没有属于任何簇 (或是之前被标记为噪声的)，加入当前簇
                    if (clusterAssignment[neighborIdx] == 0 || clusterAssignment[neighborIdx] == NOISE) {
                        clusterAssignment[neighborIdx] = clusterID;
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // 4. 数据整理与可视化
        // ---------------------------------------------------------
        // 统计发现的簇数量
        int totalClustersFound = clusterID;

        // 为了可视化，我们需要把“噪声点”也算作一种特殊的“簇”，或者直接丢弃。
        // 这里我们将噪声点放入第 (totalClustersFound + 1) 个数组中显示。
        boolean hasNoise = false;
        for(int c : clusterAssignment) {
            if(c == NOISE) {
                hasNoise = true;
                break;
            }
        }

        int visualArraySize = hasNoise ? totalClustersFound + 1 : totalClustersFound;

        System.out.println("聚类完成！发现簇数量: " + totalClustersFound + (hasNoise ? " (+ 噪声)" : ""));

        // 构建 PicUtility 需要的 double[][][]
        // 维度 1: 簇的索引
        // 维度 2: 该簇内的点列表
        ArrayList<ArrayList<double[]>> visualList = new ArrayList<>();
        for (int k = 0; k < visualArraySize; k++) {
            visualList.add(new ArrayList<>());
        }

        for (int i = 0; i < size; i++) {
            int cID = clusterAssignment[i];
            double[] point = dataSet.get(i);

            if (cID > 0) {
                // 正常的簇 (ID 从 1 开始，存入 List index cID-1)
                visualList.get(cID - 1).add(point);
            } else if (cID == NOISE) {
                // 噪声点，存入 List 的最后一个位置
                visualList.get(visualArraySize - 1).add(point);
            }
        }

        // 转换为数组
        double[][][] datas = new double[visualArraySize][][];
        for (int n = 0; n < visualArraySize; n++) {
            double[][] clusterArr = new double[visualList.get(n).size()][2];
            for (int m = 0; m < clusterArr.length; m++) {
                clusterArr[m] = visualList.get(n).get(m);
            }
            datas[n] = clusterArr;
        }

        // 5. 调用可视化
        try {
            // 参数2是颜色数量，如果有噪声，我们把噪声也算一个颜色
            PicUtility.show(datas, visualArraySize, "dbscan");
        } catch (Exception e) {
            System.err.println("可视化出错，请检查 PicUtility。");
        }
    }
}