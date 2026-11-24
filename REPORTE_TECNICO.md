# Reporte técnico — Maíz IA: chatbot de sanidad del maíz on-device

**Desarrollador:** Jesús Enrique Flores Riera — Estudiante de Geomática
**Proyecto:** Reto 11 — Aplicación móvil con inteligencia artificial generativa
**Repositorio:** https://github.com/jf-floresriera/Reto11

---

## 1. Resumen

Maíz IA es un chatbot para Android que responde preguntas sobre sanidad del cultivo de maíz
(enfermedades foliares, manejo, productos registrados en Colombia y Estados Unidos). A diferencia
de chatbots basados en servicios en la nube (OpenAI, Gemini, DeepSeek), este corre **100 % en el
dispositivo**, sin conexión a internet, usando un modelo de lenguaje de 1.000 millones de
parámetros especializado en el dominio mediante preentrenamiento adaptativo (DAPT) y ajuste
supervisado (SFT).

## 2. El modelo

### 2.1 Origen y entrenamiento

El modelo parte de `meta-llama/Llama-3.2-1B-Instruct` y se adapta en dos etapas:

1. **DAPT (Domain-Adaptive Pretraining) con QLoRA**: preentrenamiento adicional sobre un corpus
   propio de 41.573 documentos en español e inglés, minados de OpenAlex, repositorios DSpace
   (UNAL, AGROSAVIA, CIMMYT, CGSpace), Europe PMC, y los registros oficiales de productos
   fitosanitarios ICA (Colombia) y EPA (Estados Unidos). Se entrenaron tres mezclas de datos
   (M0, M1, M2); la mezcla M2 (todo el corpus) fue la seleccionada.
2. **SFT (Supervised Fine-Tuning)**: ajuste conversacional en tres niveles de respuesta
   (agricultor, técnico, científico) y dos idiomas (español, inglés), usando un dataset generado
   por un modelo "profesor" (Qwen2.5-7B).

### 2.2 Selección del modelo

Se entrenaron y evaluaron tres familias de modelos (Llama 3.2 1B, Qwen 2.5 1.5B, Gemma 3 1B) bajo
las mismas condiciones. Resultados del benchmark de opción múltiple (3.303 preguntas, azar = 25 %):

| Modelo | Exactitud total | Energía de entrenamiento |
|---|---|---|
| Llama 3.2 1B base | 49,1 % | — |
| **Llama 3.2 1B + DAPT M2** | **60,2 %** | 1,03 kWh |
| Qwen 2.5 1.5B + DAPT M2 | 60,1 % | 1,68 kWh |
| Gemma 3 1B + DAPT M0 | 53,9 % | 0,27 kWh |

Llama 3.2 1B + DAPT M2 empata en exactitud con Qwen 2.5 1.5B (prueba de McNemar, p corregido por
Holm = 1, es decir sin diferencia estadísticamente significativa) con 38 % menos energía de
entrenamiento y un modelo más pequeño — la razón de elegirlo para un dispositivo móvil. Sobre este
modelo se aplicó el SFT que usa la app.

### 2.3 Preparación para el celular

El adaptador LoRA (DAPT + SFT en uno solo) se fusiona con el modelo base y se convierte a formato
GGUF, cuantizado a Q4_K_M:

| Paso | Tamaño |
|---|---|
| Modelo fusionado (bf16) | ~2,4 GB |
| GGUF f16 | 2,47 GB |
| **GGUF cuantizado Q4_K_M** | **771 MB** |

Ese archivo de 771 MB es el que se empaqueta dentro de la app. El proceso completo (fusión,
conversión, cuantización, verificación) está documentado y es reproducible en
[`colab/preparar_modelo_android.py`](colab/preparar_modelo_android.py).

## 3. Arquitectura de la app Android

- **Lenguaje/UI:** Kotlin + Jetpack Compose (Material 3).
- **Inferencia:** AAR precompilado de llama.cpp ([`llama-android`](https://github.com/ffmpegkit-maintained/llama-android),
  MIT, Maven Central). Se eligió sobre compilar llama.cpp desde cero con NDK/CMake para reducir
  los puntos de falla de un proyecto académico con un solo desarrollador.
- **Capas:**
  - `domain/`: las 6 instrucciones de sistema del SFT (idioma × nivel), el filtro de cifras sin
    verificar, y el separador de citas de fuente.
  - `data/`: copia el modelo de los assets del APK a almacenamiento interno la primera vez
    (`ModelAssetInstaller`), y envuelve la carga/generación del modelo (`ModelRepository`).
  - `ui/`: pantalla de chat (`ChatScreen`) y `ChatViewModel` (MVVM).
- **Distribución del modelo:** el `.gguf` no se versiona en git (GitHub bloquea binarios de más
  de 100 MB); se construye automáticamente en CI (GitHub Actions, `.github/workflows/build-apk.yml`)
  descargándolo desde un *Release* del mismo repositorio y empaquetándolo dentro del APK final.

### 3.1 Funcionalidades de la interfaz

- Selector de idioma (español/inglés) y nivel de respuesta (agricultor/técnico/científico).
- Historial de conversación con burbujas diferenciadas por autor.
- Efecto de "tecleo": la respuesta se revela progresivamente en pantalla. *Aclaración honesta:*
  la librería de inferencia usada (versión gratuita) no soporta streaming real de tokens — la
  respuesta completa llega de una sola vez del modelo, y la app la revela letra por letra como
  efecto visual, no como generación en tiempo real.
- Aviso visual cuando una respuesta contiene una cifra con unidad (dosis, porcentaje) que no se
  pudo verificar contra ninguna referencia.
- Separación de las citas de fuente del cuerpo de la respuesta, mostrando cada una por separado
  (preparado para más de una cita si el modelo las incluye, aunque fue entrenado para dar
  normalmente una sola).
- Sonido corto al terminar de generar una respuesta.
- Compartir la conversación completa por cualquier app instalada (WhatsApp, correo, etc.) usando
  el selector de compartir nativo de Android.
- Pantalla "Acerca de" con los créditos del desarrollador y una descripción del proyecto.

## 4. Riesgos conocidos y cómo se manejan

Documentados en el proyecto de origen y confirmados con pruebas reales sobre el modelo ya
cuantizado:

- **Fuentes posiblemente inventadas.** El modelo fue entrenado con la pregunta sola, sin el
  fragmento de origen en el prompt; aprendió a "citar" de memoria. Ejemplo real observado: ante
  una pregunta sobre la mancha de asfalto del maíz (causada por *Phyllachora maydis*), citó un
  artículo sobre *Aspergillus flavus* (una enfermedad distinta). La app no verifica estas citas
  contra ninguna fuente real — mostrarlas tal cual es una limitación conocida, no un error de la
  app. La solución de fondo (RAG: recuperar el fragmento real y mostrarlo en vez de lo que el
  modelo escriba) queda como trabajo futuro.
- **Posibles dosis inventadas:** 9,8 % de las respuestas en la evaluación original traían una
  cifra con unidad no respaldada por la referencia. La app detecta este patrón con una expresión
  regular y muestra una advertencia, pero no bloquea la respuesta.
- **Exactitud del modelo:** ~60 % en un benchmark de opción múltiple de 4 alternativas (azar =
  25 %). Debe presentarse como apoyo informativo, no como recomendación agronómica definitiva.

## 5. Estado del proyecto

| Componente | Estado |
|---|---|
| Minería, DAPT, SFT, selección del modelo | Completo |
| Fusión + cuantización GGUF | Completo |
| App Android (UI, inferencia, guardarraíles) | Completo, compilando en CI |
| Evaluación del modelo cuantizado contra el test SFT | En curso |
| Evaluación experta de la calidad de las respuestas | Pendiente |
| RAG (fuentes verificadas) | Pendiente, trabajo futuro |

## 6. Cómo reproducirlo

1. Preparar el modelo: correr `colab/preparar_modelo_android.py` en Google Colab (fusiona el
   adaptador, genera el GGUF, lo cuantiza).
2. Subir el `.gguf` resultante como asset de un Release de este repositorio.
3. Disparar el workflow `.github/workflows/build-apk.yml` (pestaña Actions → Run workflow),
   pasando la URL del asset como `modelo_url`.
4. Descargar el APK generado desde los artefactos de esa ejecución e instalarlo.
