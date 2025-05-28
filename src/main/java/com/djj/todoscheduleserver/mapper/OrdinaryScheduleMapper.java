package com.djj.todoscheduleserver.mapper;

import com.djj.todoscheduleserver.pojo.OrdinarySchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

/**
 * 普通日程Mapper接口
 */
@Mapper
public interface OrdinaryScheduleMapper {
    
    /**
     * 根据CRDT Key获取普通日程
     */
    OrdinarySchedule getByCrdtKey(@Param("crdtKey") String crdtKey);
    
    /**
     * 获取用户的所有普通日程（未删除的）
     */
    List<OrdinarySchedule> getAllByUserId(@Param("userId") Integer userId);
    
    /**
     * 获取用户在指定时间后更新的普通日程（包括已删除的）
     */
    List<OrdinarySchedule> getUpdatedAfterTimestamp(@Param("userId") Integer userId, @Param("hlcTimestamp") Long hlcTimestamp);
    
    /**
     * 新增或更新普通日程
     */
    int insertOrUpdate(OrdinarySchedule schedule);
    
    /**
     * 根据CRDT键标记普通日程为已删除 (软删除)，并更新HLC时间戳和删除时间。
     * 仅当传入的HLC时间戳较新或记录尚未删除时才更新。
     */
    int markAsDeleted(@Param("crdtKey") String crdtKey, @Param("hlcTimestamp") Long hlcTimestamp, @Param("deletedAt") Timestamp deletedAt);
} 