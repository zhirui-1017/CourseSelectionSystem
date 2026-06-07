# Current Transition State

This note records the current runnable transition state for the Spring Cloud migration.

- Page paths, static resources, and Session login remain hosted by `web-service`.
- `course-service` now owns the course, college, department, and major REST APIs.
- `selection-service` now owns the selection and course-selection REST APIs.
- `user-service` now owns the user, role, and permission REST APIs.
- `student-service` and `teacher-service` are still startup skeletons; their page paths remain routed to `web-service` for compatibility.
- Gateway routes migrated API domains to their target services first, then keeps the legacy `web-service` route as a compatibility fallback for remaining pages and APIs.
