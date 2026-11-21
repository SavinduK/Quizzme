# QuizzMe (Android)

Native Android study companion built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, **Room**, **DataStore**, and **Firebase AI Logic (Gemini)**.

## Features

- **Questions (Home)**: Interactive quiz sessions sourced from cached AI-generated lecture quizzes or saved past papers, with filtering by data source, question format (`MCQ`, `TF`, `SA`, `SEQ`), subject, and term/batch.
- **Lessons (Lectures)**: Searchable lecture notes repository with subject color coding, quick quiz launch, Markdown sharing, clipboard copy, and delete confirmation.
- **Lesson Reader**: 4-tab study workspace (`Notes`, `Quiz`, `Summary`, `Past Paper`) featuring Markdown rendering, inline note editing, streaming AI summaries, AI quiz generation with offline Room caching, and past paper quiz playback.
- **Add Notes & Add Past Paper**: Zero-permission PDF document picker (`OpenDocument`) with Gemini multimodal extraction to Markdown, manual Markdown editing, and AI auto-detection or raw JSON past paper ingestion.
- **Work Tracker**: 61-day horizontal calendar strip with task indicators, completion tracking, custom task creation, and JSON task import.
- **Configuration Settings**: Customizable quiz length (`5`, `10`, `15`, `20`), default question format, and custom AI generation instructions. AI inference uses keyless Firebase AI Logic protected by Firebase App Check.
