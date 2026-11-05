"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ErrCode = exports.BizException = void 0;
class BizException extends Error {
    code;
    httpStatus;
    constructor(code, message, httpStatus = 200) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
        this.name = 'BizException';
    }
    static of(code, message) {
        return new BizException(code, message);
    }
}
exports.BizException = BizException;
exports.ErrCode = {
    OK: 0,
    BAD_REQUEST: 40000,
    UNAUTHORIZED: 40100,
    FORBIDDEN: 40300,
    NOT_FOUND: 40400,
    CONFLICT: 40900,
    TENANT_MISSING: 40101,
    DATA_SCOPE_DENIED: 40301,
    INTERNAL: 50000,
};
//# sourceMappingURL=biz.exception.js.map