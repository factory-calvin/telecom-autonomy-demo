# Factory Droid Demo Script

## Audience: Telecom Engineering Teams

## Feature: Customer Churn Risk Score

---

## Pre-Demo Setup

**Before the presentation:**

1. Ensure clean git state: `git status` shows no uncommitted changes
2. Start the development environment: `pnpm dev`
3. Open browser to http://localhost:3000/dashboard
4. Open VS Code/IDE with Factory Droid ready
5. Have terminal visible for showing command execution

---

## Act 1: Setting the Scene (2 minutes)

### Opening

> "Today I want to show you how Factory Droid can accelerate feature development in a real telecom application. This is FactoryFone Admin Portal - a customer administration system built with Next.js and Spring Boot."

**[Show the dashboard]**

> "We have the standard telecom admin features: customer management, plan administration, device inventory, usage tracking, and support tickets. But our product team just came to us with a new requirement..."

### The Feature Request

> "They want a **Customer Churn Risk Score** displayed on the dashboard. This needs to:
>
> - Calculate a risk score based on customer behavior patterns
> - Show high-risk customers prominently on the dashboard
> - Be calculated from support ticket frequency, usage trends, and account age
> - Include both a summary KPI card and a detailed chart"

> "In a traditional workflow, this would involve:
>
> - Backend team adding the API endpoint
> - Frontend team building the UI components
> - Writing tests for both
> - Code review cycles between teams
>
> Let's see how Factory Droid handles this end-to-end."

---

## Act 2: The Prompt (1 minute)

### Single Prompt Approach

**[Type or paste this prompt into Factory Droid]**

```
Add a Customer Churn Risk Score feature to the dashboard:

1. Backend (Spring Boot):
   - Add a new endpoint GET /api/dashboard/churn-risk that calculates risk scores
   - Risk score (0-100) based on:
     - Open support tickets in last 30 days (+20 per ticket, max 60)
     - Days since last activity (+1 per 7 days inactive, max 20)
     - Account age bonus (-10 if customer > 1 year)
   - Return top 10 highest-risk customers with their scores

2. Frontend (Next.js):
   - Add a new KPI card showing "At-Risk Customers" count (score > 50)
   - Add a bar chart showing top 5 at-risk customers by name and score
   - Use the existing Telekom design system colors (use destructive/warning for high risk)
   - Follow existing patterns in use-dashboard-stats.ts and dashboard/page.tsx

3. Tests:
   - Add a backend unit test for the churn risk calculation
   - Add a frontend test for the new dashboard components
```

> "Notice I'm giving context about our tech stack and pointing to existing patterns. Factory Droid will analyze the codebase and follow our conventions."

---

## Act 3: Watch It Work (5-7 minutes)

### Narrate the Process

**[As Factory Droid begins working, narrate what's happening]**

#### Phase 1: Codebase Analysis

> "First, it's exploring our codebase - looking at the existing dashboard controller, the frontend hooks, and our component patterns. It's learning how we do things here."

**[Point out the file exploration in the output]**

#### Phase 2: Backend Implementation

> "Now it's creating the backend endpoint. Watch how it:
>
> - Follows our existing controller patterns
> - Uses the same repository injection style
> - Adds proper logging like our other endpoints
> - Creates a clean DTO for the response"

**[Show the Java code being generated]**

> "Notice it's using our existing Customer and SupportTicket repositories rather than creating new ones."

#### Phase 3: Frontend Implementation

> "Moving to the frontend now. It's:
>
> - Adding the new data types to our hooks
> - Extending the dashboard stats hook with the new fetch
> - Creating chart configurations that match our existing style"

**[Show the TypeScript/React code]**

> "See how it's using our Telekom color palette - the destructive red for high-risk indicators, following our design system."

#### Phase 4: Dashboard UI

> "Now the dashboard page updates:
>
> - New KPI card using our existing Card component pattern
> - Bar chart following the same ChartContainer setup as Revenue by Plan
> - Consistent typography with text-headline-3 for the chart title"

#### Phase 5: Tests

> "Finally, the tests. It's creating:
>
> - A JUnit test for the risk calculation logic
> - A React Testing Library test for the new UI components"

---

## Act 4: Verification (2 minutes)

### Run the Tests

> "Let's verify everything works."

**[Run the commands]**

```bash
# Backend tests
pnpm test:backend

# Frontend tests
pnpm test:frontend

# Lint check
pnpm lint
```

> "All tests passing, no lint errors. The code follows our conventions."

### Live Demo

**[Refresh the dashboard in browser]**

> "And here's our new feature live:
>
> - The 'At-Risk Customers' KPI card showing the count
> - The churn risk chart with our top at-risk customers
> - Telekom magenta and red highlighting the severity"

**[Hover over chart elements to show tooltips]**

---

## Act 5: The Diff (1 minute)

### Review Changes

```bash
git diff --stat
```

> "Let's look at what was created:
>
> - Backend: New endpoint in DashboardController, plus test
> - Frontend: Extended hook, updated dashboard page, new test
> - All following existing patterns - this code looks like our team wrote it"

**[Show a specific file diff if time permits]**

---

## Act 6: Key Takeaways (2 minutes)

### What We Just Saw

> "In about 10 minutes, Factory Droid delivered a complete feature that would typically take:
>
> - A backend developer: 2-4 hours
> - A frontend developer: 2-4 hours
> - Integration and testing: 1-2 hours
> - Code review iterations: Variable
>
> That's potentially a full day of work across multiple engineers."

### Why This Matters for Telecom

> "For telecom specifically, this is powerful because:
>
> 1. **Domain complexity**: Droid understood telecom concepts - churn risk, usage patterns, customer lifecycle
> 2. **Full-stack coherence**: Backend and frontend work together seamlessly, no integration gaps
> 3. **Pattern consistency**: It learned from our existing code and replicated our style
> 4. **Compliance-ready**: Generated code follows our established patterns, making security and code review easier"

### The Human Role

> "Engineers aren't replaced - we're elevated:
>
> - We define the requirements and business logic
> - We review and refine the generated code
> - We make architectural decisions
> - We handle edge cases and production concerns
>
> Factory Droid handles the implementation grunt work, letting us focus on what matters."

---

## Closing

> "This was a single feature. Imagine applying this across your backlog - Sprint velocity increases dramatically while code quality stays consistent."

> "Questions?"

---

## Appendix: Backup Prompts

If the main demo encounters issues, here are simpler alternatives:

### Alternative 1: Dashboard Enhancement Only

```
Add a new "Average Revenue Per User (ARPU)" KPI card to the dashboard.
Calculate it as total monthly revenue divided by active customers.
Display it with the DollarSign icon, following the existing card pattern.
```

### Alternative 2: Backend Only

```
Add a new endpoint GET /api/customers/by-status that returns
customer counts grouped by status (ACTIVE, SUSPENDED, CANCELLED).
Follow the existing pattern in DashboardController.
```

### Alternative 3: Frontend Only

```
Add a search/filter input to the customers table that filters
by customer name or email. Use the existing Input component
from shadcn/ui.
```

---

## Technical Notes

### Files Likely Modified

**Backend:**

- `backend/src/main/java/com/example/demo/controller/DashboardController.java`
- `backend/src/test/java/com/example/demo/controller/DashboardControllerTest.java` (new)

**Frontend:**

- `hooks/use-dashboard-stats.ts`
- `app/dashboard/page.tsx`
- `__tests__/components/dashboard.test.tsx` (new)

### Expected Risk Score Calculation

```java
// Pseudocode for risk calculation
int riskScore = 0;

// Support tickets factor (max 60 points)
int recentTickets = countTicketsInLast30Days(customer);
riskScore += Math.min(recentTickets * 20, 60);

// Inactivity factor (max 20 points)
int daysSinceActivity = calculateDaysSinceLastActivity(customer);
riskScore += Math.min(daysSinceActivity / 7, 20);

// Loyalty bonus (subtract 10 if > 1 year)
if (customerAge > 365) {
    riskScore = Math.max(0, riskScore - 10);
}

return Math.min(riskScore, 100);
```

### Design System Colors Reference

| Risk Level     | Color Token     | Hex Value |
| -------------- | --------------- | --------- |
| High (>70)     | `--destructive` | `#e82010` |
| Medium (50-70) | `--warning`     | `#f97012` |
| Low (<50)      | `--success`     | `#00b367` |

---

## Troubleshooting

### If Backend Fails to Start

```bash
pnpm reset:db  # Reset database
pnpm dev:backend  # Restart backend
```

### If Frontend Has Type Errors

```bash
pnpm lint  # Check for issues
# Factory Droid can fix these with a follow-up prompt
```

### If Tests Fail

- Check that the database has seeded data
- Ensure both frontend and backend are running
- Ask Factory Droid to debug: "The churn risk test is failing, can you investigate?"
