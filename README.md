# Property Rental System

The Property Rental System is a full‑stack web application designed to simplify the rental process for both property owners and potential tenants. The system features a modern React‑based frontend with a Java servlet backend and PostgreSQL for data storage. It utilizes JWT authentication for secure access and offers role‑based functionality.

## Table of Contents

- [Overview](#overview)
- [Front-End Access](#front-end-access)
- [Key Features](#key-features)
- [Demo User Details](#demo-user-details)
- [Repository Structure](#repository-structure)
- [Installation & Deployment](#installation--deployment)
- [Contributing](#contributing)
- [License](#license)

## Overview

Property Rental System provides:
- A responsive and modern user interface (built in React) to search, view, and book property listings.
- Secure user authentication using JSON Web Tokens (JWT).
- Separate dashboards:
  - **Property Owners:** Manage listings, approve or decline booking requests.
  - **Potential Renters:** Browse listings, view property details, and submit/cancel booking requests.
- An intuitive image gallery for property listings (with support for multiple images).

## Front-End Access

The deployed front-end of the project is accessible at:

**[https://online-prs-frontend.vercel.app/](https://online-prs-frontend.vercel.app/)**

> Replace the above URL with your actual frontend URL.  
> Visitors can use this link to view all public features, browse listings, and check out detailed property information.

## Key Features

- **User Authentication:**  
  Secure registration and login with JWT, enforcing role-based access control.
- **Owner Dashboard:**  
  Easily create, edit, and delete listings and manage booking requests.
- **Tenant Dashboard:**  
  Search, filter, and sort listings. View listing details, including image galleries, and submit or cancel booking requests.
- **Responsive Design:**  
  Optimized for viewing on desktops, tablets, and mobile devices.
- **Modern UI:**  
  Clean, modern, and user-friendly interface with interactive components like image sliders and real-time notifications.

## Demo User Details

To experience the project as both a property owner and a potential renter, you can use the following demo credentials (or similar sample users provided in the repository):

- **Potential Renter Demo:**
  - **Email:** jerryjames@gmail.com
  - **Password:** mypassword

- **Property Owner Demo:**
  - **Email:** ewomazinoek@gmail.com
  - **Password:** mypassword

> **Note:** These demo credentials are provided for testing and exploration purposes only. In a production environment, always use secure and unique credentials for every user.

## Repository Structure

PropertyRentalSystem/
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/propertyrental/   # Contains Servlets, DBConnection, etc.
│   │       └── resources/
│   └── pom.xml
├── frontend/
│   ├── public/
│   └── src/
│       ├── components/                   # React components (Dashboards, ListingDetails, etc.)
│       ├── pages/                        # Public pages (LandingPage, Login, Register, etc.)
│       └── css/                          # CSS styling files
└── README.md

- **backend:** Contains your Java servlet application including configurations and database connection code.
- **frontend:** Contains the React application that interacts with the backend API.
- **README.md:** This file with project overview, installation, and usage instructions.

