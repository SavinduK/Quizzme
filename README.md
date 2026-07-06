QuizMe

QuizMe is an AI-powered quiz generator built with Expo and React Native. Upload your lecture slides, and the app extracts the text, saves it locally as a ".txt" file, and uses the Gemini API to generate multiple-choice questions (MCQs). Previously generated questions are cached, allowing you to access them even when you're offline.

Features

- 📄 Upload lecture slides (PDF)
- 📝 Automatically extract and save slide text as a ".txt" file
- 🤖 Generate MCQs using the Gemini API
- 💾 Cache generated quizzes for offline access
- 📚 Organize quizzes by lecture

Getting Started

Prerequisites

- Node.js (LTS recommended)
- npm
- Expo CLI (optional)

Installation

Clone the repository:

git clone https://github.com/SavinduK/Quizzme.git
cd quizme

Install dependencies:

npm install

Start the Expo development server:

npx expo start

Open the app using Expo Go or run it on an Android emulator, iOS Simulator, or a connected device.

Gemini API Setup

QuizMe requires a Gemini API key to generate quizzes.

1. Open the Google Cloud Console.
2. Create a project (or select an existing one).
3. Enable the Gemini API (Generative Language API) for your project.
4. Create an API key.
5. Copy the API key.
6. Launch the QuizMe app.
7. Open the Settings screen.
8. Paste the API key into the Gemini API Key field.
9. Select the Gemini model you want to use.
10. Save your settings.

The app will use this API key for all AI-powered quiz generation. Your API key is stored locally on your device and is not included in the repository.

How It Works

1. Upload a lecture PDF.
2. The app extracts the text from the document.
3. The extracted text is saved locally as a ".txt" file.
4. The text is sent to the Gemini API.
5. Gemini generates multiple-choice questions.
6. The generated questions are cached locally, allowing you to review them even without an internet connection.

License

This project is licensed under the MIT License.
