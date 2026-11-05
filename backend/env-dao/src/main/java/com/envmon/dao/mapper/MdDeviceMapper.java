package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.MdDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MdDeviceMapper extends BaseMapper<MdDevice> {

    /**
     * 接入层专用：报文到达时还没有租户上下文，按 MN 反查设备必须忽略租户插件。
     * 返回值里带 tenantId，后续可用于补齐上下文。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM md_device WHERE mn = #{mn} AND deleted = 0 LIMIT 1")
    MdDevice selectByMnIgnoreTenant(@Param("mn") String mn);
}
