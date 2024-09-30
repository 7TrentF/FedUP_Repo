# FedUp Food Waste App

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
Usage
User Authentication
Register or log in using your Google account.
After logging in, users are directed to the inventory screen.
Inventory Management
Add new ingredients by entering details such as name, category (Fridge, Freezer, Pantry, or Uncategorized), expiration date, quantity, and optional notes.
Edit or delete ingredients as needed.
Sort ingredients by category, expiration date, or alphabetically.
Recipe Functionality
Search for recipes based on the ingredients available in your inventory.
View detailed recipe instructions.
Settings
Manage the following in the settings screen:

Email Address: Update the email address used for the account.
Theme: Change the app theme.
Data Sharing: Manage data-sharing preferences.
Change Password: Update your account password.
Log Out: Sign out of the app.
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
License 
This project is licensed under the MIT License. See the LICENSE file for more 
details. 
Acknowledgments 
 Firebase for authentication and real-time database services. 
 Recipe - Food - Nutrition API for recipe information.
 REST API tested on Swagger - https://fedupmanagementapi20240925105410.azurewebsites.net/swagger/index.html