# Сохранённые изменения существующей сборки

Это копии изменённых скриптов рабочего каталога `rebuild/`, а не новая система
сборки. Существующий `rebuild/` исключён из Git; копии сохраняют изменения фичи
для ревью и переноса. Запускать `build.ps1` непосредственно отсюда нельзя.

Соответствие:

- `build.ps1`, `patch_apk.py`, `dual_scale_patch.py` → одноимённые файлы `rebuild/`.
- `../tests/check_dual_scale_apk.py`, `check_marker_apk.py`, `validate_apk.py`
  → одноимённые файлы `rebuild/tests/`.

Сборка остаётся `rebuild/build.ps1` и использует существующие Android SDK,
JDK, Apktool, оригинальный APK и локальный тестовый ключ.
Новые Java-классы копируются из `source/app/src/main/java/` перед
компиляцией. Остальные изменённые Java-файлы MainUi и IndicatorLayout имеют
синхронные копии в `source/` и `rebuild/java/`.

Модели проверяются командой `source/tests/run.ps1`. Проверки конечного APK:

```powershell
python rebuild/tests/check_dual_scale_apk.py output/VolumeControl-2.7.1-redesign17-dual-autohide-api18.apk
python rebuild/tests/validate_apk.py output/VolumeControl-2.7.1-redesign17-dual-autohide-api18.apk
```

Остальные прежние регрессии запускает основной скрипт сборки.

Проверка root/child-прозрачности и таймера 2000+300 мс: `source/tests/check_overlay_visibility.py` (также включена в основной скрипт).
