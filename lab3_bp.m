% ==========================================================
% 实验项目：BP
% ==========================================================
clc; clear; close all;

%% 1. 定义数据
% P: 输入数据 (x轴)
P = [0 1 2 3 4 5 6 7 8];
% T: 目标数据 (y轴，需要拟合的曲线)
T = [0 0.84 0.91 0.14 -0.77 -0.96 -0.28 0.66 0.99];

%% 2. 创建网络
% [10] 表示隐藏层有 10 个神经元
net = feedforwardnet(10);

% 设置训练算法
net.trainFcn = 'trainlm'; 

% 配置输入输出
net = configure(net, P, T);

% 设置隐藏层和输出层的激活函数
% 第一层(隐藏层)用 tansig，第二层(输出层)用 purelin (线性)
net.layers{1}.transferFcn = 'tansig';
net.layers{2}.transferFcn = 'purelin';

%% 3. 设置训练参数
net.trainParam.epochs = 50; % 最大训练次数
net.trainParam.goal = 0.01;   % 目标误差 (根据数据量级调整)
net.trainParam.showWindow = true; % 显示训练窗口

%% 4. 训练网络
disp('开始训练...');
[net, tr] = train(net, P, T);

%% 5. 仿真与验证
% 使用训练好的网络预测 P 对应的输出
Y = net(P);

% --- 测试特定点 ---
% 截图中的测试点
test_input = 6.5;
test_output = net(test_input);
fprintf('输入 %.1f 的预测结果为: %.4f\n', test_input, test_output);

%% 6. 绘图结果分析
figure;
hold on;
plot(P, T, 'ro', 'MarkerSize', 10, 'LineWidth', 2); % 原始数据点 (红圈)
plot(P, Y, 'b-x', 'LineWidth', 1.5);                % 神经网络拟合曲线 (蓝叉线)
plot(test_input, test_output, 'gs', 'MarkerSize', 12, 'MarkerFaceColor', 'g'); % 测试点 (绿方块)

legend('真实值 (Target)', 'BP预测值 (Output)', '测试点 (Test Point)');
title('BP 神经网络函数逼近效果');
xlabel('输入 P');
ylabel('输出 T');
grid on;
hold off;

disp('运行结束！');