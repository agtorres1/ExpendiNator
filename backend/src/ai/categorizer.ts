/**
 * Asigna una categoría del usuario a cada ítem.
 *
 * Hoy combina la sugerencia del LLM con las palabras clave del usuario.
 * Es el punto donde, en la parte 2, se va a llamar al clasificador entrenado
 * en Azure Machine Learning (managed online endpoint) sin tocar el resto del flujo.
 */

export type UserCategory = { id: number | null; name: string; keywords: string[] };

export type CategoryAssignment = {
  categoryId: number | null;
  categoryName: string | null;
  source: "llm" | "keywords" | "none";
};

function normalize(s: string): string {
  return s
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "");
}

export function assignCategory(
  title: string,
  llmSuggestion: string | null,
  categories: UserCategory[]
): CategoryAssignment {
  if (llmSuggestion) {
    const match = categories.find((c) => normalize(c.name) === normalize(llmSuggestion));
    if (match) return { categoryId: match.id, categoryName: match.name, source: "llm" };
  }

  const t = normalize(title);
  for (const c of categories) {
    if (c.keywords.some((k) => k && t.includes(normalize(k)))) {
      return { categoryId: c.id, categoryName: c.name, source: "keywords" };
    }
  }

  return { categoryId: null, categoryName: null, source: "none" };
}
