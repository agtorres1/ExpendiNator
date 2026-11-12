import { AzureOpenAI } from "openai";

/**
 * Cliente de Azure OpenAI (modelo desplegado en Microsoft Foundry).
 * Variables necesarias en .env:
 *   AZURE_OPENAI_ENDPOINT    https://<tu-recurso>.openai.azure.com/
 *   AZURE_OPENAI_API_KEY     clave del recurso
 *   AZURE_OPENAI_DEPLOYMENT  nombre del deployment (ej: gpt-4.1-mini)
 *   AZURE_OPENAI_API_VERSION opcional, default 2024-10-21
 */
let client: AzureOpenAI | null = null;

export function isAzureConfigured(): boolean {
  return Boolean(
    process.env.AZURE_OPENAI_ENDPOINT &&
      process.env.AZURE_OPENAI_API_KEY &&
      process.env.AZURE_OPENAI_DEPLOYMENT
  );
}

export function getDeploymentName(): string {
  return process.env.AZURE_OPENAI_DEPLOYMENT ?? "";
}

export function getAzureClient(): AzureOpenAI {
  if (!isAzureConfigured()) {
    throw new Error(
      "Azure OpenAI no está configurado (faltan AZURE_OPENAI_ENDPOINT / AZURE_OPENAI_API_KEY / AZURE_OPENAI_DEPLOYMENT)"
    );
  }
  if (!client) {
    client = new AzureOpenAI({
      endpoint: process.env.AZURE_OPENAI_ENDPOINT,
      apiKey: process.env.AZURE_OPENAI_API_KEY,
      deployment: process.env.AZURE_OPENAI_DEPLOYMENT,
      apiVersion: process.env.AZURE_OPENAI_API_VERSION ?? "2024-10-21",
    });
  }
  return client;
}
