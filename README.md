# Aplicación Android para el sistema Huerto/Depósito

Proyecto Android (Kotlin + Jetpack Compose + Hilt + Firebase RTDB) que interactúa con los ESP32 de Depósito e Invernadero. La app está operativa y consume datos reales en tiempo real.

## Estado actual

La app ya implementa funcionalmente:

- **Dashboard en tiempo real**: clima superior/inferior, estado de válvulas, bombas, nivel de depósito y modo de riego.
- **Control manual**: activación de válvulas con estados explícitos (enviado, confirmado, timeout, error, bloqueo por nivel bajo).
- **Configuración**: edición de umbrales, calibraciones e intervalos con validación de negocio antes de guardar.
- **Historial de eventos**: consultas limitadas por volumen para evitar descargar el árbol completo.
- **Histórico**: últimas 24h móviles para clima y suelo, más retrospectiva climática por día seleccionado con comparativa opcional entre dos fechas.
- **Indicador de frescura**: muestra si los datos son recientes, obsoletos o último estado conocido.
- **Navegación Compose**: 5 secciones funcionales con BottomNavigation.
- **Arquitectura MVVM**: Hilt para inyección, Flows para estado reactivo, repositorio Firebase como capa de datos.

## Requisitos

- Android Studio reciente con soporte para AGP 8.13.x y JDK 17 para compilar, o JDK 21 si solo quieres usar el entorno descargado por `scripts/setup-jdk.sh`.
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
│   │   ├── data/
│   │   │   ├── HuertoRepository.kt   (capa de datos Firebase con consultas acotadas)
│   │   │   └── HuertoModels.kt       (modelos UI y de dominio)
│   │   └── ui/
│   │       ├── home/HomeViewModel.kt (estado, validación, control manual)
│   │       └── screens/Screens.kt    (5 pantallas Compose funcionales)
│   └── src/main/res                  (strings, temas, manifest, etc.)
├── build.gradle.kts                  ← plugins raíz
├── settings.gradle.kts               ← configuración repositorios + módulos
├── gradlew / gradlew.bat + wrapper   ← Gradle 8.13
└── docs/requirements.md              ← requerimientos funcionales de la app
```

## Dependencias principales

- Jetpack Compose BOM `2024.04.00` (Material 3, Navigation).
- Firebase BoM `33.3.0` (Auth + Database KTX).
- Hilt plugin `2.51.1`, runtime `2.51` + `androidx.hilt:hilt-navigation-compose:1.2.0`.

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

## Validación de negocio

La configuración se valida antes de guardar con reglas de negocio explícitas:

- Umbrales de riego entre 0% y 100%, con `min < max` para histéresis.
- Intervalo de lectura entre 1s y 10min.
- Máximo de riego entre 1min y 4h.
- Retardo de reencendido entre 5s y 1h.
- Calibraciones de sensor con `min > max` y margen mínimo de 500 unidades raw.

## Control manual

Al activar una válvula desde la app:

1. Se publica una intención atómica en `comandos/modoGlobal/*` y `comandos/valvulas/vX/*`.
2. La app muestra "orden enviada" mientras espera confirmación.
3. Si la telemetría confirma el cambio, se muestra "confirmada".
4. Si pasan 8s sin confirmación, se muestra error con contexto (nivel bajo, conectividad).

## Histórico y frescura

- Las consultas de histórico y eventos usan `orderByChild` + `limitToLast`/`startAt` para no descargar ramas completas.
- La pestaña `Histórico` tiene dos bloques:
  - `Últimas 24h`: ventana móvil real para clima y humedad de suelo por línea.
  - `Clima por día seleccionado`: carga los 24 buckets horarios de `historico/clima/agregados/hora/{yyyy}/{MM}/{dd}` y permite comparar una segunda fecha en una segunda gráfica.
- La retrospectiva por fecha solo aplica a variables climáticas; la humedad de suelo sigue limitada a la vista de últimas 24h.
- Un indicador de frescura clasifica los datos en: recientes (<15s), obsoletos (15-60s) o último estado conocido (>60s).
