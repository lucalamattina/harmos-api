-- Limpiar tablas existentes (en orden inverso a las dependencias)
DELETE FROM user_specialty;
DELETE FROM patient_doctor;
DELETE FROM announcement_specialty;
DELETE FROM announcement;
DELETE FROM schedule;
DELETE FROM patients;
DELETE FROM users_roles;
DELETE FROM users;
DELETE FROM specialties;
DELETE FROM roles;

-- Insertar roles
INSERT INTO roles (id, role) VALUES
(1, 'DOCTOR'),
(2, 'ADMINISTRATOR');

-- Insertar especialidades
INSERT INTO specialties (id, name) VALUES
(1, 'TO'),
(2, 'FONO'),
(3, 'KINE'),
(4, 'PSICO'),
(5, 'FISIO');

-- Insertar usuarios (doctores)
INSERT INTO users (id, email, password, first_name, last_name, phone) VALUES
(1, 'doctor1@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Juan', 'Pérez', '1234567890'),
(2, 'doctor2@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'María', 'González', '0987654321'),
(3, 'doctor3@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Carlos', 'Rodríguez', '5555555555'),
(4, 'doctor4@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Laura', 'Martínez', '1112223333'),
(5, 'doctor5@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Pedro', 'Sánchez', '4445556666'),
(6, 'doctor6@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Ana', 'López', '7778889999'),
(7, 'doctor7@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Miguel', 'Torres', '1231231234'),
(8, 'doctor8@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Sofía', 'Ramírez', '4564564567'),
(9, 'doctor9@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Diego', 'Gómez', '7897897890'),
(10, 'doctor10@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Valentina', 'Herrera', '3213213210'),
(11, 'doctor11@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Matías', 'Soto', '6546546543'),
(12, 'doctor12@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Camila', 'Cortés', '9879879876'),
(13, 'doctor13@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Lucas', 'Fuentes', '1234567890'),
(14, 'doctor14@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Valeria', 'Valenzuela', '0987654321'),
(15, 'doctor15@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Facundo', 'Araya', '5555555555'),
(16, 'doctor16@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Agustina', 'Morales', '1112223333'),
(17, 'doctor17@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Santiago', 'Navarro', '4445556666'),
(18, 'doctor18@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Martina', 'Silva', '7778889999'),
(19, 'doctor19@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Facundo', 'Díaz', '1231231234'),
(20, 'doctor20@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Sofía', 'Pérez', '4564564567'),
(21, 'doctor21@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Juan', 'González', '7897897890'),
(22, 'doctor22@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'María', 'Rodríguez', '3213213210'),
(23, 'doctor23@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Carlos', 'Martínez', '6546546543'),
(24, 'doctor24@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Laura', 'Sánchez', '9879879876'),
(25, 'doctor25@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Pedro', 'López', '1234567890'),
(26, 'doctor26@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Catalina', 'Soto', '9019019012'),
(27, 'doctor27@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Emiliano', 'Cortés', '0120120123'),
(28, 'doctor28@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Renata', 'Fuentes', '1231231234'),
(29, 'doctor29@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Maximiliano', 'Valenzuela', '2342342345'),
(30, 'doctor30@example.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Antonella', 'Araya', '3453453456'),
(31, 'alejandro.rolandelli@gmail.com', '$2a$10$dAH3Y/lJdVXet4ALyeHy7.i.3QckyKhzD6uZIvchipxex1CXpc2Dm', 'ALE', 'ROLO', '3453453456'),
(32, 'noreplyharmos@gmail.com', '$2a$10$dAH3Y/lJdVXet4ALyeHy7.i.3QckyKhzD6uZIvchipxex1CXpc2Dm', 'SUPER', 'USER', '3453453456');

-- Asignar roles a usuarios
INSERT INTO users_roles (app_user_id, roles_id) VALUES
(1, 1), (2, 1), (3, 1), (4, 1), (5, 1),
(6, 1), (7, 1), (8, 1), (9, 1), (10, 1),
(11, 1), (12, 1), (13, 1), (14, 1), (15, 1),
(16, 1), (17, 1), (18, 1), (19, 1), (20, 1),
(21, 1), (22, 1), (23, 1), (24, 1), (25, 1),
(26, 1), (27, 1), (28, 1), (29, 1), (30, 1),
(31, 1);

-- Asignar especialidades a usuarios (distribuidas aleatoriamente)
INSERT INTO user_specialty (user_id, specialty_id) VALUES
(1, 1), (1, 2), (2, 3), (2, 4), (3, 5),
(4, 1), (4, 3), (5, 2), (5, 4), (6, 5),
(7, 1), (7, 5), (8, 2), (8, 3), (9, 4),
(10, 5), (10, 1), (11, 2), (11, 4), (12, 3),
(13, 5), (13, 2), (14, 1), (14, 3), (15, 4),
(16, 5), (16, 1), (17, 2), (17, 3), (18, 4),
(19, 5), (19, 2), (20, 1), (20, 4), (21, 3),
(22, 5), (22, 1), (23, 2), (23, 3), (24, 4),
(25, 5), (25, 2), (26, 1), (26, 4), (27, 3),
(28, 5), (28, 1), (29, 2), (29, 3), (30, 4),
(31, 1), (31, 2), (31, 3), (31, 4), (31, 5);

-- Insertar pacientes (50 pacientes)
INSERT INTO patients (id, name, phone, status) VALUES
(1, 'Ana Martínez', '1111111111', 'ACTIVE'),
(2, 'Pedro López', '2222222222', 'ACTIVE'),
(3, 'Laura Sánchez', '3333333333', 'ACTIVE'),
(4, 'Miguel Torres', '4444444444', 'ACTIVE'),
(5, 'Sofía Ramírez', '5555555555', 'ACTIVE'),
(6, 'Diego Gómez', '6666666666', 'ACTIVE'),
(7, 'Valentina Herrera', '7777777777', 'ACTIVE'),
(8, 'Matías Soto', '8888888888', 'ACTIVE'),
(9, 'Camila Cortés', '9999999999', 'ACTIVE'),
(10, 'Lucas Fuentes', '1010101010', 'ACTIVE'),
(11, 'Valeria Valenzuela', '1111111112', 'ACTIVE'),
(12, 'Facundo Araya', '1212121212', 'ACTIVE'),
(13, 'Agustina Morales', '1313131313', 'ACTIVE'),
(14, 'Santiago Navarro', '1414141414', 'ACTIVE'),
(15, 'Martina Silva', '1515151515', 'ACTIVE'),
(16, 'Facundo Díaz', '1616161616', 'ACTIVE'),
(17, 'Sofía Pérez', '1717171717', 'ACTIVE'),
(18, 'Juan González', '1818181818', 'ACTIVE'),
(19, 'María Rodríguez', '1919191919', 'ACTIVE'),
(20, 'Carlos Martínez', '2020202020', 'ACTIVE'),
(21, 'Laura Sánchez', '2121212121', 'ACTIVE'),
(22, 'Pedro López', '2222222223', 'ACTIVE'),
(23, 'Catalina Soto', '2323232323', 'ACTIVE'),
(24, 'Emiliano Cortés', '2424242424', 'ACTIVE'),
(25, 'Renata Fuentes', '2525252525', 'ACTIVE'),
(26, 'Maximiliano Valenzuela', '2626262626', 'ACTIVE'),
(27, 'Antonella Araya', '2727272727', 'ACTIVE'),
(28, 'Santiago Morales', '2828282828', 'ACTIVE'),
(29, 'Martina Navarro', '2929292929', 'ACTIVE'),
(30, 'Facundo Silva', '3030303030', 'ACTIVE'),
(31, 'Sofía Díaz', '3131313131', 'ACTIVE'),
(32, 'Juan Pérez', '3232323232', 'ACTIVE'),
(33, 'María González', '3333333334', 'ACTIVE'),
(34, 'Carlos Rodríguez', '3434343434', 'ACTIVE'),
(35, 'Laura Martínez', '3535353535', 'ACTIVE'),
(36, 'Pedro Sánchez', '3636363636', 'ACTIVE'),
(37, 'Catalina López', '3737373737', 'ACTIVE'),
(38, 'Emiliano Soto', '3838383838', 'ACTIVE'),
(39, 'Renata Cortés', '3939393939', 'ACTIVE'),
(40, 'Maximiliano Fuentes', '4040404040', 'ACTIVE'),
(41, 'Antonella Valenzuela', '4141414141', 'ACTIVE'),
(42, 'Joaquín Soto', '4242424242', 'ACTIVE'),
(43, 'Catalina Cortés', '4343434343', 'ACTIVE'),
(44, 'Emiliano Fuentes', '4444444445', 'ACTIVE'),
(45, 'Renata Valenzuela', '4545454545', 'ACTIVE'),
(46, 'Maximiliano Araya', '4646464646', 'ACTIVE'),
(47, 'Antonella Morales', '4747474747', 'ACTIVE'),
(48, 'Santiago Navarro', '4848484848', 'ACTIVE'),
(49, 'Martina Silva', '4949494949', 'ACTIVE'),
(50, 'Facundo Díaz', '5050505050', 'ACTIVE');

-- Asignar doctores a pacientes (cada paciente tiene 1-3 doctores)
INSERT INTO patient_doctor (patient_id, doctor_id) VALUES
(1, 1), -- Ana Martínez - Juan Pérez
(1, 2), -- Ana Martínez - María González
(2, 1), -- Pedro López - Juan Pérez
(3, 2), -- Laura Sánchez - María González
(4, 3), -- Miguel Torres - Carlos Rodríguez
(5, 3); -- Sofía Ramírez - Carlos Rodríguez

-- Insertar horarios
INSERT INTO schedule (id, location, day_of_week, hour_from, minute_from, hour_to, minute_to, doctor_id) VALUES
(1, 'Consultorio 1', 1, 9, 0, 17, 0, 1), -- Lunes
(2, 'Consultorio 2', 2, 9, 0, 17, 0, 1), -- Martes
(3, 'Consultorio 3', 3, 9, 0, 17, 0, 2), -- Miércoles
(4, 'Consultorio 4', 4, 9, 0, 17, 0, 2), -- Jueves
(5, 'Consultorio 5', 5, 9, 0, 17, 0, 3); -- Viernes

-- Insertar anuncios (25 anuncios)
INSERT INTO announcement (id, title, content, date, created_by_id) VALUES
(1, 'Nuevo Equipamiento', 'Se ha adquirido nuevo equipamiento para terapia ocupacional', CURRENT_TIMESTAMP, 1),
(2, 'Cambio de Horarios', 'A partir de la próxima semana, los horarios de atención serán de 9:00 a 17:00', CURRENT_TIMESTAMP, 2),
(3, 'Capacitación', 'Próxima capacitación en nuevas técnicas de rehabilitación', CURRENT_TIMESTAMP, 3),
(4, 'Mantenimiento', 'El consultorio estará cerrado por mantenimiento el próximo fin de semana', CURRENT_TIMESTAMP, 4),
(5, 'Nuevo Servicio', 'Se ha implementado un nuevo servicio de telemedicina', CURRENT_TIMESTAMP, 5);

-- Asignar especialidades a anuncios
INSERT INTO announcement_specialty (announcement_id, specialty_id) VALUES
(1, 1), -- Nuevo Equipamiento - TO
(2, 3), -- Cambio de Horarios - KINE
(2, 4), -- Cambio de Horarios - PSICO
(3, 5), -- Capacitación - FISIO
(4, 1), -- Mantenimiento - TO
(4, 2), -- Mantenimiento - FONO
(5, 3), -- Nuevo Servicio - KINE
(6, 4), -- Actualización de Protocolos - PSICO
(7, 5), -- Feriado - FISIO
(8, 1), -- Nuevo Personal - TO
(9, 2), -- Mejoras en Instalaciones - FONO
(10, 3), -- Actualización de Software - KINE
(11, 4), -- Capacitación Interna - PSICO
(12, 5), -- Mantenimiento Preventivo - FISIO
(13, 1), -- Nuevo Protocolo COVID - TO
(14, 2), -- Cambio de Ubicación - FONO
(15, 3), -- Actualización de Tarifas - KINE
(16, 3), -- Nuevo Equipamiento Kinesiología - KINE
(17, 2), -- Capacitación en Fonoaudiología - FONO
(18, 4), -- Actualización de Psicología - PSICO
(19, 5), -- Mejoras en Fisioterapia - FISIO
(20, 1), -- Nuevo Servicio de TO - TO
(21, 3), -- Actualización de Equipos - KINE
(22, 2), -- Nuevo Personal Especializado - FONO
(23, 4), -- Mejoras en Atención - PSICO
(24, 5), -- Actualización de Procedimientos - FISIO
(25, 1); -- Nuevo Servicio de Emergencias - TO 

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('specialties_id_seq', (SELECT MAX(id) FROM specialties));
SELECT setval('roles_id_seq', (SELECT MAX(id) FROM roles));
SELECT setval('patients_id_seq', (SELECT MAX(id) FROM patients));
SELECT setval('schedule_id_seq', (SELECT MAX(id) FROM schedule));
SELECT setval('announcement_id_seq', (SELECT MAX(id) FROM announcement));