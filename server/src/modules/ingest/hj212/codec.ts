import { crc16Hex } from './crc16';

export interface Hj212Header {
  QN: string;
  ST: string;
  CN: string;
  PW: string;
  MN: string;
  Flag: string;
  PN?: string;
}

export interface Hj212Frame {
  header: Hj212Header;
  /** CP 原文，不含 && */
  cp: string;
  /** CP 解析后的键值对；同一参数多值以逗号分隔时会展开为独立键 */
  params: Record<string, string>;
  crc: string;
  crcOk: boolean;
  raw: string;
}

const START = '##';
const HEAD_MAX = 6; // "##" + 4 位长度

/**
 * HJ 212 报文编解码。
 *
 * 结构： ## + 数据段长度(4位十进制，字节数) + 数据段 + CRC16(4位大写十六进制)
 * 数据段： QN=..;ST=..;CN=..;PW=..;MN=..;Flag=..;PN=..;CP=&&...&&
 */
export class Hj212Codec {
  /** 组装完整报文（自动计算长度与 CRC） */
  static encode(header: Hj212Header, cp: string): string {
    const body = Hj212Codec.encodeBody(header, cp);
    const len = Buffer.byteLength(body, 'utf8');
    const lenStr = String(len).padStart(4, '0');
    return `${START}${lenStr}${body}${crc16Hex(body)}`;
  }

  /** 只拼数据段，不含 ## 长度 与 CRC */
  static encodeBody(header: Hj212Header, cp: string): string {
    const parts = [
      `QN=${header.QN}`,
      `ST=${header.ST}`,
      `CN=${header.CN}`,
      `PW=${header.PW}`,
      `MN=${header.MN}`,
      `Flag=${header.Flag}`,
    ];
    if (header.PN) parts.push(`PN=${header.PN}`);
    parts.push(`CP=&&${cp}&&`);
    return parts.join(';');
  }

  /** 生成 QN：yyyyMMddHHmmssSSS + 3 位序号 */
  static nextQn(seq = 1, now = new Date()): string {
    const p = (n: number, w = 2) => String(n).padStart(w, '0');
    const ts =
      `${now.getFullYear()}${p(now.getMonth() + 1)}${p(now.getDate())}` +
      `${p(now.getHours())}${p(now.getMinutes())}${p(now.getSeconds())}${p(now.getMilliseconds(), 3)}`;
    return `${ts}${p(seq % 1000, 3)}`;
  }

  /** 解析一条完整报文；结构不合法返回 null，CRC 不匹配时 crcOk=false */
  static decode(raw: string): Hj212Frame | null {
    const text = raw.trim();
    if (!text.startsWith(START) || text.length < HEAD_MAX + 4) return null;

    const lenStr = text.slice(2, HEAD_MAX);
    if (!/^\d{4}$/.test(lenStr)) return null;
    const declared = Number(lenStr);

    const bodyStart = HEAD_MAX;
    const bodyEnd = bodyStart + declared;
    if (text.length < bodyEnd + 4) return null;

    const body = text.slice(bodyStart, bodyEnd);
    const crc = text.slice(bodyEnd, bodyEnd + 4).toUpperCase();
    const crcOk = crc16Hex(body) === crc;

    const params = Hj212Codec.parseBody(body);
    if (!params) return null;

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

  private static parseBody(body: string):
    | { header: Record<string, string>; cp: string; map: Record<string, string> }
    | null {
    const cpIdx = body.indexOf('CP=');
    if (cpIdx < 0) return null;

    const headerPart = body.slice(0, cpIdx);
    const cpPart = body.slice(cpIdx + 3);

    const header: Record<string, string> = {};
    for (const seg of headerPart.split(';')) {
      const i = seg.indexOf('=');
      if (i <= 0) continue;
      header[seg.slice(0, i).trim()] = seg.slice(i + 1).trim();
    }

    const cp = cpPart.replace(/^&&/, '').replace(/&&$/, '');
    return { header, cp, map: Hj212Codec.parseParams(cp) };
  }

  /**
   * CP 解析：以 ; 分对，值内部可能再用 , 分隔多参数
   * 例：DataTime=20261104141700;a21026-Rtd=33.215,a21026-Flag=N
   */
  static parseParams(cp: string): Record<string, string> {
    const out: Record<string, string> = {};
    let lastKey: string | null = null;

    for (const seg of cp.split(';')) {
      if (!seg) continue;
      for (const token of seg.split(',')) {
        const t = token.trim();
        if (!t) continue;
        const eq = t.indexOf('=');
        if (eq > 0) {
          const key = t.slice(0, eq).trim();
          out[key] = t.slice(eq + 1).trim();
          lastKey = key;
        } else if (lastKey) {
          // 少见：值内包含逗号且未带 =，追加到上一个键
          out[lastKey] = `${out[lastKey]},${t}`;
        }
      }
    }
    return out;
  }

  /** 构造应答报文：数据上传回 QnRtn，控制命令回 ExeRtn */
  static ack(
    frame: Hj212Frame,
    result: 1 | 2 | 3 = 1,
    mode: 'qnrtn' | 'exerntn' = 'qnrtn',
  ): string {
    const cp = mode === 'exerntn' ? `ExeRtn=${result}` : `QnRtn=${result}`;
    return Hj212Codec.encode(
      {
        QN: frame.header.QN,
        ST: frame.header.ST,
        CN: frame.header.CN,
        PW: frame.header.PW,
        MN: frame.header.MN,
        Flag: frame.header.Flag,
      },
      cp,
    );
  }
}

/**
 * 流式拆包器：TCP 会粘包/半包，必须按 "## + 4 位长度" 切帧。
 * 用法：const frames = splitter.push(chunk)
 */
export class FrameSplitter {
  private buf = Buffer.alloc(0);
  private static readonly MAX_BUFFER = 1024 * 1024;

  push(chunk: Buffer): string[] {
    this.buf = Buffer.concat([this.buf, chunk]);
    if (this.buf.length > FrameSplitter.MAX_BUFFER) {
      // 防御：异常设备持续灌数据
      this.buf = Buffer.alloc(0);
      return [];
    }

    const frames: string[] = [];
    for (;;) {
      const start = this.buf.indexOf(START, 0, 'utf8');
      if (start < 0) {
        // 没有帧头，保留最后 1 字节以防 "##" 被切开
        this.buf = this.buf.subarray(Math.max(0, this.buf.length - 1));
        break;
      }
      if (start > 0) this.buf = this.buf.subarray(start);

      if (this.buf.length < HEAD_MAX) break;

      const lenStr = this.buf.subarray(2, HEAD_MAX).toString('ascii');
      if (!/^\d{4}$/.test(lenStr)) {
        // 帧头是假的，跳过继续找
        this.buf = this.buf.subarray(2);
        continue;
      }

      const bodyLen = Number(lenStr);
      const total = HEAD_MAX + bodyLen + 4;
      if (this.buf.length < total) break; // 半包，等下一批

      frames.push(this.buf.subarray(0, total).toString('utf8'));
      this.buf = this.buf.subarray(total);
    }
    return frames;
  }

  reset(): void {
    this.buf = Buffer.alloc(0);
  }
}
