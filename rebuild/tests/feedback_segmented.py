from pathlib import Path
source=Path(__file__).with_name('feedback_emulator.py').read_text(encoding='utf-8')
exec(compile(source.split('checks=[]')[0],str(Path(__file__).with_name('feedback_emulator.py')),'exec'))
configure(style=2);before=level();f=frame();x=(f[0]+f[2])//2;y=(f[1]+f[3])//2
cmd=PREFIX+['shell','CLASSPATH=/data/local/tmp/f02-gesture.jar','app_process','/system/bin','TouchGesture',str(x),str(y),'4000','0']
proc=subprocess.Popen(cmd,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);time.sleep(1.2);screenshot('feedback-segmented-held');response=proc.communicate(timeout=15)[0];assert b'TouchGesture delivered' in response and level()==before
print('API18 segmented held screenshot and unchanged level PASS')
