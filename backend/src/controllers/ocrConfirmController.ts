import { Request, Response } from "express";
import { CategoryModel } from "../models/Category.js";
import { ExpenseModel } from "../models/Expense.js";
import { ReceiptScanModel } from "../models/ReceiptScan.js";
import { sanitizeDateInput } from "../utils/date.js";

export const OcrConfirmController = {
  async confirm(req: Request, res: Response) {
    try {
      const userId = req.userId!;
      const items = req.body.items;

      if (!Array.isArray(items) || items.length === 0) {
        return res.status(400).json({ error: "items es requerido y debe ser una lista" });
      }

      const created = [];

      for (const it of items) {
        const title = it.title?.trim();
        const amount = Number(it.amount);
        const categoryName = it.categoryName || it.category || null;
        const categoryId = it.categoryId ?? null;
        const dateCandidate = it.date || new Date().toISOString();

        let normalizedDate: string;
        try {
          normalizedDate = sanitizeDateInput(dateCandidate);
        } catch (err) {
          continue;
        }

        if (!title || isNaN(amount)) continue;

        let categoryIdToPersist = null;
        if (categoryId !== null && categoryId !== undefined) {
          const cat = await CategoryModel.findById(userId, Number(categoryId));
          if (cat) {
            categoryIdToPersist = cat.id;
          }
        }

        if (!categoryIdToPersist && categoryName) {
          const cat = await CategoryModel.findByName(userId, categoryName);
          if (cat) categoryIdToPersist = cat.id;
        }

        const newExpense = await ExpenseModel.create({
          user_id: userId,
          title,
          amount,
          date: normalizedDate,
          category_id: categoryIdToPersist
        });

        created.push(newExpense);
      }

      // Guardamos lo que el usuario confirmó/corrigió junto al escaneo original (feedback para evaluar y reentrenar)
      const receiptId = req.body.receiptId;
      if (typeof receiptId === "string" && !receiptId.startsWith("mock-")) {
        try {
          await ReceiptScanModel.saveConfirmation(receiptId, userId, items);
        } catch (err) {
          console.error("[ocr] no se pudo guardar la confirmación del escaneo", err);
        }
      }

      return res.json({ created });

    } catch (err) {
      console.error(err);
      return res.status(500).json({ error: "Error al confirmar gastos del OCR" });
    }
  }
};
