/**
 * Evaluación del extractor de tickets contra un dataset etiquetado a mano.
 *
 * Uso:  npm run eval                      (usa eval/dataset.jsonl)
 *       npm run eval -- eval/otro.jsonl
 *
 * Cada línea del dataset:
 *   {"id":"001","image":"tickets/001.jpg","expected":{"merchant":"DIA","date":"2026-11-10","total":3700,
 *    "items":[{"title":"Leche entera 1L","amount":2500,"category":"Supermercado"}]}}
 *
 * Guarda el detalle en eval/results/<fecha>_<prompt>.json para comparar versiones de prompt o de modelo.
 */
import "dotenv/config";
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, extname, join, resolve } from "node:path";
import { extractReceipt, PROMPT_VERSION } from "../src/ai/receiptExtractor.js";
import { getDeploymentName } from "../src/ai/azureClient.js";
import { DEFAULT_CATEGORIES } from "../src/config/defaultCategories.js";

type ExpectedItem = { title: string; amount: number; category?: string | null };
type Example = {
  id: string;
  image: string;
  expected: { merchant?: string | null; date: string | null; total: number; items: ExpectedItem[] };
};

const AMOUNT_TOLERANCE = 1; // pesos

const near = (a: number | null | undefined, b: number | null | undefined) =>
  a != null && b != null && Math.abs(a - b) <= AMOUNT_TOLERANCE;

const MIME: Record<string, string> = { ".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".png": "image/png", ".webp": "image/webp" };

/** Empareja ítems por monto (cada esperado se usa una sola vez). */
function matchItems(pred: { amount: number; suggestedCategory: string | null }[], exp: ExpectedItem[]) {
  const used = new Set<number>();
  const pairs: { pred: (typeof pred)[number]; exp: ExpectedItem }[] = [];
  for (const p of pred) {
    const idx = exp.findIndex((e, i) => !used.has(i) && near(p.amount, e.amount));
    if (idx >= 0) {
      used.add(idx);
      pairs.push({ pred: p, exp: exp[idx] });
    }
  }
  return pairs;
}

function percentile(values: number[], p: number) {
  if (!values.length) return 0;
  const sorted = [...values].sort((a, b) => a - b);
  return sorted[Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1)];
}

const mean = (xs: number[]) => (xs.length ? xs.reduce((a, b) => a + b, 0) / xs.length : 0);
const pct = (x: number) => `${(x * 100).toFixed(1)}%`;

async function main() {
  const datasetPath = resolve(process.argv[2] ?? join("eval", "dataset.jsonl"));
  const baseDir = dirname(datasetPath);
  const examples: Example[] = readFileSync(datasetPath, "utf-8")
    .split("\n")
    .filter((l) => l.trim())
    .map((l) => JSON.parse(l));

  const categories = DEFAULT_CATEGORIES.map((c) => ({ name: c.name, keywords: c.keywords }));
  console.log(`Evaluando ${examples.length} tickets | modelo=${getDeploymentName()} | prompt=${PROMPT_VERSION}\n`);

  type OkRow = {
    id: string;
    ok: true;
    totalOk: boolean;
    dateOk: boolean;
    itemPrecision: number;
    itemRecall: number;
    categoryAccuracy: number | null;
    selfConsistent: boolean;
    latencyMs: number;
    promptTokens: number | null;
    completionTokens: number | null;
    [extra: string]: unknown;
  };
  type ErrorRow = { id: string; ok: false; error: string };
  const rows: (OkRow | ErrorRow)[] = [];
  for (const ex of examples) {
    const imagePath = join(baseDir, ex.image);
    try {
      const image = readFileSync(imagePath);
      const { receipt, telemetry } = await extractReceipt(image, MIME[extname(imagePath).toLowerCase()] ?? "image/jpeg", categories);

      const pairs = matchItems(receipt.items, ex.expected.items);
      const withCategory = pairs.filter((p) => p.exp.category !== undefined);
      const itemsSum = receipt.items.reduce((a, x) => a + x.amount, 0);

      const row: OkRow = {
        id: ex.id,
        ok: true,
        totalOk: near(receipt.total, ex.expected.total),
        dateOk: receipt.date === ex.expected.date,
        itemPrecision: receipt.items.length ? pairs.length / receipt.items.length : ex.expected.items.length ? 0 : 1,
        itemRecall: ex.expected.items.length ? pairs.length / ex.expected.items.length : 1,
        categoryAccuracy: withCategory.length
          ? withCategory.filter((p) => (p.pred.suggestedCategory ?? null) === (p.exp.category ?? null)).length / withCategory.length
          : null,
        selfConsistent: near(itemsSum, receipt.total),
        ...telemetry,
        predicted: receipt,
        expected: ex.expected,
      };
      rows.push(row);
      console.log(
        `${ex.id.padEnd(8)} total ${row.totalOk ? "✓" : "✗"}  fecha ${row.dateOk ? "✓" : "✗"}  ` +
          `ítems P=${pct(row.itemPrecision)} R=${pct(row.itemRecall)}  ${telemetry.latencyMs} ms`
      );
    } catch (err) {
      rows.push({ id: ex.id, ok: false, error: String(err) });
      console.log(`${ex.id.padEnd(8)} ERROR ${err}`);
    }
  }

  const ok = rows.filter((r): r is OkRow => r.ok);
  const catScores = ok.map((r) => r.categoryAccuracy).filter((x): x is number => x !== null);
  const summary = {
    model: getDeploymentName(),
    promptVersion: PROMPT_VERSION,
    tickets: rows.length,
    errors: rows.length - ok.length,
    totalAccuracy: mean(ok.map((r) => (r.totalOk ? 1 : 0))),
    dateAccuracy: mean(ok.map((r) => (r.dateOk ? 1 : 0))),
    itemPrecision: mean(ok.map((r) => r.itemPrecision)),
    itemRecall: mean(ok.map((r) => r.itemRecall)),
    categoryAccuracy: catScores.length ? mean(catScores) : null,
    selfConsistency: mean(ok.map((r) => (r.selfConsistent ? 1 : 0))),
    latencyP50Ms: percentile(ok.map((r) => r.latencyMs), 50),
    latencyP95Ms: percentile(ok.map((r) => r.latencyMs), 95),
    promptTokens: ok.reduce((a, r) => a + (r.promptTokens ?? 0), 0),
    completionTokens: ok.reduce((a, r) => a + (r.completionTokens ?? 0), 0),
  };

  console.log("\n── Resumen ─────────────────────────────");
  console.log(`Total correcto        ${pct(summary.totalAccuracy)}`);
  console.log(`Fecha correcta        ${pct(summary.dateAccuracy)}`);
  console.log(`Ítems (precisión)     ${pct(summary.itemPrecision)}`);
  console.log(`Ítems (recall)        ${pct(summary.itemRecall)}`);
  console.log(`Categoría             ${summary.categoryAccuracy === null ? "-" : pct(summary.categoryAccuracy)}`);
  console.log(`Suma ítems = total    ${pct(summary.selfConsistency)}`);
  console.log(`Latencia p50 / p95    ${summary.latencyP50Ms} / ${summary.latencyP95Ms} ms`);
  console.log(`Tokens (in / out)     ${summary.promptTokens} / ${summary.completionTokens}`);
  console.log(`Errores               ${summary.errors}`);

  const outDir = join("eval", "results");
  mkdirSync(outDir, { recursive: true });
  const outFile = join(outDir, `${new Date().toISOString().replace(/[:.]/g, "-")}_${PROMPT_VERSION}.json`);
  writeFileSync(outFile, JSON.stringify({ summary, rows }, null, 2));
  console.log(`\nDetalle guardado en ${outFile}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
