package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

/** 只暴露 insert 语义的 BaseMapper，业务层不得调用 update/delete */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
}
