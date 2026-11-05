import { Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcryptjs';
import { SysUser } from '../../database/entities/sys-user.entity';
import { SysTenant } from '../../database/entities/sys-tenant.entity';
import { JwtPayload } from '../../common/guards/jwt-auth.guard';
import { LoginDto } from './dto/login.dto';
import type { RequestContext } from '../../common/context/request.context';

const SCOPE_RANK: Record<string, number> = { SELF: 1, ORG: 2, REGION: 3, ALL: 4 };

@Injectable()
export class AuthService {
  constructor(
    @InjectRepository(SysUser) private readonly users: Repository<SysUser>,
    @InjectRepository(SysTenant) private readonly tenants: Repository<SysTenant>,
    private readonly jwt: JwtService,
  ) {}

  async login(dto: LoginDto) {
    const qb = this.users
      .createQueryBuilder('u')
      .where('u.username = :username', { username: dto.username })
      .andWhere('u.deleted = 0');

    if (dto.tenantCode) {
      const tenant = await this.tenants.findOne({
        where: { tenantCode: dto.tenantCode, deleted: 0 },
      });
      if (!tenant) throw new UnauthorizedException('租户不存在');
      qb.andWhere('u.tenant_id = :tenantId', { tenantId: tenant.id });
    }

    const user = await qb.getOne();
    // 统一提示，避免暴露"用户是否存在"
    if (!user) throw new UnauthorizedException('用户名或密码错误');
    if (user.status !== 1) throw new UnauthorizedException('账号已停用或锁定');

    const ok = await bcrypt.compare(dto.password, user.passwordHash);
    if (!ok) throw new UnauthorizedException('用户名或密码错误');

    const roles = user.roles.map((r) => r.roleCode);
    const scope = user.roles
      .map((r) => r.dataScope)
      .sort((a, b) => (SCOPE_RANK[b] ?? 0) - (SCOPE_RANK[a] ?? 0))[0] ?? 'SELF';

    const payload: JwtPayload = {
      sub: user.id,
      tenantId: user.tenantId,
      username: user.username,
      orgId: user.orgId,
      roles,
      scope: scope as RequestContext['dataScope'],
    };

    await this.users.update(user.id, { lastLoginAt: new Date() });

    return {
      token: await this.jwt.signAsync(payload),
      user: {
        id: user.id,
        username: user.username,
        realName: user.realName,
        orgId: user.orgId,
        roles,
        dataScope: scope,
      },
    };
  }
}
