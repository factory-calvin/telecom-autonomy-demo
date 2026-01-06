# Create Demo Script

Create a demo script for demonstrating Factory CLI capabilities with this application.

## Requirements

1. **Create DEMO_SCRIPT.md** in the project root with:
   - Prerequisites and setup instructions (including `droid` CLI command)
   - A compelling story/narrative for the demo (2-3 sentences explaining the scenario)
   - Step-by-step walkthrough broken into timed sections:
     - Part 1: Setting the Scene (2 min) - show current state, identify the gap
     - Part 2: Using Factory CLI (5-7 min) - the main demo with a realistic prompt
     - Part 3: Iteration (optional, 3-5 min) - show refinement capability
     - Part 4: Wrap Up (1-2 min) - summarize and call to action
   - Reset instructions using git checkout/clean
   - Troubleshooting section
   - Alternative demo prompts for variety
   - Duration table

2. **Create /demo_script page** in the frontend app that:
   - Renders the script in an intuitive, interactive UI
   - Uses collapsible sections for each part
   - Includes copy buttons on all code blocks
   - Shows timing indicators for each section
   - Has a "Back to Dashboard" link
   - Uses a full-screen overlay layout (no app shell)
   - Styled with Tailwind using zinc color palette

3. **Add a hidden link** to the demo script page from the Settings page (subtle, bottom-right corner)

4. **Update README.md** with a callout linking to DEMO_SCRIPT.md

## Factory CLI Usage

The Factory CLI is invoked using the `droid` command:

```bash
# Launch Factory CLI
droid

# Check version
droid --version
```

## Demo Prompt Guidelines

The Factory CLI prompt in the demo should:

- Request a feature that does NOT already exist in the app
- Be achievable in 5-7 minutes
- Touch multiple files (shows Factory's codebase understanding)
- Use existing components/patterns (shows Factory follows conventions)
- Have a visible UI result (easy to demo)
- Be simple enough to explain to non-technical audiences

## Important

Before writing the demo prompt, explore the codebase to understand what features already exist so you don't duplicate them.
