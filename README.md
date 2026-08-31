# SymbioDisplay

App de Android TV que muestra `https://symbio2.vercel.app/client` en un WebView
de pantalla completa, forzado a renderizar como si la TV fuera Full HD (1920x1080),
sin importar si el panel es 4K.

No pude generar el `.apk` directamente en este entorno porque no tengo acceso
a internet aquí (no puedo descargar el Android SDK ni las dependencias de Gradle).
El proyecto está 100% listo, solo falta compilarlo. Tenés dos caminos, elegí el que te resulte más cómodo:

---

## Opción A (recomendada, sin instalar nada): GitHub Actions

1. Creá un repositorio nuevo en GitHub (puede ser privado).
2. Subí **todo** el contenido de esta carpeta al repo (por ejemplo arrastrando los
   archivos en la web de GitHub, o con `git init / git add . / git commit / git push`).
3. Andá a la pestaña **Actions** del repo. Va a correr solo (o si no, hacé click en
   "Build SymbioDisplay APK" → "Run workflow").
4. Cuando termine (2-4 minutos), entrá al resultado del workflow y descargá el
   artifact **SymbioDisplay-debug-apk**. Ahí adentro está el `app-debug.apk`.
5. Pasá el `.apk` a la TV (pendrive, `adb install`, Google Drive, Send Files to TV, etc.)
   e instalalo (puede que necesites habilitar "Fuentes desconocidas" en Configuración
   de Android TV).

Este `.apk` es de **debug**, lo cual está perfecto para instalar y probar directamente.
Si más adelante lo querés publicar en la Play Store, hay que generarlo como "release"
y firmarlo con una keystore propia — avisame y te preparo eso también.

---

## Opción B: Android Studio (en tu PC)

1. Instalá [Android Studio](https://developer.android.com/studio) (gratis).
2. Abrí la carpeta `SymbioDisplay` con **File → Open**.
3. Dejá que sincronice (va a descargar Gradle y el SDK automáticamente la primera vez).
4. Conectá tu Android TV por `adb` (o un emulador de TV) y apretá **Run ▶**.
   O bien: **Build → Build Bundle(s)/APK(s) → Build APK(s)** para generar el `.apk`
   sin instalarlo directamente.
5. El `.apk` queda en `app/build/outputs/apk/debug/app-debug.apk`.

---

## Qué configuré

- **URL cargada**: `https://symbio2.vercel.app/client`
- **Ícono y banner**: generados a partir de la imagen que me pasaste (banner 320x180
  requerido por el launcher de Android TV, más íconos adaptativos en todas las
  densidades).
- **Forzado a 1080p**: el WebView se dibuja en un lienzo fijo de 1920x1080 px y
  después se estira con la GPU (`scaleX`/`scaleY`) para cubrir toda la pantalla real,
  sea cual sea la resolución del panel (4K, 1080p, etc). Esto asegura que el
  contenido se vea proporcionalmente igual en cualquier TV, sin achicarse en 4K.
  Ver comentarios en `MainActivity.kt` para el detalle técnico.
- **Modo kiosco básico**: pantalla completa sin barras de navegación, pantalla
  siempre encendida (`FLAG_KEEP_SCREEN_ON`) — pensado para cartelería/señalética.
- **Auto-reload**: si falla la carga de la página (sin internet, error del server),
  reintenta cargar cada 5 segundos automáticamente.
- **Package name**: `com.symbio.display`
- **Orientación**: horizontal fija (landscape), típico de Android TV.

## Si querés cambiar algo

- **URL**: editá `TARGET_URL` en `app/src/main/java/com/symbio/display/MainActivity.kt`.
- **Nombre visible**: `app/src/main/res/values/strings.xml` (`app_name`).
- **Ícono/banner**: reemplazá los archivos en `app/src/main/res/mipmap-*/` y
  `app/src/main/res/drawable/banner.png`.
