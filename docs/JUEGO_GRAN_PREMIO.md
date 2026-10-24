# BoxBox Grand Prix — juego de estrategia y carrera

## Alcance y arquitectura

Se ampliaron únicamente dos archivos existentes del videojuego:

- `src/controller/MiniGameViewController.java`
- `src/view/MiniGameView.fxml`

El resto son clases, estilos, documentación y verificadores propios del juego.
El estilo está en `src/view/game/game.css`, adjunto al nodo del juego y con
selectores `.grand-prix-game`. La navegación y la hoja de estilo general se conservan.
Se reutilizan JavaFX, Canvas, FXML, `AppAudio` y la canción original
`assets/audio/game/game.mp3`.

### Responsabilidades

| Archivo / paquete | Función |
|---|---|
| `model.game.GameRules` | Configuración validada, dificultad, circuitos, estilos, banderas y puntuación |
| `model.game.TyreCompound` | Compuestos, ritmo, desgaste y agarre según agua |
| `model.game.RaceEngine` | Carrera de paso fijo, tráfico, adelantamientos, pits, recursos e incidentes |
| `model.game.RaceCar` | Estado mutable privado de cada monoplaza |
| `model.game.RaceWeather` | Pronóstico y evolución gradual de agua en pista |
| `model.game.RaceSnapshot` | Instantánea inmutable para la presentación |
| `model.game.GameProgress` | Campeonato y estadísticas de esta sesión; registro idempotente de resultados |
| `model.game.ArcadeSession` | Juego clásico de esquivar obstáculos |
| `view.game.GrandPrixRenderer` | Dibujo vectorial, coches, minimapa, clima y mecánicos |
| `controller.MiniGameViewController` | Controles, teclado, animación, ciclo de vida y presentación de instantáneas |

El modelo del juego funciona sin JavaFX, JDBC ni HTTP. Los pilotos y circuitos
del GP son **ficticios**; los tiempos son segundos del juego y no resultados
históricos. La selección de escudería conserva los colores del minijuego anterior.

## Cómo jugar

1. Ejecutar `BoxBox.launch` en Eclipse como habitualmente y entrar a **Arcade**.
2. Seleccionar **Gran Premio**, dificultad, 6/10/16 vueltas, circuito, clima y goma
   inicial. Para aprender: **Aprendiz**, 6 vueltas, **Seco estable** y medios.
3. Pulsar **Iniciar GP**. La vista enfoca el marcador y la pista. Aceleración y
   frenada son automáticas; la trayectoria y las decisiones son del jugador.
4. Revisar vida/temperatura de gomas, combustible, ERS, pronóstico y rivales.
   En seco, considerar un cambio a otro compuesto hacia la mitad de la carrera.
5. Elegir el compuesto de la próxima parada y pulsar **BOX** o **P**. Entra al
   alcanzar la zona de boxes (88% de vuelta); si ya la pasó, espera a la siguiente.
   El compuesto confirmado queda bloqueado. Para cambiarlo, cancelar y volver a pedir.
6. Cruzar la meta y consultar clasificación, tiempos, sanciones, puntos y estadísticas.
   Los resultados se enfocan automáticamente. Allí están **Siguiente Gran Premio**
   y **Volver a configurar carrera**, según corresponda.

### Controles

| Control | Efecto real |
|---|---|
| Flechas izquierda/derecha | Cambiar trayectoria, intentar pasar o evitar contacto |
| Asistencia de trayectoria | Recuperar una trazada y buscar espacio tras soltar las flechas |
| P / BOX | Pedir o cancelar parada antes de entrar |
| Conservar / Equilibrado / Atacar | Cambiar ritmo, consumo, desgaste, temperatura y riesgo |
| E / selector ERS | Alternar equilibrado, desplegar y recuperar energía |
| D / Solicitar DRS | Autorizar DRS; solo se abre si cumple todas las condiciones |
| Espacio / Pausa | Detener/reanudar el reloj y el movimiento |
| Ritmo 1/2/4 | Acelerar el reloj abreviado del GP; 1× = 3 segundos de juego por segundo real |
| Siguiente GP | Avanzar a la siguiente prueba del campeonato desde los resultados |

Mantener P, D o Espacio no ejecuta varias veces la acción por repetición del teclado.
Salir del módulo o perder el foco pausa la carrera; al volver hay que reanudar.
La canción se reproduce en bucle y se libera al salir del juego. Las entradas de
teclado se reciben dentro del propio juego, sin instalar filtros globales de escena.

## Mecánicas conectadas

- **Parrilla:** ocho coches, incluido el jugador. Los siete rivales tienen ritmo
  y perfiles agresivo, conservador, estratega o equilibrado.
- **Tráfico:** coches en la misma trayectoria bloquean el paso. La IA mantiene
  sus maniobras durante varios segundos; los agresivos pueden defender la trazada.
  Un adelantamiento contabilizado exige 8 m de ventaja, con una ventana de 12 s
  entre maniobras del mismo par, evitando inflar estadísticas por oscilaciones.
- **Neumáticos:** blandos rápidos y de vida corta, medios equilibrados, duros
  resistentes; intermedios y lluvia para agua moderada/intensa. El desgaste depende
  de distancia, estilo, abrasión del circuito, compuesto y agua.
- **Pits:** entrada 7 s, servicio 2.4–3.05 s y salida 7 s. El coche avanza por
  pit lane a 22 m/s y permanece detenido durante el servicio; la pérdida frente
  a los rivales surge del modelo. Un 12% de los servicios con incidentes habilitados
  añade 2–5 s. Hay animación de mecánicos y cambios de posición reales del juego.
  Se permiten varias paradas y los rivales también eligen sus compuestos/paradas.
- **Clima:** seco, variable, lluvia creciente o secado; sol, nubes, lluvia y lluvia
  intensa. La previsión indica vueltas aproximadas del líder hasta el siguiente
  cambio. La cantidad de agua evoluciona gradualmente y cambia el agarre.
- **DRS:** exige dos vueltas completadas, verde, agua menor al 18%, autorización,
  zona habilitada y rival aproximadamente a menos de un segundo.
- **ERS:** desplegar aumenta ritmo y consume batería; recuperar cede ritmo para
  recargarla. La frenada también recupera energía. La batería está acotada a 0–100%.
- **Combustible:** consumo por distancia y estilo, sin repostaje. Un ataque sostenido
  puede dejar combustible insuficiente; agotarlo antes de meta causa abandono.
- **Incidentes:** errores de conducción y neutralización SC o VSC, con velocidad
  limitada y bloqueo de adelantamientos en pista. Los boxes siguen abiertos.
- **Reglas simplificadas del juego:** tres salidas sostenidas de pista añaden 5 s;
  contacto evitable atacando, 5 s; terminar en seco sin dos compuestos, 15 s.
  La clasificación final usa tiempo de llegada más sanciones. Son sanciones de
  este juego, no una reproducción completa del reglamento deportivo FIA.

## Fórmulas y reproducibilidad

El motor acumula tiempo y avanza en pasos de **0.1 s**. Una semilla fija reproduce
la misma carrera con las mismas decisiones, independientemente de cómo se agrupen
los frames. Cada nuevo GP de la interfaz recibe una semilla distinta.

La velocidad parte de `longitud / tiempo de referencia por vuelta`, multiplicada
por ritmo individual, dificultad de IA, conducción, compuesto, agarre por agua,
temperatura, desgaste y sector. DRS añade un factor 1.06 y desplegar ERS, 1.055
cuando hay batería. Tráfico, errores y neutralización limitan la velocidad.

Para desgaste normalizado `w` en [0,1], el factor de rendimiento es:

```text
D(w) = 1 − 0.11·w − 0.28·(max(0,w−0.6)/0.4)²
consumo = distancia / longitud_de_vuelta · 2.05 · factor_del_estilo
desgaste += distancia / longitud_de_vuelta · desgaste_del_compuesto
            · factor_del_estilo · abrasión · factor_del_agua
```

Las vueltas se cronometran al cruzar la línea, interpolando dentro del paso.
Las diferencias en directo se estiman por distancia; en la clasificación final
se usan los tiempos de llegada corregidos. La temperatura converge hacia un
objetivo según compuesto, estilo y agua, y penaliza estar lejos del óptimo.
Los coeficientes son de equilibrio lúdico, no calibración física de un F1 real.

## Campeonato, puntos y recompensas

**Campeonato · 3 GP** encadena Costa, Sierra y Nocturno, con longitudes, ritmos y
abrasiones diferentes. Acumula puntos y victorias de todos los participantes.
P1 vale **25**, P2 18, P3 15, y siguen 12/10/8/6/4…; no hay extra de vuelta rápida.

Las estadísticas personales incluyen carreras, victorias, podios, puntos, mejor
vuelta, XP y nivel. Los XP premian vueltas limpias, maniobras completadas y paradas
oportunas con un compuesto adecuado; no premian parar con neumáticos casi nuevos
sin necesidad. Las sanciones reducen XP. El resultado de un GP se registra una sola vez.
La progresión permanece durante esta sesión del controlador; no se guarda en SQL
ni se conserva tras cerrar sesión o la aplicación.

**Arcade clásico** mantiene flechas (también A/D), obstáculos, colisiones, color
de escudería y +10 puntos por coche evitado. Sus paneles se adaptan a ese modo.

## Verificación ejecutada

Desde BoxBox:

```powershell
java tools/BuildBoxBox.java
java tools/VerifyGame.java
java tools/VerifyGame.java --ui
java tools/VerifyGame.java --ui --history-regression
```

Resultados de la última versión:

- **93 fuentes Java compiladas** para Java 25 con el JDK 26 y JavaFX local existentes.
- **380 aserciones específicas** y **136704 verificaciones de invariantes de coches**.
- **29 GP completos del modelo**: 24 combinaciones de dificultad/clima de seis
  vueltas, pruebas de 10 y 16 vueltas y un campeonato de tres carreras.
- Pruebas de desgaste/agarre, estilo/consumo, ERS, DRS, SC/VSC, paradas múltiples,
  parada lenta, penalizaciones, orden final, pausa, independencia de frames y XP.
- JavaFX real: canción original en **PLAYING**, flechas, P sin repetición accidental,
  pausa, bloqueo/cancelación de compuesto, estado visual de mecánicos, GP completo,
  campeonato de tres GP, resultados, estadísticas, clásico y liberación de medios.
- Galería de interfaz: `private/verification/game-ui-490b03e7-0bc1-43ce-bb07-cbc2a8d4cf8a/`.
- `ApplicationSmoke` pasó login, música/video, seis módulos anteriores, perfiles,
  tablas de campeonato, mapa, bingo cargado, integración del juego/flechas y logout.
- Los verificadores previos de seguridad, análisis histórico, fallos de archivo y
  recursos locales pasaron. El test histórico anterior asume que se inicia en 2024;
  el archivo actual ya inicia en **2026:1**. `GameHistoryRegression` ejecuta ese
  mismo test sin modificarlo, seleccionando explícitamente su fixture **2024:1**;
  filtros, análisis, simulador educativo, guía, multimedia y demo pasaron.

La opción `--history-regression` requiere MySQL local como las pruebas históricas.
Las pruebas propias del juego (`--ui` sin esa opción) no usan SQL ni HTTP. Las
capturas son salida privada. Continúan las advertencias heredadas de APIs
obsoletas/genéricos y `Unsafe` de JavaFX; la compilación y las verificaciones finalizan.

## Límites deliberados de esta versión

Es un juego 2D de estrategia con acelerador/frenada automáticos, no un motor 3D
de conducción ni una reproducción de circuitos reales. La IA es de reglas y las
condiciones están generadas para el juego. SC/VSC, sanciones y temperaturas son
simplificaciones explicadas dentro de la pantalla. La progresión es local a la
sesión y no existe clasificación multijugador. No requiere dependencias nuevas.
