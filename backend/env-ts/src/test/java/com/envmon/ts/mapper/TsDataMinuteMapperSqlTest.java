package com.envmon.ts.mapper;

import com.envmon.dao.mapper.TsDataMinuteMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验带 {@code <script>} 的注解 SQL 能被 MyBatis 正确解析。
 * 没有数据库也能跑，专门防 &lt; 未转义、if 标签写错这类启动才暴露的问题。
 */
class TsDataMinuteMapperSqlTest {

    @Test
    void 小时聚合脚本解析并剔除停运区间() throws Exception {
        String sql = boundSql(true);

        assertTrue(sql.contains("NOT EXISTS"), "必须排除停运区间");
        assertTrue(sql.contains("m.ts < st.end_at"), "结束时间为空表示仍在停运，需要 OR 条件");
        assertTrue(sql.contains("m.station_id = ?"), "指定站房时应带站房条件");
        assertTrue(sql.contains("GROUP BY hour_ts"), "按小时分组");
    }

    @Test
    void 不指定站房时不加站房条件() throws Exception {
        String sql = boundSql(false);

        assertFalse(sql.contains("m.station_id = ?"));
        assertTrue(sql.contains("NOT EXISTS"));
    }

    private String boundSql(boolean withStation) throws Exception {
        Method method = TsDataMinuteMapper.class.getMethod("aggregateByHour",
                LocalDateTime.class, LocalDateTime.class, Long.class);
        String script = method.getAnnotation(Select.class).value()[0];

        SqlSource sqlSource = new XMLLanguageDriver()
                .createSqlSource(new Configuration(), script, TsDataMinuteMapper.class);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("from", LocalDateTime.of(2026, 11, 4, 0, 0));
        params.put("to", LocalDateTime.of(2026, 11, 5, 0, 0));
        if (withStation) {
            params.put("stationId", 1L);
        }
        return sqlSource.getBoundSql(params).getSql();
    }
}
