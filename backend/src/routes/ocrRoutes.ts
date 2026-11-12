import express from "express";
import multer from "multer";
import { OcrConfirmController } from "../controllers/ocrConfirmController.js";
import { MockOcrSimpleController } from "../controllers/ocrController.js";
import { OcrParseController } from "../controllers/ocrParseController.js";
import { verifyToken } from "../middleware/verifyToken.js";

export const ocrRouter = express.Router();

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 8 * 1024 * 1024 },
  fileFilter: (_req, file, cb) => cb(null, file.mimetype.startsWith("image/")),
});

// Escaneo real con Azure OpenAI (multipart/form-data, campo "image")
ocrRouter.post("/parse", verifyToken, upload.single("image"), OcrParseController.parse);

// Mock original: sirve para probar la app sin Azure
ocrRouter.get("/mock", verifyToken, MockOcrSimpleController.parse);

ocrRouter.post("/confirm", verifyToken, OcrConfirmController.confirm);
