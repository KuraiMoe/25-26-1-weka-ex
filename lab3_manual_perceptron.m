% ==========================================================
% 实验内容：感知机
% ==========================================================

clc; clear; close all;

%% 1. 定义数据
% P: 输入向量 (2行4列，代表4个点的x,y坐标)
P = [-0.5 -0.5  0.3  0; 
     -0.5  0.5 -0.5  1];
% T: 目标类别 (1行4列，1代表一类，0代表另一类)
T = [1 1 0 0];

%% 2. 初始化网络
% 使用 modern 函数 'perceptron' 替代过时的 'newp'
net = perceptron; 
net = configure(net, P, T); % 自动配置输入输出维度

% 获取初始权重 w 和偏置 b
w = net.IW{1,1};
b = net.b{1};

%% 3. 绘图初始化
figure;
plotpv(P, T);             % 绘制样本点(圆圈和十字)
line_handle = plotpc(w, b); % 绘制初始分类线
title('感知机分类线调整过程');
xlabel('x1'); ylabel('x2');
disp('按任意键开始训练...');
pause; % 等待用户按键

%% 4. 手动训练循环
% 使用 max_epochs 防止死循环
max_epochs = 20; 

for epoch = 1:max_epochs
    % --- Step A: 前向计算 ---
    % 计算网络输出
    y = hardlim(w * P + b);
    
    % --- Step B: 计算误差 ---
    e = T - y;
    
    % 检查是否全部分类正确
    % mae(e) 是平均绝对误差，如果为0说明全对
    if ~any(e) 
        disp(['>>> 训练成功收敛！总共迭代次数: ', num2str(epoch)]);
        break; 
    end
    
    % --- Step C: 更新权重和偏置 ---
    % 权重更新量 dw = 误差 * 输入的转置
    dw = e * P';  
    
    % 偏置更新量 db = 误差求和
    db = sum(e);  
    
    % 更新参数
    w = w + dw;
    b = b + db;
    
    % --- Step D: 更新可视化 ---
    % 更新图上的分类线
    line_handle = plotpc(w, b, line_handle);
    
    disp(['第 ', num2str(epoch), ' 次迭代: 发现误差，正在调整分类线...']);
    pause(0.8); % 暂停0.8秒，让你看清楚线是怎么动的
end

%% 5. 结果验证
disp('=== 最终结果 ===');
disp('最终权重 w:'); disp(w);
disp('最终偏置 b:'); disp(b);
disp('最终分类输出 y:'); disp(y);