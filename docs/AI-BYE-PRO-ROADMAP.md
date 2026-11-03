# AI BYE PRO development stages

## Implemented in 10.0.79
- Blank multi-line composer and removal of the advisory banner and quick prompt strip.

## Implemented in 10.0.80 — Android image attachment UI
- Android Photo Picker supports up to four images per message.
- Composer previews, per-image removal, and previews inside user chat messages.
- Persistable read access is requested for selected photo URIs.
- Selected images remain local: current `/api/v1/ai` accepts only `AiRequest(prompt: String)`, so image bytes are not sent to the server or vision model yet. The app explicitly tells the user that analysis is not connected.

## 10.0.81 — validation correction
- Restored the original non-empty Arabic and English dictionary values for the advisory and input hint keys. The chat screen still omits the advisory banner and the input remains visually blank; keeping dictionary values intact preserves the project's translation contract tests.

## Next required stage — real multimodal analysis
- Verify and implement a secure image-capable backend contract without changing the Web repository.
- Connect a vision-capable model, add size/type/count limits and consent/error handling.
- Add end-to-end tests proving the selected image bytes reach the model and the answer is grounded in the image.

## 10.0.82 — on-device image understanding (no paid API)
- Added a local multimodal inference path through the existing LiteRT-LM runtime; selected images are copied to temporary app-cache files and passed as image content to the local model.
- Image bytes stay on-device; no cloud API or paid key is used for image analysis.
- Enforces up to four images and a 10 MiB per-image limit, validates image MIME types, and deletes temporary copies after inference.
- Vision is enabled only for a model file whose name identifies Gemma 3n; other local text-only models are not presented as vision-capable.
- The feature requires the user to supply/install a compatible .litertlm vision model. A compatible Gemma 3n E2B package is approximately 3.66 GB and its published device-memory guidance is 8 GB; actual performance and compatibility still require device testing.
