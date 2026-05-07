# Requisitos funcionales — App Android Huerto/Depósito

## 1. Objetivo
Crear una aplicación Android (Kotlin + Jetpack Compose + Firebase) que permita monitorizar los sensores del sistema, operar el riego de forma manual/automática, ajustar parámetros del huerto, revisar actividad reciente y visualizar tanto una ventana móvil de últimas 24 horas como una retrospectiva climática por fecha seleccionada.

## 2. Fuentes de datos (Realtime Database `v1`)
| Bloque | Rutas clave | Uso en la app |
| --- | --- | --- |
| `telemetria/deposito` | `nivelBajo`, `llenando`, `valvulas/*`, `bombas/*` | Indicadores en dashboard, alertas.
| `telemetria/huerto` | `superior/*`, `inferior/*`, `humedadSuelo/sensorX` | Tarjetas de sensores y estado en vivo.
| `historico/clima/muestras` | `ts`, `superior/*`, `inferior/*` | Gráfica simple de las últimas 24 horas.
| `historico/clima/agregados/hora/{yyyy}/{MM}/{dd}` | `bucketStartTs`, métricas `avg/min/max` | Retrospectiva climática de las 24 horas naturales del día elegido y comparativa opcional.
| `historico/suelo/muestras` | `ts`, `sensor1`, `sensor2`, `sensor3` | Evolución de humedad de suelo de las últimas 24 horas.
| `estado` | `modoGlobal`, `regar/lineaX` | Mostrar estado operativo y origen. Conceptualmente, `modoGlobal` equivale a modo de riego.
| `comandos` | `modoGlobal`, `valvulas/vX` | Enviar acciones manuales de válvulas y transición a modo manual desde la app.
| `config` | `huerto/*`, `deposito/*` | Formularios de configuración.
| `logs/eventos` | eventos recientes | Pantalla secundaria de actividad/log técnico.

## 3. Pantallas previstas
1. **Pantalla principal**
    - Cards visibles y atractivas con temperatura/humedad `superior` e `inferior`.
    - Humedad de suelo por línea (`L1`, `L2`, `L3`) mostrando al menos el porcentaje actual y, si cabe, el valor raw.
    - Estado de bomba de impulsión con indicador `ON/OFF`.
    - Estado del modo de riego (`AUTO` o `MANUAL`).
    - El objetivo de esta pantalla es operación rápida y lectura inmediata, no detalle histórico.
2. **Control manual**
   - Estado de válvulas `V1`, `V2`, `V3` con indicador claro `ON/OFF`.
   - Botones para activar manualmente `V1`, `V2`, `V3`; al pulsar cualquier válvula la app debe tratar el sistema como modo manual.
   - Feedback claro de que en modo manual se puede superar el umbral máximo de la lógica automática.
3. **Actividad / Log**
    - Lista cronológica de `logs/eventos` en formato casi raw.
    - Cada fila muestra hora, tipo, origen, detalle, línea y estado si aplica.
    - Vista secundaria de diagnóstico, no parte del dashboard principal.
4. **Configuración**
    - Formulario para `% mínimo` y `% máximo` de la lógica automática de riego.
    - Mostrar en tiempo real los valores raw y porcentaje actual de cada sensor de humedad de suelo.
    - Permitir calibración raw por sensor (`min = 0%`, `max = 100%`).
    - Ajustes de depósito (`maximoRiegoMs`, `delayReencendido`).
    - Validación y guardado atómico en `config/*`.
5. **Histórico**
    - Mostrar una vista de `Últimas 24h` con ventana móvil real desde el momento actual para:
      - temperatura/humedad `superior` e `inferior`
      - humedad de suelo de `sensor1..3`
    - Mostrar una vista de `Clima por día seleccionado` con selector `año/mes/día`.
    - Leer `historico/clima/agregados/hora/{yyyy}/{MM}/{dd}` para presentar las 24 horas naturales del día elegido.
    - Permitir activar una segunda fecha climática como comparativa, mostrándola en una segunda gráfica debajo de la principal.
    - Mantener la retrospectiva por fecha solo para clima; la humedad de suelo no participa en la comparativa por fecha.

## 4. Navegación
- Bottom navigation con 5 tabs: `Inicio`, `Control`, `Histórico`, `Configuración`, `Actividad`.
- La pantalla principal concentra operación rápida y lectura de estado.
- La pestaña `Histórico` combina una vista reciente de últimas 24h y una retrospectiva climática por fecha seleccionada.

## 5. Estados y permisos
- En el alcance actual no se contempla sistema de roles ni `usuarios/*`.
- La app se asume de uso controlado para uno o dos usuarios de confianza.
- La vista principal prioriza simplicidad operativa; el detalle temporal se consulta en la pantalla de actividad.
- Si en el futuro se necesita autenticación real, habrá que introducirla como fase separada.

## 6. Consideraciones UI/UX
- Jetpack Compose + Material 3.
- La interfaz no debe ser espartana ni puramente utilitaria; debe tener color, jerarquía visual y sensación de producto cuidado.
- Usar una dirección visual fresca y tecnológica inspirada en huerto/agua/clima: verdes, turquesas, azules y acentos cálidos para alertas.
- Pantalla principal con cards expresivas, iconografía clara y estados `ON/OFF` muy legibles.
- Evitar una UI gris o plana; usar fondos con profundidad sutil, agrupación visual y contraste suficiente.

## 7. Datos históricos
- `logs/eventos` ya puede mostrar eventos con `serverTimestamp` de Firebase para actividad reciente.
- La app ya muestra una ventana móvil de últimas 24 horas leyendo muestras crudas con timestamp para clima y suelo.
- La app ya consume buckets horarios agregados para cargar retrospectiva climática por día seleccionado y comparativa opcional entre dos fechas.

## 8. Trabajo pendiente
1. Añadir pruebas básicas de `HomeViewModel` y repositorios fake, especialmente para validaciones, control manual y parsing de agregados horarios.
2. Endurecer seguridad RTDB si el sistema va a salir de un entorno controlado.
3. Evaluar alertas/notificaciones sobre `telemetria/deposito/nivelBajo` y estados obsoletos.

Este documento servirá como referencia para el desarrollo de la app y se actualizará conforme se añadan nuevas funcionalidades.
