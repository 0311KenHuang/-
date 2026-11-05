"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Ctx = void 0;
const node_async_hooks_1 = require("node:async_hooks");
const biz_exception_1 = require("../errors/biz.exception");
const storage = new node_async_hooks_1.AsyncLocalStorage();
exports.Ctx = {
    run(ctx, fn) {
        return storage.run(ctx, fn);
    },
    get() {
        return storage.getStore();
    },
    tenantId() {
        const ctx = storage.getStore();
        if (!ctx?.tenantId) {
            throw new biz_exception_1.BizException(biz_exception_1.ErrCode.TENANT_MISSING, '缺少租户上下文');
        }
        return ctx.tenantId;
    },
    userId() {
        return storage.getStore()?.userId ?? 0;
    },
    traceId() {
        return storage.getStore()?.traceId ?? '-';
    },
};
//# sourceMappingURL=request.context.js.map