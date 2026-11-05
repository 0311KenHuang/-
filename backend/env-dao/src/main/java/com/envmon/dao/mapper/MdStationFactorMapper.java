package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.MdStationFactor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MdStationFactorMapper extends BaseMapper<MdStationFactor> {

    /**
     * 接入层专用：量程配置本来就要按站房取，接入进程没有租户上下文，
     * 必须显式绕过租户插件；stationId 已经由 MN 归属解析得到。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM md_station_factor WHERE station_id = #{stationId} AND deleted = 0 ORDER BY sort")
    List<MdStationFactor> selectByStationIgnoreTenant(@Param("stationId") Long stationId);
}
