"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FrameSplitter = exports.Hj212Codec = void 0;
const crc16_1 = require("./crc16");
const START = '##';
const HEAD_MAX = 6;
class Hj212Codec {
    static encode(header, cp) {
        const body = Hj212Codec.encodeBody(header, cp);
        const len = Buffer.byteLength(body, 'utf8');
        const lenStr = String(len).padStart(4, '0');
        return `${START}${lenStr}${body}${(0, crc16_1.crc16Hex)(body)}`;
    }
    static encodeBody(header, cp) {
        const parts = [
            `QN=${header.QN}`,
            `ST=${header.ST}`,
            `CN=${header.CN}`,
            `PW=${header.PW}`,
            `MN=${header.MN}`,
            `Flag=${header.Flag}`,
        ];
        if (header.PN)
            parts.push(`PN=${header.PN}`);
        parts.push(`CP=&&${cp}&&`);
        return parts.join(';');
    }
    static nextQn(seq = 1, now = new Date()) {
        const p = (n, w = 2) => String(n).padStart(w, '0');
        const ts = `${now.getFullYear()}${p(now.getMonth() + 1)}${p(now.getDate())}` +
            `${p(now.getHours())}${p(now.getMinutes())}${p(now.getSeconds())}${p(now.getMilliseconds(), 3)}`;
        return `${ts}${p(seq % 1000, 3)}`;
    }
    static decode(raw) {
        const text = raw.trim();
        if (!text.startsWith(START) || text.length < HEAD_MAX + 4)
            return null;
        const lenStr = text.slice(2, HEAD_MAX);
        if (!/^\d{4}$/.test(lenStr))
            return null;
        const declared = Number(lenStr);
        const bodyStart = HEAD_MAX;
        const bodyEnd = bodyStart + declared;
        if (text.length < bodyEnd + 4)
            return null;
        const body = text.slice(bodyStart, bodyEnd);
        const crc = text.slice(bodyEnd, bodyEnd + 4).toUpperCase();
        const crcOk = (0, crc16_1.crc16Hex)(body) === crc;
        const params = Hj212Codec.parseBody(body);
        if (!params)
            return null;
        return {
            header: {
                QN: params.header.QN ?? '',
                ST: params.header.ST ?? '',
                CN: params.header.CN ?? '',
                PW: params.header.PW ?? '',
                MN: params.header.MN ?? '',
                Flag: params.header.Flag ?? '',
                PN: params.header.PN,
            },
            cp: params.cp,
            params: params.map,
            crc,
            crcOk,
            raw: text.slice(0, bodyEnd + 4),
        };
    }
    static parseBody(body) {
        const cpIdx = body.indexOf('CP=');
        if (cpIdx < 0)
            return null;
        const headerPart = body.slice(0, cpIdx);
        const cpPart = body.slice(cpIdx + 3);
        const header = {};
        for (const seg of headerPart.split(';')) {
            const i = seg.indexOf('=');
            if (i <= 0)
                continue;
            header[seg.slice(0, i).trim()] = seg.slice(i + 1).trim();
        }
        const cp = cpPart.replace(/^&&/, '').replace(/&&$/, '');
        return { header, cp, map: Hj212Codec.parseParams(cp) };
    }
    static parseParams(cp) {
        const out = {};
        let lastKey = null;
        for (const seg of cp.split(';')) {
            if (!seg)
                continue;
            for (const token of seg.split(',')) {
                const t = token.trim();
                if (!t)
                    continue;
                const eq = t.indexOf('=');
                if (eq > 0) {
                    const key = t.slice(0, eq).trim();
                    out[key] = t.slice(eq + 1).trim();
                    lastKey = key;
                }
                else if (lastKey) {
                    out[lastKey] = `${out[lastKey]},${t}`;
                }
            }
        }
        return out;
    }
    static ack(frame, result = 1, mode = 'qnrtn') {
        const cp = mode === 'exerntn' ? `ExeRtn=${result}` : `QnRtn=${result}`;
        return Hj212Codec.encode({
            QN: frame.header.QN,
            ST: frame.header.ST,
            CN: frame.header.CN,
            PW: frame.header.PW,
            MN: frame.header.MN,
            Flag: frame.header.Flag,
        }, cp);
    }
}
exports.Hj212Codec = Hj212Codec;
class FrameSplitter {
    buf = Buffer.alloc(0);
    static MAX_BUFFER = 1024 * 1024;
    push(chunk) {
        this.buf = Buffer.concat([this.buf, chunk]);
        if (this.buf.length > FrameSplitter.MAX_BUFFER) {
            this.buf = Buffer.alloc(0);
            return [];
        }
        const frames = [];
        for (;;) {
            const start = this.buf.indexOf(START, 0, 'utf8');
            if (start < 0) {
                this.buf = this.buf.subarray(Math.max(0, this.buf.length - 1));
                break;
            }
            if (start > 0)
                this.buf = this.buf.subarray(start);
            if (this.buf.length < HEAD_MAX)
                break;
            const lenStr = this.buf.subarray(2, HEAD_MAX).toString('ascii');
            if (!/^\d{4}$/.test(lenStr)) {
                this.buf = this.buf.subarray(2);
                continue;
            }
            const bodyLen = Number(lenStr);
            const total = HEAD_MAX + bodyLen + 4;
            if (this.buf.length < total)
                break;
            frames.push(this.buf.subarray(0, total).toString('utf8'));
            this.buf = this.buf.subarray(total);
        }
        return frames;
    }
    reset() {
        this.buf = Buffer.alloc(0);
    }
}
exports.FrameSplitter = FrameSplitter;
//# sourceMappingURL=codec.js.map