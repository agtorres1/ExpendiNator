# 📱 ExpendiNator

**ExpendiNator** es una aplicación móvil para el registro y trackeo de gastos personales.
Escanea tickets con la cámara de tu celular y los gastos se reconocen y categorizan automaticamente para que puedas llevar el control de tus finanzas de forma simple y visual.

---

## 🧭 ¿Qué podés hacer con ExpendiNator?

### 📸 Escaneo inteligente de tickets
- Usá la **cámara** para capturar un ticket de compra.
- La app procesa la imagen, detecta los gastos y **reconoce automáticamente**:
  - nombre del gasto
  - cantidad y monto
  - categoría sugerida según palabras clave
  - fecha del ticket
  
- Editá o eliminá entradas fácilmente desde la pantalla de detalle.
 
---

### 🏷️ Categorías personalizables
- Creá tus propias categorías con:
  - un nombre
  - un color
  - palabras clave para categorizarlas
  
---

### 📊 Seguimiento de presupuestos
- Definí **presupuestos por categoría** (mensuales, semanales o anuales).
- Visualizá cuánto llevás gastado con una **barra de progreso dinámica**.

---

### 📈 Reportes y resumen de gastos
- Vista general de tus gastos agrupados por categoría.
- Totales del período y desglose visual.
- Historial y listado completo.

---

### 🧩 Widget de acceso rápido
- Acceso directo al escaneo del ticket.
- Vista rápida del total gastado del día o del mes.

---
### Screenshots


<img width="300" height="750" alt="image" src="https://github.com/user-attachments/assets/b58d9419-aee8-4cf9-a17b-eee075c5f55c" />
<img width="300" height="750" alt="image" src="https://github.com/user-attachments/assets/f5c61df5-5c3a-4c45-ad8b-ed0f54c2cbd7" />
<img width="300" height="750" alt="image" src="https://github.com/user-attachments/assets/4fe1a8de-a5f8-49cc-94a4-cf7e5876d18f" />
<img width="300" height="750" alt="image" src="https://github.com/user-attachments/assets/bbaa8101-bfa9-4ef9-a9d1-d687751c8c0a" />
<img width="300" height="750" alt="image" src="https://github.com/user-attachments/assets/1c574286-7d71-4a0b-ac54-bce424fc6468" />
<img width="300" height="750" alt="image" src="https://github.com/user-attachments/assets/8d2cef5b-992d-4ec3-ade8-3822d2ef7445" />

---

# 🚀 Cómo correr el backend (servidor)

El proyecto incluye un servidor **Node.js + Express** dentro de la carpeta `backend`.

## 1️⃣ Requisitos
- **Node.js**
- **MySQL**
- **npm**

---

## 2️⃣ Crear la base de datos

Ejecutar en MySQL el archivo `db-script.sql` que se encuentra dentro de la carpeta `backend/sql`.
Esto crea la base `expendinatordb` y todas las tablas necesarias

--- 

## 3️⃣ Configurar variables de entorno

Dentro de `backend/`, copiá `.env.example` a `.env` y completá las credenciales de tu instancia local de MySQL.
El `.env` no se sube al repo.

Si ya tenías la base creada de antes, ejecutá también `backend/sql/002-receipt-scans.sql`.

## 4️⃣ Instalar dependencias

En la carpeta `backend/` ejecutar:
`npm install`

## 5️⃣ Levantar el servidor

Ejecutar:
`npm run dev`

El backend quedará disponible en:
`http://localhost:3000`

Listo para recibir requests desde la app!

---

# 🤖 Escaneo de tickets con IA (Azure)

El escaneo usa un modelo multimodal desplegado en **Microsoft Foundry** (Azure OpenAI), que lee la foto del ticket y devuelve los ítems, montos, fecha y comercio en un JSON con forma fija (Structured Outputs).

```
App (foto) ──► POST /ocr/parse ──► Azure OpenAI (Foundry) ──► categorizador ──► revisión en la app ──► POST /ocr/confirm
                                          │                                                          │
                                          └──────────────── receipt_scans (extracción + corrección) ◄─┘
```

## Configuración

1. En el portal de Microsoft Foundry, creá un proyecto y desplegá un modelo con visión (por ejemplo `gpt-4.1-mini`).
2. Copiá el endpoint y la clave del recurso y completá en `backend/.env`:
   ```
   AZURE_OPENAI_ENDPOINT=https://<tu-recurso>.openai.azure.com/
   AZURE_OPENAI_API_KEY=...
   AZURE_OPENAI_DEPLOYMENT=gpt-4.1-mini
   ```
3. Levantá el backend y sacá una foto desde la app.

Sin esas variables, `/ocr/parse` responde 503; el endpoint `/ocr/mock` sigue disponible para probar la app sin Azure.

## Piezas

| Archivo | Qué hace |
|---|---|
| `backend/prompts/receipt-extraction.v1.md` | Prompt versionado. Cada escaneo guarda qué versión se usó |
| `backend/src/ai/receiptExtractor.ts` | Llamada al modelo con JSON Schema estricto y telemetría (latencia, tokens) |
| `backend/src/ai/categorizer.ts` | Asigna categoría (sugerencia del LLM + palabras clave). Acá se va a enchufar un clasificador de Azure ML |
| `backend/sql/002-receipt-scans.sql` | Registro de cada escaneo y de lo que el usuario corrigió al confirmar |
| `backend/eval/` | Evaluación contra un dataset de tickets reales (`npm run eval`). Ver su README |

