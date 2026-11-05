import { crc16, crc16Hex } from '../src/modules/ingest/hj212/crc16';
import { Hj212Codec, FrameSplitter } from '../src/modules/ingest/hj212/codec';

const HEADER = {
  QN: '20261104141700001',
  ST: '32',
  CN: '2051',
  PW: '123456',
  MN: 'FHZY-S-01DTU01',
  Flag: '4',
};

const CP =
  'DataTime=20261104141700;a21026-Rtd=33.215,a21026-Flag=N;a00000-Rtd=42.650,a00000-Flag=N';

describe('CRC16/MODBUS', () => {
  it('标准校验值：123456789 -> 0x4B37', () => {
    expect(crc16('123456789')).toBe(0x4b37);
    expect(crc16Hex('123456789')).toBe('4B37');
  });

  it('输出恒为 4 位大写十六进制', () => {
    for (const s of ['', '##', 'A', '中文']) {
      expect(crc16Hex(s)).toMatch(/^[0-9A-F]{4}$/);
    }
  });
});

describe('Hj212Codec', () => {
  it('编码后再解码，字段与参数应完全一致', () => {
    const raw = Hj212Codec.encode(HEADER, CP);
    expect(raw.startsWith('##')).toBe(true);

    const frame = Hj212Codec.decode(raw);
    expect(frame).not.toBeNull();
    expect(frame!.crcOk).toBe(true);
    expect(frame!.header.MN).toBe(HEADER.MN);
    expect(frame!.header.CN).toBe('2051');
    expect(frame!.params['DataTime']).toBe('20261104141700');
    expect(frame!.params['a21026-Rtd']).toBe('33.215');
    expect(frame!.params['a21026-Flag']).toBe('N');
  });

  it('长度字段等于数据段字节数', () => {
    const raw = Hj212Codec.encode(HEADER, CP);
    const declared = Number(raw.slice(2, 6));
    const body = raw.slice(6, 6 + declared);
    expect(Buffer.byteLength(body, 'utf8')).toBe(declared);
    expect(raw.slice(6 + declared)).toHaveLength(4);
  });

  it('CRC 错误时应能解出结构但标记 crcOk=false', () => {
    const raw = Hj212Codec.encode(HEADER, CP);
    const tampered = raw.slice(0, -1) + (raw.endsWith('0') ? '1' : '0');
    const frame = Hj212Codec.decode(tampered);
    expect(frame).not.toBeNull();
    expect(frame!.crcOk).toBe(false);
  });

  it('结构非法返回 null', () => {
    expect(Hj212Codec.decode('随便一段文字')).toBeNull();
    expect(Hj212Codec.decode('##12xx')).toBeNull();
    expect(Hj212Codec.decode('##0001QN=1')).toBeNull();
  });

  it('应答报文携带 QnRtn / ExeRtn', () => {
    const raw = Hj212Codec.encode(HEADER, CP);
    const frame = Hj212Codec.decode(raw)!;

    const qnAck = Hj212Codec.decode(Hj212Codec.ack(frame, 1, 'qnrtn'))!;
    expect(qnAck.header.QN).toBe(HEADER.QN);
    expect(qnAck.params['QnRtn']).toBe('1');

    const exeAck = Hj212Codec.decode(Hj212Codec.ack(frame, 2, 'exerntn'))!;
    expect(exeAck.params['ExeRtn']).toBe('2');
  });
});

describe('FrameSplitter', () => {
  const f1 = Hj212Codec.encode(HEADER, CP);
  const f2 = Hj212Codec.encode({ ...HEADER, QN: '20261104141800001' }, CP);

  it('一次收到多条报文应全部拆出', () => {
    const splitter = new FrameSplitter();
    const frames = splitter.push(Buffer.from(f1 + f2, 'utf8'));
    expect(frames).toHaveLength(2);
    expect(frames[0]).toBe(f1);
    expect(frames[1]).toBe(f2);
  });

  it('半包应等待下一批数据', () => {
    const splitter = new FrameSplitter();
    const half = Math.floor(f1.length / 2);
    expect(splitter.push(Buffer.from(f1.slice(0, half), 'utf8'))).toHaveLength(0);
    const rest = splitter.push(Buffer.from(f1.slice(half), 'utf8'));
    expect(rest).toHaveLength(1);
    expect(rest[0]).toBe(f1);
  });

  it('帧头前有垃圾字节时能自动对齐', () => {
    const splitter = new FrameSplitter();
    const frames = splitter.push(Buffer.from(`\r\nxxxx${f1}`, 'utf8'));
    expect(frames).toHaveLength(1);
    expect(frames[0]).toBe(f1);
  });

  it('假帧头不会卡死拆包器', () => {
    const splitter = new FrameSplitter();
    const frames = splitter.push(Buffer.from(`##abcd${f1}`, 'utf8'));
    expect(frames).toHaveLength(1);
    expect(frames[0]).toBe(f1);
  });
});
