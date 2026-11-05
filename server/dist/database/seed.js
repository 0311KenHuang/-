"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
require("reflect-metadata");
const typeorm_1 = require("typeorm");
const bcrypt = __importStar(require("bcryptjs"));
const sys_tenant_entity_1 = require("./entities/sys-tenant.entity");
const sys_role_entity_1 = require("./entities/sys-role.entity");
const sys_user_entity_1 = require("./entities/sys-user.entity");
const sys_audit_log_entity_1 = require("./entities/sys-audit-log.entity");
const md_enterprise_entity_1 = require("./entities/md-enterprise.entity");
const md_station_entity_1 = require("./entities/md-station.entity");
const md_device_entity_1 = require("./entities/md-device.entity");
const md_station_factor_entity_1 = require("./entities/md-station-factor.entity");
const alm_event_entity_1 = require("./entities/alm-event.entity");
const ops_task_entity_1 = require("./entities/ops-task.entity");
const hj212_frame_entity_1 = require("./entities/hj212-frame.entity");
const ds = new typeorm_1.DataSource({
    type: 'mysql',
    host: process.env.DB_HOST ?? '127.0.0.1',
    port: Number(process.env.DB_PORT ?? 3306),
    username: process.env.DB_USER ?? 'root',
    password: process.env.DB_PASSWORD ?? 'root123456',
    database: process.env.DB_NAME ?? 'env_mon',
    charset: 'utf8mb4',
    timezone: '+08:00',
    synchronize: true,
    logging: false,
    entities: [
        sys_tenant_entity_1.SysTenant,
        sys_role_entity_1.SysRole,
        sys_user_entity_1.SysUser,
        sys_audit_log_entity_1.SysAuditLog,
        md_enterprise_entity_1.MdEnterprise,
        md_station_entity_1.MdStation,
        md_device_entity_1.MdDevice,
        md_station_factor_entity_1.MdStationFactor,
        alm_event_entity_1.AlmEvent,
        ops_task_entity_1.OpsTask,
        hj212_frame_entity_1.Hj212Frame,
    ],
});
async function main() {
    await ds.initialize();
    console.log('数据库已连接');
    const tenants = ds.getRepository(sys_tenant_entity_1.SysTenant);
    let tenant = await tenants.findOne({ where: { tenantCode: 'DEMO' } });
    if (!tenant) {
        tenant = await tenants.save(tenants.create({ tenantCode: 'DEMO', name: '成都市演示监管域', tenantType: 'REG', status: 1 }));
        console.log('租户已创建 DEMO');
    }
    const roles = ds.getRepository(sys_role_entity_1.SysRole);
    const roleDefs = [
        { roleCode: 'SUP', name: '平台超级管理员', dataScope: 'ALL' },
        { roleCode: 'REG', name: '监管用户', dataScope: 'REGION' },
        { roleCode: 'ORG', name: '运维单位管理员', dataScope: 'ORG' },
        { roleCode: 'MGR', name: '运维主管', dataScope: 'ORG' },
        { roleCode: 'ENG', name: '运维工程师', dataScope: 'SELF' },
        { roleCode: 'ENT', name: '企业用户', dataScope: 'ORG' },
    ];
    for (const def of roleDefs) {
        const exists = await roles.findOne({ where: { tenantId: tenant.id, roleCode: def.roleCode } });
        if (!exists) {
            await roles.save(roles.create({ ...def, tenantId: tenant.id, deleted: 0 }));
        }
    }
    console.log('角色已就绪');
    const users = ds.getRepository(sys_user_entity_1.SysUser);
    const adminName = 'admin';
    let admin = await users.findOne({ where: { tenantId: tenant.id, username: adminName } });
    if (!admin) {
        const supRole = await roles.findOneOrFail({
            where: { tenantId: tenant.id, roleCode: 'SUP' },
        });
        const mngRole = await roles.findOneOrFail({
            where: { tenantId: tenant.id, roleCode: 'MGR' },
        });
        admin = await users.save(users.create({
            tenantId: tenant.id,
            username: adminName,
            passwordHash: await bcrypt.hash('Admin@123', 10),
            realName: '系统管理员',
            orgId: null,
            status: 1,
            deleted: 0,
            roles: [supRole, mngRole],
        }));
        console.log('管理员已创建 admin / Admin@123');
    }
    const ents = ds.getRepository(md_enterprise_entity_1.MdEnterprise);
    const demoEnts = [
        { creditCode: '91510100MA6C0DEMO1', name: '丰华纸业有限公司', shortName: '丰华纸业', industryCode: 'C22' },
        { creditCode: '91510100MA6C0DEMO2', name: '光明玻璃制品有限公司', shortName: '光明玻特', industryCode: 'C30' },
    ];
    const entIds = [];
    for (const def of demoEnts) {
        let e = await ents.findOne({ where: { tenantId: tenant.id, creditCode: def.creditCode } });
        if (!e) {
            e = await ents.save(ents.create({
                ...def,
                tenantId: tenant.id,
                regionId: 1,
                address: '四川省成都市演示路 1 号',
                isKeyUnit: 1,
                status: 1,
                deleted: 0,
            }));
        }
        entIds.push(e.id);
    }
    console.log('企业已就绪');
    const stations = ds.getRepository(md_station_entity_1.MdStation);
    const demoStations = [
        { code: 'FHZY-S-01', name: '丰华纸业-水', type: 'WASTE_WATER', ent: entIds[0], lng: '103.9301', lat: '30.5712' },
        { code: 'FHZY-G-01', name: '丰华纸业-锅炉', type: 'WASTE_GAS', ent: entIds[0], lng: '103.9305', lat: '30.5716' },
        { code: 'GMZT-G-01', name: '光明玻特废气', type: 'WASTE_GAS', ent: entIds[1], lng: '103.9412', lat: '30.5803' },
    ];
    for (const s of demoStations) {
        const exists = await stations.findOne({
            where: { tenantId: tenant.id, stationCode: s.code },
        });
        if (exists)
            continue;
        const saved = await stations.save(stations.create({
            tenantId: tenant.id,
            stationCode: s.code,
            name: s.name,
            stationType: s.type,
            enterpriseId: s.ent,
            regionId: 1,
            lng: s.lng,
            lat: s.lat,
            address: '四川省成都市演示路 1 号',
            bizStatus: 'NORMAL',
            isFocus: 0,
            deleted: 0,
        }));
        const factors = ds.getRepository(md_station_factor_entity_1.MdStationFactor);
        const defs = s.type === 'WASTE_WATER'
            ? [
                { code: 'a21026', unit: 'mg/L', limit: '40', main: 1 },
                { code: 'a00000', unit: 'l/s', limit: null, main: 1 },
                { code: 'pH', unit: '', limit: '9', main: 0 },
            ]
            : [
                { code: 'a21002', unit: 'mg/m3', limit: '50', main: 1 },
                { code: 'a21001', unit: 'mg/m3', limit: '100', main: 1 },
                { code: 'a21003', unit: 'mg/m3', limit: '20', main: 0 },
            ];
        for (const f of defs) {
            await factors.save(factors.create({
                tenantId: tenant.id,
                stationId: saved.id,
                factorCode: f.code,
                unit: f.unit,
                rangeMin: '0',
                rangeMax: s.type === 'WASTE_WATER' ? '500' : '1000',
                stdLimit: f.limit,
                isMain: f.main,
                deleted: 0,
            }));
        }
    }
    console.log('站房与因子已就绪');
    await ds.destroy();
    console.log('初始化完成');
}
main().catch((e) => {
    console.error(e);
    process.exit(1);
});
//# sourceMappingURL=seed.js.map