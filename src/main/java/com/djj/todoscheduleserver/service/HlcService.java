package com.djj.todoscheduleserver.service;

/**
 * 用于在服务器上管理混合逻辑时钟 (HLC) 时间戳的服务。
 * 确保服务器源的事件获得因果正确且唯一的时间戳。
 */
public interface HlcService {

    /**
     * 生成一个新的HLC时间戳。
     * 此时间戳应大于任何先前生成或观察到的HLC时间戳，以维护因果顺序。
     *
     * @param userId 可选：如果需要用户特定的HLC时钟，则使用此参数；但全局时钟更简单。
     * @return 新的HLC时间戳 (long)。
     */
    long now(Integer userId);

    /**
     * 根据从客户端或其他来源收到的时间戳更新服务器的HLC。
     * 确保如果收到的时间戳更大，服务器的时钟会前进。
     *
     * @param receivedHlc 从外部来源收到的HLC时间戳。
     * @param userId 可选：用于用户特定的HLC时钟。
     * @return 服务器可能更新后的当前HLC时间戳。
     */
    long updateWithTimestamp(long receivedHlc, Integer userId);

} 