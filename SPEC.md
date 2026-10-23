# SPEC.md

## Project Title
F1 Championship Management System

---

## Overview

A JavaFX desktop application that manages Formula 1 championship data, including drivers, constructors, races, and results.

The system integrates external API data, stores it in a MySQL database, and allows full interaction through CRUD operations and statistical analysis.

---

## Functional Requirements

### 1. Driver Management
- View all drivers
- Add new drivers
- Edit driver data
- Delete drivers
- Assign driver to constructor (by season)
- Display driver statistics:
  - Total points
  - Wins
  - Podiums

---

### 2. Constructor Management
- Register constructors
- Edit constructor data
- Delete constructors
- View associated drivers
- Calculate constructor points

---

### 3. Circuit Management
- Store circuit data
- Display circuits
- Associate races with circuits

---

### 4. Race Management
- Create race entries
- Assign circuit
- Set race date

---

### 5. Results Management (CORE SYSTEM)

- Register race results:
  - Select race
  - Select driver
  - Assign position (1–20)

- Automatically calculate points based on position

Example:
1st → 25 points  
2nd → 18 points  
3rd → 15 points  

---

### 6. Standings System

#### Driver Standings
- Calculate total points
- Sort drivers by points
- Assign ranking position

#### Constructor Standings
- Aggregate points by constructor
- Generate ranking table

---

### 7. API Integration

- Fetch data from F1 API:
  - Drivers
  - Constructors
  - Circuits

- Parse JSON responses
- Store in database

---

### 8. Multimedia Features (Creativity)

- Assign audio file to each driver
- Play audio when driver is selected
- Optional background music

---

### 9. Mini Game (Optional Advanced Feature)

- Simple car racing simulation
- Player selects driver/team
- Track lap time
- Store results

---

## Non-Functional Requirements

- Use JavaFX for UI
- Use MySQL Workbench for database
- Ensure responsiveness and usability
- Maintain clean visual design
- Include proper error handling

---

## Database Design

### Tables

#### drivers
- id_driver (PK)
- given_name
- family_name
- nationality

#### constructors
- id_constructor (PK)
- name
- country

#### circuits
- id_circuit (PK)
- name
- country

#### races
- id_race (PK)
- name
- date
- id_circuit (FK)

#### results
- id_result (PK)
- id_race (FK)
- id_driver (FK)
- id_constructor (FK)
- position
- points

---

## Relationships

- One race → one circuit
- One driver → many results
- One constructor → many drivers
- One result → links race, driver, constructor

---

## User Interface

- Dashboard (main menu)
- Drivers view
- Constructors view
- Races view
- Results input view
- Standings view

Components:
- TableView
- ComboBox
- Buttons
- Forms

---

## Evaluation Criteria Alignment

This system satisfies:

- MySQL integration ✔
- MVC architecture ✔
- Full CRUD ✔
- Relational database ✔
- API usage ✔
- UI design ✔
- Creativity ✔

---

## Expected Outcome

A fully functional desktop system capable of managing F1 data with real-world structure and interactive features.

### API Source (MANDATORY)

The system MUST use the following API:

Base URL:
https://api.jolpi.ca/ergast/f1/

Endpoints:
- drivers → /drivers.json
- constructors → /constructors.json
- circuits → /circuits.json

Requirements:
- Fetch data from the API
- Parse JSON responses
- Extract relevant fields
- Store the data into MySQL tables
- Avoid duplicate entries
- API must ONLY be used for initial data loading