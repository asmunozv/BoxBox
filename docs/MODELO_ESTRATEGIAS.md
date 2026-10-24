# Modelo educativo de estrategia y análisis

## Separación de evidencias

- **Observado:** resultados, puntos, parrilla, clasificación, vueltas, stints y pits
  presentes en las instantáneas de Jolpica/OpenF1.
- **Calculado:** sumas, medias, medianas, pendientes y cambios de orden obtenidos de
  las observaciones disponibles; siempre se informa cobertura y tamaño de muestra.
- **Estimado/simulado:** parámetros de un modelo contrafactual simplificado. No son
  tiempos oficiales ni predicciones garantizadas.

## Estadísticas y comparaciones

Puntos: suma decimal de los puntos oficiales de carrera y sprint. Victorias y
podios cuentan exclusivamente sesiones RACE. Las medias de parrilla excluyen el
valor 0 (salida desde pit lane); los campos desconocidos quedan ausentes. La
clasificación cronometrada se distingue de la parrilla sancionada. Las medias por
equipo combinan inscripciones de coches y no constituyen una medida causal de habilidad.

Las curvas de temporada muestran puntos acumulados sobre carreras realmente
preparadas. No se reconstruyen sanciones de campeonato ajenas a los resultados.
Una temporada parcial se identifica como tal en la cobertura y la explicación.

Ritmo: mediana de vueltas disponibles, excluyendo vuelta 1, entrada/salida de pits,
outlaps y ventanas SC/VSC identificables en mensajes de control. La comparación de
pilotos usa el conjunto común de números de vuelta. Los contadores son propios de
cada piloto, por lo que un doblaje no es una sincronización de tiempo absoluto.
No se corrige tráfico ni se asume que toda vuelta restante sea de pista libre.

Tendencia de stint por mínimos cuadrados:

```text
b = (n * sum(x*y) - sum(x)*sum(y)) / (n*sum(x*x) - sum(x)^2)
x = vueltas transcurridas desde el inicio del stint; y = segundos observados
```

Se requieren al menos cuatro muestras. La pendiente observada combina desgaste,
combustible, tráfico, conducción y pista. No se identifica automáticamente como
degradación física del neumático.

## Detector educativo de posibles undercut/overcut

Busca pares de paradas separadas por hasta cinco vueltas. Compara el orden en la
vuelta anterior a la primera y posterior a la segunda. Un piloto que estaba detrás
y aparece delante genera una explicación de **compatibilidad**, según haya parado
antes o después. No se adjudica causalidad a la parada. La falta de referencia o de
intercambio se informa; la ventana es una heurística documentada, no una regla de F1.

## Simulador de tiempo libre de tráfico

```text
t_lap = base + delta_compuesto + d * edad_neumatico
        - f * (vuelta - 1) + calentamiento_si_edad_0
        + ajuste_salida_si_vuelta_1
T = suma(t_lap) + perdida_neta_pits * numero_paradas
delta_T = T_alternativa_modelada - T_historica_modelada
```

La pérdida se añade a la vuelta de entrada y el nuevo juego comienza en la vuelta
siguiente. La edad inicial y las edades de los juegos históricos se conservan,
incluyendo neumáticos ya usados. La sintaxis `17:HARD:3` indica parada tras vuelta
17 con un juego HARD de edad 3; `17:HARD` asume explícitamente un juego nuevo.

Parámetros iniciales orientativos, editables:

- `d`: mediana de pendientes netas observadas; si resulta negativa se inicia en 0.
  Esa limitación no demuestra ausencia de desgaste: el combustible puede ocultarlo.
- `delta_compuesto`: diferencias de medianas respecto de HARD si hay muestras;
  compuestos sin observaciones parten del **supuesto editable** 0, identificado en UI.
- Pérdida neta: exceso conjunto de vueltas de entrada/salida frente a dos vueltas
  limpias vecinas, mediano entre paradas con datos. Es una aproximación confundida
  por tráfico, combustible y calentamiento, no equivale a lane_duration ni stop_duration.
- `f`, calentamiento y ajuste de salida: cero por defecto, como simplificación.
- `base`: mediana de tiempos filtrados después de sustraer los términos anteriores.

No se usa el tiempo total oficial para forzar una coincidencia perfecta. El error
absoluto medio mostrado utiliza las vueltas empleadas para calibrar, y se etiqueta
como **descriptivo**, no como validación independiente ni prueba de precisión predictiva.

La distancia es la realmente recorrida por el piloto. No se simula que un abandonado
complete vueltas no observadas. Si faltan stints, compuestos o edades necesarios,
el modelo se informa como no calculable. El modelo inicial admite solo seco.

## Sensibilidad y limitaciones

Se repite la comparación en cuatro combinaciones de ±20% de degradación y pérdida
de pits. El mínimo/máximo es un rango de sensibilidad, **no un intervalo de confianza**.
Si el signo cambia se informa que la conclusión no es robusta a esos supuestos.

No hay modelo de tráfico, adelantamientos, reacción de rivales, meteorología,
SC/VSC contrafactual, fallos o reglamento completo. No se devuelve una posición
final hipotética. Una alternativa de un solo compuesto puede ser reglamentariamente
inválida en seco; se conserva como experimento educativo etiquetado.

## Complejidad

- Estadísticas y acumulados: O(E), con E inscripciones preparadas.
- Mediana: O(V log V), con V vueltas seleccionadas.
- OLS de un stint: O(V); filtrado de incidencias ordena los eventos disponibles.
- Simulación: O(L + P), L vueltas y P paradas ordenadas; memoria O(L).
- Calibración actual: O(V * S), S stints; se mantiene independiente de JavaFX.

## Pruebas reales de esta entrega

`AnalysisChecks` ejecutó 36 aserciones: tiempos, medianas, puntos/sprints,
pendientes, condiciones inválidas, edades usadas, reinicios de edad, coste de pits,
igualdad de escenarios e idempotencia del archivo. Casos sintéticos están marcados
como pruebas unitarias y no se presentan como datos de carreras.

Contrastes con el archivo real: Verstappen 2024 suma 437 puntos, 9 victorias y
14 podios; Baréin tiene 57 vueltas y la estrategia del ganador conserva edad
inicial 3 y paradas tras vueltas 17 y 37. En la ejecución registrada, el tiempo
histórico modelado fue 5493.391 s frente a 5504.742 s oficiales. Es un contraste
descriptivo de un caso, no una validación externa del contrafactual.

## Referencias y antecedentes

- Jolpica-F1: https://github.com/jolpica/jolpica-f1/tree/main/docs
- OpenF1: https://openf1.org/docs/
- Heilmeier, Graf y Lienkamp, *A Race Simulation for Strategy Decisions in Circuit
  Motorsports*, 2018, DOI: https://doi.org/10.1109/ITSC.2018.8570012
- Implementación de investigación y modelos por vuelta:
  https://github.com/TUMFTM/race-simulation
- Antecedente de visualización estratégica: https://github.com/Casper-Guo/Armchair-Strategist

BoxBox diferencia su propuesta mediante recorrido educativo integrado en español,
trazabilidad y experimentos transparentes; no afirma haber inventado el análisis
de estrategia o los gráficos de stints.
