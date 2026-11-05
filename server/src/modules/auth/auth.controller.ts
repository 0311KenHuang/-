import { Body, Controller, Get, Post, Req } from '@nestjs/common';
import type { Request } from 'express';
import { AuthService } from './auth.service';
import { LoginDto } from './dto/login.dto';
import { Audit, CurrentUser, Public } from '../../common/decorators';
import type { RequestContext } from '../../common/context/request.context';

@Controller('auth')
export class AuthController {
  constructor(private readonly auth: AuthService) {}

  @Public()
  @Post('login')
  @Audit('auth', 'LOGIN')
  login(@Body() dto: LoginDto, @Req() _req: Request) {
    return this.auth.login(dto);
  }

  @Get('profile')
  profile(@CurrentUser() ctx: RequestContext) {
    return ctx;
  }
}
