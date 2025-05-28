package com.djj.todoscheduleserver.mapper;

import com.djj.todoscheduleserver.pojo.crdt.Device;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface DeviceMapper {
    /**
     * 根据ID查找设备
     */
    Device findById(@Param("id") String id);
    
    /**
     * 获取用户的所有设备
     */
    List<Device> findByUserId(@Param("userId") Integer userId);

    /**
     * 插入新设备
     */
    int insert(Device device);

    /**
     * 更新设备信息
     */
    int update(Device device);

    /**
     * 更新设备的最后同步时间戳
     */
    int updateLastSyncHlcTimestamp(
            @Param("id") String id, 
            @Param("lastSyncHlcTimestamp") Long lastSyncHlcTimestamp, 
            @Param("updatedAt") Timestamp updatedAt
    );
    
    /**
     * 删除设备
     */
    int delete(@Param("id") String id);
} 