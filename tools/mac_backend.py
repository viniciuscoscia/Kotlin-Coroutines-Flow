"""Owned loopback backend lifecycle; no service installation or inference calls."""
import argparse,json,os,pathlib,signal,subprocess,sys,time,urllib.request

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action',choices=['start','run','status','stop'])
    parser.add_argument('--profile',choices=['dev','optimized'],default='dev')
    parser.add_argument('--port',type=int,default=8080)
    args=parser.parse_args()
    if not 1024<=args.port<=65535: parser.error('Use an unprivileged port')
    root=pathlib.Path(__file__).resolve().parent.parent
    runtime=root/'build/mac-runtime';runtime.mkdir(parents=True,exist_ok=True)
    state=runtime/f'{args.profile}-{args.port}.json'
    def identity(pid):
        found=subprocess.run(['ps','-p',str(pid),'-o','lstart=','-o','command='],capture_output=True,text=True)
        return found.stdout.strip() if found.returncode==0 else ''
    def owned():
        if not state.exists():return None
        record=json.loads(state.read_text())
        if identity(record['pid'])!=record['identity']:return None
        return record
    def stop():
        record=owned()
        if not record: print(json.dumps({'stopped':False,'reason':'no matching owned process'}));return
        os.kill(record['pid'],signal.SIGTERM)
        for _ in range(100):
            if not identity(record['pid']):break
            time.sleep(.05)
        else: raise RuntimeError('Owned process did not stop; inspect its log')
        state.unlink(missing_ok=True)
        print(json.dumps({'stopped':True,'pid':record['pid']}))
    if args.action=='stop':stop();return
    if args.action=='status':print(json.dumps({'running':bool(owned()),'state':str(state),'process':owned()}));return
    if owned():raise RuntimeError('Owned backend already running; use status or stop')
    base=f'http://127.0.0.1:{args.port}'
    try:urllib.request.urlopen(base+'/ready',timeout=.5)
    except OSError:pass
    else:raise RuntimeError('Port already serves another backend; no process changed')
    jar=root/'build/deployment/jvm/server.jar'
    executable=root/'build/deployment/native/server'
    argv=([str(pathlib.Path('/opt/homebrew/opt/openjdk@25/bin/java')),'-jar',str(jar)] if args.profile=='dev' else [str(executable)])
    argv += ['--server.address=127.0.0.1',f'--server.port={args.port}',f'--spring.profiles.active={args.profile}',f'--lab.sandbox={runtime / "sandbox"}']
    log=runtime/f'{args.profile}-{args.port}.log'
    log_offset=log.stat().st_size if log.exists() else 0
    with log.open('ab') as stream:process=subprocess.Popen(argv,cwd=root,stdin=subprocess.DEVNULL,stdout=stream,stderr=subprocess.STDOUT,start_new_session=True)
    record={'pid':process.pid,'identity':identity(process.pid),'argv':argv,'log':str(log),'base_url':base}
    state.write_text(json.dumps(record,indent=2))
    try:
        deadline=time.monotonic()+60
        while time.monotonic()<deadline:
            if process.poll() is not None:raise RuntimeError(f'Backend exited; inspect {log}')
            try:
                response=json.load(urllib.request.urlopen(base+'/ready',timeout=1))
                if response.get('ready'):break
            except OSError:pass
            time.sleep(.05)
        else:raise TimeoutError(f'Readiness failed; inspect {log}')
        print(json.dumps({'ready':True,'profile':args.profile,**record}),flush=True)
        if args.action=='run':
            # EOF from the owning SSH session also closes this foreground run.
            import select
            with log.open() as live:
                live.seek(log_offset)
                while process.poll() is None:
                    content=live.read()
                    if content:print(content,end='',flush=True)
                    if select.select([sys.stdin],[],[],.25)[0] and not sys.stdin.buffer.read(1):break
    except BaseException:
        stop();raise
    finally:
        if args.action=='run':stop()

if __name__=='__main__':main()
