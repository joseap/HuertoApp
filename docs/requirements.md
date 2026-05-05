# Requisitos funcionales — App Android Huerto/Depósito

## 1. Objetivo
Crear una aplicación Android (Kotlin + Jetpack Compose + Firebase) que permita monitorizar los sensores del sistema, operar el riego de forma manual/automática, ajustar parámetros del huerto, revisar actividad reciente y visualizar la evolución climática de las últimas 24 horas.

## 2. Fuentes de datos (Realtime Database `v1`)
| Bloque | Rutas clave | Uso en la app |
| --- | --- | --- |
| `telemetria/deposito` | `nivelBajo`, `llenando`, `valvulas/*`, `bombas/*` | Indicadores en dashboard, alertas.
| `telemetria/huerto` | `superior/*`, `inferior/*`, `humedadSuelo/sensorX` | Tarjetas de sensores y estado en vivo.
| `historico/clima/muestras` | `ts`, `superior/*`, `inferior/*` | Gráfica simple de las últimas 24 horas.
| `estado` | `modoGlobal`, `regar/lineaX` | Mostrar estado operativo y origen. Conceptualmente, `modoGlobal` equivale a modo de riego.
| `comandos` | `valvulas/vX` | Enviar acciones manuales de válvulas desde la app.
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
5. **Histórico climático (últimas 24h)**
    - Mostrar la evolución de temperatura y humedad `superior` e `inferior` durante las últimas 24 horas.
    - Leer `historico/clima/muestras` con consultas acotadas por `ts` y `limit`, sin descargar el árbol completo.
    - No incluye comparación de periodos ni agregados por día/mes/año.

## 4. Navegación
- Bottom navigation con 4 tabs: `Inicio`, `Actividad`, `Configuración`, `Histórico`.
- La pantalla principal concentra operación rápida y lectura de estado.
- La pestaña `Histórico` muestra una vista simple de las últimas 24 horas.

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
- La app puede mostrar un histórico simple de las últimas 24 horas leyendo muestras crudas con timestamp.
- La comparación avanzada por periodos queda pospuesta para no crear dependencia operativa de backend o coste adicional.

## 8. Próximos pasos de implementación
1. Inicializar proyecto Android Studio en `AndroidApp/` (Empty Compose Activity).
2. Añadir Firebase (BoM) y configurar Auth + Database.
3. Implementar capa de datos (Repository + UseCases) con `kotlinx.coroutines.flow` para escuchar DB.
4. Construir pantallas Compose según las 5 vistas descritas.
5. Añadir pruebas básicas (ViewModel & repositorios fake) y un README específico con instrucciones de build.

Este documento servirá como referencia para el desarrollo de la app y se actualizará conforme se añadan nuevas funcionalidades.
