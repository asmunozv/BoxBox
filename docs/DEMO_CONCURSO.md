# Demostración Be a Dragon — carrera verificada, sin internet

## Preparación

1. Iniciar MySQL80 y ejecutar `BoxBox.launch` en Eclipse (JDK 26, objetivo 25).
2. Entrar con una cuenta de aplicación existente. No usar boxbox_app como login UI.
3. Pulsar **Demo sin internet**. Lee `datasets/demo-2024.json`, valida sus huellas
   y añade solo documentos faltantes en MySQL. No hace llamadas HTTP.
4. Selección: temporada 2024, R1 Bahrain Grand Prix.

La base local ya está preparada. El paquete incluye las 24 carreras y resultados/
clasificaciones de 2024, seis sprints y detalle de Baréin. La preparación de otras
carreras utiliza internet una vez y después deja sus observaciones en MySQL.

## Recorrido de cinco minutos

| Tiempo orientativo | Acción | Evidencia que mostrar |
|---|---|---|
| 0:00–0:40 | Resumen, filtros y fuente | 20 pilotos, 57 vueltas; ganador con 26 puntos oficiales conservados |
| 0:40–1:20 | Narrativa y posiciones | Cambios observados, P1 arriba, pilotos ocultables y puntos con referencia de pits |
| 1:20–2:10 | Neumáticos | Max: SOFT 1–17, HARD 18–37, SOFT 38–57; juego inicial ya usado (edad 3) |
| 2:10–2:50 | Pits | Tránsito separado del estacionario; este último no disponible para Baréin 2024 |
| 2:50–3:30 | Comparador | Temporada completa preparada; Max 437 puntos, 9 victorias, 14 podios; muestras visibles |
| 3:30–4:40 | Simulador | Cambiar primera parada 17→14; variar degradación; observar delta y sensibilidad |
| 4:40–5:00 | Supuestos y procedencia | Qué está observado, calculado o estimado y qué no permite concluir |

Para el experimento, la sintaxis permite `14:HARD:0;37:SOFT:0`. También admite
cero o varias paradas, con validación. Los coeficientes son aproximados/editables.
No prometer que adelantar una parada habría cambiado de manera exacta la posición final.

## Respuestas técnicas defendibles

- **¿Por qué no una IA generativa?** Las reglas usan observaciones y cálculos
  reproducibles, con menor riesgo de atribuir causas no verificadas.
- **¿Cuál es la innovación?** Integrar interpretación en español, narrativa,
  procedencia y contrafactuales transparentes. Existen antecedentes de análisis y
  simulación; no se reclama invención absoluta de estos conceptos.
- **¿Cómo se valida el modelo?** Aserciones analíticas, casos de borde y contraste
  descriptivo histórico. La precisión predictiva y la utilidad educativa no están
  demostradas mediante un estudio externo.
- **¿Por qué no devuelve una posición final?** No modela adelantamientos ni
  respuestas de rivales. El tiempo estimado no garantiza un orden de llegada.
- **¿Qué pasa si falta una fuente?** Se conserva lo preparado y se muestran ausencias.
  No se inventan compuestos, tiempos estacionarios o posiciones.
- **¿Depende de internet durante la exposición?** No para el paquete preparado;
  requiere MySQL local. El botón de preparación HTTP está separado del recorrido offline.

## Prueba de utilidad pendiente de realizar con personas

Protocolo propuesto: 5–8 aficionados de niveles distintos. Antes/después, preguntas
sobre stint, diferencia pit lane/estacionario, undercut y lectura de incertidumbre.
Registrar aciertos, tareas completadas, tiempo, errores y comentarios. Usar preguntas
equivalentes y no afirmar causalidad de aprendizaje con una muestra exploratoria.
No hay resultados de usuarios medidos todavía.
