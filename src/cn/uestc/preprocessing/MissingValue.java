package cn.uestc.preprocessing;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;

public class MissingValue {

    public static void main(String[] args) throws Exception {
        System.out.println("Step_1. 读取数据...");
        String fn = "C:/Program Files/Weka-3-8-6/data/laborMissing.arff";
        DataSource source = new DataSource(fn);
        Instances instances = source.getDataSet();
        int dim = instances.numAttributes();
        int num = instances.numInstances();

        System.out.println("Step 2. 缺失值处理...");
        //计算全局平均值
        double[] meanV = new double[dim];
        for (int i = 0; i < dim; i++) {
            meanV[i] = 0;
            int count = 0;
            for (int j = 0; j < num; j++) {
                if (!instances.instance(j).isMissing(i)){
                    meanV[i] += instances.instance(j).value(i);
                count++;
                }
            }
            meanV[i] = meanV[i] / count;
        }
        //利用全局平均值代替缺失值
        for(int i=0;i<dim;i++){
            for(int j=0;j<num;j++){
                    if(instances.instance(j).isMissing(i)) {
                        instances.instance(j).setValue(i, meanV[i]);
                    }
            }
        }
        System.out.println("Step3.保存数据到新文件·.");
        DataSink.write("laborMissing_handle.arff", instances);
        System.out.println("Finish.");
    }
}
