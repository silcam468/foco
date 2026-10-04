# Foco

App Android para concentrarte: bloquea las apps que elijas, manualmente, por tiempo o con horarios automáticos.

## Cómo obtener el APK (sin Android Studio)
1. Creá un repositorio nuevo en GitHub (o vaciá el anterior).
2. Subí **todo el contenido de esta carpeta** (incluida `.github` y `app/foco.jks`).
3. Pestaña **Actions** → **Build Foco APK** → **Run workflow**.
4. Cuando termine (unos minutos), en **Artifacts** descargá `Foco-debug-apk`, descomprimilo e instalá `app-debug.apk`.

Si ya tenías instalada una versión anterior de Foco, desinstalala primero (esta usa otra clave de firma).
Desde esta versión, las próximas compilaciones se instalan encima sin perder tus ajustes.

## Primer uso
1. Abrí Foco y tocá **Abrir ajustes de Accesibilidad** → activá Foco.
   Si el interruptor está en gris: Ajustes → Aplicaciones → Foco → ⋮ → *Permitir ajustes restringidos*.
2. Marcá las apps a bloquear.
3. Empezá una sesión o creá un horario automático.
