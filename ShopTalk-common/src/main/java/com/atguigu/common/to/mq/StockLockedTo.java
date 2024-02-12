package com.atguigu.common.to.mq;

import lombok.Data;

@Data
public class StockLockedTo {
    /**
     * 库存工作单的id
     */
    private Long id;
    /**
     * 工作单详情类
     */
    private StockDetailTo detailTo;
}
 