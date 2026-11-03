# AI BYE PRO development stages

## Implemented in 10.0.79
- Blank multi-line composer and removal of the advisory banner and quick prompt strip.

## Implemented in 10.0.80 — Android image attachment UI
- Added Android Photo Picker with a maximum of four selected images per message.
- Added image previews and per-image removal in the composer.
- Added image previews to user chat messages.
- Image-only messages can be placed in the conversation history.
- Safety behavior: the current `/api/v1/ai` contract accepts only `AiRequest(prompt: String)`. Therefore attached images are intentionally NOT uploaded to the server or passed to the text-only model. The assistant clearly tells the user that visual analysis is not yet connected instead of pretending to analyze the images.

## Next required stage — real multimodal analysis
- Add a verified server contract that accepts image content securely (prefer short-lived upload URLs or bounded multipart/base64 payloads).
- Connect the AI backend to a vision-capable model; enforce MIME/size/count limits, authorization, privacy consent, and error handling.
- Add end-to-end tests proving that image bytes reach the model and that the model response is grounded in the image.
- Do not modify the Web repository as part of this Android client-only change.
