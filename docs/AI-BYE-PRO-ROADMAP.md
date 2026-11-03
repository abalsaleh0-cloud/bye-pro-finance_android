# AI BYE PRO development stages

## Implemented in 10.0.79
- Clear the chat composer by default: no example or prompt text is displayed inside the writing field.
- Expand the composer into a multi-line writing area and give the conversation more vertical room by removing the disclaimer banner and quick-prompt chip strip.
- Remove the “this analysis is advisory” disclaimer from the conversation screen.

## Planned stage: image attachments in AI chat
- Add Android Photo Picker attachment selection and visible image previews in the conversation composer.
- Support selecting, removing, and sending one or more images with a user message.
- Connect the attachment flow to a real multimodal AI endpoint and enforce image size/type limits.
- Preserve the existing text-only chat fallback and never claim an image was analyzed unless the AI service actually received and processed it.

**Dependency:** the current Android chat request contract sends only a text prompt (AiRequest.prompt). Genuine image understanding therefore requires a compatible multimodal endpoint/API contract before end-to-end image upload can be marked complete. The Web repository remains unchanged.
