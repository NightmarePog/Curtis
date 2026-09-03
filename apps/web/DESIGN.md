# Curtis design guide

Curtis is a focused school workspace, not a marketing site or a game.

## Look and feel

- Graphite dark theme with a complete light theme.
- School blue for primary actions.
- Red only for errors and destructive actions.
- Barlow for normal text; IBM Plex Mono for timers and scores.
- Solid panels, thin borders, small corners, and very little shadow.
- No gradients, glass effects, decorative animation, confetti, or gamification.

## Product rules

- Give each page one clear heading and one obvious next action.
- Students see active quizzes first and get a distraction-free attempt view.
- Teachers see their quiz library first; authoring and starting are contextual.
- Administrators see management tools.
- Use real panels for real content, not decoration.

## Interaction rules

- Interactive targets are at least 44×44px.
- Keyboard focus is always visible.
- Use native controls where possible.
- Never rely on colour alone for answer state.
- Show loading and error feedback near the affected content.
- Respect `prefers-reduced-motion`.

## Copy and review

- Student text is friendly Czech without childish gamification.
- Teacher and administrator text is direct and professional.
- Errors say what happened and what the user can do next.
- Check 375px, 768px, 1024px, and 1440px in both themes, at 200% zoom, and
  with keyboard-only navigation.
