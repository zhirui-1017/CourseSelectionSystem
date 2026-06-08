# Current Transition State

This note records the current runnable transition state for the Spring Cloud migration.

- Page paths, static resources, and Session login remain hosted by `web-service`.
- `course-service` now owns the course, college, department, and major REST APIs.
- `selection-service` now owns the selection and course-selection REST APIs.
- `user-service` now owns the user, role, and permission REST APIs.
- `user-service` user list now aggregates student, teacher, and admin accounts; register, login, password reset, and password change endpoints have concrete student/teacher/admin implementations.
- `student-service` now owns the student REST APIs under `/api/v1/students/**`.
- `teacher-service` now owns the teacher REST APIs under `/api/v1/teachers/**`.
- Student and teacher page paths remain routed to `web-service` for compatibility.
- Gateway routes migrated API domains to their target services first, then keeps the legacy `web-service` route as a compatibility fallback for remaining pages and APIs.
