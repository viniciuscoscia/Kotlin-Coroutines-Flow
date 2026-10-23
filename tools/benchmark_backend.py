"""Standard-library backend benchmark: real starts, file copy/read, WS acknowledgment.
No inferred or generated measurements. Python3.10+; JDK25 supplied in each argv.
"""
from __future__ import annotations
import argparse, base64, csv, hashlib, json, os, platform, secrets
import re, socket, statistics, struct, subprocess, sys, time, urllib.error, urllib.parse, urllib.request
from pathlib import Path

def request(base, path, data=None, headers=None):
    req=urllib.request.Request(base+path,data=data,headers=headers or {})
    with urllib.request.urlopen(req,timeout=2) as response:
        return response.read()

def read_exact(sock, length):
    chunks=bytearray()
    while len(chunks)<length:
        part=sock.recv(length-len(chunks))
        if not part: raise RuntimeError('WebSocket closed before acknowledgment')
        chunks.extend(part)
    return bytes(chunks)

def websocket_ack(base, token):
    target=urllib.parse.urlsplit(base)
    if target.scheme!='http' or target.hostname not in ('127.0.0.1','localhost','::1'):
        raise ValueError('Benchmark supports only local plain HTTP/WS; use a private loopback tunnel')
    port=target.port or 80
    with socket.create_connection((target.hostname,port),timeout=5) as sock:
        key=base64.b64encode(secrets.token_bytes(16)).decode()
        # Byte-at-a-time headers avoid consuming a following WebSocket frame.
        header=(f'GET /events HTTP/1.1\r\nHost: {target.hostname}:{port}\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: {key}\r\nSec-WebSocket-Version: 13\r\n\r\n').encode()
        sock.sendall(header); reply=bytearray()
        while not reply.endswith(b'\r\n\r\n'):
            reply.extend(read_exact(sock,1))
            if len(reply)>16384: raise RuntimeError('Oversized WebSocket handshake')
        lines=reply.decode('ascii').split('\r\n')
        if ' 101 ' not in lines[0]: raise RuntimeError(f'Upgrade failed: {lines[0]}')
        headers={line.split(':',1)[0].lower():line.split(':',1)[1].strip() for line in lines[1:] if ':' in line}
        expected=base64.b64encode(hashlib.sha1((key+'258EAFA5-E914-47DA-95CA-C5AB0DC85B11').encode()).digest()).decode()
        if headers.get('sec-websocket-accept')!=expected: raise RuntimeError('Invalid WebSocket accept key')
        payload=token.encode(); mask=secrets.token_bytes(4)
        sock.sendall(bytes([0x81,0x80|len(payload)])+mask+bytes(value^mask[i%4] for i,value in enumerate(payload)))
        deadline=time.monotonic()+5
        while time.monotonic()<deadline:
            first,second=read_exact(sock,2); opcode=first&15; length=second&127
            if length==126: length=struct.unpack('!H',read_exact(sock,2))[0]
            elif length==127: length=struct.unpack('!Q',read_exact(sock,8))[0]
            if length>8192: raise RuntimeError('Oversized WebSocket frame')
            server_mask=read_exact(sock,4) if second&128 else None
            content=read_exact(sock,length)
            if server_mask: content=bytes(v^server_mask[i%4] for i,v in enumerate(content))
            if opcode==8: raise RuntimeError('Server closed WebSocket')
            if opcode!=1: continue
            result=json.loads(content)
            if result.get('event')=='ack' and result.get('probe')==token:
                sock.sendall(bytes([0x88,0x80])+secrets.token_bytes(4))
                return result
        raise TimeoutError('No matching WebSocket acknowledgment')

def rss_bytes(pid):
    if sys.platform=='win32':
        import ctypes
        from ctypes import wintypes
        class Counters(ctypes.Structure):
            _fields_=[('cb',wintypes.DWORD),('PageFaultCount',wintypes.DWORD)]+[(n,ctypes.c_size_t) for n in ('PeakWorkingSetSize','WorkingSetSize','QuotaPeakPagedPoolUsage','QuotaPagedPoolUsage','QuotaPeakNonPagedPoolUsage','QuotaNonPagedPoolUsage','PagefileUsage','PeakPagefileUsage')]
        kernel=ctypes.WinDLL('kernel32',use_last_error=True); psapi=ctypes.WinDLL('psapi',use_last_error=True)
        kernel.OpenProcess.restype=wintypes.HANDLE
        kernel.CloseHandle.argtypes=[wintypes.HANDLE]
        psapi.GetProcessMemoryInfo.argtypes=[wintypes.HANDLE,ctypes.POINTER(Counters),wintypes.DWORD]
        handle=kernel.OpenProcess(0x410,False,pid)
        if not handle: raise OSError('OpenProcess failed')
        try:
            counters=Counters(); counters.cb=ctypes.sizeof(counters)
            if not psapi.GetProcessMemoryInfo(handle,ctypes.byref(counters),counters.cb): raise OSError('GetProcessMemoryInfo failed')
            return counters.WorkingSetSize
        finally: kernel.CloseHandle(handle)
    return int(subprocess.check_output(['ps','-o','rss=','-p',str(pid)],text=True).strip())*1024

def measure(variant, iteration, base, output):
    try:
        request(base,'/ready')
    except (OSError,urllib.error.URLError): pass
    else: raise RuntimeError('A backend already answers readiness; stop it to avoid measuring another process')
    token='probe-'+secrets.token_hex(12)
    log=output/f"{variant['name']}-{iteration}.log"
    started=time.perf_counter()
    with log.open('wb') as stream:
        process=subprocess.Popen(variant['argv'],cwd=variant.get('cwd'),env={**os.environ,**variant.get('env',{})},stdout=stream,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW if sys.platform=='win32' else 0)
        try:
            deadline=time.monotonic()+60
            while time.monotonic()<deadline:
                if process.poll() is not None: raise RuntimeError(f'Backend exited {process.returncode}; inspect {log}')
                try:
                    if json.loads(request(base,'/ready')).get('ready'): break
                except (OSError,ValueError,urllib.error.URLError): pass
                time.sleep(.02)
            else: raise TimeoutError('Readiness timed out')
            ready_ms=(time.perf_counter()-started)*1000
            file_start=time.perf_counter()
            original=f'{token}\nactual-file-content\n'.encode()
            boundary='lab-'+secrets.token_hex(12)
            body=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="probe.txt"\r\nContent-Type: text/plain\r\n\r\n'.encode()+original+f'\r\n--{boundary}--\r\n'.encode())
            receipt=json.loads(request(base,'/files',body,{'Content-Type':f'multipart/form-data; boundary={boundary}'}))
            copied=request(base,'/files/'+urllib.parse.quote(receipt['name'],safe=''))
            if copied!=original: raise AssertionError('Real imported copy/read did not preserve content')
            file_ms=(time.perf_counter()-file_start)*1000
            ws_start=time.perf_counter(); websocket_ack(base,token); ws_ms=(time.perf_counter()-ws_start)*1000
            return {'variant':variant['name'],'iteration':iteration,'pid':process.pid,'ready_ms':ready_ms,'file_copy_read_ms':file_ms,'websocket_roundtrip_ms':ws_ms,'usable_ms':(time.perf_counter()-started)*1000,'rss_bytes':rss_bytes(process.pid),'os':platform.platform(),'machine':platform.machine(),'argv':json.dumps(variant['argv'])}
        finally:
            if process.poll() is None:
                process.terminate()
                try: process.wait(timeout=10)
                except subprocess.TimeoutExpired: process.kill(); process.wait(timeout=5)

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('config',type=Path); parser.add_argument('--output',type=Path,default=Path('build/benchmark/measured')); parser.add_argument('--samples',type=int,default=10)
    args=parser.parse_args(); config=json.loads(args.config.read_text()); variants=config['variants']
    if not variants or any(not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._-]{0,63}',v.get('name','')) or not v.get('argv') for v in variants): parser.error('Each variant needs a safe unique name and argv')
    if len({v['name'] for v in variants})!=len(variants): parser.error('Variant names must be unique')
    base=urllib.parse.urlsplit(config.get('base_url','http://127.0.0.1:18080'))
    if base.scheme!='http' or base.hostname not in ('127.0.0.1','localhost','::1'): parser.error('Base URL must be loopback HTTP')
    if args.samples!=10 and not config.get('smoke_only'): parser.error('Acceptance requires ten starts per variant; set smoke_only for explicit smoke checks')
    args.output.mkdir(parents=True,exist_ok=True)
    metadata={'recorded_at':time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime()),'os':platform.platform(),'python':sys.version,'preparation':config.get('preparation',{}),'smoke_only':bool(config.get('smoke_only')),'samples_per_variant':args.samples,'measurement':'fresh process; readiness; actual multipart copy+HTTP read; RFC6455 matching probe acknowledgment; RSS'}
    (args.output/'metadata.json').write_text(json.dumps(metadata,indent=2))
    rows=[]
    try:
        # Alternate order between rounds to reduce systematic filesystem/cache ordering bias.
        for iteration in range(1,args.samples+1):
            for variant in variants if iteration%2 else list(reversed(variants)):
                row=measure(variant,iteration,config.get('base_url','http://127.0.0.1:18080'),args.output)
                rows.append(row); print(json.dumps(row),flush=True)
                with (args.output/'measurements.csv').open('w',newline='') as stream:
                    writer=csv.DictWriter(stream,fieldnames=list(rows[0])); writer.writeheader(); writer.writerows(rows)
    finally:
        summary={v['name']:{metric:{'median':statistics.median(values),'p95_nearest_rank':sorted(values)[max(0,__import__('math').ceil(len(values)*.95)-1)]} for metric in ('ready_ms','file_copy_read_ms','websocket_roundtrip_ms','usable_ms','rss_bytes') if (values:=[r[metric] for r in rows if r['variant']==v['name']])} for v in variants}
        (args.output/'summary.json').write_text(json.dumps(summary,indent=2))
if __name__=='__main__': main()
