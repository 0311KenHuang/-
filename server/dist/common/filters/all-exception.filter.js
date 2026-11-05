"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.AllExceptionFilter = void 0;
const common_1 = require("@nestjs/common");
const biz_exception_1 = require("../errors/biz.exception");
const request_context_1 = require("../context/request.context");
let AllExceptionFilter = class AllExceptionFilter {
    logger = new common_1.Logger('Exception');
    catch(exception, host) {
        const res = host.switchToHttp().getResponse();
        const traceId = request_context_1.Ctx.traceId();
        let httpStatus = common_1.HttpStatus.OK;
        let code = biz_exception_1.ErrCode.INTERNAL;
        let message = '系统繁忙，请稍后重试';
        if (exception instanceof biz_exception_1.BizException) {
            code = exception.code;
            message = exception.message;
            httpStatus = exception.httpStatus;
        }
        else if (exception instanceof common_1.HttpException) {
            httpStatus = exception.getStatus();
            const body = exception.getResponse();
            message =
                typeof body === 'string'
                    ? body
                    : body.message || exception.message;
            if (Array.isArray(message))
                message = message.join('; ');
            code =
                httpStatus === 401
                    ? biz_exception_1.ErrCode.UNAUTHORIZED
                    : httpStatus === 403
                        ? biz_exception_1.ErrCode.FORBIDDEN
                        : httpStatus === 404
                            ? biz_exception_1.ErrCode.NOT_FOUND
                            : biz_exception_1.ErrCode.BAD_REQUEST;
        }
        else {
            const err = exception;
            this.logger.error(`[${traceId}] ${err?.message}`, err?.stack);
        }
        res.status(httpStatus).json({ code, message, data: null, traceId });
    }
};
exports.AllExceptionFilter = AllExceptionFilter;
exports.AllExceptionFilter = AllExceptionFilter = __decorate([
    (0, common_1.Catch)()
], AllExceptionFilter);
//# sourceMappingURL=all-exception.filter.js.map