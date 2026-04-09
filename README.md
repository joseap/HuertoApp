# Aplicación Android para el sistema Huerto/Depósito

Proyecto Android (Kotlin + Jetpack Compose + Firebase) que interactúa con los ESP32 de Depósito e Invernadero. Incluye navegación inferior, pantallas placeholder para los 5 flujos definidos y dependencias listas para Firebase RTDB, Auth y Hilt.

## Requisitos

- Android Studio Giraffe/Flamingo o superior con JDK 17 (o ejecuta el script descrito abajo).
- SDK Platform 34 instalada.
- Acceso a una instancia de Firebase Realtime Database siguiendo la estructura `v1` descrita en `../FirebaseEstructura.md`.
- Archivo `app/google-services.json` generado desde Firebase (no se versiona). Usa el ejemplo `app/google-services.json.example` como plantilla.

### Configuración rápida del JDK

Para mantener el repositorio ligero no se versiona el JDK. Si trabajas fuera de Android Studio o necesitas compilar desde CLI, ejecuta:

```bash
cd AndroidApp
./scripts/setup-jdk.sh
export JAVA_HOME="$(pwd)/.jdks/jdk-21.0.2+13"
export PATH="$JAVA_HOME/bin:$PATH"
```

El script descarga Temurin 21 y lo deja en `.jdks/`. Añade las dos últimas líneas a tu shell (`.bashrc`, `.zshrc`, etc.) para no repetirlas.

## Estructura

```
AndroidApp/
├── app/                     ← módulo principal Compose + Hilt
│   ├── src/main/java/com/alejandro/huerto
│   │   ├── MainActivity.kt
│   │   ├── HuertoApp.kt              (NavHost y BottomNav)
│   │   ├── HuertoApplication.kt      (@HiltAndroidApp)
│   │   └── ui/…                      (theme + pantallas placeholder)
│   └── src/main/res                  (strings, temas, manifest, etc.)
├── build.gradle.kts                  ← plugins raíz
├── settings.gradle.kts               ← configuración repositorios + módulos
├── gradlew / gradlew.bat + wrapper   ← Gradle 8.7
└── docs/requirements.md              ← requerimientos funcionales de la app
```

## Dependencias principales

- Jetpack Compose BOM `2024.04.00` (Material 3, Navigation).
- Firebase BoM `33.3.0` (Auth + Database KTX).
- Hilt `2.51` + `androidx.hilt:hilt-navigation-compose`.

## Cómo compilar

```bash
cd AndroidApp
./scripts/setup-jdk.sh                 # sólo la primera vez
export JAVA_HOME="$(pwd)/.jdks/jdk-21.0.2+13"
export PATH="$JAVA_HOME/bin:$PATH"
cp app/google-services.json.example app/google-services.json   # rellena los valores reales
./gradlew tasks                        # verifica wrapper y dependencias
./gradlew assembleDebug                # genera APK debug
```

> En Android Studio basta con **File → Open…** y seleccionar la carpeta `AndroidApp/`.

## Próximos pasos recomendados

1. Conectar el proyecto a tu Firebase (añadir `google-services.json`).
2. Implementar capa de datos (repositorios con Realtime Database y Auth) usando los modelos descritos en `docs/requirements.md`.
3. Sustituir los placeholders de cada pantalla por composables reales:
   - Dashboard sensores + acceso a histórico.
   - Control manual de válvulas y modo global.
   - Formularios de configuración condicionados por roles.
   - Historial de eventos y gráfica histórica de temperaturas.
4. Añadir `ViewModel`s con Hilt y Flows para reflejar el estado en tiempo real.

Con esta base, puedes iterar sobre UI/UX y lógica de negocio sin tocar los proyectos de firmware.
