import { IsNotEmpty, IsOptional, IsString, MaxLength } from 'class-validator';

export class LoginDto {
  @IsString()
  @IsNotEmpty({ message: '用户名不能为空' })
  @MaxLength(64)
  username: string;

  @IsString()
  @IsNotEmpty({ message: '密码不能为空' })
  @MaxLength(64)
  password: string;

  /** 多租户环境下用于定位租户；只有一个租户时可省略 */
  @IsOptional()
  @IsString()
  tenantCode?: string;
}
