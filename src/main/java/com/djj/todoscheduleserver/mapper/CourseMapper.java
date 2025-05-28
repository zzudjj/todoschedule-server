package com.djj.todoscheduleserver.mapper;

import com.djj.todoscheduleserver.pojo.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

/**
 * 课程Mapper接口
 */
@Mapper
public interface CourseMapper {
    
    /**
     * 根据CRDT键获取课程
     */
    Course getByCrdtKey(@Param("crdtKey") String crdtKey);
    
    /**
     * 获取用户的所有课程（未删除的）
     */
    List<Course> getAllByUserId(@Param("userId") Integer userId);
    
    /**
     * 获取用户在指定时间后更新的课程（包括已删除的）
     */
    List<Course> getUpdatedAfterTimestamp(@Param("userId") Integer userId, @Param("hlcTimestamp") Long hlcTimestamp);
    
    /**
     * 新增或更新课程
     */
    int insertOrUpdate(Course course);
    
    /**
     * 标记课程为已删除
     */
    int markAsDeleted(@Param("crdtKey") String crdtKey, @Param("hlcTimestamp") Long hlcTimestamp, @Param("deletedAt") Timestamp deletedAt);
} 