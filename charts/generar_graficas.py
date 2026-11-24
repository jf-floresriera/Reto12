"""Genera gráficas reales (PNG) a partir de los datos exactos de resumen_colab.json
que el usuario pegó en el chat. Nada de cifras inventadas: todo viene de ese JSON.
"""
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

# ---- Datos reales, copiados de resumen_colab.json (benchmark TOTAL, n=3303) ----
datos = {
    "Llama 3.2 1B": {
        "base":   {"acc": 0.4908, "ci": (0.4738, 0.5071), "kwh": None},
        "DAPT M0": {"acc": 0.5583, "ci": (0.5425, 0.5767), "kwh": 0.218},
        "DAPT M2": {"acc": 0.6016, "ci": (0.5870, 0.6179), "kwh": 1.035},
        "DAPT M2 + SFT": {"acc": 0.5837, "ci": (0.5686, 0.6007), "kwh": 1.035 + 0.055},
    },
    "Qwen 2.5 1.5B": {
        "base":   {"acc": 0.5362, "ci": (0.5195, 0.5531), "kwh": None},
        "DAPT M0": {"acc": 0.5668, "ci": (0.5483, 0.5843), "kwh": 0.345},
        "DAPT M2": {"acc": 0.6013, "ci": (0.5852, 0.6179), "kwh": 1.677},
    },
    "Gemma 3 1B": {
        "base":   {"acc": 0.4738, "ci": (0.4559, 0.4914), "kwh": None},
        "DAPT M0": {"acc": 0.5386, "ci": (0.5216, 0.5556), "kwh": 0.272},
    },
}

AZUL, VERDE, DORADO, ROJO = "#1a73e8", "#34a853", "#fbbc04", "#ea4335"
COLOR_ETAPA = {"base": "#9aa0a6", "DAPT M0": DORADO, "DAPT M2": AZUL, "DAPT M2 + SFT": VERDE}

# ---------------------------------------------------------------------------
# Gráfica 1: exactitud en el benchmark, por familia y etapa, con IC 95 %
# ---------------------------------------------------------------------------
fig, ax = plt.subplots(figsize=(10, 6), dpi=160)

familias = list(datos.keys())
etapas = ["base", "DAPT M0", "DAPT M2", "DAPT M2 + SFT"]
ancho = 0.2
x = np.arange(len(familias))

for i, etapa in enumerate(etapas):
    accs, errs, xs = [], [], []
    for j, fam in enumerate(familias):
        if etapa in datos[fam]:
            d = datos[fam][etapa]
            accs.append(d["acc"] * 100)
            errs.append((d["acc"] - d["ci"][0]) * 100)
            xs.append(x[j] + (i - 1.5) * ancho)
    if accs:
        barras = ax.bar(xs, accs, ancho, yerr=errs, capsize=3,
                         color=COLOR_ETAPA[etapa], label=etapa, edgecolor="white", linewidth=0.6)
        # La etiqueta va POR ENCIMA del límite superior de la barra de error, no sobre la barra.
        for b, a, e in zip(barras, accs, errs):
            ax.annotate(f"{a:.1f}", (b.get_x() + b.get_width() / 2, a + e + 1.0), ha="center",
                        va="bottom", fontsize=8, color="#202124")

ax.axhline(25, color=ROJO, linestyle="--", linewidth=1.2, label="Azar (25 %)")
ax.set_xticks(x)
ax.set_xticklabels(familias, fontsize=11)
ax.set_ylabel("Exactitud en el benchmark (%)", fontsize=11)
ax.set_title("Especializar el modelo mejora la exactitud en las 3 familias\n"
             "(3.303 preguntas, barras de error = IC 95 %)", fontsize=12, fontweight="bold")
ax.set_ylim(0, 75)
ax.legend(frameon=False, ncol=5, loc="upper center", bbox_to_anchor=(0.5, -0.08), fontsize=9)
ax.spines["top"].set_visible(False)
ax.spines["right"].set_visible(False)
fig.tight_layout()
fig.savefig("/projects/sandbox/charts/benchmark_por_familia.png", bbox_inches="tight")
plt.close(fig)

# ---------------------------------------------------------------------------
# Gráfica 2: frontera de eficiencia — energía de entrenamiento vs. exactitud
# ---------------------------------------------------------------------------
fig, ax = plt.subplots(figsize=(8, 6), dpi=160)

# Límites fijos ANTES de anotar, para que la flecha no obligue a Matplotlib
# a expandir el lienzo (esa era la causa de la página larga y vacía).
ax.set_xlim(0, 2.0)
ax.set_ylim(52, 64)

puntos = [
    ("Llama M0", 0.218, 0.5583, AZUL, "o"),
    ("Llama M2", 1.035, 0.6016, AZUL, "*"),
    ("Qwen M0", 0.345, 0.5668, DORADO, "o"),
    ("Qwen M2", 1.677, 0.6013, DORADO, "*"),
    ("Gemma M0", 0.272, 0.5386, VERDE, "o"),
]
for nombre, kwh, acc, color, marcador in puntos:
    tam = 420 if marcador == "*" else 180
    ax.scatter(kwh, acc * 100, s=tam, c=color, marker=marcador, edgecolor="black", linewidth=0.6, zorder=3)
    ax.annotate(nombre, (kwh, acc * 100), textcoords="offset points", xytext=(8, 6), fontsize=10)

ax.annotate("Mismo resultado,\n38 % menos energía\ny modelo más chico",
            xy=(1.035, 0.6016 * 100), xytext=(0.55, 62.3),
            arrowprops=dict(arrowstyle="->", color="#5f6368"), fontsize=9, color="#5f6368")

ax.set_xlabel("Energía de entrenamiento (kWh GPU)", fontsize=11)
ax.set_ylabel("Exactitud en el benchmark (%)", fontsize=11)
ax.set_title("Llama 3.2 1B + DAPT M2 empata al modelo más grande\ncon mucho menos costo de entrenamiento",
             fontsize=12, fontweight="bold")
ax.spines["top"].set_visible(False)
ax.spines["right"].set_visible(False)
fig.tight_layout()
fig.savefig("/projects/sandbox/charts/eficiencia_energia_vs_exactitud.png")
plt.close(fig)

print("Listo: benchmark_por_familia.png y eficiencia_energia_vs_exactitud.png")
