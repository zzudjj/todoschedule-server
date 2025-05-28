package com.djj.todoscheduleserver.service.impl;

import com.djj.todoscheduleserver.service.HlcService;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * HlcService的基本内存实现。
 * 警告：这是一个简化版本。生产级的HLC可能需要：
 * - 时钟逻辑组件的持久化，以便在重启后继续工作。
 * - 更复杂的时钟偏斜和物理时间跳跃处理机制。
 * - 如果服务器以分布式方式运行，可能需要特定于节点的标识符。
 */
@Service
public class HlcServiceImpl implements HlcService {

    // HLC 通常是 ((物理时间左移) | 逻辑计数器)
    // 为简单起见，我们将使用一个单独的AtomicLong来粗略地组合这些概念。
    // 它旨在大致按时间排序且唯一。
    private final AtomicLong currentHlc = new AtomicLong(System.currentTimeMillis() << 16); // 使用当前时间初始化，左移以便为计数器留出空间
    private static final int LOGICAL_SHIFT = 16; // 逻辑计数器的位数，实际上允许每毫秒65536个事件。

    @Override
    public synchronized long now(Integer userId) {
        long physicalTimeMs = System.currentTimeMillis();
        long lastPhysical = currentHlc.get() >> LOGICAL_SHIFT;
        long currentLogical = currentHlc.get() & ((1L << LOGICAL_SHIFT) - 1);

        long nextPhysical;
        long nextLogical;

        if (physicalTimeMs > lastPhysical) {
            nextPhysical = physicalTimeMs;
            nextLogical = 0;
        } else {
            // 物理时间相同或已倒退（时钟偏斜），增加逻辑部分
            nextPhysical = lastPhysical;
            nextLogical = currentLogical + 1;
            if (nextLogical >= (1L << LOGICAL_SHIFT)) {
                // 逻辑计数器溢出，必须推进物理时间组件
                // 这种情况在有足够逻辑位的情况下很少见，但表明事件发生率非常高或时钟显著倒退。
                // 对于这个简单实现，我们只是将其物理部分从先前的值增加1毫秒。
                nextPhysical = lastPhysical + 1;
                nextLogical = 0;
                // 在真实系统中记录警告：HLC逻辑部分溢出。
            }
        }
        long newHlc = (nextPhysical << LOGICAL_SHIFT) | nextLogical;
        currentHlc.set(newHlc);
        return newHlc;
    }

    @Override
    public synchronized long updateWithTimestamp(long receivedHlc, Integer userId) {
        long physicalTimeMs = System.currentTimeMillis();
        long currentServerHlc = currentHlc.get();

        long receivedPhysical = receivedHlc >> LOGICAL_SHIFT;
        // long receivedLogical = receivedHlc & ((1L << LOGICAL_SHIFT) - 1); // 收到的逻辑部分

        long serverPhysical = currentServerHlc >> LOGICAL_SHIFT;
        long serverLogical = currentServerHlc & ((1L << LOGICAL_SHIFT) - 1);

        long nextPhysical;
        long nextLogical;

        // 首先比较物理部分，如果物理部分相等，则考虑逻辑部分。
        // 主要思想：如果收到的HLC遥遥领先，服务器时钟应该跳跃。
        // 如果服务器物理时间领先，则服务器时钟正常，或者只需要逻辑增量。

        if (physicalTimeMs > serverPhysical && physicalTimeMs > receivedPhysical) {
            // 当前物理时间是最新的
            nextPhysical = physicalTimeMs;
            nextLogical = 0;
        } else if (receivedPhysical > serverPhysical) {
            // 收到的HLC的物理部分更大
            nextPhysical = receivedPhysical;
            // 当从消息中采用未来的物理时间时，将逻辑部分重置为0或酌情使用消息的逻辑部分。
            // 对于Synk，它通常基于消息的时间戳，因此我们采用其逻辑部分。
            nextLogical = receivedHlc & ((1L << LOGICAL_SHIFT) - 1); 
        } else if (serverPhysical > receivedPhysical) {
            // 服务器的物理部分更大，只需确保在physicalTimeMs与serverPhysical相同时逻辑部分递增
            if (physicalTimeMs == serverPhysical) {
                nextPhysical = serverPhysical;
                nextLogical = serverLogical + 1;
            } else { // physicalTimeMs < serverPhysical (时钟倒退)
                nextPhysical = serverPhysical; // 保留服务器较高的物理时间
                nextLogical = serverLogical + 1; // 增加逻辑部分
            }
        } else { // serverPhysical == receivedPhysical (服务器物理时间等于接收到的物理时间)
            nextPhysical = serverPhysical; // 或 receivedPhysical，它们是相同的
            long maxLogical = Math.max(serverLogical, receivedHlc & ((1L << LOGICAL_SHIFT) - 1));
            nextLogical = maxLogical + 1;
        }
        
        // 如果我们增加了逻辑部分，检查逻辑溢出
        if (nextLogical >= (1L << LOGICAL_SHIFT)) {
            nextPhysical++; // 增加物理部分
            nextLogical = 0; // 重置逻辑部分
        }

        long newHlc = (nextPhysical << LOGICAL_SHIFT) | nextLogical;
        currentHlc.set(newHlc);
        return newHlc;
    }
} 