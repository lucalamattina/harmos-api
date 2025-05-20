package ar.edu.itba.harmos.models

enum class PatientStatus {
    ACTIVE,         // Paciente activo y en tratamiento
    INACTIVE,       // Paciente inactivo temporalmente
    DISCHARGED,     // Paciente dado de alta
    PENDING,        // Paciente en lista de espera
    REFERRED       // Paciente derivado a otro profesional
} 