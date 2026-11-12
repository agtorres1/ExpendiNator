# Evaluación del escaneo de tickets

Mide qué tan bien el modelo lee tickets reales, para comparar versiones del prompt o modelos distintos con números en vez de a ojo.

## Armar el dataset

1. Sacá fotos de 30 a 50 tickets reales y variados: supermercado, farmacia, nafta, delivery, tickets largos, arrugados, con descuentos.
2. Guardalas en `eval/tickets/` (001.jpg, 002.jpg, …).
3. Copiá `dataset.example.jsonl` a `dataset.jsonl` y cargá una línea por ticket con los valores **correctos**, leídos por vos.
   - `amount` es el importe total de cada línea; los descuentos van en negativo.
   - `category` es opcional: si la ponés, también se mide la categorización.

`tickets/`, `dataset.jsonl` y `results/` están en el `.gitignore`, porque los tickets pueden tener datos personales y el repo es público.

## Correr

```bash
npm run eval
```

Muestra el resultado por ticket y un resumen. El detalle queda en `eval/results/`.

| Métrica | Qué mide |
|---|---|
| Total correcto | El total leído coincide con el real (±$1) |
| Fecha correcta | La fecha coincide exactamente |
| Ítems precisión / recall | De los ítems que devolvió, cuántos son reales / de los reales, cuántos encontró (emparejados por monto) |
| Categoría | De los ítems emparejados, cuántos tienen la categoría esperada |
| Suma ítems = total | Consistencia interna, una señal de que se saltó o inventó líneas |
| Latencia / tokens | Para estimar costo y experiencia de usuario |

## Flujo de trabajo

1. Corré la evaluación con el prompt actual y anotá el resumen.
2. Copiá el prompt a `prompts/receipt-extraction.v2.md`, cambiá lo que quieras y actualizá `PROMPT_VERSION` en `src/ai/receiptExtractor.ts`.
3. Volvé a correr y compará. Si mejora, queda; si no, volvés a v1.

Lo mismo sirve para comparar modelos: cambiá `AZURE_OPENAI_DEPLOYMENT` en el `.env`.
