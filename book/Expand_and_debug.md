# 源码学习与调试扩展 #
如果感觉程序跳的不准，或者想调试程序，可执行相应方法的Main方法，观察输出图片，以定位问题。

## 目标中心点识别 ##
起始坐标与瓶子位置起始坐标相同，结束坐标将Y轴改为瓶子颈部Y轴，以减少寻找时间。大概思路为：

- 依次计算所有坐标点RGB，如果与背景色相同，则移除，不予理会。背景色取法为当前行的起始点与结束点RGB。
- 将未过滤掉的坐标点以一定误差来进行分类，放入Map中，处理完成成后，移除坐标点较少的坐标点集合。留下3-5个候选集合，然后再一一排除
- 比如瓶子头部圆球移除，判断坐标点是否在圆内
- 坐标点相对瓶子位置移除，如果瓶子位于屏幕左边，此时若坐标点最大值还位于瓶子左边，此集合点一定有问题，反之亦然
- 最后再通过判断点是否连续，来移除较大Y值坐标点集合
- 若上一步操作导致目标点定位到背景色，此时有可能是因为目标块比较小，最高点比瓶子颈部坐标要低，此时可缩短结束Y轴坐标，再找一次。最终通过点数排序，移除较少点的集合
- 计算仅剩集合中的最大X轴与最小X轴，得到中心点坐标

## Main方法调试 ##
![目标位置调试](https://i.imgur.com/SuAx0XL.png)