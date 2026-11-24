# =========================================================================
# Notebook de Colab — proyecto maize_llm, Reto 11
# Fusionar adaptador LoRA + cuantizar a GGUF + evaluar el modelo cuantizado.
# Cada bloque "# Celda N" es una celda independiente de Colab. Copia cada
# celda completa (no a mano encima de una vieja) para evitar errores de
# copiado y pegado.
# =========================================================================

# Celda 1 — Dependencias
!pip install -q transformers==4.51.3 peft==0.15.2 accelerate torch==2.11.0 huggingface_hub

# =========================================================================
# Celda 2 — Montar Drive y autenticarse en Hugging Face
# Llama-3.2-1B-Instruct es un modelo con licencia: hay que haberla aceptado
# en huggingface.co y generar un token (Settings > Access Tokens).
# El token se lee del secreto de Colab 'HF_TOKEN' (panel de llave a la
# izquierda del notebook), no de un prompt interactivo.
# =========================================================================
from google.colab import drive, userdata
from huggingface_hub import login, whoami

drive.mount('/content/drive')

try:
    token = userdata.get('HF_TOKEN')
except userdata.SecretNotFoundError:
    raise SystemExit(
        "No existe un secreto llamado 'HF_TOKEN'. Crealo en el panel de "
        "llave a la izquierda del notebook, con ese nombre exacto."
    )
except userdata.NotebookAccessError:
    raise SystemExit(
        "El secreto 'HF_TOKEN' existe pero este notebook no tiene permiso "
        "para leerlo. Activa el interruptor de acceso junto al secreto."
    )

token = (token or '').strip()
if not token.startswith('hf_') or ' ' in token or len(token) > 60:
    raise SystemExit('El secreto HF_TOKEN no parece valido: debe ser unicamente el texto que empieza por "hf_".')

login(token)
print('Sesion Hugging Face:', whoami()['name'])

# =========================================================================
# Celda 3 — Fusionar el adaptador LoRA (DAPT M2 + SFT, un solo adapter) con el base
# =========================================================================
from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel
import torch

BASE = "meta-llama/Llama-3.2-1B-Instruct"
ADAPTER_PATH = "/content/drive/MyDrive/maize_llm/runs/colab_sft_v1_llama32_1b/adapter"
MERGED_OUT   = "/content/drive/MyDrive/maize_llm/runs/merged_llama32_1b_maize"

tok = AutoTokenizer.from_pretrained(ADAPTER_PATH)
base_model = AutoModelForCausalLM.from_pretrained(BASE, torch_dtype=torch.bfloat16, device_map="cpu")
model = PeftModel.from_pretrained(base_model, ADAPTER_PATH)
merged = model.merge_and_unload()

merged.save_pretrained(MERGED_OUT, safe_serialization=True)
tok.save_pretrained(MERGED_OUT)
print("Modelo fusionado guardado en:", MERGED_OUT)

# =========================================================================
# Celda 4 — Clonar llama.cpp y convertir el modelo fusionado a GGUF (f16)
# =========================================================================
!git clone --depth 1 https://github.com/ggml-org/llama.cpp /content/llama.cpp
!pip install -q -r /content/llama.cpp/requirements.txt

GGUF_DIR = "/content/drive/MyDrive/maize_llm/runs/gguf"
!mkdir -p {GGUF_DIR}

!python /content/llama.cpp/convert_hf_to_gguf.py {MERGED_OUT} \
    --outfile {GGUF_DIR}/maize_llama32_1b_f16.gguf \
    --outtype f16

# =========================================================================
# Celda 5 — Compilar llama-quantize y cuantizar a Q4_K_M
# =========================================================================
!cmake -S /content/llama.cpp -B /content/llama.cpp/build -DGGML_CUDA=OFF -DCMAKE_BUILD_TYPE=Release
!cmake --build /content/llama.cpp/build --target llama-quantize -j$(nproc)

!/content/llama.cpp/build/bin/llama-quantize \
    {GGUF_DIR}/maize_llama32_1b_f16.gguf \
    {GGUF_DIR}/maize_llama32_1b_Q4_K_M.gguf \
    Q4_K_M

!ls -lh {GGUF_DIR}

# =========================================================================
# Celda 6 — Verificacion rapida en el propio Colab (antes de llevarlo al celular)
# =========================================================================
!cmake --build /content/llama.cpp/build --target llama-cli -j$(nproc)
!/content/llama.cpp/build/bin/llama-cli \
    -m {GGUF_DIR}/maize_llama32_1b_Q4_K_M.gguf \
    -c 2048 -n 200 \
    -p "<|start_header_id|>system<|end_header_id|>\n\nEres un asistente de sanidad vegetal para el cultivo de maiz en Colombia y Estados Unidos. Responde para un agricultor: lenguaje sencillo, frases cortas y pasos practicos, sin tecnicismos. No inventes dosis, cifras ni productos. Si mencionas un producto o ingrediente activo, indica el pais de registro (ICA en Colombia, EPA en Estados Unidos) y que siempre se debe seguir la etiqueta registrada. Termina citando la fuente.<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n¿Como reconozco la mancha de asfalto en mi maiz?<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"

# =========================================================================
# Celdas 7-8 (OPCIONAL) — Exportar tambien a ONNX int4, solo para comparar.
# No es necesario para el Reto 11.
# =========================================================================
!pip install -q onnxruntime-genai onnxruntime

ONNX_DIR = "/content/drive/MyDrive/maize_llm/runs/onnx/maize_llama32_1b_int4"
!mkdir -p /content/onnx_cache
!python -m onnxruntime_genai.models.builder \
    -i {MERGED_OUT} \
    -o {ONNX_DIR} \
    -p int4 \
    -e cpu \
    -c /content/onnx_cache

!ls -lh {ONNX_DIR}

# =========================================================================
# Celda 9 — Copiar el GGUF a disco LOCAL de Colab (Drive montado por FUSE es
# lento para leer archivos grandes la primera vez) e instalar llama-cpp-python.
#
# CAMBIO DE RUMBO: dejamos de usar llama-server por HTTP. Esta version de
# llama.cpp (1.0.0-dev) trae una interfaz de chat embebida que parece haber
# reorganizado las rutas de la API (por eso /v1/chat/completions daba 404
# aunque el servidor estuviera vivo). En vez de perseguir esa API, llamamos
# al modelo directo desde Python con llama-cpp-python: mismo motor GGUF,
# sin servidor, sin puertos, sin rutas que puedan cambiar entre versiones.
# =========================================================================
import os, shutil

!pip install -q llama-cpp-python

GGUF_DIR = globals().get("GGUF_DIR", "/content/drive/MyDrive/maize_llm/runs/gguf")
MODEL_DRIVE = f"{GGUF_DIR}/maize_llama32_1b_Q4_K_M.gguf"
MODEL_LOCAL = "/content/maize_llama32_1b_Q4_K_M.gguf"

if not os.path.exists(MODEL_LOCAL):
    print("Copiando el modelo de Drive a disco local (una sola vez)...")
    shutil.copy(MODEL_DRIVE, MODEL_LOCAL)
print(f"Modelo local listo: {os.path.getsize(MODEL_LOCAL) / 1e6:.0f} MB")

# =========================================================================
# Celda 10 — Cargar el modelo directamente en este proceso de Python.
# Tarda un poco la primera vez (carga ~770 MB a memoria); no hay que esperar
# a ningun servidor ni verificar ningun puerto.
# =========================================================================
from llama_cpp import Llama

llm = Llama(
    model_path=MODEL_LOCAL,
    n_ctx=2048,
    n_threads=4,
    verbose=False,
)
print("Modelo cargado en memoria, listo para generar.")

# =========================================================================
# Celda 10b (diagnostico opcional) — Confirmar la ruta del archivo de prueba.
# Ya se confirmo una vez que es esta ruta; se deja el buscador por si cambia:
#   /content/drive/MyDrive/maize_llm/runs/colab_sft_v1_llama32_1b/sft_generaciones_test.jsonl
# =========================================================================
import subprocess

encontrados = subprocess.run(
    ["find", "/content/drive/MyDrive", "-ipath", "*sft_v1*test.jsonl"],
    capture_output=True, text=True,
).stdout.strip().splitlines()

for ruta in encontrados:
    print("Candidato:", ruta)

# =========================================================================
# Celda 11 — Generar respuesta con el GGUF cuantizado para cada conversacion
# del test, y comparar contra la respuesta original (sin cuantizar) que ya
# trae el archivo sft_generaciones_test.jsonl.
# =========================================================================
import json, re, datetime

SFT_TEST_PATH = "/content/drive/MyDrive/maize_llm/runs/colab_sft_v1_llama32_1b/sft_generaciones_test.jsonl"
RESULTS_PATH  = f"/content/drive/MyDrive/maize_llm/runs/eval_gguf_q4km_{datetime.date.today()}.jsonl"

QUESTION_FIELD = "pregunta"
LEVEL_FIELD    = "nivel"   # valores: agricultor / tecnico / cientifico (iguales en ES y EN)
LANG_FIELD     = "lang"    # valores: es / en

SYSTEM_PROMPTS = {
    ("es", "agricultor"): "Eres un asistente de sanidad vegetal para el cultivo de maiz en Colombia y Estados Unidos. Responde para un agricultor: lenguaje sencillo, frases cortas y pasos practicos, sin tecnicismos. No inventes dosis, cifras ni productos. Si mencionas un producto o ingrediente activo, indica el pais de registro (ICA en Colombia, EPA en Estados Unidos) y que siempre se debe seguir la etiqueta registrada. Termina citando la fuente.",
    ("es", "tecnico"): "Eres un asistente de sanidad vegetal para el cultivo de maiz en Colombia y Estados Unidos. Responde para un tecnico o agronomo: momentos de aplicacion, umbrales, metodos y manejo integrado. No inventes dosis, cifras ni productos. Si mencionas un producto o ingrediente activo, indica el pais de registro (ICA en Colombia, EPA en Estados Unidos) y que siempre se debe seguir la etiqueta registrada. Termina citando la fuente.",
    ("es", "cientifico"): "Eres un asistente de sanidad vegetal para el cultivo de maiz en Colombia y Estados Unidos. Responde para un investigador: mecanismos, evidencia, condiciones y limitaciones de los estudios. No inventes dosis, cifras ni productos. Si mencionas un producto o ingrediente activo, indica el pais de registro (ICA en Colombia, EPA en Estados Unidos) y que siempre se debe seguir la etiqueta registrada. Termina citando la fuente.",
    ("en", "agricultor"): "You are a plant-health assistant for maize in Colombia and the United States. Answer for a farmer: plain language, short sentences and practical steps, no jargon. Do not invent doses, figures or products. If you mention a product or active ingredient, state the country of registration (ICA in Colombia, EPA in the United States) and that the registered label must always be followed. End by citing the source.",
    ("en", "tecnico"): "You are a plant-health assistant for maize in Colombia and the United States. Answer for a technician or agronomist: timing, thresholds, methods and integrated management. Do not invent doses, figures or products. If you mention a product or active ingredient, state the country of registration (ICA in Colombia, EPA in the United States) and that the registered label must always be followed. End by citing the source.",
    ("en", "cientifico"): "You are a plant-health assistant for maize in Colombia and the United States. Answer for a researcher: mechanisms, evidence, conditions and limitations of the studies. Do not invent doses, figures or products. If you mention a product or active ingredient, state the country of registration (ICA in Colombia, EPA in the United States) and that the registered label must always be followed. End by citing the source.",
}

DOSE_RE = re.compile(r"\d+[.,]?\d*\s*(L/ha|kg/ha|g/L|%|d[ii]as|days)", re.IGNORECASE)
CITA_RE = re.compile(r"(fuente|source)\s*:", re.IGNORECASE)

def preguntar(system, user, max_tokens=320):
    resultado = llm.create_chat_completion(
        messages=[{"role": "system", "content": system}, {"role": "user", "content": user}],
        temperature=0.0,
        max_tokens=max_tokens,
        repeat_penalty=1.1,
    )
    return resultado["choices"][0]["message"]["content"]

with open(SFT_TEST_PATH, encoding="utf-8") as f:
    items = [json.loads(linea) for linea in f]
print(f"{len(items)} conversaciones de prueba cargadas")

resultados = []
for i, item in enumerate(items):
    lang = item[LANG_FIELD]
    level = item[LEVEL_FIELD]
    pregunta = item[QUESTION_FIELD]
    system = SYSTEM_PROMPTS.get((lang, level))
    if system is None:
        registro = {"idx": i, "error": f"combinacion lang/level sin system prompt: {lang}/{level}"}
    else:
        try:
            respuesta = preguntar(system, pregunta)
            registro = {
                "idx": i,
                "lang": lang,
                "level": level,
                "pregunta": pregunta,
                "respuesta_gguf": respuesta,
                "respuesta_original": item.get("respuesta"),
                "referencia": item.get("referencia"),
                "cita_fuente": bool(CITA_RE.search(respuesta)),
                "cifra_posible_sin_respaldo": bool(DOSE_RE.search(respuesta)),
            }
        except Exception as e:
            registro = {"idx": i, "error": str(e)}
    resultados.append(registro)

    if i % 20 == 0 or i == len(items) - 1:
        with open(RESULTS_PATH, "w", encoding="utf-8") as out:
            for r_ in resultados:
                print(json.dumps(r_, ensure_ascii=False), file=out)
        print(f"{i + 1}/{len(items)} procesadas - guardado en {RESULTS_PATH}")

print("Evaluacion terminada. Resultados en:", RESULTS_PATH)

# =========================================================================
# Celda 12 — Resumen rapido (no hay servidor que cerrar: el modelo vive en
# la variable `llm` de este mismo proceso de Python).
# =========================================================================
import pandas as pd

df = pd.DataFrame([r for r in resultados if "error" not in r])
if len(df):
    print(df.groupby(["lang", "level"])[["cita_fuente", "cifra_posible_sin_respaldo"]].mean())
else:
    print("No hay resultados validos para resumir; revisa los errores en", RESULTS_PATH)
