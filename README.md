# 🔍 ServicesFinder

- **📚 Course:** CS 175 – Android Mobile Development
- **👨‍🏫 Professor:** Yan Chen
- **📅 Semester:** Fall 2025

---
### 👥 Team Number: 09
## Team Members and Contributions
| Name | Contribution |
|------|--------------|
| **Van Anh Tran** | Category filtering engine, Firestore data services, provider side authentication system, localization for 4 languages: English (US), Chinese, Vietnamese, Spanish (US), preparing presentation |
| **Ben Nguyen** | Provider UI layouts, category chips, customer reviews, review integration |
| **Nhat Anh Nguyen** | Filtering and search, customer profile, customer authentication UI, localization UI, favorites feature, review integration, image compression, smooth animations, error handling with retry dialogs, performance optimization |
| **Rohan Mehta** | UI design, final review, presenting |

---

### 📱 APK File:
https://drive.google.com/file/d/1GHE4sdEyJbap8_SnTVIY1ZLuwNL2vEVY/view?usp=sharing

### 📋 Backlog:
https://docs.google.com/spreadsheets/d/1pm37TSBmQL9C6HI7-OMaLhnKDPHRMzfbEjpT8pUAz8k/edit?gid=0#gid=0

---

## 📖 Project Description
**ServicesFinder** is a mobile app connecting local service providers with customers seeking services like Hair Care, Nail Services, Automotive Repair, Education, Pet Care, and more.

### 🛒 Customer Features
- **Authentication:** Sign in/up with email or phone number, seamless role switching to provider portal
- **Browse & Search:** Find services by category or keyword with real-time search
- **Advanced Sorting:** Sort by Most Recent, Price (Low/High), Highest Rating, or Popularity with actual review data
- **Language Support:** Switch between English, Spanish, Vietnamese, and Chinese with full UI localization
- **Favorites:** Save preferred providers with heart button, view all favorites in profile
- **Reviews & Ratings:** Read detailed reviews, write reviews with star ratings (requires login)
- **Profile Management:** View favorites, manage account settings, and logout
- **Contact Providers:** Call, email, or navigate to provider location directly from service details

### 🏢 Provider Features
- **Flexible Registration:** Sign up with phone or email, seamless role switching to customer portal
- **Service Management:** Add, edit, and delete multiple services with rich descriptions
- **Profile Management:** Edit personal information, change password securely
- **Image Upload:** Add service photos with automatic compression (1920x1080 @ 85% quality)
- **Dashboard:** View all services, manage listings, access account settings
- **Account Control:** Secure password change and account deletion options  

---

## 🔄 Dual-Role Architecture

ServicesFinder supports two distinct user roles with separate data management:

- **🏢 Providers:** Create and manage service listings, manage profile
- **🛒 Customers:** Browse services, write reviews, save favorites, manage preferences

**Key Design:**
- Separate Firestore collections (`providers/` and `customers/`)
- Independent authentication and session management
- Role-based UI and permissions
- Users can have both roles using different accounts

---

## ⚡ Key Technical Highlights

### Performance & UX Enhancements
- **Image Compression:** Automatic upload optimization to 1920x1080 @ 85% quality
- **Smooth Animations:** Fade transitions for loading and content states
- **Error Handling:** Network connectivity checks with user-friendly retry dialogs
- **Loading States:** Progress indicators throughout the app for better user feedback

### Advanced Features
- **Real Rating Sort:** Dynamic sorting by actual review ratings with asynchronous data fetching
- **Review Integration:** Customer names linked to reviews for transparency and accountability
- **Favorites System:** Persistent favorite providers with heart icon toggle
- **Role Switcher:** One-click navigation between customer and provider authentication portals
- **Multilingual Support:** Complete localization for 4 languages including role switcher UI

### Code Quality
- **MVC Architecture:** Clean separation of concerns with Model-View-Controller pattern
- **Utility Classes:** Reusable components (AnimationHelper, NetworkHelper, RetryDialog, ProToast)
- **Firebase Integration:** Secure authentication, real-time Firestore sync, and cloud storage
- **Material Design 3:** Modern UI components following Android design guidelines

---

## 🛠️ Technical Requirements

- Android Studio
- Java
- Firebase Authentication
- Firebase Firestore
- Firebase Storage
- Glide (Image loading library)
- MinSDK: 24+
- TargetSDK: 34

---

## Project Structure

```bash
app/
├── java/
│   └── edu/
│       └── sjsu/
│           └── android/
│               └── servicesfinder/
│                   ├── controller/
│                   │   ├── CatalogueController.java
│                   │   ├── CustomerController.java
│                   │   ├── FirestoreStringTranslator.java
│                   │   ├── FormHelper.java
│                   │   ├── HomeController.java
│                   │   ├── ProviderController.java
│                   │   ├── ProviderServiceController.java
│                   │   ├── ReviewAdapter.java
│                   │   ├── ServiceCardAdapter.java
│                   │   ├── SessionManager.java
│                   │   └── UIHelper.java
│                   │
│                   ├── database/
│                   │   ├── CatalogueDatabase.java
│                   │   ├── CustomerDatabase.java
│                   │   ├── FirestoreHelper.java
│                   │   ├── ProviderDatabase.java
│                   │   ├── ProviderServiceDatabase.java
│                   │   ├── ReviewDatabase.java
│                   │   ├── ServiceDatabase.java
│                   │   └── StorageHelper.java
│                   │
│                   ├── model/
│                   │   ├── Catalogue.java
│                   │   ├── Customer.java 
│                   │   ├── Provider.java
│                   │   ├── ProviderService.java
│                   │   ├── Review.java
│                   │   └── Service.java
│                   │
│                   ├── util/
│                   │   ├── AnimationHelper.java 
│                   │   ├── NetworkHelper.java 
│                   │   ├── ProToast.java
│                   │   └── RetryDialog.java   
│                   │
│                   └── view/
│                       ├── CustomerAuthActivity.java  
│                       ├── CustomerProfileActivity.java
│                       ├── EditProfileActivity.java
│                       ├── MainActivity.java 
│                       ├── MultiSelectDropdown.java
│                       ├── ProviderDashboardActivity.java
│                       ├── ProviderEntryActivity.java
│                       └── ServiceDetailActivity.java 
│
└── res/
    ├── layout/
    │   ├── activity_customer_auth.xml         
    │   ├── activity_customer_profile.xml   
    │   └── ...
    ├── values/
    ├── drawable/
    ├── mipmap/
    └── xml/
```





