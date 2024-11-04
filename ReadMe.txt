# FedUp Food Waste App final part

*The readMe

FedUp is a mobile application designed to help users manage their food inventory effectively, reduce food waste, and explore recipes based on available ingredients. The app provides an intuitive interface for logging in, managing ingredients, and discovering new recipes.

## Features

- **User Authentication:** Register or log in using your Google account.
- **Inventory Management:** Add, edit, or delete ingredients with details such as name, category, expiration date, quantity, and optional notes.
- **Sorting Options:** Sort ingredients by category (Uncategorized, Fridge, Freezer, Pantry), expiration date, and alphabetical order.
- **Recipe Functionality:** Search for recipes based on available ingredients.
- **Settings Management:** Customize user settings including username, theme, data sharing preferences, password changes, and more.

## Table of Contents
- [Introduction](#introduction)
- [Getting Started](#getting-started)
- [System Requirements](#system-requirements)
- [Usage](#usage)
  - [User Authentication](#user-authentication)
  - [Inventory Management](#inventory-management)
  - [Recipe Functionality](#recipe-functionality)
  - [Settings](#settings)
- [Automated Testing](#automated-testing)
- [License](#license)
- [Acknowledgments](#acknowledgments)

## Getting Started

### Clone the Repository
```bash
git clone https://github.com/7TrentF/FedUP_Repo.git
cd fedup-food-waste-app

Open in Android Studio
Launch Android Studio.
Select Open an existing Android Studio project.
Navigate to the cloned repository and open it.
Build and run the app on your Android device or emulator.
System Requirements
Android Studio (version 4.1 or higher)
Android SDK (version 29 or higher)
Gradle (version 6.5 or higher)
Java Development Kit (JDK) 8 or higher
Firebase account


Usage:

User Authentication
Register or log in using your Google account.
Users are presented with biometrics as an extra security layer if they have it enabled or have biometrics available on their device.
After logging in, users are directed to the inventory screen.
Inventory Management
Add new ingredients by entering details such as name, category (Fridge, Freezer, Pantry, or Uncategorized), expiration date, quantity, and optional notes.
Edit or delete ingredients as needed.
Sort ingredients by category, expiration date, or alphabetically.
Recipes that are nearing expiration or expired will notify the user whether they are on or off the app via a settings toggle for notifications should they need to.
Recipe Functionality
Search for recipes based on the ingredients available in your inventory.
View detailed recipe instructions.
Users can perform these functions via offline if they want to and when they get connected to the internet it will sync their data 


Settings
Manage the following in the settings screen:

Email Address: Update the email address used for the account.
Theme: Change the app theme.
Data Sharing: Manage data-sharing preferences.
Change Password: Update your account password.
Log Out: Sign out of the app.
Enable face scan biometrics: Toggle whether you would want the biometrics to be used as an extra security layer when you sign in.
Language: Change the universal language displayed in the application from Afrikaans and Default English.
Enable notifications: Toggle whether or not to recieve notifications about expiring ingredients.
Notification Timing: Opt in when to recieve notifications on when ingredients are about to expire between 1 day, 3 days, 5 days, or 7 days before.
Share Data: Allow you data to be shared with third parties
Help & Support: Access help resources and contact support.
Clear Cache: Clear the app cache to free up space.
Automated Testing
The FedUp app uses GitHub Actions for automated testing of CRUD operations. Each commit triggers a series of tests to ensure the app behaves as expected. The tests cover:

Create, Read, Update, and Delete (CRUD) operations for ingredients.
User input validation.
Integration tests for API calls.
Check out the CI/CD pipeline: FedUp GitHub Actions

Video Demonstration 
The link to the video demonstration the the usage can be found here:  
https://drive.google.com/file/d/1X0EjCra26o9uNT5aGVjIEyi4EpuV_dVx/view?usp=sharing 


FedUp Food Waste App - Release Notes
Version 2.0 (Final Release) - November 2024
Major New Features
1. Enhanced Security
•	Biometric Authentication 🔐 
o	Added face scan biometric authentication as an additional security layer
o	Users can enable/disable biometric login through settings
o	Seamless integration with existing Google authentication
2. Improved Recipe Integration
•	Advanced Recipe Generation 🍳 
o	Implemented Spoonacular Recipe-Food-Nutrition API integration
o	Real-time recipe suggestions based on available ingredients
o	Detailed recipe instructions and information
o	Replaced prototype's basic recipe search functionality
3. Smart Inventory Management
•	Enhanced Ingredients Filtering 🔍 
o	Advanced sorting capabilities: 
	Category-based filtering (Fridge, Freezer, Pantry, Uncategorized)
	Expiration date sorting
	Alphabetical ordering
o	Improved ingredient categorization system
4. Offline Functionality
•	Offline Sync Capability 💫 
o	Added offline mode support for core functionality
o	Automatic data synchronization when internet connection is restored
o	Seamless experience regardless of connectivity status
5. Notification System
•	Real-time Notifications 🔔 
o	New notification system for expiring ingredients
o	Customizable notification timing (1, 3, 5, or 7 days before expiration)
o	Push notifications work both in-app and background
o	Toggle notifications on/off through settings
o	Integration with Firebase Cloud Messaging
6. Language Support
•	Multi-language Support 🌐 
o	Added Afrikaans language support
o	Default English language option
o	Universal language switching through settings
o	Localized UI elements and content
Additional Improvements
Settings Enhancements
•	Added notification preferences management
•	Implemented biometric authentication toggle
•	Added language selection options
•	Enhanced data sharing controls
Backend Improvements
•	Upgraded Firebase integration for real-time database services
•	Implemented cloud messaging for push notifications
•	Migrated API hosting to Vercel from Azure
Technical Updates
•	Enhanced offline data handling capabilities
•	Improved synchronization mechanisms
•	Upgraded notification delivery system
•	Enhanced security implementations
API Changes
•	Switched from Asp.Net Core Web APi to Node.js api
•	Integrated Spoonacular API for enhanced recipe functionality
•	Implemented Firebase Cloud Messaging for notifications
Known Issues
•	None reported in this release

Development Notes
All innovative features planned in the prototype phase have been successfully implemented in this final release, significantly enhancing the app's functionality and user experience.
________________________________________
Note: This release represents a significant upgrade from the prototype version, with all planned innovative features successfully implemented and additional improvements added based on development insights.


License 
This project is licensed under the MIT License. See the LICENSE file for more 
details.

 
Acknowledgments 
 Firebase for authentication and real-time database services as well as cloud messaging for push notifications. 
 Recipe - Food - Nutrition API for recipe information. - https://rapidapi.com/spoonacular/api/recipe-food-nutrition
 REST API on vercel hosting - https://vercel.com/trents-projects-8ca3ed96/fed-up-api

