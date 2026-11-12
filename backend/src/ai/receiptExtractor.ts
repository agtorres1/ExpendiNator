import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { getAzureClient, getDeploymentName } from "./azureClient.js";

/** Versión del prompt. Cambiala cada vez que edites el archivo de prompt, así cada corrida queda trazada. */
export const PROMPT_VERSION = "receipt-extraction.v1";

const PROMPT_PATH = join(
  dirname(fileURLToPath(import.meta.url)),
  "..",
  "..",
  "prompts",
  `${PROMPT_VERSION}.md`
);
const PROMPT_TEMPLATE = readFileSync(PROMPT_PATH, "utf-8");

export type CategoryHint = { name: string; keywords: string[] };

export type ExtractedItem = {
  title: string;
  quantity: number | null;
  amount: number;
  suggestedCategory: string | null;
};

export type ExtractedReceipt = {
  merchant: string | null;
  date: string | null;
  currency: string;
  items: ExtractedItem[];
  total: number | null;
  warnings: string[];
};

export type ExtractionTelemetry = {
  model: string;
  promptVersion: string;
  latencyMs: number;
  promptTokens: number | null;
  completionTokens: number | null;
};

/** JSON Schema para Structured Outputs: el modelo está obligado a devolver exactamente esta forma. */
function buildSchema(categoryNames: string[]) {
  return {
    type: "object",
    additionalProperties: false,
    required: ["merchant", "date", "currency", "items", "total", "warnings"],
    properties: {
      merchant: { type: ["string", "null"] },
      date: { type: ["string", "null"], description: "YYYY-MM-DD" },
      currency: { type: "string", description: "Código ISO, normalmente ARS" },
      total: { type: ["number", "null"] },
      warnings: { type: "array", items: { type: "string" } },
      items: {
        type: "array",
        items: {
          type: "object",
          additionalProperties: false,
          required: ["title", "quantity", "amount", "suggestedCategory"],
          properties: {
            title: { type: "string" },
            quantity: { type: ["number", "null"] },
            amount: { type: "number" },
            suggestedCategory: {
              anyOf: [
                categoryNames.length
                  ? { type: "string", enum: categoryNames }
                  : { type: "string" },
                { type: "null" },
              ],
            },
          },
        },
      },
    },
  };
}

function renderPrompt(categories: CategoryHint[]): string {
  const list = categories.length
    ? categories
        .map((c) => `- ${c.name}${c.keywords.length ? ` (palabras clave: ${c.keywords.join(", ")})` : ""}`)
        .join("\n")
    : "(el usuario no tiene categorías; usá null)";
  return PROMPT_TEMPLATE.replace("{{CATEGORIES}}", list);
}

export async function extractReceipt(
  image: Buffer,
  mimeType: string,
  categories: CategoryHint[]
): Promise<{ receipt: ExtractedReceipt; telemetry: ExtractionTelemetry }> {
  const client = getAzureClient();
  const model = getDeploymentName();
  const started = Date.now();

  const completion = await client.chat.completions.create({
    model,
    temperature: 0,
    messages: [
      { role: "system", content: renderPrompt(categories) },
      {
        role: "user",
        content: [
          { type: "text", text: "Extraé los datos de este ticket." },
          {
            type: "image_url",
            image_url: { url: `data:${mimeType};base64,${image.toString("base64")}`, detail: "high" },
          },
        ],
      },
    ],
    response_format: {
      type: "json_schema",
      json_schema: {
        name: "receipt",
        strict: true,
        schema: buildSchema(categories.map((c) => c.name)),
      },
    },
  });

  const latencyMs = Date.now() - started;
  const choice = completion.choices[0];

  if (choice.message.refusal) {
    throw new Error(`El modelo rechazó la solicitud: ${choice.message.refusal}`);
  }
  if (choice.finish_reason === "content_filter") {
    throw new Error("La respuesta fue bloqueada por el filtro de contenido de Azure");
  }

  const receipt = JSON.parse(choice.message.content ?? "{}") as ExtractedReceipt;

  return {
    receipt,
    telemetry: {
      model: completion.model ?? model,
      promptVersion: PROMPT_VERSION,
      latencyMs,
      promptTokens: completion.usage?.prompt_tokens ?? null,
      completionTokens: completion.usage?.completion_tokens ?? null,
    },
  };
}
