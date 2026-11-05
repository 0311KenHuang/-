package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.OpsPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OpsPlanMapper extends BaseMapper<OpsPlan> {

    @Select("SELECT * FROM ops_plan WHERE tenant_id = #{tenantId} AND period = #{period} AND deleted = 0")
    List<OpsPlan> selectByPeriod(@Param("tenantId") Long tenantId, @Param("period") String period);
}
