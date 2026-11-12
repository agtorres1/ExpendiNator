import { pool } from "../config/db.js";

/**
 * Cada escaneo queda registrado con lo que devolvió el modelo y, al confirmar,
 * con lo que el usuario corrigió. Esa diferencia es el dataset para evaluar
 * el prompt y para entrenar el clasificador de categorías.
 */
export const ReceiptScanModel = {
  async create(scan: {
    id: string;
    userId: number;
    model: string;
    promptVersion: string;
    latencyMs: number;
    promptTokens: number | null;
    completionTokens: number | null;
    extraction: unknown;
  }) {
    await pool.query(
      `INSERT INTO receipt_scans
         (id, user_id, model, prompt_version, latency_ms, prompt_tokens, completion_tokens, extraction_json)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        scan.id,
        scan.userId,
        scan.model,
        scan.promptVersion,
        scan.latencyMs,
        scan.promptTokens,
        scan.completionTokens,
        JSON.stringify(scan.extraction),
      ]
    );
  },

  async saveConfirmation(id: string, userId: number, confirmedItems: unknown) {
    await pool.query(
      `UPDATE receipt_scans
          SET confirmed_json = ?, confirmed_at = CURRENT_TIMESTAMP
        WHERE id = ? AND user_id = ?`,
      [JSON.stringify(confirmedItems), id, userId]
    );
  },
};
