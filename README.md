# MyDiary: Your AI-Powered Journal
MyDiary is a comprehensive journaling application designed to help users record their thoughts and experiences, offering AI-powered summarization and mood analysis. It features robust user authentication, real-time data synchronization, and a seamless user experience enhanced by modern Android UI components and backend services.

## Features
- Secure Authentication & User Roles:

  - User registration and login powered by Firebase.

  - Supports multiple user roles:

    - Users: Can create, manage, and view their own journal entries, with the option to make them public or private. They can also view public journals from other users and interact with therapists.
    
    - Therapists: Can view public journal entries of users and interact with users for support and guidance.
    
    - Admins: Have moderation capabilities across the entire platform, including managing users and journals.

- Journaling with Privacy Control: Create, view, and manage personal journal entries. Each entry can be toggled between private (only visible to the user who created it) and public (visible to other users and therapists).

- AI Summarization: Leverage the Grok API via a Flask backend to get concise summaries of your journal entries.

- Mood Selection: Easily tag your journal entries with your current mood using a dropdown selector.

- Image Attachment: Store and display images associated with journal entries, managed by a PHP-based server.

- Journal Filtering & Search: Filter entries by "All Entries" (including public entries from others) or "My Entries" (private and public entries by the current user), and search by keywords within content, mood, or username.

- Mood Analytics: Visualize your mood trends over time using interactive charts, providing insights into your emotional well-being.

- Real-time Data: Synchronize journal entries and user interactions across devices using Firebase Realtime Database.

- Intuitive UI/UX: Enhanced with Lottie animations for engaging visuals and Glide for efficient image loading.

## Technologies Used
This application is built with a diverse set of technologies across its frontend (Android app) and backend services.

### Frontend (Android Application)
- Android Studio: The official Integrated Development Environment for Android application development.

- Kotlin: The primary programming language for building the Android application, offering conciseness and safety.

- XML: Used for designing the user interface layouts of the Android application.

- Lottie: An animation library by Airbnb that parses Adobe After Effects animations exported as JSON, allowing for beautiful, scalable animations in the app.

- Glide: A fast and efficient open-source media management and image loading framework for Android, used for displaying images seamlessly.

- OkHttp: An open-source HTTP client developed by Square, used for making efficient network requests to the Flask backend.

- JSON: Data interchange format used for communication between the Android app and the Flask backend.

- PhilJay/MPAndroidChart: A powerful Android charting library used for displaying interactive mood analytics and other data visualizations.

### Backend (AI Summarization & API)
- Python: The programming language used to develop the backend services.

- Flask: A lightweight Python web framework used to build the RESTful API endpoint for AI summarization.

- Grok API: Integrated into the Flask backend to provide the artificial intelligence capabilities for summarizing journal entries.

### Database & Authentication
- Firebase: Google's mobile and web application development platform used for:

  - Firebase Authentication: Handles user registration, login, and session management.
  
  - Firebase Realtime Database: A NoSQL cloud database used for storing and synchronizing journal entries and user data in real-time.

### Image Storage
- XAMPP: An open-source cross-platform web server solution stack package (Apache, MySQL, PHP, Perl). It's used here to host the PHP script and potentially a MySQL database if image metadata is stored there.

- PHP: A server-side scripting language used to handle image uploads and serve stored images for the application.

## Setup Instructions
To run this application, you will need to set up the Android app, the Flask backend, the XAMPP/PHP image server, and configure a Firebase project.

1. Firebase Project Setup
  - Go to the Firebase Console.
  
  - Create a new Firebase project.
  
  - Add an Android app to your Firebase project, following the on-screen instructions to download google-services.json and place it in your Android project's app/ directory.
  
  - Enable Firebase Authentication (Email/Password provider).
  
  - Enable Firebase Realtime Database and configure its security rules to allow read/write access based on your application's logic (refer to the project's Firebase rules for specifics, especially for Users and Journals nodes).

2. Android Application (MyDiary)
  - Clone this repository to your local machine.
  
  - Open the project in Android Studio.
  
  - Ensure you have downloaded the google-services.json file from your Firebase project and placed it in the app/ directory.
  
  - Build and run the application on an Android emulator or a physical device.

3. Flask Backend (AI Summarization)
  - Navigate to the backend/ directory (assuming you have one) containing your Flask application code.
  
  - Install required Python packages:
  
    pip install Flask groq
  
  - Set up your Grok API key as an environment variable or directly in your Flask code (though environment variables are recommended for security).
  
  - Run the Flask application:

    python Summarizer.py

  - Important: Update the summarizePrompt function in AIJournalFragment.kt to point to the correct IP address and port where your Flask server is running (e.g., http://192.168.1.X:5000/summarize).

4. XAMPP/PHP Image Server
  - Download and install XAMPP on your local machine.
  
  - Start the Apache web server and MySQL database (if used for image metadata) from the XAMPP control panel.
  
  - Place your PHP image handling scripts (e.g., upload.php, get_image.php) in the htdocs directory (or a subdirectory within it) of your XAMPP installation.
  
  - Important: Update the image loading/uploading logic in your Android app to point to the correct URL for your PHP server (e.g., http://192.168.1.X/your_image_server_folder/).
