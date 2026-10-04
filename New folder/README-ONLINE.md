# Foco — compilación online

Este proyecto está preparado para compilarse automáticamente con GitHub Actions.

## Cómo obtener el APK sin Android Studio

1. Creá una cuenta gratuita en GitHub si no tenés una.
2. Creá un repositorio nuevo, por ejemplo `FocoApp`.
3. Subí **todo el contenido de esta carpeta** al repositorio (incluida la carpeta `.github`).
4. Entrá en la pestaña **Actions** del repositorio.
5. Elegí **Build Foco APK** y tocá **Run workflow**.
6. Cuando termine, abrí la ejecución y en **Artifacts** descargá `Foco-debug-apk`.
7. Descomprimí ese archivo y vas a encontrar `app-debug.apk`.
8. Pasalo al Samsung e instalalo.

El workflow instala JDK 17, Android SDK 35 y Gradle 8.10 automáticamente, y compila la app.
