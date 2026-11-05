package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.AlmRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AlmRuleMapper extends BaseMapper<AlmRule> {

    /** 接入层无登录态，按租户+规则码显式查询 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM alm_rule WHERE tenant_id = #{tenantId} AND rule_code = #{ruleCode} "
            + "AND deleted = 0 LIMIT 1")
    AlmRule selectByCode(@Param("tenantId") Long tenantId, @Param("ruleCode") String ruleCode);
}
