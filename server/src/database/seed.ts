/**
 * 初始化演示数据：租户 / 角色 / 管理员 / 企业 / 站房
 * 运行：npm run seed
 */
import 'reflect-metadata';
import { DataSource } from 'typeorm';
import * as bcrypt from 'bcryptjs';

import { SysTenant } from './entities/sys-tenant.entity';
import { SysRole } from './entities/sys-role.entity';
import { SysUser } from './entities/sys-user.entity';
import { SysAuditLog } from './entities/sys-audit-log.entity';
import { MdEnterprise } from './entities/md-enterprise.entity';
import { MdStation } from './entities/md-station.entity';
import { MdDevice } from './entities/md-device.entity';
import { MdStationFactor } from './entities/md-station-factor.entity';
import { AlmEvent } from './entities/alm-event.entity';
import { OpsTask } from './entities/ops-task.entity';
import { Hj212Frame } from './entities/hj212-frame.entity';

const ds = new DataSource({
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
    SysTenant,
    SysRole,
    SysUser,
    SysAuditLog,
    MdEnterprise,
    MdStation,
    MdDevice,
    MdStationFactor,
    AlmEvent,
    OpsTask,
    Hj212Frame,
  ],
});

async function main() {
  await ds.initialize();
  console.log('数据库已连接');

  const tenants = ds.getRepository(SysTenant);
  let tenant = await tenants.findOne({ where: { tenantCode: 'DEMO' } });
  if (!tenant) {
    tenant = await tenants.save(
      tenants.create({ tenantCode: 'DEMO', name: '成都市演示监管域', tenantType: 'REG', status: 1 }),
    );
    console.log('租户已创建 DEMO');
  }

  const roles = ds.getRepository(SysRole);
  const roleDefs: Array<Pick<SysRole, 'roleCode' | 'name' | 'dataScope'>> = [
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

  const users = ds.getRepository(SysUser);
  const adminName = 'admin';
  let admin = await users.findOne({ where: { tenantId: tenant.id, username: adminName } });
  if (!admin) {
    const supRole = await roles.findOneOrFail({
      where: { tenantId: tenant.id, roleCode: 'SUP' },
    });
    const mngRole = await roles.findOneOrFail({
      where: { tenantId: tenant.id, roleCode: 'MGR' },
    });
    admin = await users.save(
      users.create({
        tenantId: tenant.id,
        username: adminName,
        passwordHash: await bcrypt.hash('Admin@123', 10),
        realName: '系统管理员',
        orgId: null,
        status: 1,
        deleted: 0,
        roles: [supRole, mngRole],
      }),
    );
    console.log('管理员已创建 admin / Admin@123');
  }

  const ents = ds.getRepository(MdEnterprise);
  const demoEnts = [
    { creditCode: '91510100MA6C0DEMO1', name: '丰华纸业有限公司', shortName: '丰华纸业', industryCode: 'C22' },
    { creditCode: '91510100MA6C0DEMO2', name: '光明玻璃制品有限公司', shortName: '光明玻特', industryCode: 'C30' },
  ];
  const entIds: number[] = [];
  for (const def of demoEnts) {
    let e = await ents.findOne({ where: { tenantId: tenant.id, creditCode: def.creditCode } });
    if (!e) {
      e = await ents.save(
        ents.create({
          ...def,
          tenantId: tenant.id,
          regionId: 1,
          address: '四川省成都市演示路 1 号',
          isKeyUnit: 1,
          status: 1,
          deleted: 0,
        }),
      );
    }
    entIds.push(e.id);
  }
  console.log('企业已就绪');

  const stations = ds.getRepository(MdStation);
  const demoStations = [
    { code: 'FHZY-S-01', name: '丰华纸业-水', type: 'WASTE_WATER', ent: entIds[0], lng: '103.9301', lat: '30.5712' },
    { code: 'FHZY-G-01', name: '丰华纸业-锅炉', type: 'WASTE_GAS', ent: entIds[0], lng: '103.9305', lat: '30.5716' },
    { code: 'GMZT-G-01', name: '光明玻特废气', type: 'WASTE_GAS', ent: entIds[1], lng: '103.9412', lat: '30.5803' },
  ];
  for (const s of demoStations) {
    const exists = await stations.findOne({
      where: { tenantId: tenant.id, stationCode: s.code },
    });
    if (exists) continue;
    const saved = await stations.save(
      stations.create({
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
      }),
    );

    const factors = ds.getRepository(MdStationFactor);
    const defs =
      s.type === 'WASTE_WATER'
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
      await factors.save(
        factors.create({
          tenantId: tenant.id,
          stationId: saved.id,
          factorCode: f.code,
          unit: f.unit,
          rangeMin: '0',
          rangeMax: s.type === 'WASTE_WATER' ? '500' : '1000',
          stdLimit: f.limit,
          isMain: f.main,
          deleted: 0,
        }),
      );
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
