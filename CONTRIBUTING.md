# Contributing to Bowels

## Dev setup

```bash
npm install
npx expo start
```

## Code conventions

- TypeScript strict mode
- Keep inputs tap-only (no typing) unless explicitly requested
- Prefer SQLite-backed queries in `db/queries.ts` for anything persisted
- Keep screens thin; put logic in `utils/` or `db/`

## Lint & typecheck

```bash
npm run typecheck
npm run lint
```

