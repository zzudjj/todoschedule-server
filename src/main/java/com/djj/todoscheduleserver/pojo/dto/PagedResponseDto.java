package com.djj.todoscheduleserver.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 分页响应数据传输对象
 */
@Data
@Schema(description = "分页响应数据传输对象")
public class PagedResponseDto<T> {
    
    @Schema(description = "数据列表")
    private List<T> data;
    
    @Schema(description = "分页信息")
    private PageInfo pagination;
    
    /**
     * 分页信息
     */
    @Data
    @Schema(description = "分页信息")
    public static class PageInfo {
        
        @Schema(description = "总记录数")
        private long total;
        
        @Schema(description = "当前页码")
        private int page;
        
        @Schema(description = "每页记录数")
        private int pageSize;
        
        @Schema(description = "是否有更多记录")
        private boolean hasMore;
        
        @Schema(description = "下一页游标，用于基于游标的分页")
        private String nextCursor;
    }
}
