# SupplyChain OS: Frontend

Next.js App Router, TypeScript, Tailwind CSS, Zustand, Framer Motion, PixiJS, Recharts. The design is in [`docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md).

```bash
npm install
cp .env.example .env.local   # optional: only needed if the backend isn't on port 8081
npm run dev                  # http://localhost:3000
```

| Command             | Purpose                                  |
| ------------------- | ---------------------------------------- |
| `npm run lint`      | ESLint                                   |
| `npm run typecheck` | Strict TypeScript check                  |
| `npm run format`    | Prettier (`format:check` to verify only) |
| `npm test`          | Vitest                                   |
| `npm run build`     | Production build                         |
