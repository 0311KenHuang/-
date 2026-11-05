"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.crc16 = crc16;
exports.crc16Hex = crc16Hex;
function crc16(buf) {
    const data = typeof buf === 'string' ? Buffer.from(buf, 'utf8') : buf;
    let crc = 0xffff;
    for (const byte of data) {
        crc ^= byte;
        for (let i = 0; i < 8; i++) {
            crc = crc & 1 ? (crc >> 1) ^ 0xa001 : crc >> 1;
        }
    }
    return crc & 0xffff;
}
function crc16Hex(buf) {
    return crc16(buf).toString(16).toUpperCase().padStart(4, '0');
}
//# sourceMappingURL=crc16.js.map