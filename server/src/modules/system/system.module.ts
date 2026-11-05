import { Controller, Get, Module, Query } from '@nestjs/common';
import { InjectRepository, TypeOrmModule } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { SysUser } from '../../database/entities/sys-user.entity';
import { SysRole } from '../../database/entities/sys-role.entity';
import { Ctx } from '../../common/context/request.context';
import { Roles } from '../../common/decorators';

@Controller('system')
export class SystemController {
  constructor(
    @InjectRepository(SysUser) private readonly users: Repository<SysUser>,
    @InjectRepository(SysRole) private readonly roles: Repository<SysRole>,
  ) {}

  @Get('user')
  @Roles('ORG', 'REG')
  async listUsers(@Query('page') page = '1', @Query('pageSize') pageSize = '20') {
    const tenantId = Ctx.tenantId();
    const [list, total] = await this.users.findAndCount({
      where: { tenantId, deleted: 0 },
      order: { id: 'DESC' },
      skip: (Number(page) - 1) * Number(pageSize),
      take: Math.min(Number(pageSize), 200),
    });
    // 永远不要把 passwordHash 返回给前端
    return {
      list: list.map(({ passwordHash: _p, ...rest }) => rest),
      total,
      page: Number(page),
      pageSize: Number(pageSize),
    };
  }

  @Get('role')
  listRoles() {
    return this.roles.find({ where: { tenantId: Ctx.tenantId(), deleted: 0 } });
  }
}

@Module({
  imports: [TypeOrmModule.forFeature([SysUser, SysRole])],
  controllers: [SystemController],
})
export class SystemModule {}
