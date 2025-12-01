# ServicesFinder - Version 2.0

---

**Course:** CS 175 – Android Mobile Development
**Professor:** Yan Chen
**Semester:** Fall 2025

---
### Team Number: 09
## Team Members and Contributions
| Name | Contribution |
|------|--------------|
| **Van Anh Tran** | Category filtering engine, Firestore data services, provider side authentication system, localization for 4 languages: English (US), Chinese, Vietnamese, Spanish (US), preparing presentation |
| **Ben Nguyen** | Provider UI layouts, category chips, customer reviews, review integration |
| **Nhat Anh Nguyen** | Filtering and search, customer profile, customer authentication UI, localization UI, favorites feature, review integration, image compression, smooth animations, error handling with retry dialogs, performance optimization |
| **Rohan Mehta** | UI design, final review, presenting |

---

### APK File: 
https://drive.google.com/file/d/1GHE4sdEyJbap8_SnTVIY1ZLuwNL2vEVY/view?usp=sharing 

### Backlog: 
https://docs.google.com/spreadsheets/d/1pm37TSBmQL9C6HI7-OMaLhnKDPHRMzfbEjpT8pUAz8k/edit?gid=0#gid=0

---

## Project Description
**ServicesFinder** is a mobile app connecting local service providers with customers seeking services like Hair Care, Nail Services, Automotive Repair, Education, Pet Care, and more.

### Customer Features
- **Authentication:** Sign in/up with email or phone number
- **Browse & Search:** Find services by category or keyword with real-time search
- **Sorting:** Sort by Most Recent, Price (Low/High), Rating, or Popularity
- **Language Support:** Switch between English, Spanish, Vietnamese, and Chinese
- **Favorites:** Save preferred providers with heart button
- **Reviews:** Read and write reviews (requires login)
- **Profile:** View favorites and manage account with logout

### Provider Features
- **Registration:** Sign up with phone or email
- **Service Management:** Add, edit, and delete multiple services
- **Profile Management:** Edit personal information
- **Image Upload:** Add service photos with auto-compression (1920x1080 @ 85% quality)
- **Account Control:** Delete account option  

---

## Dual-Role Architecture

ServicesFinder supports two distinct user roles with separate data management:

- **Providers:** Create and manage service listings, manage profile
- **Customers:** Browse services, write reviews, save favorites, manage preferences

**Key Design:**
- Separate Firestore collections (`providers/` and `customers/`)
- Independent authentication and session management
- Role-based UI and permissions
- Users can have both roles using different accounts

---

## Technical Requirements

- Android Studio
- Java
- Firebase Authentication
- Firebase Firestore
- Firebase Storage
- MinSDK: 24+

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





