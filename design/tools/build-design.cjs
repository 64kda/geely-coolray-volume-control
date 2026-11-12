// Vector design deliverables only. Does not modify or rebuild either APK.
const fs = require('node:fs');
const path = require('node:path');
const runtime = 'C:/Users/XGamerXFuryX/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules';
const sharp = require(path.join(runtime, 'sharp'));
const lucide = require(path.join(runtime, 'lucide'));
const root = path.resolve(__dirname, '..');
for (const dir of ['concepts', 'icon', 'icon/android', 'scale']) fs.mkdirSync(path.join(root, dir), {recursive:true});
const C={bg:'#121212',panel:'#1F1F1F',sel:'#282828',green:'#1ED760',white:'#F5F5F5',muted:'#B3B3B3',line:'#353535'};
const esc=s=>String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/"/g,'&quot;');
const rect=(x,y,w,h,fill,r=0,stroke='none')=>`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${r}" fill="${fill}" stroke="${stroke}"/>`;
const text=(x,y,s,size=24,fill=C.white,weight=400,anchor='start')=>`<text x="${x}" y="${y}" font-family="Segoe UI,Arial,sans-serif" font-size="${size}" font-weight="${weight}" fill="${fill}" text-anchor="${anchor}">${esc(s)}</text>`;
function icon(name,x,y,size=28,color=C.muted){
  const nodes=lucide.icons[name]; if(!nodes)throw new Error('Unknown Lucide icon '+name);
  return `<g transform="translate(${x} ${y}) scale(${size/24})" fill="none" stroke="${color}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">`+nodes.map(([tag,attrs])=>`<${tag} ${Object.entries(attrs).map(([k,v])=>`${k}="${esc(v)}"`).join(' ')}/>`).join('')+'</g>';
}
const line=(x,y,x2,y2,color=C.line)=>`<path d="M${x} ${y}H${x2}" stroke="${color}"/>`;
function toggle(x,y,on=true){return rect(x,y,64,36,on?C.green:'#727272',18)+`<circle cx="${x+(on?46:18)}" cy="${y+18}" r="14" fill="white"/>`;}
function button(x,y,w,label,name,primary=false){return rect(x,y,w,62,primary?C.green:C.panel,31)+icon(name,x+22,y+18,26,primary?C.bg:C.white)+text(x+60,y+40,label,24,primary?C.bg:C.white,600);}
function slider(x,y,w,pct=65){return rect(x,y,w,10,'#434343',5)+rect(x,y,w*pct/100,10,C.green,5)+`<circle cx="${x+w*pct/100}" cy="${y+5}" r="13" fill="${C.white}"/>`;}
function vbar(x,y,h=320,expanded=true){
  const w=expanded?98:36;
  let s=rect(x,y,w,h,C.sel,Math.min(w/2,36),C.line);
  if(expanded){s+=text(x+w/2,y+42,'65%',22,C.white,600,'middle');s+=icon('Volume2',x+35,y+h-49,28,C.white);}
  const top=y+(expanded?65:16),length=h-(expanded?145:32),cx=x+w/2;
  s+=rect(cx-5,top,10,length,'#4B4B4B',5)+rect(cx-5,top+length*.35,10,length*.65,C.green,5);
  s+=`<circle cx="${cx}" cy="${top+length*.35}" r="${expanded?12:9}" fill="${C.white}"/>`;
  return s;
}
function hbar(x,y,w=430){return rect(x,y,w,94,C.sel,32,C.line)+icon('Volume2',x+25,y+32,30,C.white)+slider(x+84,y+42,w-188)+text(x+w-26,y+55,'65%',24,C.white,600,'end');}
function svg(body,w=1920,h=640,bg=C.bg){return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}"><title>Регулятор громкости — дизайн-концепция</title>${bg?rect(0,0,w,h,bg):''}${body}</svg>`;}
const nav=[['SlidersHorizontal','Управление'],['PanelRight','Шкала'],['Volume2','Звук'],['AudioLines','Эквалайзер'],['Power','Работа приложения'],['Info','О приложении']];
function rail(selected=0){
 let s=rect(0,0,356,640,'#0C0C0C')+icon('SlidersHorizontal',34,35,38,C.green)+text(91,53,'Регулятор',27,C.white,700)+text(91,85,'громкости',27,C.white,700);
 nav.forEach(([i,label],n)=>{const y=126+n*72;if(n===selected)s+=rect(18,y,320,62,C.sel,12);s+=icon(i,38,y+18,27,n===selected?C.green:C.muted)+text(84,y+40,label,23,n===selected?C.white:C.muted,n===selected?600:400);});
 return s;
}
const status=(x,y)=>text(x,y+26,'Регулятор включён',23,C.muted)+toggle(x+254,y);

let a=rail()+text(408,70,'Управление',42,C.white,700)+status(1090,38);
a+=text(408,126,'Громкость всегда под рукой',24,C.muted)+line(408,158,1858,158);
a+=text(408,213,'АУДИОКАНАЛ',18,C.muted,600)+rect(408,235,890,68,C.panel,12)+icon('Music2',432,256,28,C.green)+text(480,279,'Музыка и мультимедиа',28,C.white,600)+icon('ChevronDown',1240,257,26);
a+=text(408,417,'65',106,C.white,700)+text(542,414,'%',42,C.muted)+text(409,453,'Текущий уровень',22,C.muted)+slider(675,386,620);
a+=button(408,516,234,'Микшер','SlidersVertical')+button(660,516,284,'Эквалайзер','AudioLines');
a+=rect(1390,190,468,416,C.panel,20)+text(1420,230,'Предпросмотр шкалы',24,C.white,600)+vbar(1569,254,280)+text(1624,574,'Справа · сплошная',20,C.muted,400,'middle');

let b=rect(0,0,1920,640,C.bg)+icon('SlidersHorizontal',44,36,40,C.green)+text(104,66,'Регулятор громкости',35,C.white,700)+status(1490,38)+line(44,110,1876,110);
b+=text(44,165,'Настройки',25,C.muted);
nav.forEach(([i,label],n)=>{const col=n%2,row=Math.floor(n/2),x=44+col*398,y=193+row*120;b+=rect(x,y,378,100,C.panel,12)+icon(i,x+22,y+33,32,n===0?C.green:C.muted)+text(x+72,y+58,label,23,C.white,600)+icon('ChevronRight',x+338,y+38,24);});
b+=rect(916,151,960,445,C.panel,20)+text(954,205,'Музыка и мультимедиа',30,C.white,600)+icon('ChevronDown',1815,180,27)+text(954,323,'65%',80,C.white,700)+text(1198,313,'Текущий уровень',22,C.muted);
b+=hbar(954,365,884)+text(954,496,'Горизонтальная шкала',24,C.white,600)+text(954,534,'Крупная область касания · минимум деталей',22,C.muted)+text(44,607,'Все настройки доступны в разделах',21,C.muted);

let c=icon('SlidersHorizontal',36,32,36,C.green)+text(93,59,'Громкость',32,C.white,700)+status(1490,32);
const tabWidths=[226,166,162,218,298,230];let tx=36;
nav.forEach(([i,label],n)=>{const w=tabWidths[n],y=103;if(n===0)c+=rect(tx,y,w,60,C.sel,12);c+=icon(i,tx+15,y+18,24,n===0?C.green:C.muted)+text(tx+51,y+39,label,22,n===0?C.white:C.muted,600);tx+=w+13;});
c+=text(48,231,'Музыка и мультимедиа',32,C.white,700)+icon('ChevronDown',499,210,28)+text(48,283,'Выбранный канал',22,C.muted);
c+=text(48,403,'65%',100,C.white,700)+slider(343,355,767)+text(343,407,'Плавная регулировка одним движением',22,C.muted);
c+=button(48,473,240,'Микшер','SlidersVertical')+button(306,473,283,'Эквалайзер','AudioLines');
c+=rect(1254,200,618,392,C.panel,20)+text(1288,242,'Компактная шкала у края',26,C.white,600)+vbar(1745,270,270,false)+text(1288,319,'Тонкая полоса',26,C.white,600)+text(1288,355,'с заметным бегунком',24,C.muted)+text(1288,423,'65%',62,C.white,700)+text(1288,488,'Уровень виден при касании',22,C.muted)+text(1288,527,'Без перекрытия центра экрана',22,C.muted);

const files=[['concepts/01-studio.svg',svg(a)],['concepts/02-sections.svg',svg(b)],['concepts/03-compact.svg',svg(c)]];
let scale=text(64,66,'Шкала громкости',38,C.white,700)+text(64,108,'Три состояния одного оформления',22,C.muted);
scale+=text(64,177,'Вертикальная',25,C.white,600)+vbar(137,218,320)+text(401,177,'Горизонтальная',25,C.white,600)+hbar(401,270,560)+text(401,410,'Уровень 65% · белый бегунок · зелёное заполнение',21,C.muted);
scale+=text(1070,177,'Компактная',25,C.white,600)+vbar(1152,218,320,false)+text(1070,589,'Видимая полоса уже области касания',19,C.muted);
files.push(['scale/volume-scale.svg',svg(scale,1480,640)]);
async function build(){
 for(const [file,data] of files){fs.writeFileSync(path.join(root,file),data);await sharp(Buffer.from(data)).png().toFile(path.join(root,file.replace(/\.svg$/,'.png')));}
 await require('./build-icon.cjs')();
 console.log('Built 3 concepts, scale sheet and icon exports. APK unchanged.');
}
build().catch(e=>{console.error(e);process.exitCode=1;});
