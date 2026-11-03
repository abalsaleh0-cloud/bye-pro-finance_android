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
