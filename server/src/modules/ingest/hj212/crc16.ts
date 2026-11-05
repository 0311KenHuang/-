/**
 * CRC16/MODBUS（多项式 0xA001，初值 0xFFFF）。
 * HJ 212 报文尾部的 4 位十六进制即该值的高位在前表示。
 */
export function crc16(buf: Buffer | string): number {
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

/** 返回 4 位大写十六进制，如 "4B37" */
export function crc16Hex(buf: Buffer | string): string {
  return crc16(buf).toString(16).toUpperCase().padStart(4, '0');
}
