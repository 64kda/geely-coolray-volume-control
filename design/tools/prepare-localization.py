"""Build resource drafts from the extracted APK, preserving names and enum order."""
from pathlib import Path
import csv
import json
import re
import xml.etree.ElementTree as ET

BASE = Path(__file__).resolve().parents[2]
OUT = BASE / 'design' / 'localization'
OUT.mkdir(parents=True, exist_ok=True)
default = {s.attrib['name']: s.text or '' for s in ET.parse(BASE/'analysis/strings-default.xml').getroot()}
existing = {s.attrib['name']: s.text or '' for s in ET.parse(BASE/'analysis/strings-ru.xml').getroot()}
translations = {
 'app_name':'Регулятор громкости',
 'BtnShowIndicator':'Показать шкалу', 'BtnHideIndicator':'Скрыть шкалу',
 'LabelGravity':'Положение шкалы', 'LabelSize':'Толщина шкалы',
 'LabelColor':'Цвет заполнения', 'LabelBackgroundColor':'Цвет фона шкалы',
 'LabelTransparensy':'Непрозрачность',
 'LabelRunAfterReboot':'Запускать после перезагрузки', 'PickColor':'Выбор цвета',
 'LabelAutoHide':'Скрывать шкалу после использования',
 'TextViewRed':'Красный', 'TextViewGreen':'Зелёный', 'TextViewBlue':'Синий',
 'LabelSelectColorCircle':'Цветовой круг', 'LabelApply':'Применить', 'LabelCancel':'Отмена',
 'LabelBarType':'Вид шкалы', 'StreamType':'Аудиоканал',
 'ShowNotification':'Показывать системную панель громкости',
 'NotificationSound':'Звук при изменении громкости',
 'LabelXPos':'Смещение по горизонтали', 'LabelYPos':'Смещение по вертикали',
 'LabelOverlaySB':'Размещать поверх строки состояния',
 'LabelDonate':'Поддержать разработчика',
 'System':'Системные звуки', 'Music':'Музыка и мультимедиа', 'Ring':'Звонок',
 'Alarm':'Будильник', 'Notification':'Уведомления', 'VoiceCall':'Разговор',
 'DTMF':'Тоновый набор', 'Equalizer':'Эквалайзер', 'Mixer':'Микшер',
 'LabelActiveColor':'Цвет активной вкладки', 'LabelInactiveColor':'Цвет неактивной вкладки',
 'Delay':'Задержка перед регулировкой: 1 секунда', 'Bass':'Усиление басов',
 'Virtualizer':'Расширение стереобазы', 'Enable':'Включить эквалайзер',
 'AdvDonation':'Реклама спонсоров',
 'PaypalDonation':'Пожертвование через PayPal / Google Checkout',
 'BuyPro':'Купить версию Pro',
 'LabelEnableEqMix':'Разрешить окно эквалайзера и микшера',
 'ActionOn0Volume':'Режим звонка при нулевой громкости', 'Length':'Длина шкалы',
 'store_picture_title':'Сохранение изображения',
 'store_picture_message':'Разрешить рекламе сохранить изображение в галерее?',
 'accept':'Разрешить', 'decline':'Отмена',
 'create_calendar_title':'Создание события',
 'create_calendar_message':'Разрешить рекламе создать событие в календаре?',
 'cast_notification_connecting_message':'Подключение к %1$s',
 'cast_notification_connected_message':'Подключено к %1$s',
 'cast_notification_disconnect':'Отключиться',
 'wallet_buy_button_place_holder':'Купить с помощью Google',
}
# Brands and the donation URL are exact values, not prose to translate.
preserve={'auth_google_play_services_client_google_display_name',
          'auth_google_play_services_client_facebook_display_name','DonateHTTP'}
result={k: translations.get(k, existing.get(k, v)) for k,v in default.items()}
assert set(translations) <= set(default), 'Unexpected resource name'
for k,v in default.items():
    assert re.findall(r'%\d+\$[a-z]',v)==re.findall(r'%\d+\$[a-z]',result[k]), k
    if k not in preserve and result[k]==v:
        raise AssertionError('Untranslated resource: '+k)

def android_text(value):
    return value.replace('\\','\\\\').replace("'", "\\'").replace('"','\\"')

res=ET.Element('resources')
for key,value in result.items():
    ET.SubElement(res,'string',name=key).text=android_text(value)
ET.indent(res)
dir_ru=OUT/'res/values-ru';dir_ru.mkdir(parents=True,exist_ok=True)
ET.ElementTree(res).write(dir_ru/'strings.xml',encoding='utf-8',xml_declaration=True)

old_arrays=json.loads((BASE/'analysis/arrays.json').read_text(encoding='utf-8'))
arrays={
 'Gravity': ['Вдоль левого края','Вдоль верхнего края','Вдоль правого края','Вдоль нижнего края',
             'У левого края сверху','У левого края по центру','У левого края снизу',
             'У верхнего края слева','У верхнего края по центру','У верхнего края справа',
             'У правого края сверху','У правого края по центру','У правого края снизу',
             'У нижнего края слева','У нижнего края по центру','У нижнего края справа'],
 'TypeBar':['Сплошная полоса','Столбики уровня'],
 'StreamType':['Автовыбор','Все каналы','Музыка и мультимедиа','Системные звуки','Звонок',
               'Уведомления','Разговор','Будильник','Тоновый набор'],
 'ActionOn0Volume':['Не менять режим','Вибрация','Без звука'],
}
res_arrays=ET.Element('resources')
array_rows=[]
for idx,(name,items) in enumerate(arrays.items()):
    original=old_arrays[hex(0x7f0a0000+idx)][0][1]
    assert len(original)==len(items),name
    element=ET.SubElement(res_arrays,'string-array',name=name)
    for pos,(en,ru) in enumerate(zip(original,items)):
        ET.SubElement(element,'item').text=ru
        array_rows.append([name,pos,en,ru])
ET.indent(res_arrays)
ET.ElementTree(res_arrays).write(dir_ru/'arrays.xml',encoding='utf-8',xml_declaration=True)
# Russian defaults prevent missing-default resources and provide a fallback.
# Existing other-language resources must be reviewed during APK integration.
dir_default=OUT/'res/values';dir_default.mkdir(parents=True,exist_ok=True)
for filename in ['strings.xml','arrays.xml']:
    (dir_default/filename).write_bytes((dir_ru/filename).read_bytes())
with (OUT/'strings-review.csv').open('w',encoding='utf-8-sig',newline='') as f:
    writer=csv.writer(f);writer.writerow(['resource','original','existing_ru','proposed_ru'])
    writer.writerows([k,default[k],existing.get(k,''),v] for k,v in result.items())
with (OUT/'arrays-review.csv').open('w',encoding='utf-8-sig',newline='') as f:
    writer=csv.writer(f);writer.writerow(['array','index','original','proposed_ru']);writer.writerows(array_rows)
summary={'status':'resource_drafts_not_applied_to_apk','strings':len(result),'arrays':len(arrays),
         'array_items':len(array_rows),'missing_keys':[],'removed_keys':[],
         'placeholder_check':'passed','array_order':'preserved',
         'scope':'all extracted string resources, including available library dialogs; hardcoded and external content require runtime inventory'}
(OUT/'validation.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(summary,ensure_ascii=False,indent=2))
