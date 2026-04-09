# Requisitos funcionales — App Android Huerto/Depósito

## 1. Objetivo
Crear una aplicación Android (Kotlin + Jetpack Compose + Firebase) que permita monitorizar los sensores del sistema, operar el riego de forma manual/automática, ajustar parámetros del huerto y consultar históricos.

## 2. Fuentes de datos (Realtime Database `v1`)
| Bloque | Rutas clave | Uso en la app |
| --- | --- | --- |
| `telemetria/deposito` | `nivelBajo`, `llenando`, `valvulas/*`, `bombas/*` | Indicadores en dashboard, alertas.
| `telemetria/huerto` | `superior/*`, `inferior/*`, `humedadSuelo/sensorX` | Tarjetas de sensores y gráficas.
| `estado` | `modoGlobal`, `regar/lineaX` | Mostrar estado operativo y origen.
| `comandos` | `valvulas/vX`, `modoGlobal` | Enviar acciones manuales desde la app (señal "deseada").
| `config` | `huerto/*`, `deposito/*` | Formularios de configuración para admins.
| `logs/eventos` | últimos N registros | Historial de eventos.

## 3. Pantallas previstas
1. **Dashboard Sensores**
   - Cards con temperatura/humedad superior & inferior.
   - Estado del depósito (nivel, llenando, bomba impulso).
   - Humedad de suelo (tres barras o chips con % + raw opcional).
   - Banda superior con modo global y necesidad de riego por línea.
2. **Control Manual**
   - Selector `AUTO`/`MANUAL` (escribe en `comandos/modoGlobal`).
   - Botones para V1-V3 con confirmación (actualiza `comandos/valvulas/vX`).
   - Mostrar última acción (`origen`, timestamp) y estado real (desde `telemetria`).
3. **Configuración** (requiere rol admin)
   - Formulario para `umbrales.min/max`, `intervaloLecturaMs`, calibración de cada sensor.
   - Ajustes de depósito (`maximoRiegoMs`, `delayReencendido`).
   - Validación y guardado atómico en `config/*`.
4. **Historial de eventos**
   - Lista cronológica de `logs/eventos` con filtros (tipo, actor).
   - Cada item muestra icono, descripción y timestamp.
5. **Consulta histórica de temperaturas**
   - Gráfica (line chart) de `superior` vs `inferior` en ventana seleccionable (6h, 24h, 7d).
   - Usa datos agregados: se pueden leer snapshots preprocesados (si existen) o, inicialmente, se consultará `logs`/`telemetria` guardando muestras cada X minutos en `logs/eventos` tipo `sample`.

## 4. Navegación
- Bottom navigation con 4 tabs: `Dashboard`, `Control`, `Configuración`, `Historial`.
- La consulta histórica de temperatura se accede desde `Dashboard` (botón "Ver histórico") y abre pantalla aparte (modal o nueva ruta).

## 5. Estados y permisos
- Autenticación con Firebase Auth (correo/clave o Google). Mapear `uid` → `usuarios/uid` para obtener `roles`.
- Roles previstos:
  - `viewer`: sólo lectura de `telemetria`, `estado`, `logs`.
  - `operator`: puede enviar `comandos` (modo y válvulas).
  - `admin`: además puede editar `config`.
- La app debe degradar la UI según permisos (deshabilitar botones, ocultar formulario).

## 6. Consideraciones UI/UX
- Jetpack Compose + Material 3.
- Tema claro con color primario inspirado en tonos verdes (#4CAF50) y acentos naranja para alertas.
- Indicadores de estado usando chips:
  - `Nivel bajo` → chip rojo si `telemetria/deposito/nivelBajo = true`.
  - `Modo` → chip azul (`AUTO`) o amarillo (`MANUAL`).
- Humedad de suelo en tarjetas con gradiente: 0–30% rojo, 30–60% amarillo, >60% verde.

## 7. Datos históricos
- Para la pantalla "Consulta histórica" se recomienda crear un nodo adicional `logs/temperaturas/YYYYMMDD` o aprovechar Firestore. Mientras tanto, se puede almacenar snapshots cada 5 minutos en `logs/eventos` con `tipo = "muestra_temp"` y leerlos filtrando por rango.
- UI: selector de rango + gráfico (Compose Chart libray o MPAndroidChart). Muestra mínimo/máximo y media.

## 8. Próximos pasos de implementación
1. Inicializar proyecto Android Studio en `AndroidApp/` (Empty Compose Activity).
2. Añadir Firebase (BoM) y configurar Auth + Database.
3. Implementar capa de datos (Repository + UseCases) con `kotlinx.coroutines.flow` para escuchar DB.
4. Construir pantallas Compose según las 5 vistas descritas.
5. Añadir pruebas básicas (ViewModel & repositorios fake) y un README específico con instrucciones de build.

Este documento servirá como referencia para el desarrollo de la app y se actualizará conforme se añadan nuevas funcionalidades.
