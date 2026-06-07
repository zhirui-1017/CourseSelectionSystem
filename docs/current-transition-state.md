# Current Transition State

This note records the current runnable transition state for the Spring Cloud migration.

- The business modules (`user-service`, `student-service`, `teacher-service`, `course-service`, `selection-service`) are still startup skeletons.
- Existing controllers and business logic are still hosted by `web-service`.
- During this transition stage, Gateway routes page paths, static resources, and `/api/v1/**` to `web-service`.
- When a business domain is migrated into its target service, move that route from the legacy `web-service` route back to the target service route and verify it through Gateway.
