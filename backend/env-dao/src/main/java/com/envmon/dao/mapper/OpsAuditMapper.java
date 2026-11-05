package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.OpsAudit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OpsAuditMapper extends BaseMapper<OpsAudit> {

    /** BR-04：同一记录驳回 3 次自动升级到运维单位管理员 */
    @Select("SELECT COUNT(1) FROM ops_audit WHERE record_id = #{recordId} AND result = 'REJECT'")
    int countRejects(@Param("recordId") Long recordId);
}
