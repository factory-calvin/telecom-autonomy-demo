# Checklists

## Pre-Flight Checklist

Before starting implementation:

- [ ] Understand the data requirements (which entities, what aggregations)
- [ ] Check if similar endpoints exist to follow as patterns
- [ ] Identify which repository methods already exist
- [ ] Determine the response structure needed by frontend

## Backend Checklist

- [ ] Repository methods added (if custom queries needed)
- [ ] Controller constructor updated (if new repository dependency)
- [ ] Response DTO records defined
- [ ] Endpoint method implemented with proper logging
- [ ] Field names use snake_case for JSON response

## Frontend Checklist

- [ ] TypeScript interfaces match API response
- [ ] Hook state and fetch logic added
- [ ] Hook return object includes new data
- [ ] UI components consume the new data
- [ ] Loading and error states handled

## Testing Checklist

- [ ] Backend unit test covers business logic
- [ ] Backend test uses mocks (not full Spring context)
- [ ] Frontend test mocks the hook
- [ ] Frontend test verifies UI rendering
- [ ] All edge cases covered (empty data, errors)

## Verification Checklist

- [ ] `pnpm test:backend` passes
- [ ] `pnpm lint` passes
- [ ] `pnpm test:frontend` passes
- [ ] Manual testing confirms API works
- [ ] Manual testing confirms UI displays correctly
