# QuizzMe (Android)

Native Android study companion built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, **Room**, **DataStore**, and direct **Gemini Developer API** integration.

## Features

- **Questions (Home)**: Interactive quiz session runner (`MCQ`, `True/False`, `Short Answer`, and `SEQ`) sourced from cached AI quizzes or saved past papers, with filtering by data source, question format, subject, term, and title.
- **Lessons (Lectures) & Left Folder Drawer**: Searchable and sortable lecture repository with a slide-out left drawer organizing items in a **Term → Title** folder tree (`Recent` or `A–Z` sort), quick quiz launch, plain-text copy/share, and delete confirmation.
- **Lesson Reader**: 4-tab study workspace (`Notes`, `Quiz`, `Summary`, `Past Paper`) featuring native HTML rendering for PDF notes and streamed summaries, tag-free plain-text note editing, AI quiz generation referencing matching past paper questions, and past paper quiz playback.
- **Add Notes & Add Past Paper**: Zero-permission PDF picker (`OpenDocument`) for extracting lecture PDFs to HTML, optional past paper image upload (`PickMultipleVisualMedia`) with multimodal question extraction, and manual text/JSON past paper ingestion.
- **Work Tracker**: 61-day horizontal calendar strip with task indicators, completion tracking, custom task creation, and JSON task import.
- **Configuration Settings**: Manual Gemini API key configuration, model tier selection (**Gemini 3.8 Flash**, **Gemini 3.5 Flash-Lite**, **Gemini 3.1 Pro**), quiz length (`5`, `10`, `15`, `20`), default question format, and custom AI generation instructions.
