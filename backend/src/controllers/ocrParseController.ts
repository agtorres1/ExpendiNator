import { randomUUID } from "node:crypto";
import { Request, Response } from "express";
import { isAzureConfigured } from "../ai/azureClient.js";
import { assignCategory, UserCategory } from "../ai/categorizer.js";
import { extractReceipt } from "../ai/receiptExtractor.js";
import { DEFAULT_CATEGORIES } from "../config/defaultCategories.js";
import { CategoryModel } from "../models/Category.js";
import { ReceiptScanModel } from "../models/ReceiptScan.js";

function todayISO(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function isValidISODate(s: string | null): s is string {
  return !!s && /^\d{4}-\d{2}-\d{2}$/.test(s) && !isNaN(Date.parse(s));
}

async function loadUserCategories(userId: number): Promise<UserCategory[]> {
  const cats = await CategoryModel.getAllByUser(userId);
  if (cats.length) {
    return cats.map((c) => ({ id: c.id, name: c.name, keywords: c.keywords ?? [] }));
  }
  return DEFAULT_CATEGORIES.map((c) => ({ id: null, name: c.name, keywords: c.keywords }));
}

export const OcrParseController = {
  async parse(req: Request, res: Response) {
    if (!isAzureConfigured()) {
      return res.status(503).json({
        error: "El escaneo con IA no está configurado en el servidor. Revisá las variables AZURE_OPENAI_* del .env.",
      });
    }

    const file = req.file;
    if (!file) {
      return res.status(400).json({ error: "Falta la imagen del ticket (campo 'image')" });
    }

    try {
      const userId = req.userId!;
      const categories = await loadUserCategories(userId);

      const { receipt, telemetry } = await extractReceipt(
        file.buffer,
        file.mimetype,
        categories.map((c) => ({ name: c.name, keywords: c.keywords }))
      );

      const date = isValidISODate(receipt.date) ? receipt.date : todayISO();
      const warnings = [...receipt.warnings];
      if (!isValidISODate(receipt.date)) warnings.push("No se pudo leer la fecha; se usó la de hoy.");

      const items = receipt.items.map((it) => {
        const cat = assignCategory(it.title, it.suggestedCategory, categories);
        return {
          title: it.title,
          amount: Number(it.amount.toFixed(2)),
          categoryId: cat.categoryId,
          categoryName: cat.categoryName,
          categorySource: cat.source,
          date,
        };
      });

      const itemsSum = Number(items.reduce((acc, x) => acc + x.amount, 0).toFixed(2));
      const total = receipt.total ?? itemsSum;
      if (receipt.total !== null && Math.abs(receipt.total - itemsSum) > 1) {
        warnings.push(`La suma de los ítems (${itemsSum}) no coincide con el total del ticket (${receipt.total}). Revisalos.`);
      }

      const receiptId = randomUUID();
      await ReceiptScanModel.create({ id: receiptId, userId, ...telemetry, extraction: receipt });

      console.log(
        `[ocr] scan=${receiptId} model=${telemetry.model} prompt=${telemetry.promptVersion} ` +
          `latency=${telemetry.latencyMs}ms tokens=${telemetry.promptTokens}/${telemetry.completionTokens} items=${items.length}`
      );

      return res.json({
        receiptId,
        merchant: receipt.merchant,
        currency: receipt.currency || "ARS",
        date,
        items,
        total: Number(total.toFixed(2)),
        warnings,
      });
    } catch (err) {
      console.error("[ocr] error", err);
      return res.status(502).json({ error: "No se pudo procesar el ticket. Probá con otra foto." });
    }
  },
};
