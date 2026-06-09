# Current Transition State

This note records the current runnable transition state for the Spring Cloud migration.

- Page paths, static resources, and Session login remain hosted by `web-service`.
- The project now tracks UTF-8 editor defaults through `.editorconfig`; service `application.properties` comments are kept as readable UTF-8 text.
- `course-service` now owns the course, college, department, and major REST APIs.
- College list pagination now uses repository-backed paging and sort normalization in `course-service`, mirrored in the `web-service` compatibility fallback.
- Course list pagination now normalizes page and sort parameters, supports course type filtering, and is mirrored in the `web-service` compatibility fallback.
- Department list pagination now applies repository-backed name/code/status filtering and sort normalization in both `course-service` and the `web-service` fallback.
- Major list pagination now applies repository-backed name/code/department/status filtering and sort normalization in both `course-service` and the `web-service` fallback.
- `selection-service` now owns the selection and course-selection REST APIs.
- Course-selection student/course list pagination now normalizes page and sort parameters with a safe sort whitelist in `selection-service`, mirrored in the `web-service` compatibility fallback.
- `web-service` now mirrors selection stats, teacher course-student lists, teacher dashboard aggregation, and grade update compatibility endpoints for both `/api/v1/selections/**` and `/api/v1/course-selections/**`.
- `user-service` now owns the user, role, and permission REST APIs.
- `user-service` user list now aggregates student, teacher, and admin accounts; register, login, password reset, and password change endpoints have concrete student/teacher/admin implementations.
- User batch deletion now validates the whole request, deduplicates IDs, and deletes student, teacher, and admin accounts by their owning domain in both `user-service` and the `web-service` compatibility fallback.
- `web-service` no longer keeps duplicate `Result` wrappers; controller and handler responses now resolve to the shared `common-lib` `Result` type.
- `web-service` keeps `exception.GlobalExceptionHandler` as its single global exception handler so business exception codes, access-denied responses, and not-found responses are handled by one advice.
- Role list filtering and permission status updates are concrete in `user-service` and mirrored in the `web-service` compatibility fallback.
- `student-service` now owns the student REST APIs under `/api/v1/students/**`.
- `teacher-service` now owns the teacher REST APIs under `/api/v1/teachers/**`.
- Student list pagination now normalizes page and sort parameters, supports name/student number/college/department/major/class/status filtering, and is mirrored in the `web-service` compatibility fallback.
- Teacher list pagination now normalizes page and sort parameters, supports name/teacher number/department/title/gender/status filtering, and is mirrored in the `web-service` compatibility fallback.
- Student and teacher page paths remain routed to `web-service` for compatibility.
- Gateway routes migrated API domains to their target services first, then keeps the legacy `web-service` route as a compatibility fallback for remaining pages and APIs.
