package rubberbigpepper.VolumeControl.redesign;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.AudioManager;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Reuses every original setting View and its listener; API18 only. */
public final class MainUi {
    private static final int BG=0xff121212,SURFACE=0xff1f1f1f,SELECTED=0xff282828;
    private static final int INK=0xfff5f5f5,MUTED=0xffb3b3b3,GREEN=0xff1ed760;
    private final Activity a;
    private final LinearLayout root,body,tabs;
    private final LinearLayout[] pages=new LinearLayout[6];
    private final Button[] tabButtons=new Button[6];
    private final Spinner channels;
    private final AudioManager audio;
    private SeekBar level;private TextView percent;private boolean touching;
    private View preview;
    private Button showScale,hideScale;
    private View[] settings;
    private Spinner percentPosition;
    private Spinner hideDelay,hideFade;
    private CheckBox guardToggle,guardFeedback;
    private Spinner guardHold;
    private final int[] hideDelays={1000,2000,3000,5000,10000},hideFades={0,150,300,600};
    private int percentChoice;
    private static boolean resetting;
    public static boolean isResetting(){return resetting;}
    private int lastStream=-1,lastMax=-1,lastValue=-1;
    private final HashSet<View> moved=new HashSet<View>();
    private final String[] titles={"Управление","Шкала","Звук","Эквалайзер","Работа приложения","О приложении"};
    public static void install(Activity activity){new MainUi(activity);}
    public static void prepare(Activity activity){
        android.content.SharedPreferences prefs=activity.getSharedPreferences("Common",0);
        if(!prefs.contains("Main color"))prefs.edit().putInt("Main color",GREEN).putInt("Background color",0xe0555555)
            .putInt("Active color",GREEN).putInt("Inactive color",SELECTED).putInt("Size",26).commit();
    }
    private MainUi(Activity activity){
        a=activity;audio=(AudioManager)a.getSystemService(Context.AUDIO_SERVICE);
        root=(LinearLayout)a.findViewById(0x7f070036);
        channels=(Spinner)a.findViewById(0x7f07004f);
        final View[] all={a.findViewById(0x7f070039),a.findViewById(0x7f07003c),a.findViewById(0x7f07003f),
            a.findViewById(0x7f070041),a.findViewById(0x7f070043),a.findViewById(0x7f070045),a.findViewById(0x7f070046),
            a.findViewById(0x7f070047),a.findViewById(0x7f070049),a.findViewById(0x7f07004b),a.findViewById(0x7f07004d),
            channels,a.findViewById(0x7f070050),a.findViewById(0x7f070052),a.findViewById(0x7f070053),
            a.findViewById(0x7f070054),a.findViewById(0x7f070055),a.findViewById(0x7f070056),
            a.findViewById(0x7f070057),a.findViewById(0x7f070060),a.findViewById(0x7f070063),
            a.findViewById(0x7f070064),a.findViewById(0x7f070065)};
        settings=all;
        // Moving a Spinner between pages can report the same selection again.
        // Delegate only an actual change, not a layout/attachment notification.
        for(View setting:all)if(setting instanceof Spinner){Spinner spinner=(Spinner)setting;selectQuietly(spinner,spinner.getSelectedItemPosition());}
        Spinner zeroAction=(Spinner)a.findViewById(0x7f070051);
        selectQuietly(zeroAction,zeroAction.getSelectedItemPosition());
        View form=a.findViewById(0x7f070020);
        ArrayList<View> rows=new ArrayList<View>();
        for(View item:all){View row=item;while(row.getParent()!=form && row.getParent()!=null && row.getParent()!=root &&
            !(row instanceof Button) && !(row instanceof CheckBox) && row.getId()!=0x7f070057 && row.getId()!=0x7f070050)
            row=(View)row.getParent();rows.add(row);}
        ArrayList<View> extra=new ArrayList<View>();
        for(int i=0;i<root.getChildCount();i++){View child=root.getChildAt(i);if(!(child instanceof ScrollView)&&child.getId()!=0x7f070061)extra.add(child);}
        boolean compact=a.getResources().getConfiguration().screenWidthDp<840;
        root.removeAllViews();root.setBackgroundColor(BG);root.setPadding(dp(compact?16:24),dp(8),dp(compact?16:24),dp(8));
        LinearLayout header=new LinearLayout(a);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView name=label("Регулятор громкости",compact?22:26,INK);name.setTypeface(null,1);
        header.addView(name,new LinearLayout.LayoutParams(0,dp(52),1));
        TextView badge=label("Настройки",17,MUTED);if(!compact)header.addView(badge);
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(56)));
        HorizontalScrollView nav=new HorizontalScrollView(a);nav.setHorizontalScrollBarEnabled(false);
        tabs=new LinearLayout(a);nav.addView(tabs,new HorizontalScrollView.LayoutParams(-2,-1));root.addView(nav,new LinearLayout.LayoutParams(-1,dp(58)));
        body=new LinearLayout(a);body.setOrientation(1);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<6;i++){
            pages[i]=new LinearLayout(a);pages[i].setOrientation(1);pages[i].setPadding(0,dp(12),0,dp(12));
            final int page=i;Button b=action(titles[i]);b.setTextSize(17);b.setCompoundDrawablesWithIntrinsicBounds(new TabIcon(i,dp(22)),null,null,null);b.setCompoundDrawablePadding(dp(8));
            b.setOnClickListener(new View.OnClickListener(){public void onClick(View v){show(page);}});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(50));lp.rightMargin=dp(8);tabs.addView(b,lp);tabButtons[i]=b;
        }
        TextView volumeHeading=label("Текущая громкость",20,MUTED);pages[0].addView(volumeHeading);
        add(0,rows.get(11));
        percent=label("",64,INK);percent.setTypeface(null,1);pages[0].addView(percent);
        level=new SeekBar(a);level.setContentDescription("Громкость выбранного канала");styleSlider(level);pages[0].addView(level,new LinearLayout.LayoutParams(-1,dp(64)));
        level.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar s){touching=true;}
            public void onStopTrackingTouch(SeekBar s){touching=false;refresh();}
            public void onProgressChanged(SeekBar s,int value,boolean user){if(user)changeVolume(value,s.getMax());}
        });
        TextView hint=label("Перетащите ползунок, чтобы изменить громкость.",17,MUTED);
        hint.setPadding(0,dp(10),0,dp(14));pages[0].addView(hint);
        LinearLayout commands=new LinearLayout(a);for(int index:new int[]{20,21}){View v=rows.get(index);detach(v);style(v);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(58),1);if(index==20)lp.rightMargin=dp(12);commands.addView(v,lp);}
        showScale=(Button)all[20];hideScale=(Button)all[21];
        final View.OnClickListener originalCommands=(View.OnClickListener)a;
        View.OnClickListener chooseScale=new View.OnClickListener(){public void onClick(View v){
            originalCommands.onClick(v);
            markScaleChoice(v==showScale);
        }};
        showScale.setOnClickListener(chooseScale);hideScale.setOnClickListener(chooseScale);
        // The original Activity starts the scale each time its form is created.
        markScaleChoice(true);
        pages[0].addView(commands);
        LinearLayout effects=new LinearLayout(a);
        for(int i=0;i<2;i++){final boolean mixer=i==0;Button b=action(mixer?"Микшер":"Эквалайзер");
            b.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
                Intent intent=new Intent();intent.setClassName(a,"rubberbigpepper.CommonCtrl.VolumeControlService");
                intent.setAction(mixer?"local.volumecontrol.redesign.MIXER":"local.volumecontrol.redesign.EQUALIZER");a.startService(intent);
            }});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(58),1);if(i==0)lp.rightMargin=dp(12);lp.topMargin=dp(10);effects.addView(b,lp);}
        pages[0].addView(effects);
        LinearLayout left=new LinearLayout(a);left.setOrientation(1);left.setPadding(dp(20),dp(20),dp(20),dp(20));left.setBackgroundDrawable(surface(SURFACE,20));
        while(pages[0].getChildCount()>0){View child=pages[0].getChildAt(0);pages[0].removeViewAt(0);left.addView(child);}
        pages[0].setOrientation(compact?1:0);LinearLayout.LayoutParams leftParams=new LinearLayout.LayoutParams(compact?-1:0,-2,compact?0:2);if(!compact)leftParams.rightMargin=dp(20);pages[0].addView(left,leftParams);
        LinearLayout right=new LinearLayout(a);right.setOrientation(1);right.setPadding(dp(24),dp(24),dp(24),dp(24));right.setBackgroundDrawable(surface(SURFACE,20));
        right.addView(label("Плавающая шкала",24,INK));right.addView(label("Пример уровня на вертикальной шкале",17,MUTED));
        preview=new Preview(a);right.addView(preview,new LinearLayout.LayoutParams(-1,dp(compact?180:240)));
        right.addView(label("Цвета и положение — на вкладке «Шкала». Удержание шкалы открывает инструменты звука.",17,MUTED));
        LinearLayout.LayoutParams rightParams=new LinearLayout.LayoutParams(compact?-1:0,-2,compact?0:1);if(compact)rightParams.topMargin=dp(16);pages[0].addView(right,rightParams);
        for(int i=0;i<=10;i++)add(1,rows.get(i));
        pages[1].addView(label("Положение процента",20,INK));
        percentPosition=new Spinner(a);
        ArrayAdapter<String> positions=new ArrayAdapter<String>(a,android.R.layout.simple_spinner_item,
            new String[]{"По центру экрана (совместимость)","Рядом со шкалой (рекомендуется)"});
        positions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        percentPosition.setAdapter(positions);
        percentChoice=a.getSharedPreferences("Common",0).getInt("Percent position",0)==1?1:0;
        percentPosition.setSelection(percentChoice);
        percentPosition.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                if(position==percentChoice)return;percentChoice=position;
                a.getSharedPreferences("Common",0).edit().putInt("Percent position",position).commit();
                if(!resetting)showScale.performClick();
            }
        });
        pages[1].addView(percentPosition,new LinearLayout.LayoutParams(-1,dp(58)));
        pages[1].addView(label("Выберите левую или правую шкалу в расположении выше. Процент следует за ней. Максимальная длина увеличена для вертикальной шкалы; ширина сохранена. Рядом со шкалой приложение показывает свой процент вместо системного уведомления.",17,MUTED));
        add(2,rows.get(12));add(2,rows.get(13));add(2,rows.get(14));
        pages[2].addView(label("Аудиоканал выбирается на вкладке «Управление».",17,MUTED));
        add(3,rows.get(17));add(3,rows.get(18));
        pages[3].addView(label("Без защиты удерживайте шкалу более 3 секунд или коснитесь её несколькими пальцами. С защитой удерживайте один палец 3 секунды плюс время подтверждения, не начиная регулировку. В окне доступны эквалайзер и микшер.",18,MUTED));
        // The original form is detached now; use the View captured before rebuilding it.
        CheckBox boot=(CheckBox)all[19];
        boot.setText("Автозапуск с магнитолой");
        add(4,rows.get(19));
        pages[4].addView(label("Включает регулятор после полной загрузки Android. После установки откройте приложение один раз. Если магнитола управляет автозапуском отдельно, разрешите его также в её настройках. Выход из сна зависит от прошивки магнитолы.",17,MUTED));
        add(4,rows.get(16));
        // Keep the original detached checkbox for legacy form serialization only.
        guardControls();
        hideDelay=hideChoice("Скрывать после","Auto hide delay ms",new String[]{"1 секунда","2 секунды","3 секунды","5 секунд","10 секунд"},hideDelays,true);
        hideFade=hideChoice("Исчезновение","Auto hide fade ms",new String[]{"Мгновенно","150 мс","300 мс","600 мс"},hideFades,false);
        pages[4].addView(label("Отсчёт начинается после отпускания шкалы. Время исчезновения задаётся отдельно.",17,MUTED));
        ((CheckBox)settings[16]).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            public void onCheckedChanged(CompoundButton button,boolean checked){
                ((CompoundButton.OnCheckedChangeListener)a).onCheckedChanged(button,checked);hideChoicesEnabled();
            }
        });
        hideChoicesEnabled();
        pages[5].addView(label("Регулятор громкости",28,INK));pages[5].addView(label("Версия 2.7.1 · новое оформление\nДля Android 4.3 и новее\nИсходные настройки и аудиоканалы сохранены.",19,MUTED));
        // The old publisher's donation action is not an audio setting.
        detach(rows.get(22));rows.get(22).setVisibility(View.GONE);
        for(View v:extra){detach(v);pages[5].addView(v);}
        for(int page=0;page<5;page++)addResetActions(page);
        show(0);refresh();
        root.postDelayed(new Runnable(){public void run(){if(root.getWindowToken()!=null){if(!touching)refresh();root.postDelayed(this,1000);}}},1000);
    }
    private void detach(View v){if(v.getParent() instanceof ViewGroup)((ViewGroup)v.getParent()).removeView(v);}
    private void addResetActions(final int page){
        LinearLayout destination=page==0?(LinearLayout)pages[0].getChildAt(0):pages[page];
        TextView note=label("Сброс затрагивает только настройки этой вкладки. Громкость сохраняется. Для обновления плавающей шкалы нажмите «Применить к шкале».",17,MUTED);
        note.setPadding(0,dp(16),0,dp(10));destination.addView(note);
        Button reset=action("Сбросить настройки вкладки");reset.setMinHeight(dp(54));
        reset.setOnClickListener(new View.OnClickListener(){public void onClick(View v){resetPage(page);}});
        destination.addView(reset,new LinearLayout.LayoutParams(-1,dp(58)));
        Button apply=action("Применить к шкале");apply.setMinHeight(dp(54));
        apply.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showScale.performClick();}});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58));lp.topMargin=dp(8);destination.addView(apply,lp);
    }
    private void resetPage(int page){
        SharedPreferences prefs=a.getSharedPreferences("Common",0);
        SharedPreferences.Editor editor=prefs.edit();
        for(Map.Entry<String,Object> entry:TabDefaults.forPage(page,prefs.getAll().keySet()).entrySet()){
            Object value=entry.getValue();
            if(value instanceof Boolean)editor.putBoolean(entry.getKey(),(Boolean)value);
            else editor.putInt(entry.getKey(),(Integer)value);
        }
        if(!editor.commit()){Toast.makeText(a,"Не удалось сохранить сброс настроек",Toast.LENGTH_LONG).show();return;}
        resetting=true;
        try{
            ((ResetHost)a).setResetColors(prefs.getInt("Main color",GREEN),prefs.getInt("Background color",0xe0555555),
                prefs.getInt("Active color",GREEN),prefs.getInt("Inactive color",SELECTED));
            switch(page){
                case 0: selectQuietly(channels,2);break;
                case 1:
                    ((SeekBar)settings[0]).setProgress(26);setColorControl((ImageView)settings[1],(SeekBar)settings[2],GREEN);
                    setColorControl((ImageView)settings[3],(SeekBar)settings[4],0xe0555555);
                    selectQuietly((Spinner)settings[5],11);((SeekBar)settings[6]).setProgress(50);
                    ((CheckBox)settings[7]).setChecked(false);((EditText)settings[8]).setText("0");((EditText)settings[9]).setText("0");
                    selectQuietly((Spinner)settings[10],0);percentChoice=0;percentPosition.setSelection(0);break;
                case 2:
                    selectQuietly((Spinner)pages[2].findViewById(0x7f070051),0);
                    ((CheckBox)settings[13]).setChecked(false);((CheckBox)settings[14]).setChecked(false);break;
                case 3:
                    ((CheckBox)settings[17]).setChecked(true);settings[18].setVisibility(View.VISIBLE);
                    setColorControl((ImageView)pages[3].findViewById(0x7f070059),(SeekBar)pages[3].findViewById(0x7f07005b),GREEN);
                    setColorControl((ImageView)pages[3].findViewById(0x7f07005d),(SeekBar)pages[3].findViewById(0x7f07005f),SELECTED);break;
                case 4:
                    ((CheckBox)settings[19]).setChecked(false);((CheckBox)settings[15]).setChecked(false);((CheckBox)settings[16]).setChecked(false);
                    hideDelay.setSelection(1);hideFade.setSelection(2);hideChoicesEnabled();
                    guardToggle.setChecked(false);guardHold.setSelection(0);guardHold.setEnabled(false);guardFeedback.setChecked(true);guardFeedback.setEnabled(false);break;
            }
        }finally{resetting=false;}
        boolean effectsOk=page!=3||EffectReset.resetLive();
        refresh();Toast.makeText(a,effectsOk?"Настройки вкладки сброшены":"Настройки сохранены, но устройство не позволило полностью сбросить эффекты",Toast.LENGTH_LONG).show();
    }
    private void setColorControl(ImageView sample,SeekBar alpha,int color){
        Bitmap image=Bitmap.createBitmap(64,40,Bitmap.Config.ARGB_8888);image.eraseColor(color);
        sample.setImageBitmap(image);alpha.setProgress(Color.alpha(color));
    }
    private Spinner hideChoice(String title,final String key,String[] labels,final int[] values,boolean delay){
        pages[4].addView(label(title,20,INK));final Spinner spinner=new Spinner(a);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(a,android.R.layout.simple_spinner_item,labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(adapter);
        int saved=AutoHideController.read(a.getSharedPreferences("Common",0),key,delay),index=0;
        for(int i=0;i<values.length;i++)if(values[i]==saved)index=i;
        spinner.setSelection(index);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                if(resetting)return;
                SharedPreferences prefs=a.getSharedPreferences("Common",0);
                // Compare normalized persisted values: attachment/reset callbacks are inert.
                if(AutoHideController.read(prefs,key,key.equals("Auto hide delay ms"))==values[position])return;
                if(!prefs.edit().putInt(key,values[position]).commit()){
                    Toast.makeText(a,"Не удалось сохранить настройку",Toast.LENGTH_LONG).show();return;
                }
                showScale.performClick();
            }
        });
        pages[4].addView(spinner,new LinearLayout.LayoutParams(-1,dp(58)));return spinner;
    }
    private void guardControls(){
        final SharedPreferences prefs=a.getSharedPreferences("Common",0);
        guardToggle=new CheckBox(a);guardToggle.setText("Защита от случайного касания");style(guardToggle);
        guardToggle.setChecked(TouchGuardController.enabled(prefs));pages[4].addView(guardToggle);
        pages[4].addView(label("Удерживать перед регулировкой",20,INK));
        guardHold=new Spinner(a);final int[] values={250,500,1000};
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(a,android.R.layout.simple_spinner_item,new String[]{"250 мс","500 мс","1000 мс"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);guardHold.setAdapter(adapter);
        int saved=TouchGuardController.hold(prefs);guardHold.setSelection(saved==500?1:saved==1000?2:0);
        guardHold.setEnabled(guardToggle.isChecked());pages[4].addView(guardHold,new LinearLayout.LayoutParams(-1,dp(58)));
        pages[4].addView(label("Удержите палец, затем ведите вдоль шкалы. Короткое касание не меняет громкость.",17,MUTED));
        guardFeedback=new CheckBox(a);guardFeedback.setText("Подсветка при удержании");style(guardFeedback);
        guardFeedback.setChecked(TouchGuardController.feedback(prefs));guardFeedback.setEnabled(guardToggle.isChecked());
        pages[4].addView(guardFeedback);
        guardFeedback.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            public void onCheckedChanged(CompoundButton button,boolean checked){
                if(resetting)return;
                if(!prefs.edit().putBoolean("Touch guard feedback",checked).commit()){
                    guardFeedback.setChecked(TouchGuardController.feedback(prefs));
                    Toast.makeText(a,"Не удалось сохранить настройку",Toast.LENGTH_LONG).show();
                }
            }
        });
        guardToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            public void onCheckedChanged(CompoundButton button,boolean checked){
                guardHold.setEnabled(checked);guardFeedback.setEnabled(checked);if(resetting)return;
                if(!prefs.edit().putBoolean("Touch guard enabled",checked).commit()){
                    Toast.makeText(a,"Не удалось сохранить настройку",Toast.LENGTH_LONG).show();return;
                }
                showScale.performClick();
            }
        });
        guardHold.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                if(resetting||TouchGuardController.hold(prefs)==values[position])return;
                if(!prefs.edit().putInt("Touch guard hold ms",values[position]).commit()){
                    Toast.makeText(a,"Не удалось сохранить настройку",Toast.LENGTH_LONG).show();return;
                }
                showScale.performClick();
            }
        });
    }
    private void hideChoicesEnabled(){
        boolean enabled=((CheckBox)settings[16]).isChecked();
        if(hideDelay!=null)hideDelay.setEnabled(enabled);if(hideFade!=null)hideFade.setEnabled(enabled);
    }
    private void selectQuietly(final Spinner spinner,int position){
        final AdapterView.OnItemSelectedListener original=(AdapterView.OnItemSelectedListener)a;
        final int[] previous={position};
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,View view,int selected,long id){
                if(selected==previous[0])return;previous[0]=selected;original.onItemSelected(parent,view,selected,id);
            }
            public void onNothingSelected(AdapterView<?> parent){original.onNothingSelected(parent);}
        });
        spinner.setSelection(position);
    }
    private void markScaleChoice(boolean shown){
        markScaleButton(showScale,shown);markScaleButton(hideScale,!shown);
    }
    private void markScaleButton(Button button,boolean chosen){
        button.setSelected(chosen);button.setTextColor(chosen?GREEN:INK);
        button.setBackgroundDrawable(scaleChoiceBackground(chosen));
        button.setCompoundDrawablesWithIntrinsicBounds(new ChoiceIcon(chosen,dp(24)),null,null,null);
        button.setCompoundDrawablePadding(dp(10));
        button.setContentDescription(button.getText()+(chosen?", выбрано":""));
    }
    private void add(int page,View view){if(moved.contains(view))return;moved.add(view);detach(view);style(view);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);pages[page].addView(view,lp);}
    private void show(int n){body.removeAllViews();detach(pages[n]);ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.addView(pages[n]);body.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
        for(int i=0;i<6;i++){tabButtons[i].setSelected(i==n);tabButtons[i].setBackgroundDrawable(actionBackground(i==n?SELECTED:BG));tabButtons[i].setTextColor(i==n?GREEN:MUTED);}
        if(n==2){View action=a.findViewById(0x7f070050);if(action!=null)action.setVisibility(View.VISIBLE);}refresh();}
    private int currentStream(){return VolumeMath.displayStream(VolumeMath.streamAt(channels.getSelectedItemPosition()),audio.getMode(),audio.isMusicActive());}
    private void refresh(){int stream=currentStream(),max=audio.getStreamMaxVolume(stream),value=audio.getStreamVolume(stream);
        if(!touching){if(level.getMax()!=max)level.setMax(max);if(level.getProgress()!=value)level.setProgress(value);}
        if(stream!=lastStream||max!=lastMax||value!=lastValue){percent.setText(VolumeMath.percent(value,max)+" %");if(preview!=null)preview.invalidate();lastStream=stream;lastMax=max;lastValue=value;}}
    private void changeVolume(int value,int max){
        int selected=VolumeMath.streamAt(channels.getSelectedItemPosition());
        int flags=PercentPlacement.systemNotification(a,a.getSharedPreferences("Common",0).getBoolean("Show notification",false))?1:0;
        if(a.getSharedPreferences("Common",0).getBoolean("Sound notification",false))flags|=4;
        try{
            if(selected==-1){for(int stream:new int[]{3,4,8,5,2,1,0})audio.setStreamVolume(stream,VolumeMath.scale(value,max,audio.getStreamMaxVolume(stream)),stream==3?flags:0);}
            else audio.setStreamVolume(currentStream(),value,flags);
            if(selected==-1||currentStream()==2){
                int mode=VolumeMath.ringerMode(value,a.getSharedPreferences("Common",0).getInt("Action on 0 volume",0));
                if(mode>=0)audio.setRingerMode(mode);
            }
            percent.setText(VolumeMath.percent(value,max)+" %");
            if(preview!=null)preview.invalidate();
        }catch(SecurityException failure){Toast.makeText(a,"Не удалось изменить громкость этого канала",Toast.LENGTH_SHORT).show();refresh();}
    }
    private int dp(int x){return Math.round(x*a.getResources().getDisplayMetrics().density);}
    private TextView label(String s,int size,int color){TextView t=new TextView(a);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private GradientDrawable surface(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private StateListDrawable scaleChoiceBackground(boolean chosen){
        StateListDrawable states=new StateListDrawable();
        int border=chosen?GREEN:0xff686868;
        GradientDrawable pressed=surface(0xff3b3b3b,12);pressed.setStroke(dp(1),border);
        states.addState(new int[]{android.R.attr.state_pressed},pressed);
        GradientDrawable focus=surface(SELECTED,12);focus.setStroke(dp(2),GREEN);
        states.addState(new int[]{android.R.attr.state_focused},focus);
        GradientDrawable normal=surface(SELECTED,12);normal.setStroke(dp(1),border);
        states.addState(new int[]{},normal);return states;
    }
    private StateListDrawable actionBackground(int color){return actionBackground(color,false);}
    private StateListDrawable actionBackground(int color,boolean outlined){
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed},surface(0xff3b3b3b,12));
        GradientDrawable focus=surface(color,12);focus.setStroke(dp(2),GREEN);
        states.addState(new int[]{android.R.attr.state_focused},focus);
        GradientDrawable normal=surface(color,12);if(outlined)normal.setStroke(dp(1),GREEN);
        states.addState(new int[]{},normal);return states;
    }
    private void styleAction(Button b){b.setTextColor(INK);b.setBackgroundDrawable(actionBackground(SELECTED));}
    private Button action(String s){Button b=new Button(a);b.setText(s);b.setTextSize(18);b.setPadding(dp(16),dp(4),dp(16),dp(4));styleAction(b);return b;}
    private void styleSlider(SeekBar s){
        LayerDrawable layers=new LayerDrawable(new Drawable[]{new TrackIcon(false,dp(7)),new TrackIcon(true,dp(7))});layers.setId(0,android.R.id.background);layers.setId(1,android.R.id.progress);s.setProgressDrawable(layers);
        GradientDrawable thumb=surface(INK,12);thumb.setSize(dp(22),dp(22));s.setThumb(thumb);s.setPadding(dp(12),dp(8),dp(12),dp(8));s.setMinimumHeight(dp(52));
        if(s.getLayoutParams()!=null){ViewGroup.LayoutParams lp=s.getLayoutParams();lp.height=dp(56);s.setLayoutParams(lp);}
    }
    private void style(View v){
        if(v instanceof RelativeLayout)styleColorRow((RelativeLayout)v);
        if(v instanceof TextView){TextView t=(TextView)v;t.setTextColor(INK);t.setTextSize(18);}
        if(v instanceof CheckBox){CheckBox c=(CheckBox)v;c.setButtonDrawable(new ColorDrawable(Color.TRANSPARENT));c.setCompoundDrawablesWithIntrinsicBounds(null,null,new ToggleIcon(dp(58),dp(34)),null);c.setCompoundDrawablePadding(dp(16));c.setMinHeight(dp(62));c.setPadding(dp(16),dp(8),dp(16),dp(8));c.setBackgroundDrawable(surface(SURFACE,12));}
        else if(v instanceof Button){styleAction((Button)v);v.setMinimumHeight(dp(54));}
        else if(v instanceof EditText){((EditText)v).setSingleLine(true);((EditText)v).setHintTextColor(MUTED);v.setMinimumHeight(dp(52));}
        else if(v instanceof Spinner){v.setMinimumHeight(dp(56));}
        else if(v instanceof SeekBar)styleSlider((SeekBar)v);
        if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++)style(group.getChildAt(i));}
    }
    private void styleColorRow(RelativeLayout row){
        ImageView sample=null;
        for(int i=0;i<row.getChildCount();i++){
            View child=row.getChildAt(i);int id=child.getId();
            if(child instanceof ImageView&&(id==0x7f07003c||id==0x7f070041||id==0x7f070059||id==0x7f07005d))sample=(ImageView)child;
        }
        if(sample==null)return;
        row.setMinimumHeight(dp(64));row.setPadding(dp(16),dp(8),dp(16),dp(8));row.setBackgroundDrawable(surface(SURFACE,12));
        RelativeLayout.LayoutParams chip=new RelativeLayout.LayoutParams(dp(72),dp(48));
        chip.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);chip.addRule(RelativeLayout.CENTER_VERTICAL);
        sample.setLayoutParams(chip);sample.setAdjustViewBounds(false);sample.setScaleType(ImageView.ScaleType.FIT_XY);
        sample.setPadding(dp(4),dp(4),dp(4),dp(4));GradientDrawable frame=surface(0xff333333,12);frame.setStroke(dp(1),0xff555555);sample.setBackgroundDrawable(frame);
        for(int i=0;i<row.getChildCount();i++)if(row.getChildAt(i) instanceof TextView){
            TextView title=(TextView)row.getChildAt(i);title.setMinWidth(0);title.setMaxWidth(Integer.MAX_VALUE);
            RelativeLayout.LayoutParams label=new RelativeLayout.LayoutParams(-1,-2);label.addRule(RelativeLayout.CENTER_VERTICAL);label.rightMargin=dp(96);title.setLayoutParams(label);
            sample.setContentDescription("Выбрать: "+title.getText());
        }
    }
    private static final class ChoiceIcon extends Drawable {
        private final Paint p=new Paint(3);private final boolean chosen;private final int size;
        ChoiceIcon(boolean value,int pixels){chosen=value;size=pixels;}
        public int getIntrinsicWidth(){return size;}public int getIntrinsicHeight(){return size;}
        public void draw(Canvas c){if(!chosen)return;Rect b=getBounds();c.save();c.translate(b.left,b.top);c.scale(b.width()/24f,b.height()/24f);
            p.setColor(GREEN);p.setStyle(Paint.Style.FILL);c.drawCircle(12,12,11,p);
            p.setColor(BG);p.setStrokeWidth(2.5f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);
            Path tick=new Path();tick.moveTo(6.5f,12);tick.lineTo(10,15.5f);tick.lineTo(17.5f,8);c.drawPath(tick,p);c.restore();}
        public void setAlpha(int value){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private static final class ToggleIcon extends Drawable {
        private final Paint p=new Paint(3);private final RectF r=new RectF();private final int w,h;
        ToggleIcon(int width,int height){w=width;h=height;}
        public int getIntrinsicWidth(){return w;}public int getIntrinsicHeight(){return h;}
        public boolean isStateful(){return true;}protected boolean onStateChange(int[] state){invalidateSelf();return true;}
        public void draw(Canvas c){boolean on=false;for(int state:getState())if(state==android.R.attr.state_checked)on=true;r.set(getBounds());p.setColor(on?GREEN:0xff727272);c.drawRoundRect(r,h/2,h/2,p);p.setColor(INK);c.drawCircle(on?r.right-h/2:r.left+h/2,r.centerY(),h*.39f,p);}
        public void setAlpha(int value){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private static final class TrackIcon extends Drawable {
        private final boolean filled;private final int height;private final Paint p=new Paint(3);private final RectF r=new RectF();
        TrackIcon(boolean value,int h){filled=value;height=h;}
        protected boolean onLevelChange(int level){invalidateSelf();return true;}
        public void draw(Canvas c){Rect b=getBounds();float width=filled?b.width()*getLevel()/10000f:b.width();r.set(b.left,b.centerY()-height/2f,b.left+width,b.centerY()+height/2f);p.setColor(filled?GREEN:0xff484848);c.drawRoundRect(r,height/2f,height/2f,p);}
        public void setAlpha(int value){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private final class Preview extends View {
        private final Paint p=new Paint(3);private final RectF r=new RectF();
        Preview(Context context){super(context);setContentDescription("Предпросмотр шкалы громкости");}
        protected void onDraw(Canvas c){
            super.onDraw(c);int stream=currentStream(),max=audio.getStreamMaxVolume(stream),value=audio.getStreamVolume(stream);
            float x=getWidth()*.8f,top=dp(24),bottom=getHeight()-dp(24),width=dp(30),f=max>0?(float)value/max:0;
            r.set(x-width/2,top,x+width/2,bottom);p.setColor(SELECTED);c.drawRoundRect(r,width/2,width/2,p);
            p.setStrokeWidth(dp(7));p.setStrokeCap(Paint.Cap.ROUND);p.setColor(0xff555555);c.drawLine(x,top+dp(20),x,bottom-dp(20),p);
            p.setColor(GREEN);float y=bottom-dp(20)-(bottom-top-dp(40))*f;c.drawLine(x,y,x,bottom-dp(20),p);
            p.setColor(INK);c.drawCircle(x,y,dp(8),p);p.setTextSize(dp(52));p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText(VolumeMath.percent(value,max)+" %",0,getHeight()*.58f,p);
        }
    }
    private static final class TabIcon extends Drawable {
        private final Paint p=new Paint(3);private final int kind,size;
        TabIcon(int k,int s){kind=k;size=s;}public int getIntrinsicWidth(){return size;}public int getIntrinsicHeight(){return size;}
        public void draw(Canvas c){Rect b=getBounds();c.save();c.translate(b.left,b.top);c.scale(b.width()/24f,b.height()/24f);p.setColor(MUTED);p.setStrokeWidth(1.8f);p.setStyle(Paint.Style.STROKE);
            if(kind==0||kind==2||kind==3){for(int i=0;i<3;i++){float x=5+i*7;c.drawLine(x,3,x,21,p);c.drawCircle(x,kind==3?8+i*4:7+(i%2)*9,2.4f,p);}}
            else if(kind==1){c.drawRoundRect(new RectF(3,3,21,21),3,3,p);c.drawLine(15,3,15,21,p);}
            else if(kind==4){c.drawArc(new RectF(3,3,21,21),-55,290,false,p);c.drawLine(12,1,12,12,p);}
            else{c.drawCircle(12,12,9,p);c.drawLine(12,11,12,17,p);c.drawPoint(12,7,p);}c.restore();}
        public void setAlpha(int value){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
